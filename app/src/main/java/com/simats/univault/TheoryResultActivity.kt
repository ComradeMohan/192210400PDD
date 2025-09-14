package com.simats.univault

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class TheoryResultActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var courseTitle: TextView
    private lateinit var scoreText: TextView
    private lateinit var percentageText: TextView
    private lateinit var totalMarksText: TextView
    private lateinit var answeredQuestionsText: TextView
    private lateinit var performanceText: TextView
    private lateinit var performanceIcon: ImageView
    private lateinit var reviewButton: Button
    private lateinit var homeButton: Button
    private lateinit var retakeButton: Button
    private lateinit var progressBar: ProgressBar
    
    // Data
    private var testResultId: Int = 0
    private var courseId: Int = 1
    private var studentId: Int = 1
    private var totalScore: Int = 0
    private var totalMarks: Int = 0
    private var percentage: Double = 0.0
    private var answeredQuestions: Int = 0
    private var totalQuestions: Int = 0
    private var questionScores: List<JSONObject> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theory_result)
        
        // Initialize UI elements
        initializeViews()
        
        // Get data from intent
        getIntentData()
        
        // Set up click listeners
        setupClickListeners()
        
        // Display results
        displayResults()
    }
    
    private fun initializeViews() {
        courseTitle = findViewById(R.id.courseTitle)
        scoreText = findViewById(R.id.scoreText)
        percentageText = findViewById(R.id.percentageText)
        totalMarksText = findViewById(R.id.totalMarksText)
        answeredQuestionsText = findViewById(R.id.answeredQuestionsText)
        performanceText = findViewById(R.id.performanceText)
        performanceIcon = findViewById(R.id.performanceIcon)
        reviewButton = findViewById(R.id.reviewButton)
        homeButton = findViewById(R.id.homeButton)
        retakeButton = findViewById(R.id.retakeButton)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun getIntentData() {
        testResultId = intent.getIntExtra("test_result_id", 0)
        courseId = intent.getIntExtra("courseId", 1)
        
        // Get student_id from shared preferences FIRST (this is the source of truth)
        val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
        studentId = sf.getInt("userID", 0)
        
        // Only use intent studentId if SharedPreferences doesn't have a valid one
        val intentStudentId = intent.getIntExtra("studentId", 0)
        if (studentId <= 0 && intentStudentId > 0) {
            studentId = intentStudentId
            // Save it to SharedPreferences for future use
            sf.edit().putInt("userID", studentId).apply()
        }
        
        // Ensure we have a valid studentId
        if (studentId <= 0) {
            studentId = 1 // Last resort default
            sf.edit().putInt("userID", studentId).apply()
        }
        
        totalScore = intent.getIntExtra("total_score", 0)
        totalMarks = intent.getIntExtra("total_marks", 0)
        percentage = intent.getDoubleExtra("percentage", 0.0)
        answeredQuestions = intent.getIntExtra("answered_questions", 0)
        totalQuestions = intent.getIntExtra("total_questions", 0)
        
        val courseName = intent.getStringExtra("courseName") ?: "Unknown Course"
        courseTitle.text = courseName
        
        Log.d("TheoryResultActivity", "Initialized with - courseId: $courseId, studentId: $studentId")
    }
    
    private fun setupClickListeners() {
        reviewButton.setOnClickListener {
            startReview()
        }
        
        homeButton.setOnClickListener {
            goHome()
        }
        
        retakeButton.setOnClickListener {
            retakeTest()
        }
    }
    
    private fun displayResults() {
        // Display basic results
        scoreText.text = "$totalScore"
        percentageText.text = "${percentage.toInt()}%"
        totalMarksText.text = "/ $totalMarks"
        answeredQuestionsText.text = "$answeredQuestions / $totalQuestions"
        
        // Set progress bar
        progressBar.max = 100
        progressBar.progress = percentage.toInt()
        
        // Determine performance level and color
        val (performance, color, icon) = when {
            percentage >= 90 -> Triple("Excellent!", Color.parseColor("#4CAF50"), android.R.drawable.ic_dialog_info)
            percentage >= 80 -> Triple("Very Good!", Color.parseColor("#8BC34A"), android.R.drawable.ic_dialog_info)
            percentage >= 70 -> Triple("Good!", Color.parseColor("#FFC107"), android.R.drawable.ic_dialog_info)
            percentage >= 60 -> Triple("Satisfactory", Color.parseColor("#FF9800"), android.R.drawable.ic_dialog_info)
            percentage >= 50 -> Triple("Needs Improvement", Color.parseColor("#FF5722"), android.R.drawable.ic_dialog_info)
            else -> Triple("Poor", Color.parseColor("#F44336"), android.R.drawable.ic_dialog_info)
        }
        
        performanceText.text = performance
        performanceText.setTextColor(color)
        performanceIcon.setColorFilter(color)
        
        // Set progress bar color
        progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
    }
    
    private fun startReview() {
        // Navigate to review activity
        val intent = Intent(this, TheoryReviewActivity::class.java).apply {
            putExtra("test_result_id", testResultId)
            putExtra("courseId", courseId)
            putExtra("studentId", studentId)
            putExtra("courseName", courseTitle.text.toString())
            putExtra("courseCode", this@TheoryResultActivity.intent.getStringExtra("courseCode"))
            putExtra("collegeName", this@TheoryResultActivity.intent.getStringExtra("collegeName"))
        }
        startActivity(intent)
    }
    
    private fun goHome() {
        // Navigate back to PrepActivity
        val intent = Intent(this, PrepActivity::class.java).apply {
            putExtra("courseCode", this@TheoryResultActivity.intent.getStringExtra("courseCode"))
            putExtra("courseName", courseTitle.text.toString())
            putExtra("collegeName", this@TheoryResultActivity.intent.getStringExtra("collegeName"))
            putExtra("courseId", courseId)
        }
        startActivity(intent)
        finish()
    }
    
    private fun retakeTest() {
        // Navigate back to Theory Questions
        val intent = Intent(this, TheoryQuestionActivity::class.java).apply {
            putExtra("courseCode", this@TheoryResultActivity.intent.getStringExtra("courseCode"))
            putExtra("courseName", courseTitle.text.toString())
            putExtra("collegeName", this@TheoryResultActivity.intent.getStringExtra("collegeName"))
            putExtra("courseId", courseId)
            putExtra("studentId", studentId)
        }
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        // Prevent going back to test
        goHome()
    }
}
