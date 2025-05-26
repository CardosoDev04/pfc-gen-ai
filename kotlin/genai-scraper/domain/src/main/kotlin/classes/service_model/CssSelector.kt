package classes.service_model

import kotlinx.serialization.Serializable

@Serializable
data class CssSelector(
    val oldId: String? = null,
    val oldCssSelector: String,
    val newCssSelector: String,
    val newId: String? = null
)
