package com.example.classroomconnect.ai

import kotlinx.serialization.Serializable

@Serializable
data class QuizResponseWrapper(
    val questions: List<QuizQuestion> = emptyList()
)

@Serializable
data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val answerIndex: Int = 0
)
