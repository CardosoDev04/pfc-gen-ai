package llm

import domain.classes.LLM
import domain.http.Uris
import domain.http.ollama.requests.OllamaGenerateRequest
import domain.http.ollama.responses.OllamaGenerateResponse
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

    override suspend fun generate(ollamaChatRequest: OllamaGenerateRequest): OllamaGenerateResponse {
        val requestBody = json.encodeToString(ollamaChatRequest).toRequestBody(mediaTypeJson)

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
    val client = OllamaClient(httpClient)

    runBlocking {
        val request = OllamaGenerateRequest(LLM.Mistral7B.modelName, "What is your name?",false)
        val response = client.generate(request)
        println(response.response)
    }
}