package modification_detection

import classes.data.Element
import classes.llm.Message
import classes.service_model.CssSelector
import classes.service_model.Modification
import domain.http.ollama.requests.OllamaChatRequest
import domain.http.ollama.requests.OllamaGenerateRequest
import domain.modification.requests.ModificationRequest
import domain.modification.requests.ScraperUpdateRequest
import domain.modification.responses.ScraperUpdateResponse
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ollama.ILLMClient
import html_fetcher.WebExtractor
import kotlinx.serialization.encodeToString

/**
 * A service for detecting modifications in HTML and updating scraper scripts.
 *
 * @property llmClient The client for interacting with the LLM.
 */
class ModificationDetectionService(
    private val llmClient: ILLMClient,
    private val getModificationModel: String,
    private val getModificationSystemPrompt: String,
) : IModificationDetectionService {
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

    override suspend fun getModification(modifiedElement: Element, newElements: List<Element>): Modification<Element> {
        val modifiedElementJson = Json.encodeToString(Element.serializer(), modifiedElement)
        val newElementsJson = Json.encodeToString(ListSerializer(Element.serializer()), newElements)
        val modificationRequest = ModificationRequest(modifiedElementJson, newElementsJson)
        val modificationRequestJson = Json.encodeToString(ModificationRequest.serializer(), modificationRequest)

        val alternativeRequest = OllamaGenerateRequest(
            model = getModificationModel,
            system = getModificationSystemPrompt,
            prompt = modificationRequestJson,
            stream = false,
            raw = false
        )

        val alternativeResponseJson = llmClient.generate(alternativeRequest).response
        val alternativeElement = Json.decodeFromString<Element>(alternativeResponseJson)

        return Modification(modifiedElement, alternativeElement)
    }

    override suspend fun modifyMistralScript(oldScript: String, modification: Modification<Element>, modelName: String, systemPrompt: String): String {
        val cssSelector = CssSelector(modification.old.locator, modification.new.locator)
        val imports = getImports(oldScript)
        val scraperUpdateRequest = ScraperUpdateRequest(imports, oldScript, listOf(cssSelector))

        return modifyScriptUnitary(scraperUpdateRequest, modelName, systemPrompt)
    }

    override suspend fun modifyMistralScript(oldScript: String, modifications: List<Modification<Element>>, modelName: String, systemPrompt: String): String {
        val locators = getLocators(modifications)
        val imports = getImports(oldScript)
        val scraperUpdateRequest = ScraperUpdateRequest(imports, oldScript, locators)

        return modifyScriptUnitary(scraperUpdateRequest, modelName, systemPrompt)
    }

    override suspend fun modifyScriptChatHistory(oldScript: String, modifications: List<Modification<Element>>, modelName: String, systemPrompt: String): String {
        val locators = getLocators(modifications)
        val imports = getImports(oldScript)

        val scraperUpdateRequest = ScraperUpdateRequest(imports, oldScript, locators)

        val messages = listOf(
            Message(role = "system", content = systemPrompt),
            Message(role = "user", content = Json.encodeToString(scraperUpdateRequest))
        )

        val chatRequest = OllamaChatRequest(
            model = modelName,
            stream = false,
            raw = false,
            messages = messages
        )

        val response = llmClient.chat(chatRequest).message.content.cleanUpdateScriptResponseJson()

        return Json.decodeFromString<String>(response).cleanUpdateScriptResponseJson()
    }

    override suspend fun modifyScriptChatHistory(oldScript: String, modifications: List<Modification<Element>>, modelName: String, messages: List<Message>): String {
        val locators = getLocators(modifications)
        val imports = getImports(oldScript)

        val scraperUpdateRequest = ScraperUpdateRequest(imports, oldScript, locators)

        val updatedMessages = messages + Message("user", Json.encodeToString(scraperUpdateRequest))

        val chatRequest = OllamaChatRequest(
            model = modelName,
            stream = false,
            raw = false,
            messages = updatedMessages
        )

        val response = llmClient.chat(chatRequest).message.content.cleanUpdateScriptResponseJson()

        return response
    }

    private suspend fun modifyScriptUnitary(scraperUpdateRequest: ScraperUpdateRequest, modelName: String, systemPrompt: String): String {
        val ollamaRequest = OllamaGenerateRequest(
            model = modelName,
            system = systemPrompt,
            prompt = Json.encodeToString(scraperUpdateRequest),
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

    private fun String.cleanUpdateScriptResponseJson(): String {
         val regex = Regex("""```kotlin\s*(.*?)\s*```""", RegexOption.DOT_MATCHES_ALL)

        val matchResult = regex.find(this)
        val code = matchResult?.groups?.get(1)?.value
        return code ?: ""
    }

    private fun getImports(script: String): String {
        val importsRegex = Regex("package[\\s\\S]*?(?=\\bclass\\b)")
        return importsRegex.find(script)?.value ?: ""
    }

    private fun getLocators(modifications: List<Modification<Element>>): List<CssSelector> {
        return modifications.map { m -> CssSelector(m.old.locator, m.new.locator) }
    }
}