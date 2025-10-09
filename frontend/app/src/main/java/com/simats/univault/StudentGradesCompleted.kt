package com.simats.univault

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
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

        listView = findViewById(R.id.coursesListView)
        etSearch = findViewById(R.id.etSearch)

        SID = intent.getStringExtra("SID") ?: ""
        DID = intent.getStringExtra("DID") ?: ""
        courseType = intent.getStringExtra("courseType") ?: ""

        adapter = CompletedCoursesAdapter(this, filteredCourses, object : CompletedCoursesAdapter.OnDeleteClickListener {
            override fun onDeleteClicked(position: Int) {
                val courseToDelete = filteredCourses[position]
                AlertDialog.Builder(this@StudentGradesCompleted)
                    .setTitle("Delete Course")
                    .setMessage("Delete ${courseToDelete.name}?")
                    .setPositiveButton("Yes") { _, _ ->
                        deleteCourseFromServer(courseToDelete, position)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        })

        listView.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterCourses(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fetchCompletedCourses(SID, DID)
    }

    private fun deleteCourseFromServer(course: CompletedCourse, position: Int) {
        val url = "http://10.86.199.54/univault/delete_stdudent_course.php"
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("student_id", SID)
            .add("course_id", course.courseId)
            .build()

        val request = Request.Builder().url(url).post(formBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@StudentGradesCompleted, "Server error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseBody ?: "")
                        if (json.getBoolean("success")) {
                            val courseToRemove = filteredCourses[position]
                            completedCourses.remove(courseToRemove)
                            filteredCourses.removeAt(position)
                            adapter.notifyDataSetChanged()
                            Toast.makeText(this@StudentGradesCompleted, "Deleted successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@StudentGradesCompleted, "Delete failed: ${json.getString("error")}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@StudentGradesCompleted, "Invalid server response", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun filterCourses(searchTerm: String) {
        val lowerSearchTerm = searchTerm.lowercase().trim()
        filteredCourses.clear()

        val results = if (lowerSearchTerm.isEmpty()) {
            completedCourses
        } else {
            completedCourses.filter {
                it.courseId.lowercase().contains(lowerSearchTerm) ||
                        it.name.lowercase().contains(lowerSearchTerm) ||
                        it.grade.lowercase().contains(lowerSearchTerm)
            }
        }

        filteredCourses.addAll(results)
        adapter.notifyDataSetChanged()
    }

    private fun fetchCompletedCourses(studentId: String, departmentId: String) {
        val url = "http://10.86.199.54/univault/student_grades_completed.php?student_id=$studentId&department_id=$departmentId"
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

                                val courseId = course.getString("id") // only for backend use
                                val courseName = course.getString("name") // clean name shown
                                val grade = course.getString("grade")

                                completedCourses.add(CompletedCourse(courseId, courseName, grade))
                            }
                            filterCourses("") // show full list initially
                        } else {
                            Toast.makeText(this@StudentGradesCompleted, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}
