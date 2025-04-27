package classes.service_model

import kotlinx.serialization.Serializable

@Serializable
data class CssSelector(
    val oldCssSelector: String,
    val newCssSelector: String
)
