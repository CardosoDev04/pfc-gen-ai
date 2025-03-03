package domain.http.ollama.requests

import domain.classes.LLM
import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatRequest(val prompt: String, val model: LLM)