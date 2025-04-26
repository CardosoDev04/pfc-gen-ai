package domain.http.ollama.responses

import classes.llm.Message
import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatResponse(val message: Message)