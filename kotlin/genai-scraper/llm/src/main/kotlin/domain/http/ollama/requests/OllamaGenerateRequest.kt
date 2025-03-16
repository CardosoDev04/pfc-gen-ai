package domain.http.ollama.requests

import kotlinx.serialization.Serializable

@Serializable
data class OllamaGenerateRequest(
    val model: String,
    val system: String,
    val prompt: String,
    val stream: Boolean,
    val raw: Boolean
)