package classes.service_model

import kotlinx.serialization.Serializable

@Serializable
data class Modification<T>(val old: T, val new: T)
