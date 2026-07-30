package com.example.classroomconnect.ui.student

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classroomconnect.R
import com.example.classroomconnect.data.models.MODEL
import com.example.classroomconnect.databinding.ActivityStudentBinding
import com.example.classroomconnect.databinding.DrawerLayoutBinding
import com.example.classroomconnect.ui.auth.MainActivity
import com.example.classroomconnect.ui.auth.ProfileActivity
import com.example.classroomconnect.ui.auth.SignUp
import com.example.classroomconnect.ui.classroom.ClassDetailActivity
import com.example.classroomconnect.ui.library.PersonalLibraryActivity
import com.example.classroomconnect.ui.quiz.QuizHistoryActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class StudentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentBinding
    private lateinit var classArrayList: ArrayList<MODEL>
    private lateinit var drawerBinding: DrawerLayoutBinding
    private lateinit var uid: String
    private lateinit var myAdapter: StudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentBinding.inflate(layoutInflater)
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
        drawerBinding.btnQuizHistory.setOnClickListener {
            startActivity(Intent(this, QuizHistoryActivity::class.java))
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

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else {
            uid = currentUser.uid
        }

        binding.rcViewStudent.layoutManager = LinearLayoutManager(this)
        classArrayList = ArrayList()
        myAdapter = StudentAdapter(classArrayList, this) { model, view ->
            showLeaveClassMenu(view, model)
        }
        binding.rcViewStudent.adapter = myAdapter
        myAdapter.setOnItemClickListener(object : StudentAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent(this@StudentActivity, ClassDetailActivity::class.java)
                intent.putExtra("ClassId", classArrayList[position].classId)
                startActivity(intent)
            }
        })

        loadClass()
        setupSearch()
        checkNotificationPermission()
        
        // Log FCM Token here for testing
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_TOKEN_DEBUG", "Your Device Token: ${task.result}")
            }
        }
        
        sendData()

        val name = intent.getStringExtra(MainActivity.KEY1)
        binding.studName.text = getString(R.string.welcome_placeholder, name)

        binding.btnJoin.setOnClickListener {
            val classCode = binding.classTopic.text.toString().trim()
            if (classCode.isEmpty()) {
                Toast.makeText(this, "Enter class code ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                val getRef = FirebaseDatabase.getInstance().getReference("Classes").child(classCode)
                getRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        joinClass(classCode)
                    } else {
                        Toast.makeText(this, "Class doesn't exist", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Unknown error occured , try again ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun joinClass(classId: String) {
        val joinRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("joinedClasses").child(classId)
        joinRef.setValue(true).addOnSuccessListener {
            Toast.makeText(this, "Class joined successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Joining failed , try again ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadClass() {
        val classRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("joinedClasses")
        classRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                classArrayList.clear()
                if (!snapshot.exists()) {
                    binding.tvNoClass.visibility = View.VISIBLE
                    binding.rcViewStudent.visibility = View.GONE
                    myAdapter.notifyDataSetChanged()
                    return
                }
                val totalClasses = snapshot.childrenCount.toInt()
                var loadedCount = 0
                for (classSnap in snapshot.children) {
                    val classId = classSnap.key ?: continue
                    FirebaseDatabase.getInstance()
                        .getReference("Classes")
                        .child(classId)
                        .get()
                        .addOnSuccessListener { classData ->
                            val model = classData.getValue(MODEL::class.java)
                            if (model != null) {
                                model.classId = classId
                                classArrayList.add(model)
                            }
                            loadedCount++
                            if (loadedCount == totalClasses) {
                                if (classArrayList.isEmpty()) {
                                    binding.tvNoClass.visibility = View.VISIBLE
                                    binding.rcViewStudent.visibility = View.GONE
                                } else {
                                    binding.tvNoClass.visibility = View.GONE
                                    binding.rcViewStudent.visibility = View.VISIBLE
                                }
                                myAdapter.updateList(classArrayList)
                            }
                        }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StudentActivity, "Error , Try again", Toast.LENGTH_SHORT).show()
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
                    Log.e("StudentActivity", "Error loading user data: ${error.message}")
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

    private fun checkNotificationPermission() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Toast.makeText(this, "Please enable notifications to stay updated!", Toast.LENGTH_LONG).show()
            Log.w("FCM_TEST", "Notifications are DISABLED in system settings")
        } else {
            Log.d("FCM_TEST", "Notifications are ENABLED")
        }
    }

    private fun showLeaveClassMenu(view: View, model: MODEL) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menu.add("Leave Class")
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.title == "Leave Class") {
                confirmLeaveClass(model)
            }
            true
        }
        popupMenu.show()
    }

    private fun confirmLeaveClass(model: MODEL) {
        AlertDialog.Builder(this)
            .setTitle("Leave Class")
            .setMessage("Are you sure you want to leave '${model.topic}'?")
            .setPositiveButton("Leave") { _, _ ->
                model.classId.let { leaveClass(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun leaveClass(classId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(uid).child("joinedClasses").child(classId)

        ref.removeValue().addOnSuccessListener {
            Toast.makeText(this, "Left class successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to leave class", Toast.LENGTH_SHORT).show()
        }
    }
}
