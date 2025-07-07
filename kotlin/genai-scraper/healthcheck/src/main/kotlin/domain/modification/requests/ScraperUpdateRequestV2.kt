package domain.modification.requests

import classes.data.Element
import classes.service_model.Modification
import kotlinx.serialization.Serializable

@Serializable
data class ScraperUpdateRequestV2(
    val imports: String,
    val script: String,
    val newElements: List<Modification<Element>>
)
