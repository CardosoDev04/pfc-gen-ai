package classes.llm

import kotlinx.serialization.Serializable

@Serializable
data class Model(val name: String, val model: String, val size: Long, val digest: String)
