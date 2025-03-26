package modification_detection

import classes.data.Element
import classes.llm.LLM
import classes.service_model.CssSelectorModification
import classes.service_model.Modification
import domain.http.ollama.requests.OllamaGenerateRequest
import domain.modification.requests.ModificationRequest
import domain.modification.requests.ScraperUpdateRequest
import domain.modification.responses.ScraperUpdateResponse
import domain.prompts.GET_MISSING_ELEMENTS_PROMPT
import domain.prompts.GET_MODIFICATION_PROMPT
import domain.prompts.SCRAPER_UPDATE_PROMPT
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import ollama.ILLMClient
import ollama.OllamaClient
import html_fetcher.WebExtractor

/**
 * A service for detecting modifications in HTML and updating scraper scripts.
 *
 * @property llmClient The client for interacting with the LLM.
 */
class ModificationDetectionService(
    private val llmClient: ILLMClient,
) : IModificationDetectionService {

    /**
     * Gets the missing elements between the previous and new HTML states.
     *
     * @param previousHTMLState The previous HTML state.
     * @param newHTMLState The new HTML state.
     * @return A list of missing elements.
     */
    override suspend fun getMissingElements(previousHTMLState: String, newHTMLState: String): List<Element> {
        val webExtractor = WebExtractor()

        val previousElements = webExtractor.getInteractiveElementsHTML(previousHTMLState)
        val newElements = webExtractor.getInteractiveElementsHTML(newHTMLState)

        return previousElements.filterNot { previousElement ->
            newElements.any { newElement ->
                previousElement.cssSelector == newElement.cssSelector
            }
        }
    }

    /**
     * Gets the modification for a modified element.
     *
     * @param modifiedElement The modified element.
     * @param newElements The new elements.
     * @return The modification for the element.
     */
    override suspend fun getModification(modifiedElement: Element, newElements: List<Element>): Modification<Element> {
        val modifiedElementJson = Json.encodeToString(Element.serializer(), modifiedElement)
        val newElementsJson = Json.encodeToString(ListSerializer(Element.serializer()), newElements)
        val modificationRequest = ModificationRequest(modifiedElementJson, newElementsJson)
        val modificationRequestJson = Json.encodeToString(ModificationRequest.serializer(), modificationRequest)

        val alternativeRequest = OllamaGenerateRequest(
            model = LLM.Mistral7B.modelName,
            system = GET_MODIFICATION_PROMPT,
            prompt = modificationRequestJson,
            stream = false,
            raw = false
        )
        val alternativeResponseJson = llmClient.generate(alternativeRequest).response
        val alternativeElement = Json.decodeFromString<Element>(alternativeResponseJson)

        return Modification(modifiedElement, alternativeElement)
    }

    /**
     * Modifies the script based on a single modification.
     *
     * @param oldScript The old script.
     * @param modification The modification to apply.
     * @return The modified script.
     */
    override suspend fun modifyScript(oldScript: String, modification: Modification<String>): String {
        val cssSelectorModification = CssSelectorModification(modification.old, modification.new)
        val scraperUpdateRequest = ScraperUpdateRequest(listOf(cssSelectorModification), oldScript)
        val scraperUpdateRequestJson = Json.encodeToString(ScraperUpdateRequest.serializer(), scraperUpdateRequest)

        return queryLLM(scraperUpdateRequestJson)
    }

    /**
     * Modifies the script based on a list of modifications.
     *
     * @param oldScript The old script.
     * @param modifications The list of modifications to apply.
     * @return The modified script.
     */
    override suspend fun modifyScript(oldScript: String, modifications: List<Modification<Element>>): String {
        val cssSelectorModifications = modifications.map { m -> CssSelectorModification(m.old.cssSelector, m.new.cssSelector) }
        val scraperUpdateRequest = ScraperUpdateRequest(cssSelectorModifications, oldScript)
        val scraperUpdateRequestJson = Json.encodeToString(ScraperUpdateRequest.serializer(), scraperUpdateRequest)

        return queryLLM(scraperUpdateRequestJson)
    }

    /**
     * Queries the LLM with the update request JSON.
     *
     * @param updateRequestJson The update request JSON.
     * @return The updated script.
     */
    private suspend fun queryLLM(updateRequestJson: String): String {
        val ollamaRequest = OllamaGenerateRequest(
            model = LLM.Mistral7B.modelName,
            system = SCRAPER_UPDATE_PROMPT,
            prompt = updateRequestJson,
            stream = false,
            raw = false
        )

        val updateScriptResponseJson = llmClient.generate(ollamaRequest).response
        val json = Json {
            ignoreUnknownKeys = true
        }
        val updateScriptResponse = json.decodeFromString<ScraperUpdateResponse>(updateScriptResponseJson)
        return updateScriptResponse.updatedCode
    }


}


fun main() {
    runBlocking {
        val httpClient = OkHttpClient()
        val llmClient = OllamaClient(httpClient)


        val oldHTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>First HTML</title>
            </head>
            <body>
                <button id="button1">Button 1</button>
                <button id="button2">Button 2</button>
                <a href="#" id="link1">Link 1</a>
                <input type="text" id="input1" />
                <textarea id="textarea1"></textarea>
            </body>
            </html>
        """.trimIndent()

        val newHTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Second HTML</title>
            </head>
            <body>
                <button id="button1">Button 1</button>
                <a href="#" id="link1">Link 1</a>
                <input type="text" id="input1" />
                <textarea id="textarea1"></textarea>
            </body>
            </html>
        """.trimIndent()
        val missingElements = ModificationDetectionService(llmClient).getMissingElements(oldHTML, newHTML)

        print(missingElements)
    }
}