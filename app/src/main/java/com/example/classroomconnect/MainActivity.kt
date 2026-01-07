package com.example.classroomconnect

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.classroomconnect.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        const val KEY1 = "com.example.classroomconnect.MainActivity.name"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successfully", Toast.LENGTH_SHORT).show()
                            redirectByRole()
                        } else {
                            Toast.makeText(this, "Failed, try again", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Enter both email and password", Toast.LENGTH_SHORT).show()
            }
        }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }
    }
    private fun redirectByRole() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("Users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.child("role").value?.toString()?.trim()?.lowercase()
                val name = snapshot.child("name").value?.toString()
                val intent = if (role == "student") {
                    Intent(this, StudentActivity::class.java)
                } else {
                    Intent(this, TeacherActivity::class.java)
                }
                intent.putExtra(KEY1, name)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
    }
}