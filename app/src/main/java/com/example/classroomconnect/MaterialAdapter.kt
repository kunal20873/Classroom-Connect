package com.example.classroomconnect

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MaterialAdapter(
    var materialList: ArrayList<Material>,
    var context: Activity,
    var onDeleteClick: (Material) -> Unit,
    var onOpenPdf: (Material) -> Unit,
    var onQuizClick: (Material) -> Unit
) : RecyclerView.Adapter<MaterialAdapter.MyViewHolder>() {

    private var userRole: String? = null

    fun setUserRole(role: String) {
        userRole = role
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val MaterialNAME = itemView.findViewById<TextView>(R.id.tvEach1)
        val materialLink = itemView.findViewById<TextView>(R.id.tvEach2)
        val btnQuiz = itemView.findViewById<TextView>(R.id.btnQuiz)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {

        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.each_material, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val currentItem = materialList[position]

        holder.MaterialNAME.text = currentItem.topic
        holder.materialLink.text = currentItem.fileName ?: "PDF File"

        // Show/Hide Quiz button based on role
        if (userRole == "Student") {
            holder.btnQuiz.visibility = View.VISIBLE
        } else {
            holder.btnQuiz.visibility = View.GONE
        }

        // Open PDF on normal click
        holder.itemView.setOnClickListener {
            onOpenPdf(currentItem)
        }

        // Quiz on quiz button click
        holder.btnQuiz.setOnClickListener {
            onQuizClick(currentItem)
        }

        // Delete on long press
        holder.itemView.setOnLongClickListener {
            onDeleteClick(currentItem)
            true
        }
    }

    override fun getItemCount(): Int {
        return materialList.size
    }
}