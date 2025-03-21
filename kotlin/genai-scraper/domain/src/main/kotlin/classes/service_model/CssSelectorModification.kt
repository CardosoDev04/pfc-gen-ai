package classes.service_model

import kotlinx.serialization.Serializable

@Serializable
data class CssSelectorModification(
    val oldSelector: String,
    val newSelector: String
)
