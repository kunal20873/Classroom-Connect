package com.example.classroomconnect


import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView
import androidx.recyclerview.widget.RecyclerView


class TeacherAdapter(var classArrayList: ArrayList<MODEL>, var context: Activity):
    RecyclerView.Adapter<TeacherAdapter.MyViewHolder> ()  {

    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val className=itemView.findViewById<TextView>(R.id.tEach1)
        val classId = itemView.findViewById<TextView>(R.id.tEach2)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TeacherAdapter.MyViewHolder {
        val  itemView = LayoutInflater.from(parent.context).inflate(R.layout.each_class,parent,false)
        return MyViewHolder(itemView)

    }

    override fun onBindViewHolder(holder: TeacherAdapter.MyViewHolder, position: Int) {
     val currentItem = classArrayList[position]
        var x=currentItem.topic
        var y=currentItem.classId
        holder.className.text =" Topic : $x"
        holder.classId.text="Class id : $y"
    }

    override fun getItemCount(): Int {
      return classArrayList.size
    }
}