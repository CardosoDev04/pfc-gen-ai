package detection

import classes.data.Element
import classes.service_model.Modification

interface IModificationDetectionService {
    fun getModification(modifiedElement: Element, newElements: List<Element>): Modification
}