package modification_detection

import classes.data.Element
import classes.llm.Message
import classes.service_model.Modification
import domain.http.ollama.requests.OllamaChatRequest
import domain.modification.requests.ModificationRequest
import domain.modification.requests.ScraperUpdateRequestV2
import domain.prompts.FEW_SHOT_GET_MISSING_ELEMENTS_WITH_EXCEPTION_SYSTEM_PROMPT
import domain.prompts.FEW_SHOT_GET_MISSING_ELEMENTS_WITH_REASONING_AND_EXCEPTION
import domain.templates.ELEMENT_RECOVERY_PROMPT_TEMPLATE
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ollama.ILLMClient

/**
 * A service for detecting modifications in HTML and updating scraper scripts.
 *
 * @property llmClient The client for interacting with the LLM.
 */
class ModificationService(
    private val llmClient: ILLMClient,
    private val getModificationModel: String,
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
            messages =
                FEW_SHOT_GET_MISSING_ELEMENTS_WITH_REASONING_AND_EXCEPTION
                        + Message(role = "user", content = modificationRequestJson)
                        + Message(role = "system", content = FEW_SHOT_GET_MISSING_ELEMENTS_WITH_EXCEPTION_SYSTEM_PROMPT),
            stream = false,
            raw = false
        )

        val uncleanedAlternativeResponse = llmClient.chat(alternativeRequest).message.content
        val alternativeResponseJson = uncleanedAlternativeResponse.extractAlternative()
        val alternativeElement = Json.decodeFromString<Element>(alternativeResponseJson)

        return Modification(modifiedElement, alternativeElement)
    }

    override suspend fun modifyScriptChatHistoryV2(
        oldScript: String,
        missingElements: List<Modification<Element>>,
        modelName: String,
        messages: List<Message>
    ): String {
        val imports = getImports(oldScript)

        val request = ScraperUpdateRequestV2(
            imports = imports,
            script = oldScript,
            newElements = missingElements
        )

        val updatedMessage = messages + Message(
            role = "user",
            content = Json.encodeToString(request)
        )

        return getModifiedScript(modelName, updatedMessage)
    }

    override suspend fun getMissingElementAlternative(
        latestSnapshotHtmlElements: List<Element>,
        exceptionMessage: String,
        missingElement: Element
    ): Modification<Element> {
        val missingElementJson = Json.encodeToString(Element.serializer(), missingElement)
        val newElementsJson = Json.encodeToString(ListSerializer(Element.serializer()), latestSnapshotHtmlElements)
        val userPrompt = String.format(ELEMENT_RECOVERY_PROMPT_TEMPLATE, exceptionMessage, missingElementJson, newElementsJson)

        val alternativeRequest = OllamaChatRequest(
            model = getModificationModel,
            messages =
                FEW_SHOT_GET_MISSING_ELEMENTS_WITH_REASONING_AND_EXCEPTION
                        + Message(role = "user", content = userPrompt)
                        + Message(role = "system", content = FEW_SHOT_GET_MISSING_ELEMENTS_WITH_EXCEPTION_SYSTEM_PROMPT),
            stream = false,
            raw = false
        )

        val uncleanedAlternativeResponse = llmClient.chat(alternativeRequest).message.content
        val alternativeResponseJson = uncleanedAlternativeResponse.extractAlternative()
        val alternativeElement = Json.decodeFromString<Element>(alternativeResponseJson)

        return Modification(missingElement, alternativeElement)
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

    private fun String.extractAlternative(): String {
        val trimmed = this.trim()
        val startTag = "<BEGIN_ALTERNATIVE>"
        val endTag = "</END_ALTERNATIVE>"

        val startIndex = trimmed.indexOf(startTag)
        val endIndex = trimmed.indexOf(endTag)

        if (startIndex == -1 || endIndex == -1 || startIndex + startTag.length >= endIndex) {
            throw IllegalArgumentException("No alternative found in the response")
        }

        return this.substring(startIndex + startTag.length, endIndex).trim()
    }
}
