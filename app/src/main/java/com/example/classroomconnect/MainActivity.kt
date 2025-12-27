package com.example.classroomconnect

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth= FirebaseAuth.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
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
    override fun onStart() {
        super.onStart()

        if (FirebaseAuth.getInstance().currentUser != null) {
            redirectByRole()
        }
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
                            val i=Intent(this, StudentActivity::class.java)
                            i.putExtra(KEY1,name.toString())
                            i.addFlags(  Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK)

                            startActivity(i)
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
                            val i=Intent(this, TeacherActivity::class.java)
                            i.putExtra(KEY1,name.toString())
                            i.addFlags(  Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(i)
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
}