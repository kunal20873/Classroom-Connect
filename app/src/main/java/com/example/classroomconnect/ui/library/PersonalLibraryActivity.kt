package com.example.classroomconnect.ui.library

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classroomconnect.data.models.Material
import com.example.classroomconnect.data.network.SupabaseClient
import com.example.classroomconnect.databinding.ActivityPersonalLibraryBinding
import com.example.classroomconnect.ui.classroom.MaterialAdapter
import com.example.classroomconnect.ui.quiz.QuizActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PersonalLibraryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPersonalLibraryBinding
    private lateinit var materialList: ArrayList<Material>
    private lateinit var myAdapter: MaterialAdapter
    private var fileUri: Uri? = null
    private lateinit var currentUserId: String

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                fileUri = uri
                binding.txtSelectedFilePersonal.text = getFileName(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            finish()
            return
        }
        currentUserId = currentUser.uid

        setupRecyclerView()
        setupSearch()
        loadPersonalMaterials()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnChoosePdfPersonal.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        binding.btnUploadPersonal.setOnClickListener {
            val topic = binding.etPersonalTopic.text.toString().trim()
            if (topic.isEmpty()) {
                Toast.makeText(this, "Enter a name for your material", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fileUri == null) {
                Toast.makeText(this, "Please choose a file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadFileToSupabase(topic)
        }
    }

    private fun setupRecyclerView() {
        materialList = ArrayList()
        myAdapter = MaterialAdapter(
            materialList,
            this,
            { selectedMaterial -> showDeleteDialog(selectedMaterial) },
            { selectedMaterial -> openFile(selectedMaterial.pdfUrl, selectedMaterial.fileName) },
            { selectedMaterial -> startQuiz(selectedMaterial) }
        )
        myAdapter.setUserRole("Student")
        binding.rcViewPersonal.layoutManager = LinearLayoutManager(this)
        binding.rcViewPersonal.adapter = myAdapter
    }

    private fun loadPersonalMaterials() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(currentUserId).child("personalMaterials")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                materialList.clear()
                for (snap in snapshot.children) {
                    val model = snap.getValue(Material::class.java)
                    if (model != null) {
                        model.materialId = snap.key
                        materialList.add(model)
                    }
                }
                
                if (materialList.isEmpty()) {
                    binding.tvNoDocs.visibility = View.VISIBLE
                    binding.rcViewPersonal.visibility = View.GONE
                } else {
                    binding.tvNoDocs.visibility = View.GONE
                    binding.rcViewPersonal.visibility = View.VISIBLE
                }
                
                myAdapter.updateList(materialList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PersonalLibraryActivity, "Failed to load library", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            binding.searchCard.visibility = View.VISIBLE
            binding.btnSearch.visibility = View.GONE
            binding.sectionLabel.visibility = View.GONE
            binding.etSearch.requestFocus()
        }

        binding.btnCloseSearch.setOnClickListener {
            binding.searchCard.visibility = View.GONE
            binding.btnSearch.visibility = View.VISIBLE
            binding.sectionLabel.visibility = View.VISIBLE
            binding.etSearch.text?.clear()
            myAdapter.filter("")
        }

        binding.etSearch.addTextChangedListener { text ->
            myAdapter.filter(text.toString())
        }
    }

    private fun uploadFileToSupabase(topic: String) {
        lifecycleScope.launch {
            try {
                val file = uriToFile(fileUri!!)
                val extension = file.extension
                val fileName = "${System.currentTimeMillis()}.$extension"
                
                withContext(Dispatchers.IO) {
                    SupabaseClient.client
                        .storage
                        .from("materials")
                        .upload(path = fileName, data = file.readBytes())
                }

                val fileUrl = "https://ttansuvasafbnrftfxor.supabase.co/storage/v1/object/public/materials/$fileName"
                val materialId = FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUserId).child("personalMaterials").push().key!!

                val material = Material(
                    materialId = materialId,
                    topic = topic,
                    pdfUrl = fileUrl,
                    fileName = file.name
                )

                FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUserId).child("personalMaterials")
                    .child(materialId).setValue(material)
                    .addOnSuccessListener {
                        file.delete()
                        Toast.makeText(this@PersonalLibraryActivity, "Saved to library!", Toast.LENGTH_SHORT).show()
                        binding.etPersonalTopic.text?.clear()
                        binding.txtSelectedFilePersonal.text = "No file selected"
                        fileUri = null
                    }

            } catch (e: Exception) {
                Toast.makeText(this@PersonalLibraryActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeleteDialog(material: Material) {
        AlertDialog.Builder(this)
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to remove this from your library?")
            .setPositiveButton("Delete") { _, _ ->
                material.materialId?.let { id ->
                    FirebaseDatabase.getInstance().getReference("Users")
                        .child(currentUserId).child("personalMaterials")
                        .child(id).removeValue().addOnSuccessListener {
                            Toast.makeText(this, "Removed from library", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFile(url: String?, fileName: String?) {
        if (url == null) return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), getMimeType(fileName))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No compatible viewer installed", Toast.LENGTH_SHORT).show()
        }
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

    private fun startQuiz(material: Material) {
        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra("MaterialTopic", material.topic)
            putExtra("PdfUrl", material.pdfUrl)
            putExtra("FileName", material.fileName)
        }
        startActivity(intent)
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
}
