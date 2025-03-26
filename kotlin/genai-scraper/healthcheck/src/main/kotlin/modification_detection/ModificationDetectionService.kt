package modification_detection

import classes.data.Element
import classes.llm.LLM
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

class ModificationDetectionService(
    private val llmClient: ILLMClient,
) : IModificationDetectionService {
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

    override suspend fun getModification(modifiedElement: Element, newElements: List<Element>): Modification {
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

    override suspend fun modifyScript(oldScript: String, modification: Modification): String {
        val scraperUpdateRequest = ScraperUpdateRequest(modification.old.toString(), modification.new.toString(), oldScript)
        val scraperUpdateRequestJson = Json.encodeToString(ScraperUpdateRequest.serializer(), scraperUpdateRequest)

        val ollamaRequest = OllamaGenerateRequest(
            model = LLM.Mistral7B.modelName,
            system = SCRAPER_UPDATE_PROMPT,
            prompt = scraperUpdateRequestJson,
            stream = false,
            raw = false
        )

        val updateScriptResponseJson = llmClient.generate(ollamaRequest).response
        val updateScriptResponse = Json.decodeFromString<ScraperUpdateResponse>(updateScriptResponseJson)

        return updateScriptResponse.updatedScript
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