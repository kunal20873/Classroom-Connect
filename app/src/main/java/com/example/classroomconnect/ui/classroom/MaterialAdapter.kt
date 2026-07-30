package com.example.classroomconnect.ui.classroom

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.classroomconnect.R
import com.example.classroomconnect.data.models.Material

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

    private var originalList: ArrayList<Material> = ArrayList(materialList)

    fun updateList(newList: ArrayList<Material>) {
        materialList = newList
        originalList = ArrayList(newList)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            originalList
        } else {
            val result = ArrayList<Material>()
            for (item in originalList) {
                if (item.topic?.contains(query, ignoreCase = true) == true) {
                    result.add(item)
                }
            }
            result
        }
        materialList = filteredList
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
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.each_material, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = materialList[position]
        holder.MaterialNAME.text = currentItem.topic
        holder.materialLink.text = currentItem.fileName ?: "Document"

        if (userRole == "Student") {
            holder.btnQuiz.visibility = View.VISIBLE
        } else {
            holder.btnQuiz.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onOpenPdf(currentItem)
        }

        holder.btnQuiz.setOnClickListener {
            onQuizClick(currentItem)
        }

        holder.itemView.setOnLongClickListener {
            onDeleteClick(currentItem)
            true
        }
    }

    override fun getItemCount(): Int {
        return materialList.size
    }
}
