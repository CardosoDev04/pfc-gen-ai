package domain.modification.requests

import classes.service_model.Locator
import kotlinx.serialization.Serializable

@Serializable
data class ScraperUpdateRequest(
    val locatorChanges: List<Locator>,
    val script: String,
    val imports: String
)
