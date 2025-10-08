package com.simats.univault

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MCQResultActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var backButton: ImageButton
    private lateinit var courseTitle: TextView
    private lateinit var resultIcon: ImageView
    private lateinit var scoreText: TextView
    private lateinit var percentageText: TextView
    private lateinit var resultMessage: TextView
    private lateinit var correctAnswers: TextView
    private lateinit var wrongAnswers: TextView
    private lateinit var timeTaken: TextView
    private lateinit var accuracyText: TextView
    private lateinit var speedText: TextView
    private lateinit var gradeText: TextView
    private lateinit var reviewButton: Button
    private lateinit var retakeButton: Button
    private lateinit var homeButton: Button
    
    // Data
    private var courseCode: String = ""
    private var courseName: String = ""
    private var collegeName: String = ""
    private var courseId: Int = 0
    private var studentId: Int = 0
    private var testResultId: Int = 0
    private var score: Int = 0
    private var totalQuestions: Int = 0
    private var timeTakenSeconds: Long = 0
    private var answers: MutableMap<Int, Int> = mutableMapOf()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mcq_result)
        
        // Get intent data
        courseCode = intent.getStringExtra("courseCode") ?: "Unknown"
        courseName = intent.getStringExtra("courseName") ?: "MCQ Test"
        collegeName = intent.getStringExtra("collegeName") ?: "Unknown College"
        courseId = intent.getIntExtra("courseId", 0)
        studentId = intent.getIntExtra("studentId", 0)
        testResultId = intent.getIntExtra("testResultId", 0)
        score = intent.getIntExtra("score", 0)
        totalQuestions = intent.getIntExtra("totalQuestions", 10)
        timeTakenSeconds = intent.getLongExtra("timeTaken", 0)
        
        // Extract answers from bundle
        val answersBundle = intent.getBundleExtra("answersBundle")
        answersBundle?.let { bundle ->
            for (i in 0 until totalQuestions) {
                val answer = bundle.getInt("question_$i", -1)
                if (answer != -1) {
                    answers[i] = answer
                }
            }
        }
        
        // Initialize UI elements
        initializeViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Display results with animation
        displayResults()
        animateResults()
    }
    
    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        courseTitle = findViewById(R.id.courseTitle)
        resultIcon = findViewById(R.id.resultIcon)
        scoreText = findViewById(R.id.scoreText)
        percentageText = findViewById(R.id.percentageText)
        resultMessage = findViewById(R.id.resultMessage)
        correctAnswers = findViewById(R.id.correctAnswers)
        wrongAnswers = findViewById(R.id.wrongAnswers)
        timeTaken = findViewById(R.id.timeTaken)
        accuracyText = findViewById(R.id.accuracyText)
        speedText = findViewById(R.id.speedText)
        gradeText = findViewById(R.id.gradeText)
        reviewButton = findViewById(R.id.reviewButton)
        retakeButton = findViewById(R.id.retakeButton)
        homeButton = findViewById(R.id.homeButton)
        
        // Set course title
        courseTitle.text = courseName
    }
    
    private fun setupClickListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }
        
        // Review button
        reviewButton.setOnClickListener {
            if (testResultId > 0) {
                val intent = Intent(this, MCQReviewActivity::class.java).apply {
                    putExtra("testResultId", testResultId)
                    putExtra("courseId", courseId)
                    putExtra("studentId", studentId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Review not available for this test", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Retake button
        retakeButton.setOnClickListener {
            val intent = Intent(this, MCQTestActivity::class.java).apply {
                putExtra("courseCode", courseCode)
                putExtra("courseName", courseName)
                putExtra("collegeName", collegeName)
                putExtra("courseId", courseId)
                putExtra("studentId", studentId)
            }
            startActivity(intent)
            finish()
        }
        
        // Home button
        homeButton.setOnClickListener {
            val intent = Intent(this, PrepActivity::class.java).apply {
                putExtra("courseCode", courseCode)
                putExtra("courseName", courseName)
                putExtra("collegeName", collegeName)
                putExtra("courseId", courseId)
            }
            startActivity(intent)
            finish()
        }
    }
    
    private fun displayResults() {
        val wrongAnswers = totalQuestions - score
        val percentage = (score * 100) / totalQuestions
        val timeMinutes = timeTakenSeconds / 60
        val timeSeconds = timeTakenSeconds % 60
        val averageTimePerQuestion = if (totalQuestions > 0) timeTakenSeconds / totalQuestions else 0
        
        // Update score display
        scoreText.text = "$score/$totalQuestions"
        percentageText.text = "$percentage%"
        
        // Update statistics
        this.correctAnswers.text = score.toString()
        this.wrongAnswers.text = wrongAnswers.toString()
        timeTaken.text = String.format("%02d:%02d", timeMinutes, timeSeconds)
        
        // Update performance analysis
        accuracyText.text = "$percentage%"
        speedText.text = String.format("%.1f min/question", averageTimePerQuestion / 60.0)
        gradeText.text = calculateGrade(percentage)
        
        // Update result message and icon based on performance
        when {
            percentage >= 90 -> {
                resultIcon.setImageResource(android.R.drawable.ic_dialog_info)
                resultIcon.setColorFilter(resources.getColor(android.R.color.holo_green_dark))
                resultMessage.text = "Outstanding! You've mastered this topic completely."
            }
            percentage >= 80 -> {
                resultIcon.setImageResource(android.R.drawable.ic_dialog_info)
                resultIcon.setColorFilter(resources.getColor(android.R.color.holo_green_dark))
                resultMessage.text = "Great Job! You passed the test with flying colors."
            }
            percentage >= 70 -> {
                resultIcon.setImageResource(android.R.drawable.ic_dialog_info)
                resultIcon.setColorFilter(resources.getColor(android.R.color.holo_orange_dark))
                resultMessage.text = "Good work! You passed the test. Keep practicing to improve."
            }
            percentage >= 60 -> {
                resultIcon.setImageResource(android.R.drawable.ic_dialog_info)
                resultIcon.setColorFilter(resources.getColor(android.R.color.holo_orange_dark))
                resultMessage.text = "You passed! Consider reviewing the material and retaking the test."
            }
            else -> {
                resultIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                resultIcon.setColorFilter(resources.getColor(android.R.color.holo_red_dark))
                resultMessage.text = "You need more practice. Review the material and try again."
            }
        }
    }
    
    private fun calculateGrade(percentage: Int): String {
        return when {
            percentage >= 97 -> "A+"
            percentage >= 93 -> "A"
            percentage >= 90 -> "A-"
            percentage >= 87 -> "B+"
            percentage >= 83 -> "B"
            percentage >= 80 -> "B-"
            percentage >= 77 -> "C+"
            percentage >= 73 -> "C"
            percentage >= 70 -> "C-"
            percentage >= 67 -> "D+"
            percentage >= 63 -> "D"
            percentage >= 60 -> "D-"
            else -> "F"
        }
    }
    
    private fun animateResults() {
        // Animate score text
        val scoreAnimation = ObjectAnimator.ofFloat(scoreText, "scaleX", 0f, 1.2f, 1f)
        scoreAnimation.duration = 800
        scoreAnimation.start()
        
        // Animate percentage text
        val percentageAnimation = ObjectAnimator.ofFloat(percentageText, "scaleY", 0f, 1.2f, 1f)
        percentageAnimation.duration = 800
        percentageAnimation.startDelay = 200
        percentageAnimation.start()
        
        // Animate result icon
        val iconAnimation = ObjectAnimator.ofFloat(resultIcon, "rotation", 0f, 360f)
        iconAnimation.duration = 1000
        iconAnimation.start()
        
        // Animate statistics with delay
        val statsViews = listOf(correctAnswers, wrongAnswers, timeTaken)
        statsViews.forEachIndexed { index, view ->
            val animation = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            animation.duration = 500
            animation.startDelay = 500 + (index * 200).toLong()
            animation.start()
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
