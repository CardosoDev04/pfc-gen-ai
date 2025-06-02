package modification_detection

import classes.data.Element
import classes.llm.LLM
import classes.llm.Message
import classes.service_model.CssSelector
import classes.service_model.Modification
import domain.http.ollama.requests.OllamaChatRequest
import domain.http.ollama.requests.OllamaGenerateRequest
import domain.modification.requests.ModificationRequest
import domain.modification.requests.ScraperUpdateRequest
import domain.modification.responses.ScraperUpdateResponse
import domain.prompts.FEW_SHOT_GET_MODIFICATION_PROMPT
import domain.prompts.GET_MISSING_ELEMENTS_MESSAGES
import domain.prompts.GET_MISSING_ELEMENTS_SYSTEM_PROMPT
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ollama.ILLMClient
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import ollama.OllamaClient

/**
 * A service for detecting modifications in HTML and updating scraper scripts.
 *
 * @property llmClient The client for interacting with the LLM.
 */
class ModificationService(
    private val llmClient: ILLMClient,
    private val getModificationModel: String,
    private val elementExtractingModel: String
) : IModificationService {
    override suspend fun getMissingElements(
        previousElements: List<Element>,
        newElements: List<Element>
    ): List<Element> {
        return previousElements.filterNot { previousElement ->
            newElements.any { newElement ->
                previousElement.cssSelector == newElement.cssSelector && previousElement.id == newElement.id && previousElement.label == newElement.label
            }
        }
    }

    override suspend fun getModification(modifiedElement: Element, newElements: List<Element>): Modification<Element> {
        val modifiedElementJson = Json.encodeToString(Element.serializer(), modifiedElement)
        val newElementsJson = Json.encodeToString(ListSerializer(Element.serializer()), newElements)
        val modificationRequest = ModificationRequest(modifiedElementJson, newElementsJson)
        val modificationRequestJson = Json.encodeToString(ModificationRequest.serializer(), modificationRequest)

        val alternativeRequest = OllamaChatRequest(
            model = getModificationModel,
            messages = FEW_SHOT_GET_MODIFICATION_PROMPT + Message(
                role = "user",
                content = modificationRequestJson
            ),
            stream = false,
            raw = false
        )

        val uncleanedAlternativeResponse = llmClient.chat(alternativeRequest).message.content
        val alternativeResponseJson = uncleanedAlternativeResponse.extractAlternative()
        val alternativeElement = Json.decodeFromString<Element>(alternativeResponseJson)

        return Modification(modifiedElement, alternativeElement)
    }

    override suspend fun modifyMistralScript(
        oldScript: String,
        modification: Modification<Element>,
        modelName: String,
        systemPrompt: String
    ): String {
        val cssSelector = CssSelector(
            modification.old.id,
            modification.old.cssSelector,
            modification.new.cssSelector,
            modification.new.id
        )
        val imports = getImports(oldScript)
        val scraperUpdateRequest = ScraperUpdateRequest(imports, oldScript, listOf(cssSelector))

        return modifyScriptUnitary(scraperUpdateRequest, modelName, systemPrompt)
    }

    override suspend fun modifyScriptChatHistory(
        oldScript: String,
        modifications: List<Modification<Element>>,
        modelName: String,
        systemPrompt: String
    ): String {
        val locators = getLocators(modifications)
        val imports = getImports(oldScript)

        val scraperUpdateRequest = ScraperUpdateRequest(imports, oldScript, locators)

        val messages = listOf(
            Message(role = "system", content = systemPrompt),
            Message(role = "user", content = Json.encodeToString(scraperUpdateRequest))
        )

        return getModifiedScript(modelName, messages)
    }

    override suspend fun modifyScriptChatHistory(
        oldScript: String,
        modifications: List<Modification<Element>>,
        modelName: String,
        messages: List<Message>
    ): String {
        val locators = getLocators(modifications)
        val imports = getImports(oldScript)

        val scraperUpdateRequest = ScraperUpdateRequest(imports, oldScript, locators)

        val updatedMessages = messages + Message("user", Json.encodeToString(scraperUpdateRequest))

        return getModifiedScript(modelName, updatedMessages)
    }

    override suspend fun getElementsFromScript(
        scraperCode: String,
        newElements: List<Element>,
        system: String,
        prompt: List<Message>
    ): List<Element> {
        val ollamaChatRequest = OllamaChatRequest(
            model = elementExtractingModel,
            stream = false,
            raw = false,
            messages = prompt
        )
        val message = Message(
            role = "user",
            content = """
        <script>
        $scraperCode
        </script>
        ```json
        [
            ${
                newElements.joinToString(",\n") {
                    """{"type": "${it.type}", "cssSelector": "${it.cssSelector}", "id": "${it.id}", "text": "${it.label}"}"""
                }
            }
        ]
        ```
    """.trimIndent()
        )

        val ollamaChatResponse = llmClient.chat(ollamaChatRequest.copy(messages = ollamaChatRequest.messages + message))

        val messageContent = ollamaChatResponse.message.content

        if (messageContent.isNotBlank()) {
            return Json.decodeFromString(ListSerializer(Element.serializer()), messageContent)
        }
        return listOf()
    }

    private suspend fun getModifiedScript(modelName: String, messages: List<Message>): String {
        val chatRequest = OllamaChatRequest(
            model = modelName,
            stream = false,
            raw = false,
            messages = messages
        )

        return llmClient.chat(chatRequest).message.content.cleanUpdateScriptResponseJson()
    }

    private suspend fun modifyScriptUnitary(
        scraperUpdateRequest: ScraperUpdateRequest,
        modelName: String,
        systemPrompt: String
    ): String {
        val ollamaRequest = OllamaGenerateRequest(
            model = modelName,
            system = systemPrompt,
            prompt = Json.encodeToString(scraperUpdateRequest),
            stream = false,
            raw = false
        )

        val updateScriptResponseJson = llmClient.generate(ollamaRequest).response.cleanUpdateScriptResponseJson()

        val updateScriptResponse = Json.decodeFromString<ScraperUpdateResponse>(updateScriptResponseJson)

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
        return modifications.map { m -> CssSelector(m.old.id, m.old.cssSelector, m.new.cssSelector, m.new.id) }
    }

    private fun String.extractAlternative(): String {
        val regex = Regex("""<BEGIN_ALTERNATIVE>\s*(.*?)\s*<END_ALTERNATIVE>""", RegexOption.DOT_MATCHES_ALL)
        val matchResult = regex.find(this)
        return matchResult?.groups?.get(1)?.value
            ?: throw IllegalArgumentException("No alternative found in the response")
    }
}

fun main() {
    val httpClient = OkHttpClient()
    val llmClient = OllamaClient(httpClient)
    val modificationService = ModificationService(
        llmClient = llmClient,
        getModificationModel = "mistral:7b",
        elementExtractingModel = "mistral:7b"
    )

    val oldScript = """
        package scraper

        import classes.data.BookingOption
        import interfaces.IScraper
        import org.openqa.selenium.By
        import org.openqa.selenium.WebDriver
        import org.openqa.selenium.support.ui.ExpectedConditions
        import org.openqa.selenium.support.ui.WebDriverWait
        import snapshots.ISnapshotService
        import java.time.Duration

        class DemoScraper(private val driver: WebDriver, private val snapshotService: ISnapshotService) : IScraper {
            override suspend fun scrape(): List<BookingOption> {
                driver.get("http://localhost:5173/")
                WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("search-button"))).click()
                snapshotService.takeSnapshotAsFile(driver)

                val optionElements = WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("item-title")))
                snapshotService.takeSnapshotAsFile(driver)

                return optionElements.map { BookingOption(it.text) }
            }
        }
    """.trimIndent()

    val newElements = emptyList<Element>()

    runBlocking {
        val missing = modificationService.getElementsFromScript(oldScript, newElements, GET_MISSING_ELEMENTS_SYSTEM_PROMPT, GET_MISSING_ELEMENTS_MESSAGES)
        println("Missing elements: $missing")
    }
}