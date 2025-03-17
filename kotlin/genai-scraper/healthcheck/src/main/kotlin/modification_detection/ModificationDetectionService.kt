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

class ModificationDetectionService(
    private val llmClient: ILLMClient,
) : IModificationDetectionService {
    override suspend fun getMissingElements(previousHTMLState: String, newHTMLState: String): List<Element> {
        val missingElementsRequest = OllamaGenerateRequest(
            model = LLM.Mistral7B.modelName,
            system = GET_MISSING_ELEMENTS_PROMPT,
            prompt = """
                BEFORE:
                $previousHTMLState
                AFTER:
                $newHTMLState
            """.trimIndent(),
            stream = false,
            raw = false
        )
        val missingElementsResponseJson = llmClient.generate(missingElementsRequest).response
        return Json.decodeFromString<List<Element>>(missingElementsResponseJson)
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
        val service = ModificationDetectionService(llmClient)
        val modification = Modification("search-button", "search-btn")
        val oldScript = "driver.get(\"http://localhost:5173/\")\n" +
                "webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id(\"search-button\"))).click()\n" +
                "val optionElements = webDriverWait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id(\"item-title\")))\n" +
                "val results = optionElements.map { BookingOption(it.text) }"
        val newScript = service.modifyScript(oldScript, modification)

        print(newScript)
    }
}