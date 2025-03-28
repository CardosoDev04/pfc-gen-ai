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

    override suspend fun modifyScript(oldScript: String, modification: Modification<String>): String {
        val locator = Locator(modification.old, modification.new)
        val scraperUpdateRequest = ScraperUpdateRequest(listOf(locator), oldScript)
        val scraperUpdateRequestJson = Json.encodeToString(ScraperUpdateRequest.serializer(), scraperUpdateRequest)

        return queryLLM(scraperUpdateRequestJson)
    }

    override suspend fun modifyScript(oldScript: String, modifications: List<Modification<String>>): String {
        val locators = modifications.map { m -> Locator(m.old, m.new) }
        val scraperUpdateRequest = ScraperUpdateRequest(locators, oldScript)
        val scraperUpdateRequestJson = Json.encodeToString(ScraperUpdateRequest.serializer(), scraperUpdateRequest)

        return queryLLM(scraperUpdateRequestJson)
    }

    private suspend fun queryLLM(updateRequestJson: String): String {
        val ollamaRequest = OllamaGenerateRequest(
            model = LLM.Mistral7B.modelName,
            system = SCRAPER_UPDATE_PROMPT,
            prompt = updateRequestJson,
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
        val httpClient = OkHttpClient.Builder()
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val llmClient = OllamaClient(httpClient)
        val mds = ModificationDetectionService(llmClient)


        val oldScraper = "import org.openqa.selenium.By\nimport org.openqa.selenium.WebDriver\nimport org.openqa.selenium.chrome.ChromeDriver\n\nfun main() {\n    val driver: WebDriver = ChromeDriver()\n    try {\n        driver.get(\"https://example.com/form\")\n        val nameField = driver.findElement(By.id(\"name\"))\n        nameField.sendKeys(\"John Doe\")\n        val submitButton = driver.findElement(By.id(\"submit-button\"))\n        submitButton.click()\n        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5))\n        println(\"Form submitted successfully\")\n    } finally {\n        driver.quit()\n    }\n}"


        val updatedScript = mds.modifyScript(oldScraper, Modification("#submit-button", "#submit-btn"))
        val updatedScript2 = mds.modifyScript(oldScraper, listOf(Modification("#name", "#name-input"), Modification("#submit-button", "#submit-btn")))

        println(updatedScript)
        println()
        println("----------------------------------------------------------------------------------------------------")
        println()
        println(updatedScript2)
    }
}