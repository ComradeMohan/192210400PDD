package com.simats.univalut

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class StudentGradesCompleted : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var etSearch: EditText
    private lateinit var courseType: String
    private lateinit var SID: String
    private lateinit var DID: String

    private var completedCourses: MutableList<CompletedCourse> = mutableListOf()
    private var filteredCourses: MutableList<CompletedCourse> = mutableListOf()
    private lateinit var adapter: CompletedCoursesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_grades_completed)

        // Initialize views
        listView = findViewById(R.id.coursesListView)
        etSearch = findViewById(R.id.etSearch) // Make sure you have an EditText with this ID in your layout!

        // Retrieve passed data
        SID = intent.getStringExtra("SID") ?: ""
        DID = intent.getStringExtra("DID") ?: ""
        courseType = intent.getStringExtra("courseType") ?: ""

        // Set up the adapter
        adapter = CompletedCoursesAdapter(this, filteredCourses)
        listView.adapter = adapter

        // Search filter listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val searchTerm = s.toString().trim().lowercase()
                filterCourses(searchTerm)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Fetch completed courses from the backend
        fetchCompletedCourses(SID, DID)
    }

    private fun filterCourses(searchTerm: String) {
        filteredCourses.clear()
        if (searchTerm.isEmpty()) {
            filteredCourses.addAll(completedCourses)
        } else {
            for (course in completedCourses) {
                if (course.name.lowercase().contains(searchTerm) ||
                    course.grade.lowercase().contains(searchTerm)) {
                    filteredCourses.add(course)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun fetchCompletedCourses(studentId: String, departmentId: String) {
        val url = "http://192.168.205.54/univault/student_grades_completed.php?student_id=$studentId&department_id=$departmentId"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@StudentGradesCompleted, "Error fetching data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)
                    runOnUiThread {
                        if (jsonResponse.getBoolean("success")) {
                            val coursesArray: JSONArray = jsonResponse.getJSONArray("courses")
                            completedCourses.clear()
                            for (i in 0 until coursesArray.length()) {
                                val course = coursesArray.getJSONObject(i)
                                val courseName = course.getString("name")
                                val grade = course.getString("grade")
                                completedCourses.add(CompletedCourse(courseName, grade))
                            }
                            // Initially show all courses
                            filteredCourses.clear()
                            filteredCourses.addAll(completedCourses)
                            adapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(this@StudentGradesCompleted, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}
