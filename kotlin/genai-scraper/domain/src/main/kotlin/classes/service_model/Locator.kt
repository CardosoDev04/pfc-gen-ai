package classes.service_model

import kotlinx.serialization.Serializable

@Serializable
data class Locator(
    val oldLocator: String,
    val newLocator: String
)
