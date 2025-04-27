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

        return queryLLMJson(scraperUpdateRequest, modelName, systemPrompt)
    }

    override suspend fun modifyCodeGenerationLLMScript(
        oldScript: String,
        modifications: List<Modification<Element>>,
        modelName: String,
        systemPrompt: String,
        prompt: String
    ): String {
        val locators = getLocators(modifications).toString()
        val imports = getImports(oldScript)

        val updatedPrompt = getUpdatedPrompt(prompt, oldScript, imports, locators)

        return queryLLMString(systemPrompt, modelName, updatedPrompt)
    }

    override suspend fun modifyMistralScript(oldScript: String, modifications: List<Modification<Element>>, modelName: String, systemPrompt: String): String {
        val locators = getLocators(modifications)
        val imports = getImports(oldScript)
        val scraperUpdateRequest = ScraperUpdateRequest(imports, oldScript, locators)

        return queryLLMJson(scraperUpdateRequest, modelName, systemPrompt)
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

        return Json.decodeFromString<ScraperUpdateResponse>(response).updatedCode
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

        return Json.decodeFromString<ScraperUpdateResponse>(response).updatedCode
    }

    private suspend fun queryLLMJson(scraperUpdateRequest: ScraperUpdateRequest, modelName: String, systemPrompt: String): String {
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

    private fun getImports(script: String): String {
        val importsRegex = Regex("package[\\s\\S]*?(?=\\bclass\\b)")
        return importsRegex.find(script)?.value ?: ""
    }

    private fun getLocators(modifications: List<Modification<Element>>): List<CssSelector> {
        return modifications.map { m -> CssSelector(m.old.locator, m.new.locator) }
    }

    private fun getUpdatedPrompt(prompt: String, oldScript: String, imports: String, locators: String): String {
        var updatedPrompt = prompt
        updatedPrompt = updatedPrompt.replace("{code}", oldScript)
        updatedPrompt = updatedPrompt.replace("{imports}", imports)
        updatedPrompt = updatedPrompt.replace("{locator_changes}", locators)

        return updatedPrompt
    }
}