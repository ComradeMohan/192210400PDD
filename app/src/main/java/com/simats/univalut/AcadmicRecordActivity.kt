package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AcadmicRecordActivity : AppCompatActivity() {

    private lateinit var progressS: ProgressBar
    private lateinit var progressA: ProgressBar
    private lateinit var progressB: ProgressBar
    private lateinit var progressC: ProgressBar
    private lateinit var progressD: ProgressBar
    private lateinit var progressE: ProgressBar
    private lateinit var downloadButton: Button
    private lateinit var backButton: ImageView

    private lateinit var pending: ConstraintLayout
    private lateinit var completed: ConstraintLayout

    private var SID: String? = null
    private var DID: String? = null

    private val client = OkHttpClient()
    private var collegeId: String? = null
    private var departmentName: String? = null
    private var allCourses: Int? = null
    private val courseNames = mutableListOf<String>()
    private val gradePoints = mutableMapOf<String, Int>() // Map to store grade -> points

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.academic_record)

        val studentID = intent.getStringExtra("studentID")
        val department = intent.getStringExtra("department")
        val collegeName = intent.getStringExtra("collegeName")

        SID = studentID
        departmentName = department

        // Initialize views
        progressS = findViewById(R.id.progressS)
        progressA = findViewById(R.id.progressA)
        progressB = findViewById(R.id.progressB)
        progressC = findViewById(R.id.progressC)
        progressD = findViewById(R.id.progressD)
        progressE = findViewById(R.id.progressE)
        downloadButton = findViewById(R.id.downloadTranscriptButton)
        backButton = findViewById(R.id.backButton)

        // Set dummy progress values for testing
        progressS.progress = 0
        progressA.progress = 0
        progressB.progress = 0
        progressC.progress = 0
        progressD.progress = 0
        progressE.progress = 0
        pending = findViewById(R.id.pendingCourses)
        completed = findViewById(R.id.completedCourses)

        pending.setOnClickListener {
            if (!SID.isNullOrEmpty() && !DID.isNullOrEmpty()) {
                val intent = Intent(this, StudentGrades::class.java).apply {
                    putExtra("SID", SID)
                    putExtra("DID", DID)
                    putExtra("courseType", "pending")
                    putExtra("COLLEGE_ID", collegeId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this@AcadmicRecordActivity, "Missing Student ID or Department ID", Toast.LENGTH_SHORT).show()
            }
        }

        completed.setOnClickListener {
            if (!SID.isNullOrEmpty() && !DID.isNullOrEmpty()) {
                val intent = Intent(this, StudentGradesCompleted::class.java).apply {
                    putExtra("SID", SID)
                    putExtra("DID", DID)
                    putExtra("courseType", "completed")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this@AcadmicRecordActivity, "Missing Student ID or Department ID", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle button clicks
        downloadButton.setOnClickListener {
            // TODO: Implement download logic (like downloading the transcript)
        }

        backButton.setOnClickListener {
            finish()
        }

        // Fetch the college ID
        collegeName?.let {
            fetchCollegeId(it)
        }
    }
    private fun fetchCollegeId(collegeName: String) {
        val url = "http://10.143.152.54/univault/get_college_id.php"

        val formBody = FormBody.Builder()
            .add("college_name", collegeName)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AcadmicRecordActivity, "Failed to fetch college ID", Toast.LENGTH_SHORT).show()
                    Log.e("CollegeID", "Network Error: ${e.message}", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                try {
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        collegeId = json.getString("college_id")
                        runOnUiThread {
                            Toast.makeText(this@AcadmicRecordActivity, "College ID: $collegeId", Toast.LENGTH_SHORT).show()

                            departmentName?.let {
                                fetchDepartmentId(collegeId!!, it)
                            }

                            // Fetch grade points after getting the college ID

                        }
                    } else {
                        val message = json.optString("message", "College not found")
                        runOnUiThread {
                            Toast.makeText(this@AcadmicRecordActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@AcadmicRecordActivity, "Invalid response format", Toast.LENGTH_SHORT).show()
                        Log.e("CollegeID", "JSON Parsing Error: ${e.message}", e)
                    }
                }
            }
        })
    }
    private fun fetchDepartmentId(collegeId: String, departmentName: String) {
        val url = "http://10.143.152.54/univault/get_department_id.php"

        val formBody = FormBody.Builder()
            .add("college_id", collegeId)
            .add("name", departmentName)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AcadmicRecordActivity, "Failed to fetch department ID", Toast.LENGTH_SHORT).show()
                    Log.e("DepartmentID", "Network Error: ${e.message}", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                try {
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val departmentId = json.getString("department_id")
                        runOnUiThread {
                            Toast.makeText(this@AcadmicRecordActivity, "Department ID: $departmentId", Toast.LENGTH_SHORT).show()
                            DID = departmentId
                            fetchCourses(departmentId)
                            collegeId?.let {
                                fetchGradePoints(it)
                            }
                        }
                    } else {
                        val message = json.optString("message", "Department not found")
                        runOnUiThread {
                            Toast.makeText(this@AcadmicRecordActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@AcadmicRecordActivity, "Invalid department response", Toast.LENGTH_SHORT).show()
                        Log.e("DepartmentID", "JSON Error: ${e.message}", e)
                    }
                }
            }
        })
    }
    private fun fetchCourses(departmentId: String) {
        val url = "http://10.143.152.54/univault/get_courses_by_department.php"

        val formBody = FormBody.Builder()
            .add("department_id", departmentId)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AcadmicRecordActivity, "Failed to fetch courses", Toast.LENGTH_SHORT).show()
                    Log.e("Courses", "Network Error: ${e.message}", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                try {
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val coursesArray = json.getJSONArray("courses")
                        courseNames.clear()
                        for (i in 0 until coursesArray.length()) {
                            val courseObj = coursesArray.getJSONObject(i)
                            val courseName = courseObj.getString("name").trim()
                            courseNames.add(courseName)
                        }
                        runOnUiThread {
                            Toast.makeText(this@AcadmicRecordActivity, "Courses loaded: ${courseNames.size}", Toast.LENGTH_SHORT).show()
                            allCourses = courseNames.size
                            Log.d("Courses", courseNames.joinToString(", "))
                        }
                    } else {
                        val message = json.optString("message", "No courses found")
                        runOnUiThread {
                            Toast.makeText(this@AcadmicRecordActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@AcadmicRecordActivity, "Error parsing courses", Toast.LENGTH_SHORT).show()
                        Log.e("Courses", "JSON Error: ${e.message}", e)
                    }
                }
            }
        })
    }
    private fun fetchGradePoints(collegeId: String) {
        val url = "http://10.143.152.54/univault/get_grade_points.php?college_id=$collegeId"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AcadmicRecordActivity, "Failed to fetch grade points", Toast.LENGTH_SHORT).show()
                    Log.e("GradePoints", "Network Error: ${e.message}", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                try {
                    val jsonArray = JSONArray(responseBody)

                    for (i in 0 until jsonArray.length()) {
                        val gradePointObj = jsonArray.getJSONObject(i)
                        val grade = gradePointObj.getString("grade")
                        val points = gradePointObj.getInt("points")
                        gradePoints[grade] = points
                    }

                    runOnUiThread {
                        Toast.makeText(this@AcadmicRecordActivity, "Grade points loaded", Toast.LENGTH_SHORT).show()
                        Log.d("GradePoints", gradePoints.toString())
                        if (SID != null && DID != null) {
                            fetchCompletedCourses(SID!!, DID!!)
                        } else {
                            Toast.makeText(this@AcadmicRecordActivity, "Student or Department ID is null", Toast.LENGTH_SHORT).show()
                        }


                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@AcadmicRecordActivity, "Error parsing grade points", Toast.LENGTH_SHORT).show()
                        Log.e("GradePoints", "JSON Error: ${e.message}", e)
                    }
                }
            }
        })
    }
    private fun fetchCompletedCourses(studentId: String, departmentId: String) {
        val url = "http://10.143.152.54/univault/student_grades_completed.php?student_id=$studentId&department_id=$departmentId"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AcadmicRecordActivity, "Error fetching data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)

                    runOnUiThread {
                        if (jsonResponse.getBoolean("success")) {
                            val coursesArray: JSONArray = jsonResponse.getJSONArray("courses")
                            var totalPoints = 0.0
                            var totalCredits = 0.0
                            val gradeCount = mutableMapOf<String, Int>()
                            val totalCourses = coursesArray.length()
                            val completedCoursesCountTextView = findViewById<TextView>(R.id.completedCoursesCount)
                            completedCoursesCountTextView.text = coursesArray.length().toString()

                            val degreeProgress = findViewById<ProgressBar>(R.id.degreeProgressBar)


                            val degreeProgressPercentage = findViewById<TextView>(R.id.degreeProgressPercentage)

                            val pendingCoursesTextView = findViewById<TextView>(R.id.pendingCoursesCount)



                            if (allCourses != null && allCourses!! > 0) {
                                degreeProgress.progress = coursesArray.length()*100/ allCourses!!
                                val percentage = (coursesArray.length() * 100 / allCourses!!)
                                degreeProgressPercentage.text = "$percentage%"

                                val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
                                sf.edit().putInt("degreeProgress", percentage).apply()

                                pendingCoursesTextView.text = (allCourses?.minus(totalCourses)).toString()
                            } else {
                                Toast.makeText(this@AcadmicRecordActivity, "Total courses not loaded yet", Toast.LENGTH_SHORT).show()
                            }



                            // Iterate through courses to calculate total points, credits, and grade counts
                            for (i in 0 until totalCourses) {
                                val course = coursesArray.getJSONObject(i)
                                val grade = course.getString("grade")
                                val credits = course.getInt("credits")
                                val gradePoint = gradePoints[grade] ?: 0
                                totalPoints += gradePoint * credits
                                totalCredits += credits

                                // Increment grade count
                                gradeCount[grade] = gradeCount.getOrDefault(grade, 0) + 1
                            }

                            val cgpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
                            val cgpaValueTextView = findViewById<TextView>(R.id.cgpaValue)
                            cgpaValueTextView.text = "%.2f".format(cgpa)

// Store CGPA as Float in SharedPreferences
                            val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
                            sf.edit().putFloat("cgpaValue", cgpa.toFloat()).apply()

                            // Update the Grade Distribution UI dynamically
                            // Iterate through the grade counts map
                            gradeCount.forEach { (grade, count) ->
                                // Calculate percentage for each grade
                                val gradePercentage = (count.toDouble() / totalCourses) * 100

                                // Find the ProgressBar and TextView for each grade dynamically
                                val progressBar = findViewById<ProgressBar>(
                                    resources.getIdentifier("progress$grade", "id", packageName)
                                )
                                val gradePercentText = findViewById<TextView>(
                                    resources.getIdentifier("${grade.lowercase()}GradePercentText", "id", packageName)

                                )

                                // Update the ProgressBar and TextView with the calculated values
                                progressBar.progress = gradePercentage.toInt()
                                gradePercentText.text = "$grade: %.2f%%".format(gradePercentage)
                            }

                            // Toast for CGPA
                            Toast.makeText(this@AcadmicRecordActivity, "CGPA: %.2f".format(cgpa), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@AcadmicRecordActivity, "No completed courses found", Toast.LENGTH_SHORT).show()

                            // upadte pending if 0
                            val pendingCoursesTextView = findViewById<TextView>(R.id.pendingCoursesCount)
                            pendingCoursesTextView.text = allCourses.toString()
                        }
                    }


                }
            }
        })
    }


}
