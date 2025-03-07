package llm

import domain.classes.LLM
import domain.http.Uris
import domain.http.ollama.requests.OllamaChatRequest
import domain.http.ollama.responses.OllamaChatResponse
import domain.http.ollama.responses.OllamaTagsResponse
import kotlinx.coroutines.runBlocking
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
): LLMClient {
    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

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

fun main() {
    val httpClient = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true }
    val client = OllamaClient(httpClient, json)

    runBlocking {
        val request = OllamaChatRequest("What is your name?", LLM.Mistral7B)
        val response = client.chat(request)
        println(response.response)
    }
}