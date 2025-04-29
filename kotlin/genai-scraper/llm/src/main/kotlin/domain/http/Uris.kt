package domain.http

object Uris {
    object Ollama {
        private const val PREFIX = "/api"
        const val GENERATE = "$PREFIX/generate"
        const val CHAT = "$PREFIX/chat"
        const val TAGS = "$PREFIX/tags"
    }
}