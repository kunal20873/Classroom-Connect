package com.example.classroomconnect

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.classroomconnect.databinding.ActivityQuizBinding

class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val materialTopic = intent.getStringExtra("MaterialTopic") ?: "Quiz"
        binding.tvQuizTitle.text = "Quiz: $materialTopic"

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}