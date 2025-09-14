package com.simats.univault

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import android.util.Log

class TheoryReviewActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var courseTitle: TextView
    private lateinit var questionNumber: TextView
    private lateinit var questionText: TextView
    private lateinit var studentAnswer: TextView
    private lateinit var completeAnswer: TextView
    private lateinit var keywordsText: TextView
    private lateinit var scoreText: TextView
    private lateinit var maxMarksText: TextView
    private lateinit var nextButton: Button
    private lateinit var previousButton: Button
    private lateinit var backButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    
    // Data
    private var testResultId: Int = 0
    private var courseId: Int = 1
    private var studentId: Int = 1
    private var reviewAnswers: MutableList<JSONObject> = mutableListOf()
    private var currentQuestionIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theory_review)
        
        // Initialize UI elements
        initializeViews()
        
        // Get data from intent
        getIntentData()
        
        // Set up click listeners
        setupClickListeners()
        
        // Load review data
        loadReviewData()
    }
    
    private fun initializeViews() {
        courseTitle = findViewById(R.id.courseTitle)
        questionNumber = findViewById(R.id.questionNumber)
        questionText = findViewById(R.id.questionText)
        studentAnswer = findViewById(R.id.studentAnswer)
        completeAnswer = findViewById(R.id.completeAnswer)
        keywordsText = findViewById(R.id.keywordsText)
        scoreText = findViewById(R.id.scoreText)
        maxMarksText = findViewById(R.id.maxMarksText)
        nextButton = findViewById(R.id.nextButton)
        previousButton = findViewById(R.id.previousButton)
        backButton = findViewById(R.id.backButton)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
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
        
        val courseName = intent.getStringExtra("courseName") ?: "Unknown Course"
        courseTitle.text = courseName
        
        Log.d("TheoryReviewActivity", "Initialized with - courseId: $courseId, studentId: $studentId")
    }
    
    private fun setupClickListeners() {
        nextButton.setOnClickListener {
            if (currentQuestionIndex < reviewAnswers.size - 1) {
                currentQuestionIndex++
                displayCurrentQuestion()
            }
        }
        
        previousButton.setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                displayCurrentQuestion()
            }
        }
        
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun loadReviewData() {
        val url = "http://10.86.199.54/univault/get_theory_review.php?test_result_id=$testResultId"
        Log.d("TheoryReviewActivity", "Loading review data from: $url")
        val queue = Volley.newRequestQueue(this)
        
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val answersArray = response.getJSONArray("answers")
                        reviewAnswers.clear()
                        
                        for (i in 0 until answersArray.length()) {
                            reviewAnswers.add(answersArray.getJSONObject(i))
                        }
                        
                        // Display first question
                        displayCurrentQuestion()
                        
                        Toast.makeText(this, "Review data loaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to load review data: ${response.getString("message")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: JSONException) {
                    Log.e("TheoryReviewActivity", "Error parsing review data: ${e.message}")
                    Toast.makeText(this, "Error parsing review data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("TheoryReviewActivity", "Error loading review data: ${error.message}")
                Toast.makeText(this, "Error loading review data: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    private fun displayCurrentQuestion() {
        if (reviewAnswers.isNotEmpty() && currentQuestionIndex < reviewAnswers.size) {
            val answer = reviewAnswers[currentQuestionIndex]
            
            // Update question number and text
            questionNumber.text = "Question ${currentQuestionIndex + 1} of ${reviewAnswers.size}"
            questionText.text = answer.getString("question_text")
            
            // Display student answer
            studentAnswer.text = answer.getString("student_answer")
            
            // Display complete answer
            completeAnswer.text = answer.getString("complete_answer")
            
            // Display keywords
            keywordsText.text = "Keywords: ${answer.getString("keywords_matched")}"
            
            // Display score
            val scoreObtained = answer.getInt("score_obtained")
            val marksAllocated = answer.getInt("marks_allocated")
            scoreText.text = "$scoreObtained"
            maxMarksText.text = "/ $marksAllocated"
            
            // Update progress
            updateProgress()
            
            // Update button states
            updateButtonStates()
        }
    }
    
    private fun updateProgress() {
        val progress = ((currentQuestionIndex + 1) * 100) / reviewAnswers.size
        progressBar.progress = progress
        progressText.text = "$progress% Complete"
    }
    
    private fun updateButtonStates() {
        // Previous button
        previousButton.isEnabled = currentQuestionIndex > 0
        
        // Next button
        nextButton.isEnabled = currentQuestionIndex < reviewAnswers.size - 1
    }
    
    override fun onBackPressed() {
        finish()
    }
}
