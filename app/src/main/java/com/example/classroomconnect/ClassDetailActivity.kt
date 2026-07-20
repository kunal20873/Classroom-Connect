package com.example.classroomconnect
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.example.classroomconnect.databinding.ActivityClassDetailBinding
import com.google.firebase.database.DataSnapshot
import android.util.Log
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import java.io.File
import java.io.FileOutputStream
import androidx.activity.result.ActivityResultLauncher
class ClassDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClassDetailBinding
    private lateinit var materialList : ArrayList<Material>
    private lateinit var myAdapter: MaterialAdapter
    private lateinit var classcode: String
    private lateinit var role : String
    private lateinit var CLASSNAME : String
    private var pdfUri: Uri? = null
    private var isInitialLoad = true
    private lateinit var classTeacherUid : String
    private  lateinit var currentUserId : String
    private lateinit var techerNAME : String
    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                pdfUri = uri
                binding.txtSelectedFile.text = getFileName(uri)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        classcode = intent.getStringExtra("ClassId") ?:run{
            Toast.makeText(this,"Class not found ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentUserId= FirebaseAuth.getInstance().currentUser?.uid.toString()
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("role")
            .get().addOnSuccessListener { snapshot ->
                role = snapshot.value.toString()
                checkRoleandUpdateUi()
            }
        binding.classCode.text = "Class ID : ${classcode}"
        materialList= ArrayList()
        binding.rcViewMaterial.layoutManager= LinearLayoutManager(this)
        myAdapter = MaterialAdapter(
            materialList,
            this,

            // Long Press -> Delete
            { selectedMaterial ->

                if (currentUserId == classTeacherUid) {
                    showDeleteDialog(selectedMaterial)
                } else {
                    Toast.makeText(
                        this,
                        "Student cannot delete material",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            },

            // Single Click -> Open PDF
            { selectedMaterial ->

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse(selectedMaterial.pdfUrl),
                        "application/pdf"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "No PDF Viewer Installed",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            },
            // Practice Quiz click
            { selectedMaterial ->
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("MaterialTopic", selectedMaterial.topic)
                    putExtra("PdfUrl", selectedMaterial.pdfUrl)
                }
                startActivity(intent)
            }
        )
        if (::role.isInitialized) {
            myAdapter.setUserRole(role)
        }
        binding.rcViewMaterial.adapter=myAdapter
        loadMaterial()
        val dataRef = FirebaseDatabase.getInstance().getReference("Classes")
        dataRef.child(classcode).get().addOnSuccessListener { snapshot ->
            CLASSNAME = snapshot.child("topic").value.toString()
            classTeacherUid = snapshot.child("uid").value.toString()
            binding.topicname.text = " Topic : $CLASSNAME"
            fetchTeacherName(classTeacherUid)
        }

        binding.btnAddMaterial.setOnClickListener {

            val material = binding.topicMaterial.text.toString().trim()

            if (material.isEmpty()) {
                Toast.makeText(this, "Enter Material Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pdfUri == null) {
                Toast.makeText(this, "Choose PDF First", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadPdfToSupabase(material)

        }

        binding.btnChoosePdf.setOnClickListener {

            pdfPickerLauncher.launch("application/pdf")

        }
        binding.btnDoubt.setOnClickListener {
            openDoubtForum()
        }
    }
    private fun listenForNewMaterial(classcode: String){
        val materialRef = FirebaseDatabase.getInstance().getReference("Classes").child(classcode).child("Material")
        materialRef.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {
                if (isInitialLoad) return
                val topic = snapshot.child("topic").value?.toString()?:"New Material"
                val message="$topic\n Class  : $CLASSNAME ($classcode)"
                showNotificationFunction("New material added",message,classcode)
            }
            override fun onChildChanged(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {}

            override fun onCancelled(error: DatabaseError) {}

        })
        materialRef.get().addOnSuccessListener {
            isInitialLoad = false
        }
    }
    private fun checkRoleandUpdateUi(){
        if (::myAdapter.isInitialized) {
            myAdapter.setUserRole(role)
        }
        if(role=="Student"){
            binding.cardAddMaterial.visibility= View.GONE
            listenForNewMaterial(classcode)
        }
        else{
            binding.cardAddMaterial.visibility= View.VISIBLE
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
                        model.materialId=classSnap.key

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
    private fun getFileName(uri: Uri): String {
        var fileName = "Selected PDF"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && index != -1) {
                fileName = it.getString(index)
            }
        }
        return fileName
    }
    private fun uriToFile(uri: Uri): File {

        val fileName = getFileName(uri)

        val file = File(cacheDir, fileName)

        contentResolver.openInputStream(uri)?.use { inputStream ->

            FileOutputStream(file).use { outputStream ->

                inputStream.copyTo(outputStream)

            }
        }
        return file
    }
    private fun uploadPdfToSupabase(materialName: String) {
        if (pdfUri == null) {
            Toast.makeText(this, "Please choose a PDF", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val file = uriToFile(pdfUri!!)
                val extension = if (file.extension.isNotEmpty()) file.extension else "pdf"
                val fileName = "${System.currentTimeMillis()}.$extension"
                withContext(Dispatchers.IO) { 
                    SupabaseClient.client
                        .storage
                        .from("materials")
                        .upload(
                            path = fileName,
                            data = file.readBytes()
                        )
                }
                val pdfUrl =
                    "https://ttansuvasafbnrftfxor.supabase.co/storage/v1/object/public/materials/$fileName"
                val materialId = FirebaseDatabase.getInstance()
                    .getReference("Classes")
                    .child(classcode)
                    .child("Material")
                    .push()
                    .key!!
                // Change this block in ClassDetailActivity.kt
                val material = Material(
                    materialId = materialId,
                    topic = materialName,
                    pdfUrl = pdfUrl, // Changed from link to pdfUrl
                    fileName = file.name // Added fileName so it shows up in the list
                )
                FirebaseDatabase.getInstance()
                    .getReference("Classes")
                    .child(classcode)
                    .child("Material")
                    .child(materialId)
                    .setValue(material)
                    .addOnSuccessListener {
                        file.delete()
                        Toast.makeText(
                            this@ClassDetailActivity,
                            "PDF Uploaded Successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.topicMaterial.text?.clear()
                        binding.txtSelectedFile.text = ""
                        pdfUri = null
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Failed to save material",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ClassDetailActivity,
                    e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun showDeleteDialog(material: Material){
        val builder=android.app.AlertDialog.Builder(this)
        builder.setTitle("Delete material")
        builder.setMessage("Are you sure? You want to delete this material")
        builder.setPositiveButton("Yes Delete "){ dialog, which ->
            deleteMaterialFromFirebase(material)

        }
        builder.setNegativeButton("No "){dialog, which ->
            dialog.dismiss()

        }
        val alert=builder.create()
        alert.show()

    }
    private fun deleteMaterialFromFirebase(material: Material){
        if(material.materialId!=null){
            val ref = FirebaseDatabase.getInstance().getReference("Classes").child(classcode).child("Material").child(material.materialId!!)
            ref.removeValue().addOnSuccessListener {
                Toast.makeText(this,"Material is successfully deleted ", Toast.LENGTH_SHORT).show()

            }.addOnFailureListener{
                Toast.makeText(this,"Fail to delete material , try again ", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun showNotificationFunction(title : String , message:String , classCode : String){
        val channelId="material_channel"
        val intent = Intent(this, ClassDetailActivity::class.java).apply {
            putExtra("ClassId",classCode)
        }
        val pendingIntent= PendingIntent.getActivity(this,classcode.hashCode(),intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE )
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId,"Material Updates",
                NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification= NotificationCompat.Builder(this,channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(message)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(classCode.hashCode(),notification)
    }
    private fun fetchTeacherName(uid: String){
        val dataREF= FirebaseDatabase.getInstance().getReference("Users")
        dataREF.child(uid).get().addOnSuccessListener { snapshot ->
            techerNAME = snapshot.child("name").value.toString()
            binding.teacherName.text="Created by : $techerNAME"
        }
    }
    private fun openDoubtForum() {
        if (::CLASSNAME.isInitialized && ::techerNAME.isInitialized) {
            val intent = Intent(this, DiscussionForum::class.java)
            intent.putExtra("ClassTopic", CLASSNAME)
            intent.putExtra("TeacherName", techerNAME)
            intent.putExtra("ClassCode", classcode)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Loading class details... please wait", Toast.LENGTH_SHORT).show()
        }
    }
}