package com.simats.univault

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import com.simats.univault.MCQResultActivity

class MCQTestActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var backButton: ImageButton
    private lateinit var courseTitle: TextView
    private lateinit var timerText: TextView
    private lateinit var questionCounter: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var questionText: TextView
    private lateinit var optionAContainer: LinearLayout
    private lateinit var optionBContainer: LinearLayout
    private lateinit var optionCContainer: LinearLayout
    private lateinit var optionDContainer: LinearLayout
    private lateinit var optionAText: TextView
    private lateinit var optionBText: TextView
    private lateinit var optionCText: TextView
    private lateinit var optionDText: TextView
    private lateinit var optionASelected: ImageView
    private lateinit var optionBSelected: ImageView
    private lateinit var optionCSelected: ImageView
    private lateinit var optionDSelected: ImageView
    private lateinit var selectedAnswerIndicator: LinearLayout
    private lateinit var selectedAnswerText: TextView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var submitButton: Button
    
    // Data
    private var currentQuestionIndex = 0
    private var selectedAnswers = mutableMapOf<Int, Int>() // questionIndex -> selectedOption
    private var timeRemaining = 15 * 60 * 1000L // 15 minutes in milliseconds
    private var timer: CountDownTimer? = null
    private var courseCode: String = ""
    private var courseName: String = ""
    private var collegeName: String = ""
    private var courseId: Int = 1
    private var studentId: Int = 1 // You can get this from shared preferences or login
    
    // MCQ Questions from server
    private var mcqQuestions = mutableListOf<MCQQuestion>()
    
    data class MCQQuestion(
        val questionId: Int,
        val question: String,
        val options: List<String>,
        val correctAnswer: Int
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mcq_test)
        
        // Get intent data
        courseCode = intent.getStringExtra("courseCode") ?: "Unknown"
        courseName = intent.getStringExtra("courseName") ?: "MCQ Test"
        collegeName = intent.getStringExtra("collegeName") ?: "Unknown College"
        courseId = intent.getIntExtra("courseId", 1)
        studentId = intent.getIntExtra("studentId", 1)
        
        // Initialize UI elements
        initializeViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Load questions from server
        loadQuestionsFromServer()
    }
    
    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        courseTitle = findViewById(R.id.courseTitle)
        timerText = findViewById(R.id.timerText)
        questionCounter = findViewById(R.id.questionCounter)
        progressBar = findViewById(R.id.progressBar)
        questionText = findViewById(R.id.questionText)
        
        optionAContainer = findViewById(R.id.optionAContainer)
        optionBContainer = findViewById(R.id.optionBContainer)
        optionCContainer = findViewById(R.id.optionCContainer)
        optionDContainer = findViewById(R.id.optionDContainer)
        
        optionAText = findViewById(R.id.optionAText)
        optionBText = findViewById(R.id.optionBText)
        optionCText = findViewById(R.id.optionCText)
        optionDText = findViewById(R.id.optionDText)
        
        optionASelected = findViewById(R.id.optionASelected)
        optionBSelected = findViewById(R.id.optionBSelected)
        optionCSelected = findViewById(R.id.optionCSelected)
        optionDSelected = findViewById(R.id.optionDSelected)
        
        selectedAnswerIndicator = findViewById(R.id.selectedAnswerIndicator)
        selectedAnswerText = findViewById(R.id.selectedAnswerText)
        
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        submitButton = findViewById(R.id.submitButton)
        
        // Set course title
        courseTitle.text = "$courseName - MCQ Test"
    }
    
    private fun setupClickListeners() {
        // Back button
        backButton.setOnClickListener {
            showExitConfirmation()
        }
        
        // Option click listeners
        optionAContainer.setOnClickListener { selectOption(0) }
        optionBContainer.setOnClickListener { selectOption(1) }
        optionCContainer.setOnClickListener { selectOption(2) }
        optionDContainer.setOnClickListener { selectOption(3) }
        
        // Navigation buttons
        previousButton.setOnClickListener { goToPreviousQuestion() }
        nextButton.setOnClickListener { goToNextQuestion() }
        submitButton.setOnClickListener { submitTest() }
    }
    
    private fun loadQuestionsFromServer() {
        // Show loading indicator
        questionText.text = "Loading questions..."
        
        val url = "http://192.168.56.1/univault/get_mcq_questions.php?course_id=$courseId&limit=15"
        Log.d("MCQTestActivity", "Loading questions from: $url")
        
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val questionsArray = response.getJSONArray("questions")
                        mcqQuestions.clear()
                        
                        for (i in 0 until questionsArray.length()) {
                            val questionObj = questionsArray.getJSONObject(i)
                            val optionsArray = questionObj.getJSONArray("options")
                            val options = mutableListOf<String>()
                            
                            for (j in 0 until optionsArray.length()) {
                                options.add(optionsArray.getString(j))
                            }
                            
                            mcqQuestions.add(
                                MCQQuestion(
                                    questionId = questionObj.getInt("question_id"),
                                    question = questionObj.getString("question_text"),
                                    options = options,
                                    correctAnswer = questionObj.getInt("correct_answer")
                                )
                            )
                        }
                        
                        // Start the test
                        startTest()
                        
                    } else {
                        showError("Failed to load questions: ${response.optString("error", "Unknown error")}")
                    }
                } catch (e: JSONException) {
                    showError("Error parsing questions: ${e.message}")
                }
            },
            { error ->
                showError("Network error: ${error.message}")
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    private fun startTest() {
        if (mcqQuestions.isEmpty()) {
            showError("No questions available")
            return
        }
        
        // Start timer
        startTimer()
        
        // Load first question
        loadQuestion(0)
        
        // Update navigation buttons
        updateNavigationButtons()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        questionText.text = "Error: $message"
    }
    
    private fun startTimer() {
        timer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerText.text = String.format("%02d:%02d", minutes, seconds)
            }
            
            override fun onFinish() {
                timerText.text = "00:00"
                showTimeUpDialog()
            }
        }.start()
    }
    
    private fun loadQuestion(questionIndex: Int) {
        if (questionIndex < 0 || questionIndex >= mcqQuestions.size) return
        
        currentQuestionIndex = questionIndex
        val question = mcqQuestions[questionIndex]
        
        // Animate question transition
        animateQuestionTransition {
            // Update question text
            questionText.text = question.question
            
            // Update options
            optionAText.text = question.options[0]
            optionBText.text = question.options[1]
            optionCText.text = question.options[2]
            optionDText.text = question.options[3]
            
            // Update progress
            questionCounter.text = "${questionIndex + 1} of ${mcqQuestions.size}"
            progressBar.progress = questionIndex + 1
            
            // Clear previous selections
            clearOptionSelections()
            
            // Show previously selected answer if any
            selectedAnswers[questionIndex]?.let { selectedOption ->
                selectOption(selectedOption, false)
            }
            
            // Update navigation buttons
            updateNavigationButtons()
        }
    }
    
    private fun animateQuestionTransition(onComplete: () -> Unit) {
        // Fade out current content
        val fadeOut = ObjectAnimator.ofFloat(questionText, "alpha", 1f, 0f)
        fadeOut.duration = 150
        fadeOut.start()
        
        // After fade out, update content and fade in
        fadeOut.addUpdateListener { animation ->
            if (animation.animatedValue as Float <= 0.1f) {
                onComplete()
                
                // Fade in new content
                val fadeIn = ObjectAnimator.ofFloat(questionText, "alpha", 0f, 1f)
                fadeIn.duration = 150
                fadeIn.start()
            }
        }
    }
    
    private fun selectOption(optionIndex: Int, animate: Boolean = true) {
        // Clear all selections first
        clearOptionSelections()
        
        // Select the chosen option
        when (optionIndex) {
            0 -> {
                optionAContainer.isSelected = true
                optionASelected.visibility = View.VISIBLE
                if (animate) animateOptionSelection(optionAContainer)
            }
            1 -> {
                optionBContainer.isSelected = true
                optionBSelected.visibility = View.VISIBLE
                if (animate) animateOptionSelection(optionBContainer)
            }
            2 -> {
                optionCContainer.isSelected = true
                optionCSelected.visibility = View.VISIBLE
                if (animate) animateOptionSelection(optionCContainer)
            }
            3 -> {
                optionDContainer.isSelected = true
                optionDSelected.visibility = View.VISIBLE
                if (animate) animateOptionSelection(optionDContainer)
            }
        }
        
        // Save the selection
        selectedAnswers[currentQuestionIndex] = optionIndex
        
        // Show selected answer indicator with animation
        val optionLetter = ('A' + optionIndex).toString()
        selectedAnswerText.text = "Selected: Option $optionLetter"
        selectedAnswerIndicator.visibility = View.VISIBLE
        if (animate) animateSelectedIndicator()
        
        // Update navigation buttons
        updateNavigationButtons()
    }
    
    private fun animateOptionSelection(optionContainer: LinearLayout) {
        // Scale animation
        val scaleX = ObjectAnimator.ofFloat(optionContainer, "scaleX", 1.0f, 1.05f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(optionContainer, "scaleY", 1.0f, 1.05f, 1.0f)
        scaleX.duration = 200
        scaleY.duration = 200
        scaleX.start()
        scaleY.start()
    }
    
    private fun animateSelectedIndicator() {
        val fadeIn = ObjectAnimator.ofFloat(selectedAnswerIndicator, "alpha", 0f, 1f)
        fadeIn.duration = 300
        fadeIn.start()
    }
    
    private fun clearOptionSelections() {
        optionAContainer.isSelected = false
        optionBContainer.isSelected = false
        optionCContainer.isSelected = false
        optionDContainer.isSelected = false
        
        optionASelected.visibility = View.GONE
        optionBSelected.visibility = View.GONE
        optionCSelected.visibility = View.GONE
        optionDSelected.visibility = View.GONE
        
        selectedAnswerIndicator.visibility = View.GONE
    }
    
    private fun updateNavigationButtons() {
        // Previous button
        previousButton.isEnabled = currentQuestionIndex > 0
        
        // Next/Submit button
        if (currentQuestionIndex == mcqQuestions.size - 1) {
            nextButton.visibility = View.GONE
            submitButton.visibility = View.VISIBLE
            submitButton.isEnabled = selectedAnswers.containsKey(currentQuestionIndex)
        } else {
            nextButton.visibility = View.VISIBLE
            submitButton.visibility = View.GONE
            nextButton.isEnabled = selectedAnswers.containsKey(currentQuestionIndex)
        }
    }
    
    private fun goToPreviousQuestion() {
        if (currentQuestionIndex > 0) {
            loadQuestion(currentQuestionIndex - 1)
        }
    }
    
    private fun goToNextQuestion() {
        if (currentQuestionIndex < mcqQuestions.size - 1) {
            loadQuestion(currentQuestionIndex + 1)
        }
    }
    
    private fun submitTest() {
        // Stop timer
        timer?.cancel()
        
        // Calculate score
        val score = calculateScore()
        val totalQuestions = mcqQuestions.size
        val timeTaken = (15 * 60 * 1000L - timeRemaining) / 1000
        
        // Save results to server
        saveTestResult(score, totalQuestions, timeTaken)
    }
    
    private fun saveTestResult(score: Int, totalQuestions: Int, timeTaken: Long) {
        val url = "http://192.168.56.1/univault/save_mcq_result.php"
        
        // Prepare answers data
        val answersData = mutableMapOf<String, Int>()
        selectedAnswers.forEach { (questionIndex, answerIndex) ->
            val questionId = mcqQuestions[questionIndex].questionId
            answersData[questionId.toString()] = answerIndex
        }
        
        val requestData = JSONObject().apply {
            put("student_id", studentId)
            put("course_id", courseId)
            put("score", score)
            put("total_questions", totalQuestions)
            put("time_taken", timeTaken)
            put("answers", JSONObject(answersData))
        }
        
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, requestData,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val testResultId = response.getInt("test_result_id")
                        
                        // Navigate to results
                        val intent = Intent(this, MCQResultActivity::class.java).apply {
                            putExtra("courseCode", courseCode)
                            putExtra("courseName", courseName)
                            putExtra("collegeName", collegeName)
                            putExtra("courseId", courseId)
                            putExtra("studentId", studentId)
                            putExtra("score", score)
                            putExtra("totalQuestions", totalQuestions)
                            putExtra("timeTaken", timeTaken)
                            putExtra("testResultId", testResultId)
                            
                            // Convert answers map to serializable format
                            val answersBundle = Bundle()
                            selectedAnswers.forEach { (questionIndex, answerIndex) ->
                                answersBundle.putInt("question_$questionIndex", answerIndex)
                            }
                            putExtra("answersBundle", answersBundle)
                        }
                        startActivity(intent)
                        finish()
                        
                    } else {
                        showError("Failed to save results: ${response.optString("error", "Unknown error")}")
                    }
                } catch (e: JSONException) {
                    showError("Error saving results: ${e.message}")
                }
            },
            { error ->
                showError("Network error saving results: ${error.message}")
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    private fun calculateScore(): Int {
        var correctAnswers = 0
        for (i in mcqQuestions.indices) {
            val selectedAnswer = selectedAnswers[i]
            val correctAnswer = mcqQuestions[i].correctAnswer
            if (selectedAnswer == correctAnswer) {
                correctAnswers++
            }
        }
        return correctAnswers
    }
    
    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit Test")
            .setMessage("Are you sure you want to exit the test? Your progress will be lost.")
            .setPositiveButton("Exit") { _, _ ->
                timer?.cancel()
                finish()
            }
            .setNegativeButton("Continue", null)
            .show()
    }
    
    private fun showTimeUpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Time's Up!")
            .setMessage("The test time has expired. Your answers will be submitted automatically.")
            .setPositiveButton("Submit") { _, _ ->
                submitTest()
            }
            .setCancelable(false)
            .show()
    }
    
    override fun onBackPressed() {
        showExitConfirmation()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
