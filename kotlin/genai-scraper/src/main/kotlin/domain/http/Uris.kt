package domain.http

object Uris {
    object Ollama {
        private const val BASE = "/api"
        const val CHAT = "${BASE}/chat"
        const val TAGS = "${BASE}/tags"
    }
}