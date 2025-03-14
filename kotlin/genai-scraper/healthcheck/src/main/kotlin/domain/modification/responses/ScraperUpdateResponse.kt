package domain.modification.responses

import kotlinx.serialization.Serializable

@Serializable
data class ScraperUpdateResponse(
    val updatedScript: String
)