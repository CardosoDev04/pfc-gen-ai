package llm

import domain.http.ollama.requests.OllamaChatRequest
import domain.http.ollama.responses.OllamaChatResponse

interface LLMClient {
    suspend fun chat(ollamaChatRequest: OllamaChatRequest): OllamaChatResponse
}