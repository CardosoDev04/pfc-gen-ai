package detection

import classes.service_model.Modification

interface IModificationDetectionService {
    fun getModifications(modifiedElement: String, newElements: List<String>): List<Modification>
}