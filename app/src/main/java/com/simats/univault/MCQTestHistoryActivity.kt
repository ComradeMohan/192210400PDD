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
    private var courseId: Int = 0
    private var studentId: Int = 0
    private var courseName: String = ""
    
    data class TestHistoryItem(
        val testResultId: Int,
        val score: Int,
        val totalQuestions: Int,
        val percentage: Double,
        val timeTaken: Int,
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
        courseId = intent.getIntExtra("courseId", 0)
        studentId = intent.getIntExtra("studentId", 0)
        courseName = intent.getStringExtra("courseName") ?: "Unknown Course"
        
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
        if (courseId <= 0 || studentId <= 0) {
            Toast.makeText(this, "Invalid course or student ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val url = "http://192.168.137.229/univault/get_mcq_test_history.php?student_id=$studentId&course_id=$courseId"
        Log.d("MCQTestHistoryActivity", "Loading test history from: $url")
        
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
                testResultId = testObj.getInt("test_result_id"),
                score = testObj.getInt("score"),
                totalQuestions = testObj.getInt("total_questions"),
                percentage = testObj.getDouble("percentage"),
                timeTaken = testObj.getInt("time_taken"),
                testDate = testObj.getString("test_date")
            )
            
            createTestHistoryItemView(testHistoryItem, i + 1)
        }
    }
    
    private fun createTestHistoryItemView(testItem: TestHistoryItem, testNumber: Int) {
        val inflater = LayoutInflater.from(this)
        val testView = inflater.inflate(R.layout.item_test_history, testHistoryList, false)
        
        // Set test number and date
        testView.findViewById<TextView>(R.id.testNumberText).text = "Test #$testNumber"
        testView.findViewById<TextView>(R.id.testDateText).text = formatDate(testItem.testDate)
        
        // Set score and percentage
        testView.findViewById<TextView>(R.id.scoreText).text = "${testItem.score}/${testItem.totalQuestions}"
        testView.findViewById<TextView>(R.id.percentageText).text = "${testItem.percentage}%"
        
        // Set time taken
        testView.findViewById<TextView>(R.id.timeText).text = formatTime(testItem.timeTaken)
        
        // Set grade with color
        val gradeText = testView.findViewById<TextView>(R.id.gradeText)
        gradeText.text = testItem.grade
        gradeText.setTextColor(getGradeColor(testItem.percentage))
        
        // Set click listener to view detailed review
        testView.setOnClickListener {
            viewTestReview(testItem.testResultId)
        }
        
        testHistoryList.addView(testView)
    }
    
    private fun viewTestReview(testResultId: Int) {
        val intent = Intent(this, MCQReviewActivity::class.java).apply {
            putExtra("testResultId", testResultId)
            putExtra("courseId", courseId)
            putExtra("studentId", studentId)
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
