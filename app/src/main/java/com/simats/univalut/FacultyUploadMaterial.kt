package com.simats.univalut

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class FacultyUploadMaterial : AppCompatActivity() {

    private val FILE_REQUEST_CODE = 1001
    private var selectedFileUri: Uri? = null
    private lateinit var collegeName: String
    private lateinit var courseCode: String
    private lateinit var pdfContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.upload_material)

        // Initialize intent values
        collegeName = intent.getStringExtra("COLLEGE_NAME") ?: return
        courseCode = intent.getStringExtra("COURSE_CODE") ?: return

        // UI references
        val courseCodeTextView: TextView = findViewById(R.id.course_code)
        courseCodeTextView.text = courseCode

        val tvSelectType: TextView = findViewById(R.id.tvSelectType)
        val selectedFileTextView: TextView = findViewById(R.id.tvSelectedFile)
        val uploadArea: LinearLayout = findViewById(R.id.uploadArea)
        val submitButton: Button = findViewById(R.id.btnSubmitResources)
        val backButton: ImageView = findViewById(R.id.backButton)
        pdfContainer = findViewById(R.id.pdfContainer)

        // Go back
        backButton.setOnClickListener { onBackPressed() }

        // Resource type selection
        tvSelectType.setOnClickListener {
            val items = arrayOf("Lecture Notes", "Assignments", "PDF Resources")
            AlertDialog.Builder(this)
                .setTitle("Select Resource Type")
                .setItems(items) { _, which -> tvSelectType.text = items[which] }
                .show()
        }

        // File picker
        uploadArea.setOnClickListener { openFilePicker() }

        // Submit upload
        submitButton.setOnClickListener {
            val selectedType = tvSelectType.text.toString()
            when {
                selectedType == "Select type" || selectedType.isBlank() -> showError("Please select a resource type.")
                selectedFileUri == null -> showError("Please select a file to upload.")
                else -> uploadFile(selectedType)
            }
        }

        // Fetch uploaded files
        fetchPDFs(collegeName, courseCode)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedFileUri = uri
                val fileName = getFileName(uri)
                findViewById<TextView>(R.id.tvSelectedFile).text = fileName ?: "Unknown file"
                Toast.makeText(this, "Selected file: $fileName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
        return result
    }

    private fun uploadFile(resourceType: String) {
        val fileUri = selectedFileUri ?: return
        val fileName = getFileName(fileUri) ?: "file_${System.currentTimeMillis()}"
        val inputStream = contentResolver.openInputStream(fileUri)
        val fileData = inputStream?.readBytes() ?: return

        val url = "http://192.168.234.54/UniValut/upload_material.php"

        val request = VolleyFileUpload(
            Request.Method.POST, url,
            {
                showSuccess("Uploaded to $collegeName/$courseCode/$fileName")
                pdfContainer.removeAllViews()
                fetchPDFs(collegeName, courseCode)
            },
            {
                showError("Upload failed: ${it.message}")
            }
        )

        request.setParams(
            mapOf(
                "college" to collegeName,
                "course" to courseCode,
                "type" to resourceType
            )
        )
        request.setFile(fileData, fileName)

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchPDFs(college: String, course: String) {
        val url = "http://192.168.234.54/univault/list_pdfs.php?college=$college&course=$course"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val filesArray = response.getJSONArray("files")
                        for (i in 0 until filesArray.length()) {
                            val obj = filesArray.getJSONObject(i)
                            val name = obj.getString("name")
                            val fileUrl = obj.getString("url")
                            val date = obj.getString("date")
                            addPDFRow(name, fileUrl, date)
                        }
                    } else {
                        Toast.makeText(this, "No files found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(this).add(request)
    }

    private fun addPDFRow(fileName: String, fileUrl: String, uploadDate: String) {
        val inflater = layoutInflater
        val rowView = inflater.inflate(R.layout.item_uploaded_pdf, pdfContainer, false)

        val fileNameText: TextView = rowView.findViewById(R.id.fileNameText)
        val uploadDateText: TextView = rowView.findViewById(R.id.uploadDateText)
        val deleteBtn: ImageButton = rowView.findViewById(R.id.deleteBtn)

        fileNameText.text = fileName
        uploadDateText.text = "Uploaded on: $uploadDate"

        // Open PDF when clicking the row
        rowView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(fileUrl), "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(intent)
        }

        // Delete button click
        deleteBtn.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)

            val alertDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            dialogView.findViewById<TextView>(R.id.dialogMessage).text =
                "Are you sure you want to delete \"$fileName\"?"

            dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                alertDialog.dismiss()
            }

            dialogView.findViewById<Button>(R.id.btnDelete).setOnClickListener {
                deleteFileFromServer(fileName)
                alertDialog.dismiss()
            }

            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alertDialog.show()
        }


        pdfContainer.addView(rowView)
    }


    private fun deleteFileFromServer(fileName: String) {
        val url = "http://192.168.234.54/UniVault/delete_material.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener {
                Toast.makeText(this, "Deleted $fileName", Toast.LENGTH_SHORT).show()
                pdfContainer.removeAllViews()
                fetchPDFs(collegeName, courseCode)
            },
            Response.ErrorListener {
                showError("Deletion failed")
            }) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "college" to collegeName,
                    "course" to courseCode,
                    "file" to fileName
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showSuccess(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
