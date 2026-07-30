package com.example.classroomconnect.ui.quiz

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.classroomconnect.R
import com.example.classroomconnect.ai.QuizQuestion

class ReviewAdapter(
    private val questions: List<QuizQuestion>,
    private val userAnswers: List<Int>
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestionText)
        val options: List<TextView> = listOf(
            view.findViewById(R.id.tvOpt1),
            view.findViewById(R.id.tvOpt2),
            view.findViewById(R.id.tvOpt3),
            view.findViewById(R.id.tvOpt4)
        )
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.each_review_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]
        val userAnswer = if (position < userAnswers.size) userAnswers[position] else -1

        holder.tvQuestion.text = "${position + 1}. ${question.question}"

        holder.options.forEachIndexed { index, textView ->
            textView.text = question.options[index]
            
            val typedValue = TypedValue()
            textView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            val onSurfaceColor = typedValue.data

            textView.setTextColor(onSurfaceColor)

            val background = GradientDrawable().apply {
                cornerRadius = 20f
                setStroke(2, Color.parseColor("#444444")) // Use a dark gray that matches the new UI
            }

            if (index == question.answerIndex) {
                // Correct Answer
                background.setColor(Color.parseColor("#154CAF50"))
                background.setStroke(4, Color.parseColor("#4CAF50"))
                textView.setTextColor(Color.parseColor("#4CAF50"))
            } else if (index == userAnswer) {
                // Wrong User Choice
                background.setColor(Color.parseColor("#15F44336"))
                background.setStroke(4, Color.parseColor("#F44336"))
                textView.setTextColor(Color.parseColor("#F44336"))
            }

            textView.background = background
        }

        if (userAnswer == question.answerIndex) {
            holder.tvStatus.text = "✅ Correct"
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            holder.tvStatus.text = "❌ Incorrect"
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"))
        }
    }

    override fun getItemCount() = questions.size
}
