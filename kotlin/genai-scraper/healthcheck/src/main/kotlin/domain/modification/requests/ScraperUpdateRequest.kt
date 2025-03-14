package domain.modification.requests

import kotlinx.serialization.Serializable

@Serializable
data class ScraperUpdateRequest(
    val oldSelector: String,
    val newSelector: String,
    val script: String
)
