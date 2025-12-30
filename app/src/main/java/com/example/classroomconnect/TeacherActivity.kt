package com.example.classroomconnect

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.classroomconnect.databinding.ActivityTeacherBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseError
import android.util.Log
import android.view.View
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classroomconnect.databinding.DrawerLayoutBinding
import com.google.firebase.database.DataSnapshot
import java.security.KeyStore


class TeacherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTeacherBinding
    private lateinit var databaseReference: DatabaseReference
    private  lateinit var classArrayList: ArrayList<MODEL>

    private lateinit var valueEventListener: ValueEventListener
    private lateinit var drawerBinding: DrawerLayoutBinding
    private lateinit var uid : String
    private lateinit var myAdapter: TeacherAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerBinding=binding.drawer
        binding.btnMenu1.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        drawerBinding.btnLogout.setOnClickListener {
            val builder=android.app.AlertDialog.Builder(this)
            builder.setTitle("Log Out ")
            builder.setMessage("Are you sure ? , you want to log out")
            builder.setPositiveButton("Yes Logout "){ dialog, which ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            }
            builder.setNegativeButton("No "){dialog, which ->
                dialog.dismiss()

            }
            val alert=builder.create()
            alert.show()
        }
        uid = FirebaseAuth.getInstance().currentUser!!.uid
        binding.rcViewTeacher.layoutManager = LinearLayoutManager(this)
        classArrayList= ArrayList()
          myAdapter= TeacherAdapter(classArrayList,this)
        binding.rcViewTeacher.adapter=myAdapter
        myAdapter.setOnItemClickListener(object : TeacherAdapter.onItemClickListener{
            override fun onItemClick(position: Int){
                val intent= Intent(applicationContext, ClassDetailActivity::class.java)

                intent.putExtra("ClassId",classArrayList[position].classId)
               startActivity(intent)
            }
        })
        loadClasses()
        sendData()
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

                if (!snapshot.exists()) {
                    binding.tvNoClassCreated.visibility = View.VISIBLE
                    binding.rcViewTeacher.visibility = View.GONE
                    myAdapter.notifyDataSetChanged()
                    return
                }
                var foundClass = false
                for (classSnap in snapshot.children) {
                    val model = classSnap.getValue(MODEL::class.java)
                    val classId = classSnap.key ?: continue
                    if (model != null && model.uid == uid) {
                        model.classId = classId
                        classArrayList.add(model)
                        foundClass = true
                    }
                }
                    if(foundClass){
                        binding.rcViewTeacher.visibility=View.VISIBLE
                        binding.tvNoClassCreated.visibility= View.GONE
                    }
                    else{
                        binding.tvNoClassCreated.visibility= View.VISIBLE
                        binding.rcViewTeacher.visibility= View.GONE
                    }
                myAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TeacherActivity, "Error , Try again", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }
    private fun sendData(){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val USERNAME = snapshot.child("name").value.toString()
                val USEREMAIL = snapshot.child("email").value.toString()
                drawerBinding.tvUserName.text="Name : $USERNAME"
                drawerBinding.tvUserEmail.text="Gmail : $USEREMAIL"
            }
    }
    private fun generateClassId(): String {
        val chars = "QWERTYUIOPASDFGHJKLZXCVBNM7894561230"
        return (1..6).map { chars.random() }.joinToString(separator = "")
    }
}