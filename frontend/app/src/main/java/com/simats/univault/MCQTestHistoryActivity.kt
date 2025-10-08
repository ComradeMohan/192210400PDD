package com.simats.univault

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException

class MCQTestHistoryActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var backButton: ImageButton
    private lateinit var courseTitle: TextView
    private lateinit var testHistoryList: LinearLayout
    private lateinit var loadingProgress: ProgressBar
    private lateinit var noTestsText: TextView
    
    // Data
    private var courseId: String=""
    private var studentId: Int = 0
    private var courseName: String = ""
    
    data class TestHistoryItem(
        val testResultId: Int,
        val testType: String, // "MCQ" or "Theory"
        val score: Int,
        val totalQuestions: Int,
        val percentage: Double,
        val timeTaken: Int?,
        val testDate: String
    ) {
        // Calculate grade from percentage
        val grade: String
            get() = when {
                percentage >= 90 -> "A+"
                percentage >= 80 -> "A"
                percentage >= 70 -> "B+"
                percentage >= 60 -> "B"
                percentage >= 50 -> "C+"
                percentage >= 40 -> "C"
                percentage >= 30 -> "D"
                else -> "F"
            }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mcq_test_history)
        
        // Get intent data
        courseId = intent.getStringExtra("courseCode") ?: ""
        studentId = intent.getIntExtra("studentId", 0)
        courseName = intent.getStringExtra("courseName") ?: "Unknown Course"

        val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
        sf.edit().putBoolean("isLoggedIn", true).apply()
        sf.edit().putInt("userID", studentId.toInt()).apply()
        
        // Log the received data for debugging
        Log.d("MCQTestHistoryActivity", "Received - courseId: '$courseId', studentId: $studentId, courseName: '$courseName'")
        
        // Initialize UI elements
        initializeViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Load test history
        loadTestHistory()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        courseTitle = findViewById(R.id.courseTitle)
        testHistoryList = findViewById(R.id.testHistoryList)
        loadingProgress = findViewById(R.id.loadingProgress)
        noTestsText = findViewById(R.id.noTestsText)
        
        courseTitle.text = "$courseName - Test History"
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun loadTestHistory() {
        if (courseId.isEmpty() || studentId <= 0) {
            Toast.makeText(this, "Invalid course or student - Course: '$courseId', Student: $studentId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val url = "http://10.86.199.54/univault/get_combined_test_history.php?student_id=$studentId&course_id=$courseId" 
        Log.d("MCQTestHistoryActivity", "Loading combined test history from: $url")
        
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val testHistoryArray = response.getJSONArray("test_history")
                        displayTestHistory(testHistoryArray)
                    } else {
                        showNoTestsMessage()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(this, "Error parsing test history: ${e.message}", Toast.LENGTH_LONG).show()
                    showNoTestsMessage()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
                showNoTestsMessage()
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    private fun displayTestHistory(testHistoryArray: org.json.JSONArray) {
        loadingProgress.visibility = View.GONE
        
        if (testHistoryArray.length() == 0) {
            showNoTestsMessage()
            return
        }
        
        testHistoryList.removeAllViews()
        
        for (i in 0 until testHistoryArray.length()) {
            val testObj = testHistoryArray.getJSONObject(i)
            val testHistoryItem = TestHistoryItem(
                testResultId = testObj.getInt("id"),
                testType = testObj.getString("test_type"),
                score = testObj.getInt("total_score"),
                totalQuestions = testObj.getInt("total_questions"),
                percentage = testObj.getDouble("percentage"),
                timeTaken = if (testObj.isNull("time_taken")) null else testObj.getInt("time_taken"),
                testDate = testObj.getString("test_date")
            )
            
            createTestHistoryItemView(testHistoryItem, i + 1)
        }
    }
    
    private fun createTestHistoryItemView(testItem: TestHistoryItem, testNumber: Int) {
        val inflater = LayoutInflater.from(this)
        val testView = inflater.inflate(R.layout.item_test_history, testHistoryList, false)
        
        // Set test number and date with test type
        val testNumberText = testView.findViewById<TextView>(R.id.testNumberText)
        testNumberText.text = "${testItem.testType} Test #$testNumber"
        
        testView.findViewById<TextView>(R.id.testDateText).text = formatDate(testItem.testDate)
        
        // Set score and percentage
        testView.findViewById<TextView>(R.id.scoreText).text = "${testItem.score}/${testItem.totalQuestions}"
        testView.findViewById<TextView>(R.id.percentageText).text = "${testItem.percentage}%"
        
        // Set time taken (handle null for theory tests)
        val timeText = testView.findViewById<TextView>(R.id.timeText)
        timeText.text = if (testItem.timeTaken != null) {
            formatTime(testItem.timeTaken)
        } else {
            "No Limit"
        }
        
        // Set grade with color
        val gradeText = testView.findViewById<TextView>(R.id.gradeText)
        gradeText.text = testItem.grade
        gradeText.setTextColor(getGradeColor(testItem.percentage))
        
        // Set click listener to view detailed review
        testView.setOnClickListener {
            viewTestReview(testItem.testResultId, testItem.testType)
        }
        
        testHistoryList.addView(testView)
    }
    
    private fun viewTestReview(testResultId: Int, testType: String) {
        val intent = if (testType == "MCQ") {
            Intent(this, MCQReviewActivity::class.java).apply {
                putExtra("testResultId", testResultId)
                putExtra("courseId", courseId) // Pass courseCode as courseId
                putExtra("studentId", studentId)
                putExtra("courseName", courseName)
            }
        } else {
            Intent(this, TheoryReviewActivity::class.java).apply {
                putExtra("test_result_id", testResultId)
                putExtra("courseCode", courseId)
                putExtra("courseName", courseName)
            }
        }
        startActivity(intent)
    }
    
    private fun showNoTestsMessage() {
        loadingProgress.visibility = View.GONE
        noTestsText.visibility = View.VISIBLE
        testHistoryList.visibility = View.GONE
    }
    
    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    private fun formatDate(dateStr: String): String {
        try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            return outputFormat.format(date)
        } catch (e: Exception) {
            return dateStr
        }
    }
    
    private fun getGradeColor(percentage: Double): Int {
        return when {
            percentage >= 90 -> resources.getColor(android.R.color.holo_green_dark)
            percentage >= 80 -> resources.getColor(android.R.color.holo_blue_dark)
            percentage >= 70 -> resources.getColor(android.R.color.holo_orange_dark)
            else -> resources.getColor(android.R.color.holo_red_dark)
        }
    }
}
