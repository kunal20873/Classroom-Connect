package com.example.classroomconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.classroomconnect.databinding.ActivityClassDetailBinding
import com.example.classroomconnect.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue

class ClassDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClassDetailBinding
    private lateinit var materialList : ArrayList<Material>
    private lateinit var myAdapter: MaterialAdapter
    private lateinit var classcode: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)


         classcode = intent.getStringExtra("ClassId") ?:run{
            Toast.makeText(this,"Class not found ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.classCode.text = "Class ID : ${classcode}"
        materialList= ArrayList()
        binding.rcViewMaterial.layoutManager= LinearLayoutManager(this)
        myAdapter= MaterialAdapter(materialList,this)
        binding.rcViewMaterial.adapter=myAdapter
        loadMaterial()

        val dataRef = FirebaseDatabase.getInstance().getReference("Classes")
        dataRef.child(classcode).get().addOnSuccessListener { snapshot ->
            val CLASSNAME = snapshot.child("topic").value.toString()
            val uid = snapshot.child("uid").value.toString()
            binding.topicname.text = " Topic : $CLASSNAME"
            fetchTeacherName(uid)

        }

        binding.btnAddMaterial.setOnClickListener {
            val MATERIAl = binding.topicMaterial.text.toString().trim()
            val LINK = binding.materialLink.text.toString().trim()
            val classOfMetarial = Material(MATERIAl, LINK)
            FirebaseDatabase.getInstance().getReference("Classes").child(classcode).child("Material").push().setValue(classOfMetarial)
                .addOnSuccessListener {
                    Toast.makeText(this, "Material added successfully", Toast.LENGTH_SHORT).show()

                }.addOnFailureListener {
                    Toast.makeText(this, "Material upload failed,try again", Toast.LENGTH_SHORT).show()
                }
        }
checkRoleandUpdateUi()
    }
    private fun checkRoleandUpdateUi(){
        val uid= FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
            .get().addOnSuccessListener { snapshot ->
                val role = snapshot.value.toString()
                if(role=="Student"){
                    binding.cardAddMaterial.visibility= View.GONE
                }
                else{
                    binding.cardAddMaterial.visibility= View.VISIBLE
                }
            }
    }
    private fun loadMaterial(){
        val materialRef= FirebaseDatabase.getInstance().getReference("Classes").child(classcode).child("Material")
        materialRef.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                materialList.clear()
                for(classSnap in snapshot.children){
                    val model = classSnap.getValue(Material::class.java)
                    if(model !=null){
                        materialList.add(model)
                    }
                }
                myAdapter.notifyDataSetChanged()
                Log.d("Material_List_Size",materialList.size.toString())

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ClassDetailActivity,"Error , try again ", Toast.LENGTH_SHORT).show()
            }

        })
    }
    private fun fetchTeacherName(uid: String){
        val dataREF= FirebaseDatabase.getInstance().getReference("Users")
        dataREF.child(uid).get().addOnSuccessListener { snapshot ->
            val techerNAME = snapshot.child("name").value.toString()
            binding.teacherName.text="Created by : $techerNAME"

        }
    }

}