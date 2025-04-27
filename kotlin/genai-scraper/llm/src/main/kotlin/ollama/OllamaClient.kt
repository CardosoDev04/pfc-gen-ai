package ollama

import domain.http.Uris
import domain.http.ollama.requests.OllamaChatRequest
import domain.http.ollama.requests.OllamaGenerateRequest
import domain.http.ollama.responses.OllamaChatResponse
import domain.http.ollama.responses.OllamaGenerateResponse
import domain.http.ollama.responses.OllamaTagsResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class OllamaClient(
    private val httpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val baseUrl: String = "http://localhost:11434"
): ILLMClient {
    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    override suspend fun generate(ollamaGenerateRequest: OllamaGenerateRequest): OllamaGenerateResponse {
        val requestBody = json.encodeToString(ollamaGenerateRequest).toRequestBody(mediaTypeJson)

        val request = Request.Builder()
            .url(baseUrl + Uris.Ollama.GENERATE)
            .post(requestBody)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")

            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            json.decodeFromString<OllamaGenerateResponse>(responseBody)
        }
    }

    override suspend fun chat(ollamaChatRequest: OllamaChatRequest): OllamaChatResponse {
        val requestBody = json.encodeToString(ollamaChatRequest).toRequestBody(mediaTypeJson)

        val request = Request.Builder()
            .url(baseUrl + Uris.Ollama.CHAT)
            .post(requestBody)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")

            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            json.decodeFromString<OllamaChatResponse>(responseBody)
        }
    }

    override suspend fun tags(): OllamaTagsResponse {
        val request = Request.Builder()
            .url(baseUrl + Uris.Ollama.TAGS)
            .get()
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")

            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            json.decodeFromString<OllamaTagsResponse>(responseBody)
        }
    }
}