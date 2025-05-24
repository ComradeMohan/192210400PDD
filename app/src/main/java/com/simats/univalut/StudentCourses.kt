package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentCourses : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_courses) // Make sure this is the name of your xml file

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCourses)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Example course list
        val courseList = listOf(
            Course("CSA101", "Introduction to Computer Science", 3),
            Course("MAT201", "Discrete Mathematics", 4),
            Course("PHY102", "Physics for Engineers", 3),
            Course("ENG103", "Technical English", 2)
        )

        // Adapter to display course list
        val adapter = CourseAdapter(courseList) { course ->
            startActivity(Intent(this, CourseMaterialsActivity::class.java))
        }

        recyclerView.adapter = adapter

        // Bottom Navigation setup
//        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
//
//        bottomNavigationView.selectedItemId = R.id.nav_courses

//        bottomNavigationView.setOnItemSelectedListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.nav_home -> {
//                    startActivity(Intent(this, StudentActivity::class.java))
//                    overridePendingTransition(0, 0) // Optional: No animation // You're already in StudentActivity, maybe refresh or do nothing
//                    true
//                }
//
//                R.id.nav_courses -> {
//
//                    true
//                }
//
//                R.id.nav_profile -> {
//                    startActivity(Intent(this, StudentProfileActivity::class.java))
//                    overridePendingTransition(0, 0) // Optional: No animation // You're already in StudentActivity, maybe refresh or do nothing
//                    true
//                }
//
//                R.id.nav_schedule -> {
//                    startActivity(Intent(this, StudentCalender::class.java))
//                    true
//                }
//
//
//                R.id.nav_profile -> {
//                    startActivity(Intent(this, StudentProfileActivity::class.java))
//                    overridePendingTransition(0, 0) // Optional: No animation // You're already in StudentActivity, maybe refresh or do nothing
//                    true
//                }
//
//                else -> false
//            }
//        }
    }
}
