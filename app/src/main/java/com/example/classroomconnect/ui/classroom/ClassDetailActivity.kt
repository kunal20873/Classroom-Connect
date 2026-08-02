package com.example.classroomconnect.ui.classroom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classroomconnect.R
import com.example.classroomconnect.data.models.Material
import com.example.classroomconnect.data.network.SupabaseClient
import com.example.classroomconnect.databinding.ActivityClassDetailBinding
import com.example.classroomconnect.ui.quiz.QuizActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ClassDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClassDetailBinding
    private lateinit var materialList: ArrayList<Material>
    private lateinit var myAdapter: MaterialAdapter
    private lateinit var classcode: String
    private lateinit var role: String
    private lateinit var CLASSNAME: String
    private var fileUri: Uri? = null
    private lateinit var classTeacherUid: String
    private lateinit var currentUserId: String
    private lateinit var techerNAME: String

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                fileUri = uri
                binding.txtSelectedFile.text = getFileName(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        classcode = intent.getStringExtra("ClassId") ?: run {
            Toast.makeText(this, "Class not found ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("role")
            .get().addOnSuccessListener { snapshot ->
                role = snapshot.value.toString()
                checkRoleandUpdateUi()
            }
        binding.classCode.text = getString(R.string.class_id_placeholder, classcode)
        materialList = ArrayList()
        binding.rcViewMaterial.layoutManager = LinearLayoutManager(this)
        myAdapter = MaterialAdapter(
            materialList,
            this,
            { selectedMaterial ->
                if (currentUserId == classTeacherUid) {
                    showDeleteDialog(selectedMaterial)
                } else {
                    Toast.makeText(this, "Student cannot delete material", Toast.LENGTH_SHORT).show()
                }
            },
            { selectedMaterial ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(selectedMaterial.pdfUrl), getMimeType(selectedMaterial.fileName))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "No compatible viewer installed", Toast.LENGTH_SHORT).show()
                }
            },
            { selectedMaterial ->
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("MaterialTopic", selectedMaterial.topic)
                    putExtra("PdfUrl", selectedMaterial.pdfUrl)
                    putExtra("FileName", selectedMaterial.fileName)
                }
                startActivity(intent)
            }
        )
        if (::role.isInitialized) {
            myAdapter.setUserRole(role)
        }
        binding.rcViewMaterial.adapter = myAdapter
        loadMaterial()
        setupSearch()
        val dataRef = FirebaseDatabase.getInstance().getReference("Classes")
        dataRef.child(classcode).get().addOnSuccessListener { snapshot ->
            CLASSNAME = snapshot.child("topic").value.toString()
            classTeacherUid = snapshot.child("uid").value.toString()
            binding.topicname.text = getString(R.string.topic_placeholder, CLASSNAME)
            fetchTeacherName(classTeacherUid)
        }

        binding.btnAddMaterial.setOnClickListener {
            val material = binding.topicMaterial.text.toString().trim()
            if (material.isEmpty()) {
                Toast.makeText(this, "Enter Material Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fileUri == null) {
                Toast.makeText(this, "Choose a file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadFileToSupabase(material)
        }

        binding.btnChoosePdf.setOnClickListener {
            // Updated to support multiple document types
            val mimeTypes = arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
                "application/msword", // .doc
                "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
                "application/vnd.ms-powerpoint" // .ppt
            )
            filePickerLauncher.launch("*/*")
        }
        binding.btnDoubt.setOnClickListener {
            openDoubtForum()
        }
    }

    private fun checkRoleandUpdateUi() {
        if (::myAdapter.isInitialized) {
            myAdapter.setUserRole(role)
        }
        if (role == "Student") {
            binding.cardAddMaterial.visibility = View.GONE
            FirebaseMessaging.getInstance().subscribeToTopic(classcode)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FCM_SUBSCRIBE", "Successfully subscribed to class topic: $classcode")
                    } else {
                        Log.e("FCM_SUBSCRIBE", "Failed to subscribe to topic", task.exception)
                    }
                }
        } else {
            binding.cardAddMaterial.visibility = View.VISIBLE
        }
    }

    private fun loadMaterial() {
        val materialRef = FirebaseDatabase.getInstance().getReference("Classes").child(classcode).child("Material")
        materialRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                materialList.clear()
                for (classSnap in snapshot.children) {
                    val model = classSnap.getValue(Material::class.java)
                    if (model != null) {
                        model.materialId = classSnap.key
                        materialList.add(model)
                    }
                }
                myAdapter.updateList(materialList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ClassDetailActivity, "Error , try again ", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            binding.searchCard.visibility = View.VISIBLE
            binding.btnSearch.visibility = View.GONE
            binding.MATERIALNAME.visibility = View.GONE
            binding.etSearch.requestFocus()
        }

        binding.btnCloseSearch.setOnClickListener {
            binding.searchCard.visibility = View.GONE
            binding.btnSearch.visibility = View.VISIBLE
            binding.MATERIALNAME.visibility = View.VISIBLE
            binding.etSearch.text?.clear()
            myAdapter.filter("")
        }

        binding.etSearch.addTextChangedListener { text ->
            myAdapter.filter(text.toString())
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "Selected File"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && index != -1) {
                fileName = it.getString(index)
            }
        }
        return fileName
    }

    private fun getMimeType(fileName: String?): String {
        if (fileName == null) return "*/*"
        return when {
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".doc") -> "application/msword"
            fileName.endsWith(".pptx") -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            fileName.endsWith(".ppt") -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }
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

    private fun uploadFileToSupabase(materialName: String) {
        if (fileUri == null) return
        lifecycleScope.launch {
            try {
                val file = uriToFile(fileUri!!)
                
                if (file.length() > 50 * 1024 * 1024) {
                    Toast.makeText(this@ClassDetailActivity, "file must be less than 50 mb", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Show progress bar and disable button
                binding.uploadProgressBar.visibility = View.VISIBLE
                binding.btnAddMaterial.isEnabled = false

                val extension = file.extension
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
                val fileUrl = "https://ttansuvasafbnrftfxor.supabase.co/storage/v1/object/public/materials/$fileName"
                val materialId = FirebaseDatabase.getInstance()
                    .getReference("Classes")
                    .child(classcode)
                    .child("Material")
                    .push()
                    .key!!
                val material = Material(
                    materialId = materialId,
                    topic = materialName,
                    pdfUrl = fileUrl,
                    fileName = file.name
                )
                FirebaseDatabase.getInstance()
                    .getReference("Classes")
                    .child(classcode)
                    .child("Material")
                    .child(materialId)
                    .setValue(material)
                    .addOnSuccessListener {
                        file.delete()
                        Toast.makeText(this@ClassDetailActivity, "Uploaded Successfully", Toast.LENGTH_SHORT).show()
                        binding.topicMaterial.text?.clear()
                        binding.txtSelectedFile.text = ""
                        fileUri = null
                        
                        // Hide progress and re-enable button
                        binding.uploadProgressBar.visibility = View.GONE
                        binding.btnAddMaterial.isEnabled = true
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@ClassDetailActivity, "Failed to save material", Toast.LENGTH_SHORT).show()
                        // Hide progress and re-enable button on failure
                        binding.uploadProgressBar.visibility = View.GONE
                        binding.btnAddMaterial.isEnabled = true
                    }

            } catch (e: Exception) {
                Toast.makeText(this@ClassDetailActivity, e.message, Toast.LENGTH_LONG).show()
                // Hide progress and re-enable button on error
                binding.uploadProgressBar.visibility = View.GONE
                binding.btnAddMaterial.isEnabled = true
            }
        }
    }

    private fun showDeleteDialog(material: Material) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Delete material")
        builder.setMessage("Are you sure? You want to delete this material")
        builder.setPositiveButton("Yes Delete ") { _, _ ->
            deleteMaterialFromFirebase(material)
        }
        builder.setNegativeButton("No ") { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun deleteMaterialFromFirebase(material: Material) {
        if (material.materialId != null) {
            val ref = FirebaseDatabase.getInstance().getReference("Classes").child(classcode).child("Material").child(material.materialId!!)
            ref.removeValue().addOnSuccessListener {
                Toast.makeText(this, "Material is successfully deleted ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchTeacherName(uid: String) {
        val dataREF = FirebaseDatabase.getInstance().getReference("Users")
        dataREF.child(uid).get().addOnSuccessListener { snapshot ->
            techerNAME = snapshot.child("name").value.toString()
            binding.teacherName.text = getString(R.string.created_by_placeholder, techerNAME)
        }
    }

    private fun openDoubtForum() {
        if (::CLASSNAME.isInitialized && ::techerNAME.isInitialized) {
            val intent = Intent(this, DiscussionForum::class.java).apply {
                putExtra("ClassTopic", CLASSNAME)
                putExtra("TeacherName", techerNAME)
                putExtra("ClassCode", classcode)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Loading class details... please wait", Toast.LENGTH_SHORT).show()
        }
    }
}
