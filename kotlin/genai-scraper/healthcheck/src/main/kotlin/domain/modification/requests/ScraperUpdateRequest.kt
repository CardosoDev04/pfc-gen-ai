package domain.modification.requests

import classes.service_model.CssSelector
import kotlinx.serialization.Serializable

@Serializable
data class ScraperUpdateRequest(
    val imports: String,
    val script: String,
    val cssSelectorChanges: List<CssSelector>,
)
