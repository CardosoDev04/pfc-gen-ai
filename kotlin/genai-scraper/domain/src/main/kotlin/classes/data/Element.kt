package classes.data

import kotlinx.serialization.Serializable

@Serializable
data class Element(
    val type: String,
    val locator: String,
    val text: String = ""
)
