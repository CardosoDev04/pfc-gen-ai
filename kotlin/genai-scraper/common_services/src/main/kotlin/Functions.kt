fun String.extractCodeFromResponse(): String {
    val regex = Regex("```(?:\\w+)?\\n([\\s\\S]*?)```")
    val matchResult = regex.find(this)
    return matchResult?.groups?.get(1)?.value ?: ""
}