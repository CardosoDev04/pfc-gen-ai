package llm

import domain.http.ollama.requests.OllamaChatRequest
import domain.http.ollama.responses.OllamaChatResponse
import domain.http.ollama.responses.OllamaTagsResponse

interface LLMClient {
    suspend fun chat(ollamaChatRequest: OllamaChatRequest): OllamaChatResponse
    suspend fun tags(): OllamaTagsResponse
}