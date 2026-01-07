package com.example.classroomconnect

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        Handler(Looper.getMainLooper()).postDelayed({
            if (user != null) {
                fetchRoleAndRedirect(user.uid)
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }, 1500)
    }

    private fun fetchRoleAndRedirect(uid: String) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val role = snapshot.child("role").value?.toString()?.trim()?.lowercase()
                    val name = snapshot.child("name").value?.toString()

                    val intent = if (role == "student") {
                        Intent(this, StudentActivity::class.java)
                    } else if (role == "teacher") {
                        Intent(this, TeacherActivity::class.java)
                    } else {
                        Intent(this, MainActivity::class.java)
                    }
                    intent.putExtra(MainActivity.KEY1, name)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }
}