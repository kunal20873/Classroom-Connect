package com.example.classroomconnect.ui.quiz

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classroomconnect.ai.QuizQuestion
import com.example.classroomconnect.databinding.ActivityReviewQuizBinding
import com.example.classroomconnect.ui.quiz.ReviewAdapter
import kotlinx.serialization.json.Json

class ReviewQuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewQuizBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val questionsJson = intent.getStringExtra("QUESTIONS_JSON")
        val userAnswersJson = intent.getStringExtra("ANSWERS_JSON")

        if (questionsJson != null && userAnswersJson != null) {
            val json = Json { ignoreUnknownKeys = true }
            try {
                val questions = json.decodeFromString<List<QuizQuestion>>(questionsJson)
                val answers = json.decodeFromString<List<Int>>(userAnswersJson)

                binding.rvReview.layoutManager = LinearLayoutManager(this)
                binding.rvReview.adapter = ReviewAdapter(questions, answers)
            } catch (e: Exception) {
                Log.e("ReviewQuizActivity", "Error parsing review data: ${e.message}")
            }
        }

        binding.btnBack.setOnClickListener { finish() }
    }
}
