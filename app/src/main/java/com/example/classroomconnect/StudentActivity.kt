package com.example.classroomconnect
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.classroomconnect.databinding.ActivityMainBinding
import com.example.classroomconnect.databinding.ActivityStudentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StudentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentBinding

    private lateinit var classArrayList: ArrayList<MODEL>
    private lateinit var database: FirebaseDatabase
    private lateinit var valueEventListener: ValueEventListener
    private lateinit var uid: String

    private lateinit var myAdapter: StudentAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding= ActivityStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)
      val  currentUser= FirebaseAuth.getInstance().currentUser
        if(currentUser==null){
            Toast.makeText(this,"Login first", Toast.LENGTH_SHORT).show()
            finish()
            return
            }else{
                uid=currentUser.uid
        }
        binding.rcViewStudent.layoutManager= LinearLayoutManager(this)
        classArrayList=ArrayList()
        myAdapter= StudentAdapter(classArrayList,this)
     binding.rcViewStudent.adapter=myAdapter
        loadClass()

          val name = intent.getStringExtra(MainActivity.KEY1)
          binding.studName.text= "Welcome $name "

        binding.btnJoin.setOnClickListener {
            val classCode = binding.classTopic.text.toString().trim()

            if(classCode.isEmpty()){
                Toast.makeText(this,"Enter class code ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                val getRef = FirebaseDatabase.getInstance().getReference("Classes").child(classCode)
                getRef.get().addOnSuccessListener { snapshot ->
                    if(snapshot.exists()){
                        joinClass(classCode)
                    }
                    else{
                        Toast.makeText(this,"Class doesn't exist", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this,"Unknown error occured , try again ", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
    private fun joinClass(classId: String){

        val joinRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("joinedClasses").child(classId)
     joinRef.setValue(true).addOnSuccessListener {
         Toast.makeText(this,"Class joined successfully", Toast.LENGTH_SHORT).show()
     }.addOnFailureListener {
         Toast.makeText(this,"Joining failed , try again ", Toast.LENGTH_SHORT).show()
     }

    }
    private fun loadClass(){

          val classRef= FirebaseDatabase.getInstance().getReference("Users").child(uid ).child("joinedClasses")
        classRef.addValueEventListener(object: ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                classArrayList.clear()
                for (classSnap in snapshot.children) {
                   val classId=classSnap.key?:continue
                    FirebaseDatabase.getInstance().getReference("Classes").child(classId).get().addOnSuccessListener { classData ->
                        val model=classData.getValue(MODEL::class.java)
                        if(model!=null){
                            model.classId=classId
                            classArrayList.add(model)
                            myAdapter.notifyDataSetChanged()
                        }

                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StudentActivity, "Error , Try again", Toast.LENGTH_SHORT)
                    .show()
            }

        })
}
}