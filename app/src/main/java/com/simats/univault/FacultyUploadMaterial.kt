package com.simats.univault

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class FacultyUploadMaterial : AppCompatActivity() {

    private val FILE_REQUEST_CODE = 1001

    private val selectedFileUris = mutableListOf<Uri>()
    private val selectedFileNames = mutableListOf<String>()

    private lateinit var collegeName: String
    private lateinit var courseCode: String
    private lateinit var pdfContainer: LinearLayout
    private lateinit var selectedFilesContainer: LinearLayout
    private lateinit var tvSelectType: TextView
    private lateinit var submitButton: Button
    private lateinit var uploadArea: LinearLayout

    // Progress tracking variables
    private var uploadSuccessCount = 0
    private var totalFilesToUpload = 0
    private var currentFileIndex = 0

    // Progress UI elements
    private var progressDialog: ProgressDialog? = null
    private var uploadProgressBar: ProgressBar? = null
    private var uploadStatusText: TextView? = null
    private var progressContainer: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.upload_material)

        collegeName = intent.getStringExtra("COLLEGE_NAME") ?: return
        courseCode = intent.getStringExtra("COURSE_CODE") ?: return

        initializeViews()
        setupClickListeners()
        fetchPDFs(collegeName, courseCode)
    }

    private fun initializeViews() {
        val courseCodeTextView: TextView = findViewById(R.id.course_code)
        courseCodeTextView.text = courseCode

        tvSelectType = findViewById(R.id.tvSelectType)
        uploadArea = findViewById(R.id.uploadArea)
        submitButton = findViewById(R.id.btnSubmitResources)
        val backButton: ImageView = findViewById(R.id.backButton)
        pdfContainer = findViewById(R.id.pdfContainer)
        selectedFilesContainer = findViewById(R.id.selectedFilesContainer)

        // Initialize progress UI elements (assuming you add them to your layout)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)
        uploadStatusText = findViewById(R.id.uploadStatusText)
        progressContainer = findViewById(R.id.progressContainer)

        // Hide progress initially
        progressContainer?.visibility = View.GONE
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvSelectType.setOnClickListener {
            val items = arrayOf("Lecture Notes", "Assignments", "PDF Resources")
            AlertDialog.Builder(this)
                .setTitle("Select Resource Type")
                .setItems(items) { _, which -> tvSelectType.text = items[which] }
                .show()
        }

        uploadArea.setOnClickListener { openFilePicker() }

        submitButton.setOnClickListener {
            val selectedType = tvSelectType.text.toString()
            when {
                selectedType == "Select type" || selectedType.isBlank() ->
                    showError("Please select a resource type.")
                selectedFileUris.isEmpty() ->
                    showError("Please select at least one file.")
                else -> {
                    startUploadProcess(selectedType)
                }
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, "Select files"), FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedFileUris.clear()
            selectedFileNames.clear()

            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val fileUri = data.clipData!!.getItemAt(i).uri
                    selectedFileUris.add(fileUri)
                    selectedFileNames.add(getFileName(fileUri) ?: "file_${System.currentTimeMillis()}")
                }
            } else if (data?.data != null) {
                val fileUri = data.data!!
                selectedFileUris.add(fileUri)
                selectedFileNames.add(getFileName(fileUri) ?: "file_${System.currentTimeMillis()}")
            }

            displaySelectedFiles()
        }
    }

    private fun displaySelectedFiles() {
        selectedFilesContainer.removeAllViews()

        for (i in selectedFileUris.indices) {
            val row = layoutInflater.inflate(R.layout.item_selected_file, selectedFilesContainer, false)

            val fileNameTv = row.findViewById<TextView>(R.id.fileNameTextView)
            val fileIcon = row.findViewById<ImageView>(R.id.file_icon)
            val renameBtn = row.findViewById<ImageButton>(R.id.renameBtn)
            val removeBtn = row.findViewById<ImageButton>(R.id.removeBtn)

            fileNameTv.text = selectedFileNames[i]
            fileIcon.setImageResource(FileUtils.getFileIconResource(selectedFileNames[i]))

            renameBtn.setOnClickListener {
                val input = EditText(this)
                input.setText(selectedFileNames[i])

                AlertDialog.Builder(this)
                    .setTitle("Rename File")
                    .setView(input)
                    .setPositiveButton("OK") { dialog, _ ->
                        val proposed = input.text.toString().trim()
                        if (proposed.isNotEmpty()) {
                            val original = selectedFileNames[i]
                            val ext = original.substringAfterLast('.', missingDelimiterValue = "")
                            val newName = if (!proposed.contains('.') && ext.isNotEmpty())
                                "$proposed.$ext" else proposed

                            selectedFileNames[i] = newName
                            fileNameTv.text = newName
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
            }

            removeBtn.setOnClickListener {
                selectedFileUris.removeAt(i)
                selectedFileNames.removeAt(i)
                displaySelectedFiles()
            }

            selectedFilesContainer.addView(row)
        }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
        return null
    }

    /**
     * Start the upload process with progress tracking
     */
    private fun startUploadProcess(resourceType: String) {
        // Initialize progress tracking
        uploadSuccessCount = 0
        totalFilesToUpload = selectedFileUris.size
        currentFileIndex = 0

        // Disable UI during upload
        submitButton.isEnabled = false
        uploadArea.isEnabled = false

        // Show progress UI
        showProgressUI()

        // Start uploading
        uploadFilesSequentially(resourceType, 0)
    }

    /**
     * Show progress UI (both dialog and inline progress)
     */
    private fun showProgressUI() {
        // Option 1: Progress Dialog (classic approach)
        progressDialog = ProgressDialog(this).apply {
            setTitle("Uploading Files")
            setMessage("Preparing upload...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = totalFilesToUpload
            progress = 0
            setCancelable(false)
            show()
        }

        // Option 2: Inline Progress (modern approach - uncomment if you prefer this)
        /*
        progressContainer?.visibility = View.VISIBLE
        uploadProgressBar?.apply {
            max = totalFilesToUpload * 100 // For percentage calculation
            progress = 0
        }
        uploadStatusText?.text = "Preparing upload..."
        */
    }

    /**
     * Update progress during upload
     */
    private fun updateProgress(fileIndex: Int, fileName: String) {
        val progressPercentage = ((fileIndex.toFloat() / totalFilesToUpload) * 100).toInt()

        // Update progress dialog
        progressDialog?.apply {
            progress = fileIndex
            setMessage("Uploading: $fileName\n(${fileIndex}/$totalFilesToUpload)")
        }

        // Update inline progress (if using)
        uploadProgressBar?.progress = fileIndex * 100
        uploadStatusText?.text = "Uploading: $fileName\n$progressPercentage% complete"
    }

    /**
     * Hide progress UI
     */
    private fun hideProgressUI() {
        progressDialog?.dismiss()
        progressDialog = null

        progressContainer?.visibility = View.GONE

        // Re-enable UI
        submitButton.isEnabled = true
        uploadArea.isEnabled = true
    }

    /**
     * Upload files sequentially with progress tracking
     */
    private fun uploadFilesSequentially(resourceType: String, index: Int) {
        if (index >= selectedFileUris.size) {
            handleUploadCompletion()
            return
        }

        currentFileIndex = index
        val fileName = selectedFileNames[index]

        // Update progress
        updateProgress(index + 1, fileName)

        uploadSingleFile(resourceType, selectedFileUris[index], fileName) { success ->
            if (success) {
                uploadSuccessCount++
            }

            // Small delay to show progress (optional, for better UX)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                uploadFilesSequentially(resourceType, index + 1)
            }, 500) // 500ms delay between uploads
        }
    }

    /**
     * Handle upload completion
     */
    private fun handleUploadCompletion() {
        hideProgressUI()

        if (uploadSuccessCount > 0) {
            // Clear selections
            selectedFileUris.clear()
            selectedFileNames.clear()
            selectedFilesContainer.removeAllViews()

            // Refresh PDF list
            pdfContainer.removeAllViews()
            fetchPDFs(collegeName, courseCode)

            // Show success message
            showUploadCompletionDialog()
        } else {
            showError("No files were uploaded successfully")
        }
    }

    /**
     * Show upload completion dialog with results
     */
    private fun showUploadCompletionDialog() {
        val message = if (uploadSuccessCount == totalFilesToUpload) {
            "✓ Successfully uploaded all $totalFilesToUpload file(s)"
        } else {
            "✓ Successfully uploaded $uploadSuccessCount of $totalFilesToUpload file(s)"
        }

        AlertDialog.Builder(this)
            .setTitle("Upload Complete")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // Reset type selection
                tvSelectType.text = "Select type"
            }
            .show()
    }

    /**
     * Upload single file with enhanced error handling
     */
    private fun uploadSingleFile(resourceType: String, fileUri: Uri, displayName: String, callback: (Boolean) -> Unit) {
        val originalName = getFileName(fileUri) ?: displayName
        val originalExt = originalName.substringAfterLast('.', missingDelimiterValue = "")
        val finalName = if (!displayName.contains('.') && originalExt.isNotEmpty())
            "$displayName.$originalExt" else displayName

        val inputStream = contentResolver.openInputStream(fileUri)
        val fileData = inputStream?.readBytes() ?: run {
            showError("Unable to read $finalName")
            callback(false)
            return
        }

        val url = "http://192.168.56.1/UniVault/upload_material.php"

        // Create request with timeout for better network handling
        val request = VolleyFileUpload(
            Request.Method.POST, url,
            { response ->
                callback(true)
            },
            { error ->
                val errorMsg = when {
                    error.networkResponse?.statusCode == 413 -> "File too large: $finalName"
                    error.networkResponse?.statusCode == 500 -> "Server error uploading: $finalName"
                    error.message?.contains("timeout", true) == true -> "Upload timeout: $finalName"
                    else -> "Upload failed: $finalName (${error.message})"
                }

                // Don't show individual error dialogs during batch upload
                // Just log or show in progress
                progressDialog?.setMessage("Error: $errorMsg")

                callback(false)
            }
        )

        // Set timeout for network requests (in milliseconds)
        request.setRetryPolicy(
            com.android.volley.DefaultRetryPolicy(
                30000, // 30 second timeout
                2, // retry attempts
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        )

        request.setParams(
            mapOf(
                "college" to collegeName,
                "course" to courseCode,
                "type" to resourceType
            )
        )
        request.setFile(fileData, finalName)

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchPDFs(college: String, course: String) {
        pdfContainer.removeAllViews()
        val url = "http://192.168.56.1/univault/list_pdfs.php?college=$college&course=$course"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
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
            }
        )

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

        rowView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(fileUrl), "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(intent)
        }

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
        val url = "http://192.168.56.1/UniVault/delete_material.php"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener {
                Toast.makeText(this, "Deleted $fileName", Toast.LENGTH_SHORT).show()
                pdfContainer.removeAllViews()
                fetchPDFs(collegeName, courseCode)
            },
            Response.ErrorListener {
                showError("Deletion failed")
            }
        ) {
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