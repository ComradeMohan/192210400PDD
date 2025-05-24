package com.simats.univalut

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast





class StudentProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var departmentTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var regNoTextView: TextView
    private lateinit var yearTextView: TextView
    private lateinit var joinedDateTextView: TextView
    private lateinit var academicRecordsButton: LinearLayout
    private lateinit var helpSupportButton: LinearLayout
    private lateinit var settingsButton: LinearLayout
    private lateinit var logoutButton: LinearLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView)
        nameTextView = findViewById(R.id.nameTextView)
        statusTextView = findViewById(R.id.statusTextView)
        departmentTextView = findViewById(R.id.departmentTextView)
        emailTextView = findViewById(R.id.emailTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        regNoTextView = findViewById(R.id.regNoTextView)
        yearTextView = findViewById(R.id.yearTextView)
        academicRecordsButton = findViewById(R.id.academicRecordsButton)
        helpSupportButton = findViewById(R.id.helpSupportButton)
        settingsButton = findViewById(R.id.settingsButton)
        logoutButton = findViewById(R.id.logoutButton)

        // Set sample data (replace with actual data fetching)
        nameTextView.text = "Comrade Mohan"
        statusTextView.text = "Student"
        departmentTextView.text = "Computer Science"
        emailTextView.text = "madiremohanreddy0400.sse@saveetha.com"
        phoneTextView.text = "+91 6281359314"
        regNoTextView.text = "Reg No: 192210400"
        yearTextView.text = "Year 3"

        // Set click listeners for the buttons
        academicRecordsButton.setOnClickListener {
            Toast.makeText(this, "Academic Records Clicked", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AcadmicRecordActivity::class.java)
            startActivity(intent)
            // Add your logic to navigate to academic records screen
        }

        helpSupportButton.setOnClickListener {
            Toast.makeText(this, "Help & Support Clicked", Toast.LENGTH_SHORT).show()
            // Add your logic to navigate to help & support screen
        }

        settingsButton.setOnClickListener {
            Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show()
            // Add your logic to navigate to settings screen
        }

        logoutButton.setOnClickListener {
            Toast.makeText(this, "Logout Clicked", Toast.LENGTH_SHORT).show()
            // Add your logout logic here (e.g., clearing session, navigating to login)
        }




    }
}