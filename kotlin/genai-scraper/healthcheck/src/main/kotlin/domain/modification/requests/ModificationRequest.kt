package domain.modification.requests

import kotlinx.serialization.Serializable

@Serializable
data class ModificationRequest(
    val modifiedElement: String,
    val newElements: String
)
