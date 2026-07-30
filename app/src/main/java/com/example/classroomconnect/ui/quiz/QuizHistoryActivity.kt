package com.example.classroomconnect.ui.quiz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classroomconnect.ai.QuizQuestion
import com.example.classroomconnect.data.models.QuizRecord
import com.example.classroomconnect.databinding.ActivityQuizHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuizHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizHistoryBinding
    private lateinit var adapter: QuizHistoryAdapter
    private val historyList = ArrayList<QuizRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadHistory()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = QuizHistoryAdapter(historyList, { record ->
            val questions = record.questions
            if (!questions.isNullOrEmpty()) {
                val intent = Intent(this, ReviewQuizActivity::class.java)
                val json = Json { ignoreUnknownKeys = true }
                intent.putExtra("QUESTIONS_JSON", json.encodeToString(questions))
                intent.putExtra("ANSWERS_JSON", json.encodeToString(record.userAnswers ?: emptyList<Int>()))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Review not available for old quizzes", Toast.LENGTH_SHORT).show()
            }
        }, { record ->
            showDeleteDialog(record)
        })
        binding.rvQuizHistory.layoutManager = LinearLayoutManager(this)
        binding.rvQuizHistory.adapter = adapter
    }

    private fun showDeleteDialog(record: QuizRecord) {
        AlertDialog.Builder(this)
            .setTitle("Delete Record")
            .setMessage("Are you sure you want to delete this quiz record?")
            .setPositiveButton("Delete") { dialog, _ ->
                record.id?.let { deleteRecordFromFirebase(it) }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteRecordFromFirebase(recordId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(uid).child("quizHistory").child(recordId)

        ref.removeValue().addOnSuccessListener {
            Toast.makeText(this, "Record deleted successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to delete record", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(uid).child("quizHistory")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                for (recordSnap in snapshot.children) {
                    try {
                        val record = recordSnap.getValue(QuizRecord::class.java)
                        if (record != null) {
                            historyList.add(record)
                        }
                    } catch (e: Exception) {
                        Log.e("QuizHistoryActivity", "Error parsing record: ${e.message}")
                    }
                }
                
                // Sort by newest first
                historyList.sortByDescending { it.timestamp }
                
                if (historyList.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.rvQuizHistory.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.rvQuizHistory.visibility = View.VISIBLE
                }
                
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@QuizHistoryActivity, "Error loading history", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
