package domain.modification.requests

import classes.service_model.CssSelectorModification
import kotlinx.serialization.Serializable

@Serializable
data class ScraperUpdateRequest(
    val selectorChanges: List<CssSelectorModification>,
    val script: String
)
