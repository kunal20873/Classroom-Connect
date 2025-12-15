package com.example.classroomconnect

import android.os.Bundle
import android.widget.Toast
import android.view.View.OnClickListener
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.classroomconnect.databinding.ActivityMainBinding
import com.example.classroomconnect.databinding.ActivityTeacherBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseError
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot

class TeacherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTeacherBinding
    private lateinit var databaseReference: DatabaseReference
    private  lateinit var classArrayList: ArrayList<MODEL>

    private lateinit var valueEventListener: ValueEventListener
    private lateinit var uid : String
    private lateinit var myAdapter: TeacherAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        uid = FirebaseAuth.getInstance().currentUser!!.uid
        binding.rcViewTeacher.layoutManager = LinearLayoutManager(this)
        classArrayList= ArrayList()
          myAdapter= TeacherAdapter(classArrayList,this)
        binding.rcViewTeacher.adapter=myAdapter
        loadClasses()
        val name = intent.getStringExtra(MainActivity.KEY1)
        binding.view1.text = "Welcome $name "
        binding.btnClass.setOnClickListener {
            val className = binding.etTopic.text.toString().trim()
            if (className.isEmpty()) {
                Toast.makeText(this, "Enter class name ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else createClass(className)

        }


    }
    private fun createClass(className: String) {
        val database = FirebaseDatabase.getInstance().getReference("Classes")

        fun tryCreateclass() {
            val topic = binding.etTopic.text.toString().trim()
            val classId = generateClassId()
            database.child(classId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    tryCreateclass()
                } else {

                    val data = ClassDetail(classId, topic, uid)
                    database.child(classId).setValue(data).addOnSuccessListener {
                        Toast.makeText(this, "Classes created successfullly", Toast.LENGTH_SHORT)
                            .show()

                    }.addOnFailureListener {
                        Toast.makeText(this, "class generation failed ", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }
        tryCreateclass()
    }
   private fun loadClasses() {
        val classRef = FirebaseDatabase.getInstance().getReference("Classes")

        classRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                classArrayList.clear()
                for (classSnap in snapshot.children) {
                    val model = classSnap.getValue(MODEL::class.java)
                    if (model != null && model.uid == uid) {
                        model.classId = classSnap.key!!
                        classArrayList.add(model)
                    }
                }
                Log.d("Class_List_size", classArrayList.size.toString())
                myAdapter.notifyDataSetChanged()

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TeacherActivity, "Error , Try again", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }
    private fun generateClassId(): String {
        val chars = "QWERTYUIOPASDFGHJKLZXCVBNM7894561230"
        return (1..6).map { chars.random() }.joinToString(separator = "")
    }
}