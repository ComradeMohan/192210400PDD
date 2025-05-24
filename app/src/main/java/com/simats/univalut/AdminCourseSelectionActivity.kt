package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AdminCourseSelectionActivity : AppCompatActivity() {

    private lateinit var adapter: FacultyCourseAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.faculty_courses)

        recyclerView = findViewById(R.id.courseRecyclerView)

        val courseList = listOf(
            FacultyCourse("CS101", "Introduction to Computer Science", "Dr. Sarah Johnson"),
            FacultyCourse("MATH201", "Advanced Calculus", "Prof. Michael Chen"),
            FacultyCourse("PHY301", "Quantum Physics", "Dr. Emily Williams"),
            FacultyCourse("ENG202", "Technical Writing", "Prof. David Miller"),
            FacultyCourse("BIO101", "General Biology", "Dr. Lisa Thompson")
        )

        adapter = FacultyCourseAdapter(courseList) { selectedCourse ->
            Toast.makeText(this, "Selected: ${selectedCourse.title}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            // You can access the selected course via the adapter or external tracking
            Toast.makeText(this, "Proceeding to next step...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AdminCourseUpload::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}
