package modification_detection

import classes.data.Element
import classes.data.ElementTypes
import classes.service_model.Modification
import domain.classes.LLM
import domain.http.ollama.requests.OllamaGenerateRequest
import domain.modification.requests.ModificationRequest
import domain.prompts.GET_MISSING_ELEMENTS_PROMPT
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
}


fun main() {
    runBlocking {
        val httpClient = OkHttpClient()
        val llmClient = OllamaClient(httpClient)
        val service = ModificationDetectionService(llmClient)
        /*val modifiedElement = Element("BUTTON", "button.btn-xs", "Sign up")
        val newElements = listOf(
            Element(ElementTypes.TEXTAREA.name, "textarea.comments", "Comments"),
            Element(ElementTypes.BUTTON.name, "link.link-register", "Register"),
            Element(ElementTypes.HEADING.name, "h2.h2-title", "Register an account.")
        )
        val modification = service.getModification(modifiedElement, newElements)
        println(modification)*/
        val previousHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Example</title></head>
            <body>
                <button id="show-p">Click to reveal</button>
                <p id="hidden-p" style="display: none;">Hello, bro!</p>
            </body>
            </html>
        """.trimIndent()
        val newHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Example</title></head>
            <body>
                <div id="reveal-message-div">Press this</div>
                <p id="hidden-text" style="visibility: hidden;">Hello, brother!</p>
            </body>
            </html>
        """.trimIndent()
        val missingElements = service.getMissingElements(previousHtml, newHtml)
        println(missingElements)
    }
}