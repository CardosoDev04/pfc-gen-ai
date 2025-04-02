package modification_detection

import classes.data.Element
import classes.service_model.Modification

interface IModificationDetectionService {
    suspend fun getMissingElements(previousHTMLState: String, newHTMLState: String): List<Element>
    suspend fun getModification(modifiedElement: Element, newElements: List<Element>): Modification<Element>
    suspend fun modifyMistralScript(oldScript: String, modification: Modification<Element>, modelName: String, prompt: String): String
    suspend fun modifyCodeLlamalScript(oldScript: String, modifications: List<Modification<Element>>, modelName: String, systemPrompt: String, prompt: String): String
    suspend fun modifyMistralScript(oldScript: String, modifications: List<Modification<Element>>, modelName: String, prompt: String): String
}