package com.simats.univalut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.simats.univalut.R.*

class AdminCourseUpload: AppCompatActivity() {

    // Sample data class for files
    data class FileItem(val name: String, val size: String, val iconRes: Int)

    // Sample file list
    private val files = mutableListOf(
        FileItem("Lecture Notes.pdf", "2.4 MB", drawable.ic_file_pdf),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.admin_course_upload)

        // Back arrow click
        findViewById<ImageView>(id.back_arrow).setOnClickListener {
            finish()
        }

        // Upload button click
        findViewById<Button>(id.upload_button).setOnClickListener {
            Toast.makeText(this, "Uploading files...", Toast.LENGTH_SHORT).show()
            // Implement your upload logic here
        }

        // Cancel button click
        findViewById<Button>(id.cancel_button).setOnClickListener {
            finish()
        }

        // Populate file list
        val fileListLayout = findViewById<LinearLayout>(id.file_list_layout)
        fileListLayout.removeAllViews()
        for ((index, file) in files.withIndex()) {
            val fileView = LayoutInflater.from(this)
                .inflate(layout.item_file, fileListLayout, false)
            fileView.findViewById<ImageView>(id.file_icon).setImageResource(file.iconRes)
            fileView.findViewById<TextView>(id.file_name).text = file.name
            fileView.findViewById<TextView>(id.file_size).text = file.size
            fileView.findViewById<ImageView>(id.file_delete).setOnClickListener {
                files.removeAt(index)
                fileListLayout.removeViewAt(index)
            }
            fileListLayout.addView(fileView)
        }

        // Upload box click (simulate file picker)
        findViewById<LinearLayout>(id.upload_box).setOnClickListener {
            Toast.makeText(this, "Open file picker...", Toast.LENGTH_SHORT).show()
            // Implement file picker logic here
        }
    }
}