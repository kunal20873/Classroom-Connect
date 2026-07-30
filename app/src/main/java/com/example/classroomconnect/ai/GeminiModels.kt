package com.example.classroomconnect.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<Content>
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate> = emptyList(),
    val promptFeedback: PromptFeedback? = null
)

@Serializable
data class Candidate(
    val content: ResponseContent? = null,
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRating> = emptyList()
)

@Serializable
data class ResponseContent(
    val parts: List<ResponsePart> = emptyList()
)

@Serializable
data class ResponsePart(
    val text: String = ""
)

@Serializable
data class PromptFeedback(
    val safetyRatings: List<SafetyRating> = emptyList()
)

@Serializable
data class SafetyRating(
    val category: String,
    val probability: String
)
