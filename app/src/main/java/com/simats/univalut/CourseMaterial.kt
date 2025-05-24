package com.simats.univalut

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CourseMaterial : AppCompatActivity() {

    private lateinit var courseCode: String
    private lateinit var courseTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.course_material_view)


        // You can now use courseCode/courseTitle to fetch PDFs for this course
    }
}
