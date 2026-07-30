package com.example.classroomconnect.ui.quiz

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.classroomconnect.ai.GeminiRepository
import com.example.classroomconnect.ai.QuizQuestion
import com.example.classroomconnect.data.models.QuizRecord
import com.example.classroomconnect.databinding.ActivityQuizBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private val repository = GeminiRepository()
    
    private var questions: List<QuizQuestion> = emptyList()
    private var userAnswersList: MutableList<Int> = mutableListOf()
    private var currentIndex = 0
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val materialTopic = intent.getStringExtra("MaterialTopic") ?: "Quiz"
        binding.tvQuizTitle.text = materialTopic

        setupButtons()
        loadQuiz(materialTopic)
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnFinish.setOnClickListener { finish() }

        val optionButtons = listOf(
            binding.btnOption1,
            binding.btnOption2,
            binding.btnOption3,
            binding.btnOption4
        )

        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleAnswer(index)
            }
        }
    }

    private fun loadQuiz(topic: String) {
        lifecycleScope.launch {
            binding.loadingView.visibility = View.VISIBLE
            binding.quizCard.visibility = View.GONE
            binding.progressCard.visibility = View.GONE
            
            val response = repository.generateQuiz(topic)
            
            if (response != null && response.questions.isNotEmpty()) {
                questions = response.questions
                binding.loadingView.visibility = View.GONE
                binding.quizCard.visibility = View.VISIBLE
                binding.progressCard.visibility = View.VISIBLE
                displayQuestion()
            } else {
                Toast.makeText(this@QuizActivity, "Failed to generate quiz. Try again.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun displayQuestion() {
        val currentQuestion = questions[currentIndex]
        
        // Fade in animation for the card
        binding.quizCard.alpha = 0f
        binding.quizCard.animate().alpha(1f).setDuration(400).start()
        
        binding.tvProgress.text = "Question ${currentIndex + 1}/${questions.size}"
        binding.quizProgressBar.progress = ((currentIndex + 1) * 100) / questions.size
        
        binding.tvQuestion.text = currentQuestion.question
        
        val buttons = listOf(
            binding.btnOption1,
            binding.btnOption2,
            binding.btnOption3,
            binding.btnOption4
        )
        
        buttons.forEachIndexed { index, button ->
            button.text = currentQuestion.options[index]
            button.isEnabled = true
            button.strokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0"))
            button.setBackgroundColor(android.graphics.Color.TRANSPARENT) // Reset to outlined style
        }
    }

    private fun handleAnswer(selectedIndex: Int) {
        val currentQuestion = questions[currentIndex]
        val buttons = listOf(
            binding.btnOption1,
            binding.btnOption2,
            binding.btnOption3,
            binding.btnOption4
        )

        buttons.forEach { it.isEnabled = false }
        userAnswersList.add(selectedIndex)

        if (selectedIndex == currentQuestion.answerIndex) {
            score++
            buttons[selectedIndex].strokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))
            buttons[selectedIndex].setBackgroundColor(android.graphics.Color.parseColor("#154CAF50")) // Subtle green tint
        } else {
            buttons[selectedIndex].strokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336"))
            buttons[selectedIndex].setBackgroundColor(android.graphics.Color.parseColor("#15F44336")) // Subtle red tint
            
            // Show correct answer
            buttons[currentQuestion.answerIndex].strokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))
        }

        binding.main.postDelayed({
            if (currentIndex < questions.size - 1) {
                currentIndex++
                // Fade out current question before displaying next
                binding.quizCard.animate().alpha(0f).setDuration(300).withEndAction {
                    displayQuestion()
                }.start()
            } else {
                showResult()
            }
        }, 1200)
    }

    private fun showResult() {
        binding.quizCard.visibility = View.GONE
        binding.progressCard.visibility = View.GONE
        binding.resultCard.visibility = View.VISIBLE
        binding.tvFinalScore.text = "Your Score: $score/${questions.size}"

        saveQuizResult()

        val feedback = when {
            score < 5 -> {
                binding.tvResultEmoji.text = "📚"
                "You need to practice a lot"
            }
            score < 8 -> {
                binding.tvResultEmoji.text = "👍"
                "Good going, keep practicing"
            }
            else -> {
                binding.tvResultEmoji.text = "🎉"
                "Well done"
            }
        }
        binding.tvResultFeedback.text = feedback
    }

    private fun saveQuizResult() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val materialTopic = intent.getStringExtra("MaterialTopic") ?: "Quiz"
        val database = FirebaseDatabase.getInstance().getReference("Users")
            .child(uid).child("quizHistory")

        val recordId = database.push().key ?: return
        val record = QuizRecord(
            id = recordId,
            topic = materialTopic,
            score = score,
            total = questions.size,
            timestamp = System.currentTimeMillis(),
            questions = questions,
            userAnswers = userAnswersList
        )

        database.child(recordId).setValue(record)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save progress", Toast.LENGTH_SHORT).show()
            }
    }
}
