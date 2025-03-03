package domain.http.ollama.responses

import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatResponse(val model: String, val response: String)
