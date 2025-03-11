package domain.http.ollama.requests

import domain.classes.LLM
import kotlinx.serialization.Serializable

@Serializable
data class OllamaGenerateRequest(val model: String, val prompt: String, val stream: Boolean)