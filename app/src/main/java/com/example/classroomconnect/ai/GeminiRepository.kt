package com.example.classroomconnect.ai

import kotlinx.serialization.json.Json

class GeminiRepository {

    private val geminiApi = GeminiApi()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateText(prompt: String): String {
        return geminiApi.generateText(prompt)
    }

    suspend fun generateQuiz(topic: String): QuizResponseWrapper? {
        val systemInstruction = """
            You are a helpful teaching assistant. 
            Generate a 10-question multiple choice quiz about the topic: $topic.
            Return ONLY a valid JSON object in the following format:
            {
              "questions": [
                {
                  "question": "Question text here?",
                  "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                  "answerIndex": 0
                }
              ]
            }
            Do not include any markdown formatting like ```json or any other text.
        """.trimIndent()

        val rawResponse = generateText(systemInstruction)
        
        return try {
            // Robustly extract JSON using regex to find the first '{' and last '}'
            val jsonRegex = "\\{.*\\}".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = jsonRegex.find(rawResponse)
            val cleanJson = match?.value ?: rawResponse.removeSurrounding("```json", "```").trim()
            
            json.decodeFromString<QuizResponseWrapper>(cleanJson)
        } catch (e: Exception) {
            android.util.Log.e("GeminiRepository", "Error parsing quiz JSON. Raw response: $rawResponse")
            android.util.Log.e("GeminiRepository", "Error details: ${e.message}")
            null
        }
    }
}
