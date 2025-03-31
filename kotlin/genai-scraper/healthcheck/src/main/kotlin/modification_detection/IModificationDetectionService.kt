package modification_detection

import classes.data.Element
import classes.service_model.Modification

interface IModificationDetectionService {
    suspend fun getMissingElements(previousHTMLState: String, newHTMLState: String): List<Element>
    suspend fun getModification(modifiedElement: Element, newElements: List<Element>): Modification<Element>
    suspend fun modifyScript(oldScript: String, modification: Modification<Element>): String
    suspend fun modifyScript(oldScript: String, modifications: List<Modification<Element>>): String
}