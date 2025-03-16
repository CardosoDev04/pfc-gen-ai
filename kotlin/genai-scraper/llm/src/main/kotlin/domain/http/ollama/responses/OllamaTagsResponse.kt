package domain.http.ollama.responses

import classes.llm.Model
import kotlinx.serialization.Serializable

@Serializable
data class OllamaTagsResponse(val models: List<Model>)
