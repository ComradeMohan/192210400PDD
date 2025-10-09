package com.simats.univault

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class StudentGrades : Activity() {
    private lateinit var courseContainer: LinearLayout
    private lateinit var btnSubmit: Button
    private lateinit var etnSearch: EditText
    private var SID = ""
    private var DID = ""
    private var COLLEGE_ID = ""
    private var courseType = ""
    private val courseGradeMap = mutableMapOf<String, String>()
    private var gradeOptions = listOf<String>()  // Grades from server
    private val allCourses = mutableListOf<Triple<String, String, String>>()  // Triple<courseId, name, credits>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_grades)

        // Get intent extras
        SID = intent.getStringExtra("SID") ?: return
        DID = intent.getStringExtra("DID") ?: return
        COLLEGE_ID = intent.getStringExtra("COLLEGE_ID") ?: return
        courseType = intent.getStringExtra("courseType") ?: "pending"

        courseContainer = findViewById(R.id.courseContainer)
        btnSubmit = findViewById(R.id.btnSubmit)
        etnSearch = findViewById(R.id.etSearch)

        // Fetch grades and courses
        fetchGradesFromServer(COLLEGE_ID)

        btnSubmit.setOnClickListener {
            if (courseGradeMap.isEmpty()) {
                Toast.makeText(this, "Please select grades before submitting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitGrades()
        }

        etnSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val searchTerm = s.toString().trim().lowercase()
                val filteredCourses = if (searchTerm.isEmpty()) {
                    allCourses
                } else {
                    allCourses.filter { it.second.lowercase().contains(searchTerm) || it.first.lowercase().contains(searchTerm) }
                }
                displayCourses(filteredCourses)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun submitGrades() {
        thread {
            var allSuccess = true
            for ((courseId, grade) in courseGradeMap) {
                try {
                    val url = URL("http://10.86.199.54/univault/submit_student_grades.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    val postData = "student_id=$SID&course_id=$courseId&grade=${URLEncoder.encode(grade, "UTF-8")}"
                    conn.outputStream.write(postData.toByteArray())
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    if (!json.optBoolean("success")) {
                        allSuccess = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    allSuccess = false
                }
            }
            runOnUiThread {
                if (allSuccess) {
                    Toast.makeText(this, "Grades submitted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Some grades failed to submit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayCourses(courses: List<Triple<String, String, String>>) {
        courseContainer.removeAllViews()
        for ((id, name, credits) in courses) {
            addCourseCard(id, name, credits)
        }
    }

    private fun fetchGradesFromServer(collegeId: String) {
        val urlStr = "http://10.86.199.54/univault/fetch_grades.php?college_id=$collegeId"
        thread {
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val grades = json.getJSONArray("grades")
                val gradeList = mutableListOf<String>()
                for (i in 0 until grades.length()) {
                    val gradeObj = grades.getJSONObject(i)
                    gradeList.add(gradeObj.getString("grade"))
                }
                gradeOptions = listOf("Select") + gradeList
                runOnUiThread {
                    fetchPendingCourses(SID, DID)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to load grades", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchPendingCourses(studentId: String, departmentId: String) {
        val urlStr = "http://10.86.199.54/univault/student_grades_pending.php?department_id=$departmentId&student_id=$studentId"
        thread {
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val courses = json.getJSONArray("courses")
                allCourses.clear()  // Clear old data before adding fresh data
                for (i in 0 until courses.length()) {
                    val course = courses.getJSONObject(i)
                    val courseId = course.getString("id")
                    val courseName = course.getString("name").trim()
                    val credits = course.getString("credits")
                    allCourses.add(Triple(courseId, courseName, credits))  // Store for filtering
                }
                runOnUiThread {
                    displayCourses(allCourses)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error fetching courses", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addCourseCard(courseId: String, name: String, credits: String) {
        val inflater = layoutInflater
        val card = inflater.inflate(R.layout.course_card_layout, null) as CardView
        val courseTitle = card.findViewById<TextView>(R.id.courseTitle)
        val courseCode = card.findViewById<TextView>(R.id.courseCode)
        val spinnerGrade = card.findViewById<Spinner>(R.id.spinnerGrade)

        courseTitle.text = name
        courseCode.text = "Credits: $credits"

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gradeOptions)
        spinnerGrade.adapter = adapter

        spinnerGrade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = gradeOptions[position]
                if (selected != "Select") {
                    courseGradeMap[courseId] = selected
                } else {
                    courseGradeMap.remove(courseId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        courseContainer.addView(card)

        val divider = View(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1
        )
        params.setMargins(0, 8, 0, 8)
        divider.layoutParams = params
        divider.setBackgroundColor(resources.getColor(android.R.color.transparent))
        courseContainer.addView(divider)
    }
}
