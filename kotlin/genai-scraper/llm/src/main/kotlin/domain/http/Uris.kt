package domain.http

object Uris {
    object Ollama {
        private const val PREFIX = "/api"
        const val GENERATE = "$PREFIX/generate"
        const val TAGS = "$PREFIX/tags"
    }
}