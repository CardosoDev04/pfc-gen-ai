package domain.http

object Uris {
    object Ollama {
        private const val BASE = "/api"
        const val GENERATE = "${BASE}/generate"
        const val TAGS = "${BASE}/tags"
    }
}