package com.example.classroomconnect

import android.app.Activity
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class DiscussionAdapter(var messageList: ArrayList<Discussion>, var context: Activity) :
RecyclerView.Adapter<DiscussionAdapter.MyViewHolder>(){
    class MyViewHolder(itemView: View):
    RecyclerView.ViewHolder(itemView){
        val senderName =itemView.findViewById<TextView>(R.id.tvSenderName)
        val message =itemView.findViewById<TextView>(R.id.tvMessageBody)

     }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DiscussionAdapter.MyViewHolder {
        val itemView= LayoutInflater.from(parent.context).inflate(R.layout.each_discussion,parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DiscussionAdapter.MyViewHolder, position: Int) {
        val currentItem=messageList[position]
        holder.senderName.text=currentItem.userNAME
        holder.message.text=currentItem.message

    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}