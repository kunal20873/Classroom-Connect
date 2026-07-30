package com.example.classroomconnect.ui.quiz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.classroomconnect.R
import com.example.classroomconnect.data.models.QuizRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuizHistoryAdapter(
    private val historyList: List<QuizRecord>,
    private val onItemClick: (QuizRecord) -> Unit,
    private val onLongClick: (QuizRecord) -> Unit
) : RecyclerView.Adapter<QuizHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTopic: TextView = view.findViewById(R.id.tvTopicName)
        val tvDate: TextView = view.findViewById(R.id.tvQuizDate)
        val tvScore: TextView = view.findViewById(R.id.tvQuizScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.each_quiz_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = historyList[position]
        holder.tvTopic.text = record.topic
        holder.tvScore.text = "${record.score}/${record.total}"
        
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(record.timestamp))

        holder.itemView.setOnClickListener {
            onItemClick(record)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(record)
            true
        }
    }

    override fun getItemCount() = historyList.size
}
