package com.example.classroomconnect

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.classroomconnect.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class SignUp : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
             lateinit var database: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
             firebaseAuth= FirebaseAuth.getInstance()
        binding.button3.setOnClickListener {
            val selectedRole = when (binding.rgRole.checkedRadioButtonId){
                R.id.rbTeacher -> "Teacher"
                else -> "Student"
            }
                val email=binding.btEmailSignup.text.toString()
                val password=binding.etPasswordSignup.text.toString()
               if(email.isNotEmpty()&&password.isNotEmpty()){
                   firebaseAuth.createUserWithEmailAndPassword(email,password)
                       .addOnCompleteListener(this) { task ->
                           if(task.isSuccessful){
                               val user = firebaseAuth.currentUser
                               user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                                   if (verificationTask.isSuccessful) {
                                       Toast.makeText(this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                                   } else {
                                       Toast.makeText(this, "Error sending verification email: ${verificationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                   }
                               }

                               val name = binding.etName.text.toString()
                               val email=binding.btEmailSignup.text.toString()
                               val uid= user!!.uid
                               val role= selectedRole
                               val userModel = User(name,role,email)
                               database= FirebaseDatabase.getInstance().getReference("Users")
                               database.child(uid).setValue(userModel).addOnSuccessListener {
                                   // User saved to database
                               }.addOnFailureListener {
                                   Toast.makeText(this,"Error saving user details",Toast.LENGTH_SHORT).show()
                               }
                               
                               val intent = Intent(this, MainActivity::class.java)
                               startActivity(intent)
                               finish()
                           }
                           else {
                               Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                           }

                       }

               }
               else {
                   Toast.makeText(this,"Enter both email and password ",Toast.LENGTH_SHORT).show()
               }
        }

          binding.textView3.setOnClickListener {
              val intent=Intent(this, MainActivity::class.java)
              startActivity(intent)
          }

    }
}