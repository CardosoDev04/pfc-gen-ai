package classes.llm

import kotlinx.serialization.Serializable

@Serializable
sealed class Role {
    data object System : Role()
    data object User : Role()
}