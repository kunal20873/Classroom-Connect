package com.example.classroomconnect

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.classroomconnect.MaterialAdapter.MyViewHolder
import android.util.Log
class MaterialAdapter(var materialList: ArrayList<Material>, var context: Activity ,
    var onDeleteClick : (Material)-> Unit) :
RecyclerView.Adapter<MaterialAdapter.MyViewHolder>(){
    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val MaterialNAME=itemView.findViewById<TextView>(R.id.tvEach1)
        val materialLink = itemView.findViewById<TextView>(R.id.tvEach2)

     }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MaterialAdapter.MyViewHolder {
        val itemView= LayoutInflater.from(parent.context).inflate(R.layout.each_material,parent,false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MaterialAdapter.MyViewHolder, position: Int) {
        val currentItem=materialList[position]
        var x= currentItem.link
        holder.MaterialNAME.text =currentItem.topic
        holder.materialLink.text=" Link : $x"
        holder.itemView.setOnLongClickListener {
            android.util.Log.d("CLICK_TEST","Long click pressed!")
            onDeleteClick(currentItem)
            true
        }
    }

    override fun getItemCount(): Int {
        return materialList.size
    }
}