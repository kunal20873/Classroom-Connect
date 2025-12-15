package com.example.classroomconnect

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.classroomconnect.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    companion object{
        const val KEY1 ="com.example.classroomconnect.MainActivity.name"

    }

    private   fun redirectByRole(){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role").get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.value?.toString()?.trim()?.lowercase()
              if(role=="student"){
                  databaseReference = FirebaseDatabase.getInstance().getReference("Users")
                  databaseReference.child(uid).get().addOnSuccessListener {
                      if(it.exists()){
                          val name =it.child("name").value
                           val intent=Intent(this, StudentActivity::class.java)
                          intent.putExtra(KEY1,name.toString())
                          startActivity(intent)
                      }
                      else {
                          Toast.makeText(this,"Error , try again ",Toast.LENGTH_SHORT).show()
                      }
                  }

              }
                else if (role=="teacher"){
                  databaseReference = FirebaseDatabase.getInstance().getReference("Users")
                  databaseReference.child(uid).get().addOnSuccessListener {
                      if(it.exists()){
                          val name =it.child("name").value
                          val intent=Intent(this, TeacherActivity::class.java)
                          intent.putExtra(KEY1,name.toString())
                          startActivity(intent)
                      }
                      else {
                          Toast.makeText(this,"Error , try again ",Toast.LENGTH_SHORT).show()
                      }
                  }
              }
                else {
                  Toast.makeText(this,"Invalid Role", Toast.LENGTH_SHORT).show()
              }
            }.addOnFailureListener {
                Toast.makeText(this,"Failed try again ",Toast.LENGTH_SHORT).show()
            }
    }
    private fun openStudentHome(){

        val intent=Intent(this, StudentActivity::class.java)
        startActivity(intent)
    }
    private fun openTeacherHome(){
        val intent = Intent(this, TeacherActivity::class.java)
        startActivity(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth= FirebaseAuth.getInstance()
        binding.btnLogin.setOnClickListener {
            val email=binding.etEmail.text.toString()
            val password=binding.etPassword.text.toString()
            if(email.isNotEmpty()&&password.isNotEmpty()){
                firebaseAuth.signInWithEmailAndPassword(email,password)
                    .addOnCompleteListener(this){ task ->
                        if(task.isSuccessful){
                            Toast.makeText(this,"Login successfully", Toast.LENGTH_SHORT).show()
                         redirectByRole()

                        }else{
                            Toast.makeText(this,"Failed , try again", Toast.LENGTH_SHORT).show()
                        }

                    }
            }
            else{
                Toast.makeText(this,"Enter both email and password", Toast.LENGTH_SHORT).show()
            }

        }

        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

    }
}