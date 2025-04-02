package modification_detection

import classes.data.Element
import classes.llm.LLM
import classes.service_model.Locator
import classes.service_model.Modification
import domain.http.ollama.requests.OllamaGenerateRequest
import domain.modification.requests.ModificationRequest
import domain.modification.requests.ScraperUpdateRequest
import domain.modification.responses.ScraperUpdateResponse
import domain.prompts.GET_MODIFICATION_PROMPT
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ollama.ILLMClient
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
                previousElement.locator == newElement.locator
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
     * Modifies the script based on a single modification using mistral.
     *
     * @param oldScript The old script.
     * @param modification The modification to apply.
     * @return The modified script.
     */
    override suspend fun modifyMistralScript(oldScript: String, modification: Modification<Element>, modelName: String, prompt: String): String {
        val locator = Locator(modification.old.locator, modification.new.locator)
        val importsRegex = Regex(".*?(?=\\bclass\\b)")
        val imports = importsRegex.find(oldScript)?.value ?: ""
        val scraperUpdateRequest = ScraperUpdateRequest(listOf(locator), oldScript, imports)
        val scraperUpdateRequestJson = Json.encodeToString(ScraperUpdateRequest.serializer(), scraperUpdateRequest)

        return queryLLMJson(scraperUpdateRequestJson, modelName, prompt)
    }

    override suspend fun modifyCodeGenerationLLMScript(
        oldScript: String,
        modifications: List<Modification<Element>>,
        modelName: String,
        systemPrompt: String,
        prompt: String
    ): String {
        val locators = modifications.map { m -> Locator(m.old.locator, m.new.locator) }.toString()
        val importsRegex = Regex("(?s)(.*?)(?=\\bclass\\b)")
        val imports = importsRegex.find(oldScript)?.value ?: ""
        var updatedPrompt = prompt
        updatedPrompt = updatedPrompt.replace("{code}", oldScript)
        updatedPrompt = updatedPrompt.replace("{imports}", imports)
        updatedPrompt = updatedPrompt.replace("{locator_changes}", locators)
        return queryLLMString(systemPrompt, modelName, updatedPrompt)
    }

    /**
     * Modifies the script based on a list of modifications.
     *
     * @param oldScript The old script.
     * @param modifications The list of modifications to apply.
     * @return The modified script.
     */
    override suspend fun modifyMistralScript(oldScript: String, modifications: List<Modification<Element>>, modelName: String, prompt: String): String {
        val locators = modifications.map { m -> Locator(m.old.locator, m.new.locator) }
        val importsRegex = Regex("(?s)(.*?)(?=\\bclass\\b)")
        val imports = importsRegex.find(oldScript)?.value ?: ""
        val scraperUpdateRequest = ScraperUpdateRequest(locators, oldScript, imports)
        val scraperUpdateRequestJson = Json.encodeToString(ScraperUpdateRequest.serializer(), scraperUpdateRequest)

        return queryLLMJson(scraperUpdateRequestJson, modelName, prompt)
    }

    /**
     * Queries the LLM with the update request JSON.
     *
     * @param updateRequestJson The update request JSON.
     * @return The updated script.
     */
    private suspend fun queryLLMJson(updateRequestJson: String, modelName: String, prompt: String): String {
        val ollamaRequest = OllamaGenerateRequest(
            model = modelName,
            system = prompt,
            prompt = updateRequestJson,
            stream = false,
            raw = false
        )

        val updateScriptResponseJson = llmClient.generate(ollamaRequest).response
        val cleanedScriptResponseJson = updateScriptResponseJson.cleanUpdateScriptResponseJson()
        val json = Json {
            ignoreUnknownKeys = true
        }
        val updateScriptResponse = json.decodeFromString<ScraperUpdateResponse>(cleanedScriptResponseJson)
        return updateScriptResponse.updatedCode
    }

    private suspend fun queryLLMString(systemPrompt: String, modelName: String, prompt: String): String {
        val ollamaRequest = OllamaGenerateRequest(
            model = modelName,
            system = systemPrompt,
            prompt = prompt,
            stream = false,
            raw = false
        )

        val updateScriptResponseJson = llmClient.generate(ollamaRequest).response
        return updateScriptResponseJson
    }

    private fun String.cleanUpdateScriptResponseJson(): String {
        // Ensure we keep the 'package' statement and everything after it
        val cleanedScript = this.substringAfter("\npackage ", missingDelimiterValue = this)

        // Restore 'package' keyword if it was the first line
        val restoredScript = if (this.startsWith("package ")) "package $cleanedScript" else cleanedScript

        // Remove formatting artifacts
        return restoredScript
            .replace(Regex("^```\\w*\\s*"), "") // Remove leading ```json, ```kotlin, ```scala, etc.
            .replace(Regex("```"), "") // Remove trailing ```
            .replace(Regex("^'''\\w*\\s*"), "") // Remove leading '''json, '''kotlin, etc.
            .replace(Regex("'''"), "") // Remove trailing '''
            .replace("json", "")
            .replace("json", "")
            .replace("kotlin", "")
            .replace("scala", "")
            .trim() // Trim unnecessary whitespace
    }
}