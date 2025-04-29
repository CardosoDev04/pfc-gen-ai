package domain.http.ollama.requests

import classes.llm.Message
import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatRequest(
    val model: String,
    val stream: Boolean,
    val raw: Boolean,
    val messages: List<Message>
)
