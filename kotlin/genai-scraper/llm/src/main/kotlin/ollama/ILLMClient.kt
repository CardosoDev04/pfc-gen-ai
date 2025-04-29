package ollama

import domain.http.ollama.requests.OllamaChatRequest
import domain.http.ollama.requests.OllamaGenerateRequest
import domain.http.ollama.responses.OllamaChatResponse
import domain.http.ollama.responses.OllamaGenerateResponse
import domain.http.ollama.responses.OllamaTagsResponse

interface ILLMClient {
    suspend fun generate(ollamaGenerateRequest: OllamaGenerateRequest): OllamaGenerateResponse
    suspend fun chat(ollamaChatRequest: OllamaChatRequest): OllamaChatResponse
    suspend fun tags(): OllamaTagsResponse
}