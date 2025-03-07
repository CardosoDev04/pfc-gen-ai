package domain.http.ollama.responses

import kotlinx.serialization.Serializable

@Serializable
data class OllamaGenerateResponse(val model: String, val response: String)
