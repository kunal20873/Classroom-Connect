package com.example.classroomconnect.data.models

import com.example.classroomconnect.ai.QuizQuestion
import kotlinx.serialization.Serializable

@Serializable
data class QuizRecord(
    val id: String? = null,
    val topic: String? = null,
    val score: Int = 0,
    val total: Int = 0,
    val timestamp: Long = 0L,
    val questions: List<QuizQuestion>? = emptyList(),
    val userAnswers: List<Int>? = emptyList()
)
