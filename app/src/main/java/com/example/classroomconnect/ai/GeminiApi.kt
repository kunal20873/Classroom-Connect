package com.example.classroomconnect.ai

import android.util.Log
import com.example.classroomconnect.BuildConfig
import com.example.classroomconnect.data.network.HttpClientProvider
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

class GeminiApi {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun generateText(prompt: String): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isEmpty() || key == "null") {
                return "Error: API Key Missing in local.properties"
            }

            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(prompt))))
            )
            
            val httpResponse = HttpClientProvider.client.post(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$key"
            ) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json")
                setBody(request)
            }

            val rawJson = httpResponse.bodyAsText()
            Log.d("GeminiApi", "RAW JSON FROM GOOGLE: $rawJson")

            val response = jsonParser.decodeFromString<GeminiResponse>(rawJson)

            val text = response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (text == null) {
                return "API_ERROR: $rawJson"
            }

            Log.d("GeminiApi", "SUCCESSFUL RESPONSE: $text")
            text

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("GeminiApi", "Critical Error", e)
            "Error: ${e.localizedMessage}"
        }
    }
}
