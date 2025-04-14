package domain.modification.requests

import kotlinx.serialization.Serializable

@Serializable
data class LlmRequest(
    val model: String,
    val stream: Boolean,
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    val content: String
)