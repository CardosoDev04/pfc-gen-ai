package modification_detection

import classes.data.Element
import classes.data.ElementTypes
import classes.service_model.Modification
import domain.classes.LLM
import domain.http.ollama.requests.OllamaGenerateRequest
import domain.modification.requests.ModificationRequest
import domain.prompts.GET_MODIFICATION_PROMPT
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import ollama.ILLMClient
import ollama.OllamaClient

class ModificationDetectionService(
    private val llmClient: ILLMClient,
) : IModificationDetectionService {
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
}


fun main() {
    runBlocking {
        val httpClient = OkHttpClient()
        val llmClient = OllamaClient(httpClient)
        val service = ModificationDetectionService(llmClient)
        val modifiedElement = Element("BUTTON", "button.btn-xs", "Sign up")
        val newElements = listOf(
            Element(ElementTypes.TEXTAREA.name, "textarea.comments", "Comments"),
            Element(ElementTypes.BUTTON.name, "link.link-register", "Register"),
            Element(ElementTypes.HEADING.name, "h2.h2-title", "Register an account.")
        )
        val modification = service.getModification(modifiedElement, newElements)
        println(modification)
    }
}