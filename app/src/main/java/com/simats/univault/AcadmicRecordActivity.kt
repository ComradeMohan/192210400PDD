package com.simats.univault

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
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.graphics.Color

class AcadmicRecordActivity : AppCompatActivity() {

    private lateinit var downloadButton: Button
    private lateinit var backButton: ImageView
    private lateinit var pending: ConstraintLayout
    private lateinit var completed: ConstraintLayout
    private lateinit var gradeDistributionLayout: LinearLayout

    private var SID: String? = null
    private var DID: String? = null

    private val client = OkHttpClient()
    private var collegeId: String? = null
    private var departmentName: String? = null
    private var allCourses: Int? = null
    private val courseNames = mutableListOf<String>()
    private val gradePoints = mutableMapOf<String, Int>() // Map to store grade -> points
    private val availableGrades = mutableListOf<String>() // List to store available grades
    private var collegeName : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.academic_record)

        val studentID = intent.getStringExtra("studentID")
        val department = intent.getStringExtra("department")
        collegeName = intent.getStringExtra("collegeName")

        SID = studentID
        departmentName = department

        // Initialize views
        downloadButton = findViewById(R.id.downloadTranscriptButton)
        backButton = findViewById(R.id.backButton)
        pending = findViewById(R.id.pendingCourses)
        completed = findViewById(R.id.completedCourses)
        gradeDistributionLayout = findViewById(R.id.gradeDistributionLayout)

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
        loadAcademicData()
    }

    private fun loadAcademicData() {
        collegeName?.let {
            fetchCollegeId(it)
        }
    }

    override fun onResume() {
        super.onResume()
        loadAcademicData()  // Refresh everything when coming back
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

                    gradePoints.clear()
                    availableGrades.clear()

                    for (i in 0 until jsonArray.length()) {
                        val gradePointObj = jsonArray.getJSONObject(i)
                        val grade = gradePointObj.getString("grade")
                        val points = gradePointObj.getInt("points")
                        gradePoints[grade] = points
                        availableGrades.add(grade)
                    }

                    runOnUiThread {
                        Toast.makeText(this@AcadmicRecordActivity, "Grade points loaded", Toast.LENGTH_SHORT).show()
                        Log.d("GradePoints", gradePoints.toString())

                        // Create dynamic grade distribution UI
                        createDynamicGradeDistribution()

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

    private fun createDynamicGradeDistribution() {
        // Clear existing views
        gradeDistributionLayout.removeAllViews()

        // Define colors for grades (you can customize these)
        val gradeColors = listOf(
            "#375E97", "#FB6542", "#FFBB00", "#CEE6F2",
            "#E3867D", "#2C5F2D", "#8B4513", "#FF6347",
            "#32CD32", "#9370DB"
        )

        availableGrades.forEachIndexed { index, grade ->
            val color = gradeColors.getOrElse(index) { "#808080" } // Default gray if no color defined

            // Inflate the grade item layout
            val gradeItemView = LayoutInflater.from(this).inflate(R.layout.item_grade_progress, gradeDistributionLayout, false)

            val gradeText = gradeItemView.findViewById<TextView>(R.id.gradeText)
            val gradeProgressBar = gradeItemView.findViewById<ProgressBar>(R.id.gradeProgressBar)
            val gradePercentText = gradeItemView.findViewById<TextView>(R.id.gradePercentText)

            // Set grade text and color
            gradeText.text = grade
            gradeText.setTextColor(Color.parseColor(color))

            // Set progress bar color
            gradeProgressBar.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(color))
            gradeProgressBar.progress = 0

            // Set initial percent text
            gradePercentText.text = "$grade: 0%"
            gradePercentText.setTextColor(Color.parseColor(color))

            // Set tags for easy retrieval later
            gradeProgressBar.tag = "progress_$grade"
            gradePercentText.tag = "percent_$grade"

            gradeDistributionLayout.addView(gradeItemView)
        }
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
                            var totalPoints = 0.0

                            for (i in 0 until totalCourses) {
                                val course = coursesArray.getJSONObject(i)
                                val grade = course.getString("grade")
                                val gradePoint = gradePoints[grade] ?: 0

                                // Increment grade count
                                gradeCount[grade] = gradeCount.getOrDefault(grade, 0) + 1
                            }

// New CGPA formula: (sum of (count × gradePoint)) / totalCourses
                            for ((grade, count) in gradeCount) {
                                val points = gradePoints[grade] ?: 0
                                totalPoints += count * points
                            }

                            val cgpa = if (totalCourses > 0) totalPoints / totalCourses else 0.0



                            val cgpaValueTextView = findViewById<TextView>(R.id.cgpaValue)
                            val animator = ValueAnimator.ofFloat(10.00f, cgpa.toFloat())
                            animator.duration = 1500  // 1.5 seconds
                            animator.interpolator = DecelerateInterpolator()
                            animator.addUpdateListener { animation ->
                                val animatedValue = animation.animatedValue as Float
                                cgpaValueTextView.text = String.format("%.2f", animatedValue)
                            }
                            animator.start()

                            // Store CGPA as Float in SharedPreferences
                            val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
                            sf.edit().putFloat("cgpaValue", cgpa.toFloat()).apply()

                            // Update the dynamic Grade Distribution UI
                            updateDynamicGradeDistribution(gradeCount, totalCourses)

                            // Toast for CGPA
                            Toast.makeText(this@AcadmicRecordActivity, "CGPA: %.2f".format(cgpa), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@AcadmicRecordActivity, "No completed courses found", Toast.LENGTH_SHORT).show()
                            // Update pending if 0
                            val pendingCoursesTextView = findViewById<TextView>(R.id.pendingCoursesCount)
                            pendingCoursesTextView.text = allCourses.toString()
                        }
                    }
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateDynamicGradeDistribution(gradeCount: Map<String, Int>, totalCourses: Int) {
        // Update each grade's progress bar and percentage
        availableGrades.forEach { grade ->
            val count = gradeCount[grade] ?: 0
            val percentage = if (totalCourses > 0) (count.toDouble() / totalCourses) * 100 else 0.0

            // Find the progress bar and text view by tag
            val progressBar = gradeDistributionLayout.findViewWithTag<ProgressBar>("progress_$grade")
            val percentText = gradeDistributionLayout.findViewWithTag<TextView>("percent_$grade")

            progressBar?.progress = percentage.toInt()
            percentText?.text = "%.1f%%".format(percentage)
        }
    }
}