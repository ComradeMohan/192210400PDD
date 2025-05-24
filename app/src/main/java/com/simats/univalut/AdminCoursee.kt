package com.simats.univalut

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.simats.univalut.R
class AdminCoursee : AppCompatActivity() {

    private lateinit var btnAddCourse: Button
    private lateinit var layoutAddCourseForm: LinearLayout
    private lateinit var btnSaveCourse: Button

    private lateinit var etCourseCode: EditText
    private lateinit var etSubjectName: EditText
    private lateinit var etStrength: EditText
    private lateinit var spinnerFaculty: Spinner

    private var isFormVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_course_activity)

        // Initialize views
        btnAddCourse = findViewById(R.id.btnAddCourse)
        layoutAddCourseForm = findViewById(R.id.layoutAddCourseForm)
        btnSaveCourse = findViewById(R.id.btnSaveCourse)

        etCourseCode = findViewById(R.id.etCourseCode)
        etSubjectName = findViewById(R.id.etSubjectName)
        etStrength = findViewById(R.id.etStrength)
        spinnerFaculty = findViewById(R.id.spinnerFaculty)

        setupSpinner()

        // Toggle form on button click
        btnAddCourse.setOnClickListener {
            isFormVisible = !isFormVisible
            layoutAddCourseForm.visibility = if (isFormVisible) View.VISIBLE else View.GONE
        }

        // Save course logic (for now just showing Toast)
        btnSaveCourse.setOnClickListener {
            val code = etCourseCode.text.toString()
            val subject = etSubjectName.text.toString()
            val faculty = spinnerFaculty.selectedItem.toString()
            val strength = etStrength.text.toString()

            if (code.isBlank() || subject.isBlank() || strength.isBlank()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Saved: $code - $subject - $faculty - $strength", Toast.LENGTH_LONG).show()

            // You can clear fields or send to backend here
            clearForm()
            layoutAddCourseForm.visibility = View.GONE
            isFormVisible = false
        }
    }

    private fun setupSpinner() {
        val facultyList = listOf("Select Faculty", "Sarah Johnson", "John Doe", "Emily Smith")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, facultyList)
        spinnerFaculty.adapter = adapter
    }

    private fun clearForm() {
        etCourseCode.text.clear()
        etSubjectName.text.clear()
        etStrength.text.clear()
        spinnerFaculty.setSelection(0)
    }
}