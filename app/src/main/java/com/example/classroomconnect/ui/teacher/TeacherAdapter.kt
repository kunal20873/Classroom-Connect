package com.example.classroomconnect.ui.teacher

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.classroomconnect.R
import com.example.classroomconnect.data.models.MODEL

class TeacherAdapter(var classArrayList: ArrayList<MODEL>, var context: Activity) :
    RecyclerView.Adapter<TeacherAdapter.MyViewHolder>() {

    private var originalList: ArrayList<MODEL> = ArrayList(classArrayList)

    fun updateList(newList: ArrayList<MODEL>) {
        classArrayList = newList
        originalList = ArrayList(newList)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            originalList
        } else {
            val result = ArrayList<MODEL>()
            for (item in originalList) {
                if (item.topic.contains(query, ignoreCase = true)) {
                    result.add(item)
                }
            }
            result
        }
        classArrayList = filteredList
        notifyDataSetChanged()
    }

    private lateinit var myListener: onItemClickListener

    interface onItemClickListener {
        fun onItemClick(position: Int) {}
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        myListener = listener
    }

    class MyViewHolder(itemView: View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView) {
        val className = itemView.findViewById<TextView>(R.id.tEach1)
        val classId = itemView.findViewById<TextView>(R.id.tEach2)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.each_class, parent, false)
        return MyViewHolder(itemView, myListener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = classArrayList[position]
        val x = currentItem.topic
        val y = currentItem.classId
        holder.className.text = " Topic : $x"
        holder.classId.text = "Class id : $y"
    }

    override fun getItemCount(): Int {
        return classArrayList.size
    }
}
