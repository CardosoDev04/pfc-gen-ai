package classes.data

import kotlinx.serialization.Serializable

@Serializable
data class Element(
    val type: String,
    val cssSelector: String,
    val text: String = ""
)
