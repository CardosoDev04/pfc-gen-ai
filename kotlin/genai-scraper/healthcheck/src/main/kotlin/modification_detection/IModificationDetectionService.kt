package modification_detection

import classes.data.Element
import classes.service_model.Modification

interface IModificationDetectionService {
    suspend fun getModification(modifiedElement: Element, newElements: List<Element>): Modification
    suspend fun modifyScript(oldScript: String, modification: Modification): String
}