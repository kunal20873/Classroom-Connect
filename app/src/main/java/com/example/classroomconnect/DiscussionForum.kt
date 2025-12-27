package com.example.classroomconnect

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.DialogTitle
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classroomconnect.databinding.ActivityDiscussionForumBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import java.util.ArrayList



class DiscussionForum : AppCompatActivity() {
    private lateinit var binding : ActivityDiscussionForumBinding
    private lateinit var userName : String
    private lateinit var classCode : String
    private var isInitialLoad = true
    private lateinit var role : String
    private lateinit var messageList: ArrayList<Discussion>
    private lateinit var myAdapter: DiscussionAdapter
    private lateinit var classTeacherId : String
    private lateinit var teacherName : String
    private lateinit var topicOfCLass : String

    private  lateinit var currentUserId : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding= ActivityDiscussionForumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("role")
            .get().addOnSuccessListener { snapshot ->
                role = snapshot.value.toString()
                listenForNewDiscussion(classCode,teacherName,topicOfCLass)
            }
         teacherName=intent.getStringExtra("TeacherName")?:""
         classCode=intent.getStringExtra("ClassCode")?:""
         topicOfCLass=intent.getStringExtra("ClassTopic")?:""


        binding.teacherNAME.text="Created by : $teacherName"
        binding.classCODE.text="CLass Id : $classCode"
        binding.nameoftopic.text= "Topic : $topicOfCLass"
        messageList= ArrayList()
        binding.rcViewDoubt.layoutManager= LinearLayoutManager(this)
        val dataRef = FirebaseDatabase.getInstance().getReference("Classes")
        dataRef.child(classCode).get().addOnSuccessListener { snapshot ->

            classTeacherId = snapshot.child("uid").value.toString()


        }
        myAdapter= DiscussionAdapter(messageList,this){ selectedDiscussion ->

            if(currentUserId!=null&&currentUserId==classTeacherId){
                showDeleteDialog(selectedDiscussion)
            }
            else {
                Toast.makeText(this,"Student can't delete the messages ", Toast.LENGTH_SHORT).show()
            }

        }

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
              if(messageList.isNotEmpty()){
                  binding.rcViewDoubt.scrollToPosition(messageList.size-1)
              }
                Log.d("Message_List_Size",messageList.size.toString())

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DiscussionForum,"Failed to load message ", Toast.LENGTH_SHORT).show()
            }

        })
    }
    private fun showDeleteDialog(discussion: Discussion){
        val builder=android.app.AlertDialog.Builder(this)
        builder.setTitle("Delete message?")
        builder.setMessage("Are you sure , you want to delete this message")
        builder.setPositiveButton("Yes , Delete "){ dialog, which ->
            deleteDiscussionFromFirebase(discussion)

        }
        builder.setNegativeButton("No "){dialog, which ->
            dialog.dismiss()

        }
        val alert=builder.create()
        alert.show()

    }
    private fun listenForNewDiscussion(classcode: String,teachername : String,topicofclass: String){
        val discussionRef = FirebaseDatabase.getInstance().getReference("Classes").child(classCode).child("Discussion")
        discussionRef.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {
                if (isInitialLoad) return
                val senderUID =snapshot.child("senderUid").value?.toString()?:return
                val sender = snapshot.child("userNAME").value?.toString()?:"Someone"
                val message =snapshot.child("message").value?.toString()?:"New doubt"
                val text = "$sender : $message"

                if(senderUID==currentUserId) return
                else{
                    showNotification("A new message to Discussion Forum",text)
                }

            }

            override fun onChildChanged(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {}

            override fun onCancelled(error: DatabaseError) {}

        })
        discussionRef.get().addOnSuccessListener {
            isInitialLoad=false
        }

    }
    private fun deleteDiscussionFromFirebase(discussion: Discussion){
        if(discussion.discussionId!=null){
            val ref = FirebaseDatabase.getInstance().getReference("Classes").child(classCode).child("Discussion").child(discussion.discussionId!!)
            ref.removeValue().addOnSuccessListener {
                Toast.makeText(this,"Message is successfully deleted ", Toast.LENGTH_SHORT).show()

            }.addOnFailureListener{
                Toast.makeText(this,"Fail to delete message , try again ", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun showNotification(title: String,message: String){
        val channelId = "Discussion_Channel"
        val intent = Intent(this, DiscussionForum::class.java).apply {
            putExtra("TeacherName",teacherName)
            putExtra("ClassCode",classCode)
            putExtra("ClassTopic",topicOfCLass)

        }
        val pendingIntent = PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE )

        val manager =getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId, "Discussion Updates ", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this,channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(),notification)
    }
    private fun createMessage(message: String){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("name").get()
            .addOnSuccessListener { snapshot ->
                 userName = snapshot.value.toString()

              val ref =  FirebaseDatabase.getInstance().getReference("Classes").child(classCode).child("Discussion").push()
                  val discussionId = ref.key!!
                val discussion= Discussion(userNAME = userName,message=message, discussionId = discussionId, senderUid = uid)
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