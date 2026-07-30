package com.example.classroomconnect.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classroomconnect.R
import com.example.classroomconnect.data.models.ClassDetail
import com.example.classroomconnect.data.models.MODEL
import com.example.classroomconnect.databinding.ActivityTeacherBinding
import com.example.classroomconnect.databinding.DrawerLayoutBinding
import com.example.classroomconnect.ui.auth.MainActivity
import com.example.classroomconnect.ui.auth.ProfileActivity
import com.example.classroomconnect.ui.classroom.ClassDetailActivity
import com.example.classroomconnect.ui.library.PersonalLibraryActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TeacherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTeacherBinding
    private lateinit var classArrayList: ArrayList<MODEL>
    private lateinit var drawerBinding: DrawerLayoutBinding
    private lateinit var uid: String
    private lateinit var myAdapter: TeacherAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerBinding = binding.drawer

        binding.btnMenu1.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        drawerBinding.btnDashboard.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        drawerBinding.btnMyLibrary.setOnClickListener {
            startActivity(Intent(this, PersonalLibraryActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        drawerBinding.btnMyProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        drawerBinding.btnLogout.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Log Out ")
            builder.setMessage("Are you sure? You want to log out")
            builder.setPositiveButton("Yes Logout ") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            builder.setNegativeButton("No ") { dialog, _ ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }

        uid = FirebaseAuth.getInstance().currentUser!!.uid
        binding.rcViewTeacher.layoutManager = LinearLayoutManager(this)
        classArrayList = ArrayList()
        myAdapter = TeacherAdapter(classArrayList, this)
        binding.rcViewTeacher.adapter = myAdapter
        myAdapter.setOnItemClickListener(object : TeacherAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent(applicationContext, ClassDetailActivity::class.java)
                intent.putExtra("ClassId", classArrayList[position].classId)
                startActivity(intent)
            }
        })

        loadClasses()
        setupSearch()
        sendData()

        val name = intent.getStringExtra(MainActivity.KEY1)
        binding.view1.text = getString(R.string.welcome_placeholder, name)

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
            val classId = generateClassId()
            database.child(classId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    tryCreateclass()
                } else {
                    val data = ClassDetail(classId, className, uid)
                    database.child(classId).setValue(data).addOnSuccessListener {
                        Toast.makeText(this, "Classes created successfullly", Toast.LENGTH_SHORT).show()
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
                if (foundClass) {
                    binding.rcViewTeacher.visibility = View.VISIBLE
                    binding.tvNoClassCreated.visibility = View.GONE
                } else {
                    binding.tvNoClassCreated.visibility = View.VISIBLE
                    binding.rcViewTeacher.visibility = View.GONE
                }
                myAdapter.updateList(classArrayList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TeacherActivity, "Error , Try again", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendData() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userName = snapshot.child("name").value.toString()
                        val userEmail = snapshot.child("email").value.toString()
                        drawerBinding.tvUserName.text = "Name : $userName"
                        drawerBinding.tvUserEmail.text = "Gmail : $userEmail"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TeacherActivity", "Error loading user data: ${error.message}")
                }
            })
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            binding.searchCard.visibility = View.VISIBLE
            binding.btnSearch.visibility = View.GONE
            binding.sectionLabel.visibility = View.GONE
            binding.etSearch.requestFocus()
        }

        binding.btnCloseSearch.setOnClickListener {
            binding.searchCard.visibility = View.GONE
            binding.btnSearch.visibility = View.VISIBLE
            binding.sectionLabel.visibility = View.VISIBLE
            binding.etSearch.text?.clear()
            myAdapter.filter("")
        }

        binding.etSearch.addTextChangedListener { text ->
            myAdapter.filter(text.toString())
        }
    }

    private fun generateClassId(): String {
        val chars = "QWERTYUIOPASDFGHJKLZXCVBNM7894561230"
        return (1..6).map { chars.random() }.joinToString(separator = "")
    }
}
