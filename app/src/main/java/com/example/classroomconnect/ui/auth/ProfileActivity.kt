package com.example.classroomconnect.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.classroomconnect.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            finish()
            return
        }
        currentUserId = currentUser.uid

        loadProfileData()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnUpdateProfile.setOnClickListener {
            val newName = binding.etName.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateProfile(newName)
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileData() {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val name = snapshot.child("name").value?.toString()
                val email = snapshot.child("email").value?.toString()
                
                binding.etName.setText(name)
                binding.etEmail.setText(email)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProfile(newName: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val newEmail = binding.etEmail.text.toString().trim()
        val oldEmail = currentUser.email ?: ""

        if (newEmail.isEmpty()) {
            Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Update Name in Database
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
        userRef.child("name").setValue(newName).addOnSuccessListener {
            
            // 2. Check if Email needs to be updated
            if (newEmail != oldEmail) {
                updateEmailInAuth(newEmail)
            } else {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update database", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEmailInAuth(newEmail: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        user.updateEmail(newEmail).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Email updated in Auth, now update in Database
                syncEmailToDatabase(newEmail)
            } else {
                val exception = task.exception
                if (exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    showReauthDialog(newEmail)
                } else {
                    Toast.makeText(this, "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showReauthDialog(newEmail: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Security Confirmation")
        builder.setMessage("To change your email, please enter your current password for security.")

        val passwordInput = android.widget.EditText(this)
        passwordInput.hint = "Current Password"
        passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 0)
        passwordInput.layoutParams = params
        container.addView(passwordInput)
        builder.setView(container)

        builder.setPositiveButton("Verify & Update") { _, _ ->
            val password = passwordInput.text.toString()
            if (password.isNotEmpty()) {
                reauthenticateAndRetry(password, newEmail)
            } else {
                Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun reauthenticateAndRetry(password: String, newEmail: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateEmailInAuth(newEmail)
            } else {
                Toast.makeText(this, "Authentication failed. Incorrect password?", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun syncEmailToDatabase(newEmail: String) {
        FirebaseDatabase.getInstance().getReference("Users")
            .child(currentUserId).child("email").setValue(newEmail)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile and Email updated successfully", Toast.LENGTH_SHORT).show()
                // Send verification to new email
                FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Auth updated, but database sync failed", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}
