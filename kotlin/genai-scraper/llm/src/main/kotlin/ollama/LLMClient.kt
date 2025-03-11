package ollama

import domain.http.ollama.requests.OllamaGenerateRequest
import domain.http.ollama.responses.OllamaGenerateResponse
import domain.http.ollama.responses.OllamaTagsResponse

interface LLMClient {
    suspend fun generate(ollamaChatRequest: OllamaGenerateRequest): OllamaGenerateResponse
    suspend fun tags(): OllamaTagsResponse
}