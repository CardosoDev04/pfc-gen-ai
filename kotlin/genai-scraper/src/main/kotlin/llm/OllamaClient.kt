package llm

import domain.http.Uris
import domain.http.ollama.requests.OllamaChatRequest
import domain.http.ollama.responses.OllamaChatResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class OllamaClient(
    private val baseUrl: String = "http://localhost:11434",
    private val httpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
): LLMClient {
    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    override suspend fun chat(ollamaChatRequest: OllamaChatRequest): OllamaChatResponse {
        val requestBody = json.encodeToString(ollamaChatRequest).toRequestBody(mediaTypeJson)

        val request = Request.Builder()
            .url(baseUrl + Uris.Ollama.CHAT)
            .post(requestBody)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            json.decodeFromString<OllamaChatResponse>(response.body.toString())
        }
    }
}