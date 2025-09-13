package com.simats.univault

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import android.util.Log

class MCQReviewActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var backButton: ImageButton
    private lateinit var testDate: TextView
    private lateinit var scoreText: TextView
    private lateinit var timeText: TextView
    private lateinit var percentageText: TextView
    private lateinit var questionsContainer: LinearLayout
    
    // Data
    private var testResultId: Int = 0
    private var courseId: Int = 0
    private var studentId: Int = 0
    
    data class ReviewQuestion(
        val questionId: Int,
        val questionText: String,
        val options: List<String>,
        val correctAnswer: Int,
        val correctOptionLetter: String,
        val selectedAnswer: Int,
        val selectedOptionLetter: String,
        val isCorrect: Boolean
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mcq_review)
        
        // Get intent data
        testResultId = intent.getIntExtra("testResultId", 0)
        courseId = intent.getIntExtra("courseId", 0)
        studentId = intent.getIntExtra("studentId", 0)
        
        // Initialize UI elements
        initializeViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Load review data
        loadReviewData()
    }
    
    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        testDate = findViewById(R.id.testDate)
        scoreText = findViewById(R.id.scoreText)
        timeText = findViewById(R.id.timeText)
        percentageText = findViewById(R.id.percentageText)
        questionsContainer = findViewById(R.id.questionsContainer)
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun loadReviewData() {
        if (testResultId <= 0) {
            Toast.makeText(this, "Invalid test result ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val url = "http://192.168.56.1/univault/get_mcq_test_review.php?test_result_id=$testResultId"
        Log.d("MCQReviewActivity", "Loading review from: $url")
        
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val testInfo = response.getJSONObject("test_info")
                        val questionsArray = response.getJSONArray("questions")
                        
                        // Update header info
                        updateHeaderInfo(testInfo)
                        
                        // Load questions
                        loadQuestions(questionsArray)
                        
                    } else {
                        Toast.makeText(this, "Failed to load review: ${response.optString("error", "Unknown error")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(this, "Error parsing review data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    private fun updateHeaderInfo(testInfo: org.json.JSONObject) {
        val score = testInfo.getInt("score")
        val totalQuestions = testInfo.getInt("total_questions")
        val percentage = testInfo.getDouble("percentage")
        val timeTaken = testInfo.getInt("time_taken")
        val testDateStr = testInfo.getString("test_date")
        
        scoreText.text = "$score/$totalQuestions"
        percentageText.text = "${percentage.toInt()}%"
        timeText.text = formatTime(timeTaken)
        testDate.text = formatDate(testDateStr)
    }
    
    private fun loadQuestions(questionsArray: org.json.JSONArray) {
        questionsContainer.removeAllViews()
        
        for (i in 0 until questionsArray.length()) {
            val questionObj = questionsArray.getJSONObject(i)
            val optionsArray = questionObj.getJSONArray("options")
            val options = mutableListOf<String>()
            
            for (j in 0 until optionsArray.length()) {
                options.add(optionsArray.getString(j))
            }
            
            val reviewQuestion = ReviewQuestion(
                questionId = questionObj.getInt("question_id"),
                questionText = questionObj.getString("question_text"),
                options = options,
                correctAnswer = questionObj.getInt("correct_answer"),
                correctOptionLetter = questionObj.getString("correct_option_letter"),
                selectedAnswer = questionObj.getInt("selected_answer"),
                selectedOptionLetter = questionObj.getString("selected_option_letter"),
                isCorrect = questionObj.getBoolean("is_correct")
            )
            
            createQuestionView(reviewQuestion, i + 1)
        }
    }
    
    private fun createQuestionView(question: ReviewQuestion, questionNumber: Int) {
        val inflater = LayoutInflater.from(this)
        val questionView = inflater.inflate(R.layout.item_question_review, questionsContainer, false)
        
        // Set question number and text
        val questionNumberText = questionView.findViewById<TextView>(R.id.questionText)
        questionNumberText.text = "Question $questionNumber"
        questionView.findViewById<TextView>(R.id.questionText).text = question.questionText
        
        // Set options
        val optionTexts = listOf(
            questionView.findViewById<TextView>(R.id.optionAText),
            questionView.findViewById<TextView>(R.id.optionBText),
            questionView.findViewById<TextView>(R.id.optionCText),
            questionView.findViewById<TextView>(R.id.optionDText)
        )
        
        val optionContainers = listOf(
            questionView.findViewById<LinearLayout>(R.id.optionAContainer),
            questionView.findViewById<LinearLayout>(R.id.optionBContainer),
            questionView.findViewById<LinearLayout>(R.id.optionCContainer),
            questionView.findViewById<LinearLayout>(R.id.optionDContainer)
        )
        
        // Removed option indicators - no symbols needed
        
        // Set option texts
        for (i in question.options.indices) {
            optionTexts[i].text = question.options[i]
        }
        
        // Highlight correct and selected answers with colors only
        for (i in 0..3) {
            val isCorrect = (i == question.correctAnswer)
            val isSelected = (i == question.selectedAnswer)
            
            when {
                isCorrect && isSelected -> {
                    // Correct answer selected - green background
                    optionContainers[i].setBackgroundResource(R.drawable.option_correct_background)
                }
                isCorrect -> {
                    // Correct answer not selected - green background
                    optionContainers[i].setBackgroundResource(R.drawable.option_correct_background)
                }
                isSelected -> {
                    // Wrong answer selected - red background
                    optionContainers[i].setBackgroundResource(R.drawable.option_incorrect_background)
                }
                else -> {
                    // Neutral option - default background
                    optionContainers[i].setBackgroundResource(R.drawable.option_neutral_background)
                }
            }
        }
        
        // Set answer summary
        val answerSummary = questionView.findViewById<TextView>(R.id.answerSummary)
        answerSummary.text = "Your Answer: ${question.selectedOptionLetter} | Correct Answer: ${question.correctOptionLetter}"
        
        questionsContainer.addView(questionView)
    }
    
    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    private fun formatDate(dateStr: String): String {
        try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            return outputFormat.format(date)
        } catch (e: Exception) {
            return dateStr
        }
    }
}
