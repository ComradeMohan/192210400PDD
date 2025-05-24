package com.simats.univalut


import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class AdminDashboard: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard1) // Replace with your actual layout XML name

        // Find the quick action buttons
        val btnPostNotice = findViewById<LinearLayout>(R.id.btnPostNotice)
        val btnUploadFiles = findViewById<LinearLayout>(R.id.btnUploadFiles)

        // Set click listeners
        btnPostNotice.setOnClickListener {
            Toast.makeText(this, "Post Notice Clicked", Toast.LENGTH_SHORT).show()
            // TODO: Launch your Post Notice activity/fragment here
        }

        btnUploadFiles.setOnClickListener {
            Toast.makeText(this, "Upload Files Clicked", Toast.LENGTH_SHORT).show()
            // TODO: Launch your Upload Files activity/fragment here
        }
    }
}