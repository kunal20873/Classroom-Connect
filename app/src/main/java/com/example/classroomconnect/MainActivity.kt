package com.example.classroomconnect

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
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
                            val user = firebaseAuth.currentUser
                            if (user != null && user.isEmailVerified) {
                                Toast.makeText(this, "Login successfully", Toast.LENGTH_SHORT).show()
                                redirectByRole()
                            } else {
                                // Email not verified
                                showVerificationDialog(email)
                                firebaseAuth.signOut()
                            }
                        } else {
                            val exception = task.exception
                            val message = when {
                                exception is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Email does not exist. Please register."
                                exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Wrong password."
                                exception is com.google.firebase.auth.FirebaseAuthException -> {
                                    when (exception.errorCode) {
                                        "ERROR_USER_NOT_FOUND" -> "Email does not exist. Please register."
                                        "ERROR_WRONG_PASSWORD" -> "Wrong password."
                                        "INVALID_LOGIN_CREDENTIALS" -> "Invalid email or password."
                                        else -> exception.message ?: "Failed, try again"
                                    }
                                }
                                else -> exception?.message ?: "Failed, try again"
                            }
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Enter both email and password", Toast.LENGTH_SHORT).show()
            }
        }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")
        
        val emailEditText = EditText(this)
        emailEditText.hint = "Enter your registered email"
        // Pre-fill with existing email if any
        emailEditText.setText(binding.etEmail.text.toString())
        
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 0)
        emailEditText.layoutParams = params
        container.addView(emailEditText)
        builder.setView(container)

        builder.setPositiveButton("Send Link") { _, _ ->
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset link sent to your email", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun showVerificationDialog(email: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Email Not Verified")
        builder.setMessage("Please verify your email address before logging in. A verification link was sent to $email.")
        
        builder.setPositiveButton("Resend Email") { _, _ ->
            // Temporarily sign in to resend verification
            // Note: In a real app, you might want to ask for password again or handle this differently
            // but for simplicity here we assume the previous login attempt just succeeded but isEmailVerified was false.
            firebaseAuth.currentUser?.sendEmailVerification()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Verification email resent", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to resend email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        builder.setNegativeButton("Dismiss") { dialog, _ -> dialog.dismiss() }
        builder.show()
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