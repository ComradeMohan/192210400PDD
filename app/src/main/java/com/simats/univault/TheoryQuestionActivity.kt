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

class TheoryQuestionActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var courseTitle: TextView
    private lateinit var questionNumber: TextView
    private lateinit var questionText: TextView
    private lateinit var answerInput: EditText
    private lateinit var nextButton: Button
    private lateinit var previousButton: Button
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    
    // Data
    private var theoryQuestions: MutableList<JSONObject> = mutableListOf()
    private var currentQuestionIndex = 0
    private var userAnswers: MutableList<String> = mutableListOf()
    private var courseCode: String = ""
    private var courseId: Int = 1 // Separate field for database lookup
    private var studentId: Int = 0 // Will be loaded from SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theory_questions)
        
        // Get student_id from shared preferences FIRST (this is the source of truth)
        val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
        studentId = sf.getInt("userID", 0)
        
        // Get intent data
        courseCode = intent.getStringExtra("courseCode") ?: "Unknown"
        courseId = intent.getIntExtra("courseId", 1) // This is separate from courseCode
        
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
        
        // Update login status
        sf.edit().putBoolean("isLoggedIn", true).apply()
        
        Log.d("TheoryQuestionActivity", "Initialized with - courseCode: $courseCode, courseId: $courseId, studentId: $studentId")
        
        // Initialize UI elements
        initializeViews()
        
        // Get course name from intent
        val courseName = intent.getStringExtra("courseName") ?: "Unknown Course"
        courseTitle.text = courseName
        
        // Set up click listeners
        setupClickListeners()
        
        // Load theory questions
        loadTheoryQuestions()
    }
    
    private fun initializeViews() {
        courseTitle = findViewById(R.id.courseTitle)
        questionNumber = findViewById(R.id.questionNumber)
        questionText = findViewById(R.id.questionText)
        answerInput = findViewById(R.id.answerInput)
        nextButton = findViewById(R.id.nextButton)
        previousButton = findViewById(R.id.previousButton)
        submitButton = findViewById(R.id.submitButton)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
    }
    
    
    private fun setupClickListeners() {
        nextButton.setOnClickListener {
            saveCurrentAnswer()
            if (currentQuestionIndex < theoryQuestions.size - 1) {
                currentQuestionIndex++
                displayCurrentQuestion()
            }
        }
        
        previousButton.setOnClickListener {
            saveCurrentAnswer()
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                displayCurrentQuestion()
            }
        }
        
        submitButton.setOnClickListener {
            saveCurrentAnswer()
            submitAnswers()
        }
    }
    
    private fun loadTheoryQuestions() {
        val url = "http://10.86.199.54/univault/get_theory_questions_updated.php?course_id=$courseCode&limit=15"
        Log.d("TheoryQuestionActivity", "Loading theory questions from: $url")
        val queue = Volley.newRequestQueue(this)
        
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val questionsArray = response.getJSONArray("questions")
                        theoryQuestions.clear()
                        
                        for (i in 0 until questionsArray.length()) {
                            theoryQuestions.add(questionsArray.getJSONObject(i))
                        }
                        
                        // Initialize user answers array
                        userAnswers = MutableList(theoryQuestions.size) { "" }
                        
                        // Display first question
                        displayCurrentQuestion()
                        
                        Toast.makeText(this, "Loaded ${theoryQuestions.size} theory questions", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to load questions: ${response.getString("message")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: JSONException) {
                    Log.e("TheoryQuestionActivity", "Error parsing questions: ${e.message}")
                    Toast.makeText(this, "Error parsing questions: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("TheoryQuestionActivity", "Error loading questions: ${error.message}")
                Toast.makeText(this, "Error loading questions: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    private fun displayCurrentQuestion() {
        if (theoryQuestions.isNotEmpty() && currentQuestionIndex < theoryQuestions.size) {
            val question = theoryQuestions[currentQuestionIndex]
            
            // Update question number and text
            questionNumber.text = "Question ${currentQuestionIndex + 1} of ${theoryQuestions.size}"
            questionText.text = question.getString("question_text")
            
            // Load saved answer
            answerInput.setText(userAnswers[currentQuestionIndex])
            
            // Update progress
            updateProgress()
            
            // Update button states
            updateButtonStates()
        }
    }
    
    private fun saveCurrentAnswer() {
        if (currentQuestionIndex < userAnswers.size) {
            userAnswers[currentQuestionIndex] = answerInput.text.toString().trim()
        }
    }
    
    private fun updateProgress() {
        val progress = ((currentQuestionIndex + 1) * 100) / theoryQuestions.size
        progressBar.progress = progress
        progressText.text = "$progress% Complete"
    }
    
    private fun updateButtonStates() {
        // Previous button
        previousButton.isEnabled = currentQuestionIndex > 0
        
        // Next button
        nextButton.isEnabled = currentQuestionIndex < theoryQuestions.size - 1
        
        // Submit button - only show on last question
        submitButton.visibility = if (currentQuestionIndex == theoryQuestions.size - 1) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    
    private fun submitAnswers() {
        // Save the last answer
        saveCurrentAnswer()
        
        // Show confirmation dialog
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Submit Answers")
            .setMessage("Are you sure you want to submit your answers? You cannot change them after submission.")
            .setPositiveButton("Submit") { _, _ ->
                // Submit to server
                submitToServer()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Do nothing
            }
            .create()
        
        alertDialog.show()
    }
    
    private fun submitToServer() {
        val url = "http://10.86.199.54/univault/submit_theory_answers_updated.php"
        val queue = Volley.newRequestQueue(this)
        
        // Prepare answers data
        val answersData = JSONObject()
        answersData.put("student_id", studentId)
        answersData.put("course_code", courseCode)
        
        val answersArray = org.json.JSONArray()
        for (i in theoryQuestions.indices) {
            val answerObj = JSONObject()
            answerObj.put("question_id", theoryQuestions[i].getInt("question_id"))
            answerObj.put("answer", userAnswers[i])
            answersArray.put(answerObj)
        }
        answersData.put("answers", answersArray)
        
        Log.d("TheoryQuestionActivity", "Submitting theory answers - studentId: $studentId, courseCode: $courseCode")
        Log.d("TheoryQuestionActivity", "Sending data: $answersData")
        
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, answersData,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        Toast.makeText(this, "Answers submitted successfully!", Toast.LENGTH_SHORT).show()
                        
                        // Navigate to Theory Result Activity
                        val intent = Intent(this, TheoryResultActivity::class.java)
                        intent.putExtra("test_result_id", response.getInt("test_result_id"))
                        intent.putExtra("courseId", courseId)
                        intent.putExtra("studentId", studentId)
                        intent.putExtra("total_score", response.getInt("total_score"))
                        intent.putExtra("total_marks", response.getInt("total_marks"))
                        intent.putExtra("percentage", response.getDouble("percentage"))
                        intent.putExtra("answered_questions", response.getInt("answered_questions"))
                        intent.putExtra("total_questions", response.getInt("total_questions"))
                        intent.putExtra("courseName", courseTitle.text.toString())
                        intent.putExtra("courseCode", this@TheoryQuestionActivity.intent.getStringExtra("courseCode"))
                        intent.putExtra("collegeName", this@TheoryQuestionActivity.intent.getStringExtra("collegeName"))
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to submit answers: ${response.getString("message")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: JSONException) {
                    Log.e("TheoryQuestionActivity", "Error parsing submit response: ${e.message}")
                    Toast.makeText(this, "Error submitting answers: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("TheoryQuestionActivity", "Error submitting answers: ${error.message}")
                Toast.makeText(this, "Error submitting answers: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    override fun onBackPressed() {
        // Show confirmation dialog when back is pressed
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit Theory Questions")
            .setMessage("Are you sure you want to exit? Your progress will be lost.")
            .setPositiveButton("Exit") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Continue") { _, _ ->
                // Do nothing
            }
            .create()
        
        alertDialog.show()
    }
}
