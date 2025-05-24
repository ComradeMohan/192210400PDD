package com.simats.univalut

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var etSearch: EditText

    private lateinit var courseCode1: TextView
    private lateinit var courseCode2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_dashboard) // Make sure the XML is named `activity_main.xml`

        // Initialize views
        tvGreeting = findViewById(R.id.tvGreeting)
        tvStudentName = findViewById(R.id.tvStudentName)
        etSearch = findViewById(R.id.etSearch)

        // Optional: Example course codes for both courses (if you want to differentiate them)
        courseCode1 = findViewById(R.id.tvCourseCode1) // You’ll need to assign these IDs in your XML if not already
        courseCode2 = findViewById(R.id.tvCourseCode2)

        // Example dynamic update
        tvGreeting.text = getGreetingMessage()
        tvStudentName.text = "Comrade Mohan" // You could dynamically fetch from login/session



        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // You're already in StudentActivity, maybe refresh or do nothing
                    true
                }
                R.id.nav_courses -> {
                    startActivity(Intent(this, StudentCourses::class.java))
                    overridePendingTransition(0, 0) // Optional: No animation
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, StudentProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_schedule -> {
                    startActivity(Intent(this, StudentCalender::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, StudentProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }




        // You can add logic to handle search or fetch courses here
    }

    private fun getGreetingMessage(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            in 17..20 -> "Good Evening,"
            else -> "Good Night,"
        }
    }
}
