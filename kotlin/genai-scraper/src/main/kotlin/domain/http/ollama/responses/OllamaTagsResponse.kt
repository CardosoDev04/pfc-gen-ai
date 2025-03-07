package domain.http.ollama.responses

import domain.classes.Model
import kotlinx.serialization.Serializable

@Serializable
data class OllamaTagsResponse(val models: List<Model>)
