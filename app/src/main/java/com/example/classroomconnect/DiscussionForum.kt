package com.example.classroomconnect

import android.os.Bundle
import android.os.Message
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classroomconnect.databinding.ActivityDiscussionForumBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

class DiscussionForum : AppCompatActivity() {
    private lateinit var binding : ActivityDiscussionForumBinding
    private lateinit var userName : String
    private lateinit var classCode : String
    private lateinit var messageList: ArrayList<Discussion>
    private lateinit var myAdapter: DiscussionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding= ActivityDiscussionForumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val teacherName=intent.getStringExtra("TeacherName")
         classCode=intent.getStringExtra("ClassCode")?:""
        val topicOfCLass=intent.getStringExtra("ClassTopic")
        binding.teacherNAME.text="Created by : $teacherName"
        binding.classCODE.text="CLass Id : $classCode"
        binding.nameoftopic.text= "Topic : $topicOfCLass"
        messageList= ArrayList()
        binding.rcViewDoubt.layoutManager= LinearLayoutManager(this)
        myAdapter= DiscussionAdapter(messageList,this)
        binding.rcViewDoubt.adapter=myAdapter
        loadMessages()
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if(message.isEmpty()){
                Toast.makeText(this,"Enter message to send ", Toast.LENGTH_SHORT).show()
            }else{
                createMessage(message)
            }
        }



    }
    private fun loadMessages(){
        val dataRef= FirebaseDatabase.getInstance().getReference("Classes").child(classCode).child("Discussion")
        dataRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for(classSnap in snapshot.children){
                    val model = classSnap.getValue(Discussion::class.java)
                    if(model!=null){
                        model.discussionId=classSnap.key
                        messageList.add(model)
                    }

                }
                myAdapter.notifyDataSetChanged()
                Log.d("Message_List_Size",messageList.size.toString())

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DiscussionForum,"Failed to load message ", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun createMessage(message: String){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("name").get()
            .addOnSuccessListener { snapshot ->
                 userName = snapshot.value.toString()

              val ref =  FirebaseDatabase.getInstance().getReference("Classes").child(classCode).child("Discussion").push()
                  val discussionId = ref.key!!
                val discussion= Discussion(userNAME = userName,message=message, discussionId = discussionId)
                   ref.setValue(discussion).addOnSuccessListener {
                       Toast.makeText(this,"Message sent", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener{
                        Toast.makeText(this,"Failed to send message , try again ", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener{
                Toast.makeText(this,"Failed to load ", Toast.LENGTH_SHORT).show()
            }

    }

}