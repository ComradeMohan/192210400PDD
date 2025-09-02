package com.simats.univault

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import android.util.Log
import android.util.TypedValue
import android.webkit.WebView
import android.webkit.WebViewClient

class ReadingActivity : AppCompatActivity() {
    
    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
    
    // Scroll tracking variables
    private var hasScrolledToMarkAsRead = false
    private var markAsReadSectionY = 0
    private var autoCheckScheduled = false
    
    // UI Elements
    private lateinit var courseName: TextView
    private lateinit var topicName: TextView
    private lateinit var topicContent: WebView
    private lateinit var topicProgress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var markAsReadCheckbox: CheckBox
    private lateinit var readStatus: TextView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var completeTopicButton: Button
    private lateinit var loadingSpinnerLayout: LinearLayout
    
    // Data
    private var courseCode: String = ""
    private var courseTitle: String = ""
    private var collegeName: String = ""
    private var selectedMode: String = ""
    private var currentTopicIndex: Int = 0
    private var topics: List<Topic> = emptyList()
    private var currentTopic: Topic? = null
    
    // Data class for Topic
    data class Topic(
        val id: String,
        val name: String,
        val content: String,
        val isRead: Boolean = false
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading)
        
        // Initialize UI elements
        initializeViews()
        
        // Get data from intent
        getIntentData()
        
        // Set up click listeners
        setupClickListeners()
        
        // Load topics for the course
        loadTopics()
    }
    
    private fun initializeViews() {
        courseName = findViewById(R.id.courseName)
        topicName = findViewById(R.id.topicName)
        topicContent = findViewById(R.id.topicWebView)
        topicContent.settings.loadsImagesAutomatically = true
        topicContent.settings.javaScriptEnabled = false
        topicContent.settings.defaultTextEncodingName = "utf-8"
        topicContent.settings.domStorageEnabled = true
        topicContent.settings.builtInZoomControls = true
        topicContent.settings.displayZoomControls = false
        topicContent.settings.useWideViewPort = false
        topicContent.settings.loadWithOverviewMode = false
        topicContent.settings.textZoom = 110
        topicContent.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        topicContent.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript("document.body.scrollHeight.toString()") { result ->
                    val px = result?.replace("\"", "")?.toFloatOrNull()?.times(resources.displayMetrics.density)
                    if (px != null && px > 0 && view != null) {
                        val params = view.layoutParams
                        params.height = px.toInt()
                        view.layoutParams = params
                    }
                }
            }
        }
        topicProgress = findViewById(R.id.topicProgress)
        progressText = findViewById(R.id.progressText)
        markAsReadCheckbox = findViewById(R.id.markAsReadCheckbox)
        readStatus = findViewById(R.id.readStatus)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        completeTopicButton = findViewById(R.id.completeTopicButton)
        loadingSpinnerLayout = findViewById(R.id.loadingSpinnerLayout)
        
        // Setup scroll listener for progress tracking
        setupScrollListener()
    }
    
    private fun setupScrollListener() {
        findViewById<ScrollView>(R.id.contentScrollView).viewTreeObserver.addOnGlobalLayoutListener {
            // Calculate the Y position of the mark as read section
            markAsReadSectionY = markAsReadCheckbox.top - findViewById<ScrollView>(R.id.contentScrollView).height + 100.dpToPx()
        }
        
        findViewById<ScrollView>(R.id.contentScrollView).setOnScrollChangeListener { _, _, _, scrollY, _ ->
            val scrollView = findViewById<ScrollView>(R.id.contentScrollView)
            val maxScrollY = scrollView.getChildAt(0).height - scrollView.height
            
            // Check if user has scrolled to the mark as read section OR to the end of content
            if ((scrollY >= markAsReadSectionY || scrollY >= maxScrollY - 50) && !hasScrolledToMarkAsRead) {
                // Only record that the user reached the section/end; do not auto-check
                hasScrolledToMarkAsRead = true
            }
        }
    }
    
    private fun hasUserScrolledToMarkAsRead(): Boolean {
        return hasScrolledToMarkAsRead
    }
    
    private fun showLoadingSpinner() {
        loadingSpinnerLayout.visibility = android.view.View.VISIBLE
        topicContent.visibility = android.view.View.GONE
    }
    
    private fun hideLoadingSpinner() {
        loadingSpinnerLayout.visibility = android.view.View.GONE
        topicContent.visibility = android.view.View.VISIBLE
    }
    
    private fun getIntentData() {
        courseCode = intent.getStringExtra("courseCode") ?: ""
        courseTitle = intent.getStringExtra("courseName") ?: "Unknown Course"
        collegeName = intent.getStringExtra("collegeName") ?: "Unknown College"
        selectedMode = intent.getStringExtra("mode") ?: "UNKNOWN"
        
        // Set course name with mode indicator
        courseName.text = "$courseTitle - $selectedMode Mode"
        
        // Save last studied course information
        saveLastStudiedCourse()
    }
    
    private fun saveLastStudiedCourse() {
        val sharedPrefs = getSharedPreferences("LastStudied", MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("last_course_code", courseCode)
            putString("last_course_name", courseTitle)
            putString("last_mode", selectedMode)
            putLong("last_study_time", System.currentTimeMillis())
        }.apply()
        
        Log.d("ReadingActivity", "Saved course data - Code: $courseCode, Name: $courseTitle, Mode: $selectedMode")
    }
    
    private fun setupClickListeners() {
        // Setup checkbox listener
        setupCheckboxListener()
        
        // Previous Button
        previousButton.setOnClickListener {
            if (currentTopicIndex > 0) {
                currentTopicIndex--
                loadCurrentTopic()
            }
        }
        
        // Next Button
        nextButton.setOnClickListener {
            if (currentTopicIndex < topics.size - 1) {
                currentTopicIndex++
                loadCurrentTopic()
            }
        }
        
        // Complete Topic Button
        completeTopicButton.setOnClickListener {
            completeCurrentTopic()
        }
    }
    
    private fun setupCheckboxListener() {
        markAsReadCheckbox.setOnCheckedChangeListener { _, isChecked ->
            currentTopic?.let { topic ->
                if (isChecked) {
                    // User manually marked as read
                    updateTopicReadStatus(topic.id, true)
                    updateReadStatus(true)
                    Log.d("ReadingActivity", "User marked topic as read: ${topic.name}")
                } else {
                    // User manually unchecked - update progress and reset tracking
                    updateTopicReadStatus(topic.id, false)
                    updateReadStatus(false)
                    hasScrolledToMarkAsRead = false
                    Log.d("ReadingActivity", "User manually unchecked topic: ${topic.name}")
                }
            }
        }
    }
    
    private fun loadTopics() {
        // Show loading state
        topicContent.loadDataWithBaseURL(
            null,
            "<html><head><meta charset=\"UTF-8\"></head><body>🔄 Loading topics from server...</body></html>",
            "text/html; charset=UTF-8",
            "utf-8",
            null
        )
        
        // Fetch topics from API based on selected mode
        fetchTopicsFromAPI()
    }
    
    // Removed hardcoded topic methods - now using API
    
    private fun loadCurrentTopic() {
        if (currentTopicIndex >= 0 && currentTopicIndex < topics.size) {
            currentTopic = topics[currentTopicIndex]
            currentTopic?.let { topic ->
                // Show loading spinner first
                // Reset checkbox immediately to avoid carryover from previous topic
                setCheckboxCheckedSilently(false)
                showLoadingSpinner()
                
                // Update topic name immediately
                topicName.text = topic.name
                
                // Delay content loading by 2 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val html = """
                        <html>
                        <head>
                          <meta charset=\"UTF-8\" />
                          <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                          <style>
                            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 90%; margin: 0 auto; padding: 20px; }
                            h2 { color: #4CAF50; border-bottom: 2px solid #ddd; padding-bottom: 10px; margin-top: 30px; }
                            ul { list-style-type: disc; padding-left: 20px; }
                            li b { color: #555; }
                            p { margin-top: 15px; }
                            hr { border: 0; height: 1px; background: #ddd; margin: 20px 0; }
                            .image-container { text-align: center; margin: 20px 0; }
                            .image-container img { max-width: 100%; height: auto; }
                          </style>
                        </head>
                        <body>${topic.content}</body>
                        </html>
                    """.trimIndent()
                    topicContent.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "utf-8", null)
                    
                    // Hide spinner and show content
                    hideLoadingSpinner()
                    
                    // Reset scroll position to top for new topic
                    findViewById<ScrollView>(R.id.contentScrollView).post {
                        findViewById<ScrollView>(R.id.contentScrollView).smoothScrollTo(0, 0)
                    }
                    
                    // Reset scroll tracking for new topic
                    hasScrolledToMarkAsRead = false
                    autoCheckScheduled = false
                    
                    // Update progress
                    updateProgress()
                    
                    // Update navigation buttons
                    updateNavigationButtons()
                    
                    // Load read status
                    loadTopicReadStatus(topic.id)
                }, 2000) // 2 second delay
            }
        }
    }
    private fun updateProgress() {
        if (topics.isEmpty()) {
            topicProgress.progress = 0
            progressText.text = "0/0"
            return
        }

        val sharedPrefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        var completedTopics = 0
        topics.forEach { topic ->
            val isRead = sharedPrefs.getBoolean("topic_${courseCode}_${getModeSuffix()}_${topic.id}", false)
            if (isRead) completedTopics++
        }

        val progressPercent = (completedTopics * 100) / topics.size
        topicProgress.progress = progressPercent
        progressText.text = "${completedTopics}/${topics.size}"
    }
    
    private fun updateNavigationButtons() {
        // Previous button
        previousButton.isEnabled = currentTopicIndex > 0
        
        // Next button - hide on last page, show on other pages
        if (currentTopicIndex == topics.size - 1) {
            nextButton.visibility = android.view.View.GONE
        } else {
            nextButton.visibility = android.view.View.VISIBLE
            nextButton.isEnabled = currentTopicIndex < topics.size - 1
        }
        
        // Complete button (show only on last topic)
        if (currentTopicIndex == topics.size - 1) {
            completeTopicButton.visibility = android.view.View.VISIBLE
            // On last topic, adjust button weights for two-button layout (Previous + Complete)
            previousButton.layoutParams = (previousButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
            completeTopicButton.layoutParams = (completeTopicButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
        } else {
            completeTopicButton.visibility = android.view.View.GONE
            // When Complete button is hidden, adjust weights for two-button layout (Previous + Next)
            previousButton.layoutParams = (previousButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
            nextButton.layoutParams = (nextButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
        }
    }
    
    private fun loadTopicReadStatus(topicId: String) {
        // Also load from local storage as fallback
        val sharedPrefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        val isRead = sharedPrefs.getBoolean("topic_${courseCode}_${getModeSuffix()}_${topicId}", false)
        
        // Only update UI if backend hasn't responded yet
        if (!markAsReadCheckbox.isChecked && !markAsReadCheckbox.isChecked) {
            setCheckboxCheckedSilently(isRead)
        }
    }
    
    private fun updateTopicReadStatus(topicId: String, isRead: Boolean) {
        // Update local storage first for immediate UI feedback
        val sharedPrefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("topic_${courseCode}_${getModeSuffix()}_${topicId}", isRead).apply()
        
        // Update backend
        updateTopicProgressToBackend(topicId, isRead)
        
        // Update overall course progress
        updateCourseProgress()

        // Refresh on-screen progress based on marked-as-read
        updateProgress()
    }
    
    private fun updateReadStatus(isRead: Boolean) {
        if (isRead) {
            readStatus.text = "✓ Completed"
            readStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            readStatus.text = "Not read yet"
            readStatus.setTextColor(getColor(android.R.color.darker_gray))
        }
    }
    
    private fun updateCourseProgress() {
        val sharedPrefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        if (topics.isEmpty()) {
            // No topics → persist 0% and exit safely
            val courseProgressPrefs = getSharedPreferences("CourseProgress", MODE_PRIVATE)
            courseProgressPrefs.edit()
                .putInt("progress_${courseCode}_${getModeSuffix()}", 0)
                .apply()
            return
        }
        var completedTopics = 0
        
        topics.forEach { topic ->
            val isRead = sharedPrefs.getBoolean("topic_${courseCode}_${getModeSuffix()}_${topic.id}", false)
            if (isRead) completedTopics++
        }
        
        val progress = (completedTopics * 100) / topics.size
        
        // Update per-mode progress for PrepActivity display
        val courseProgressPrefs = getSharedPreferences("CourseProgress", MODE_PRIVATE)
        courseProgressPrefs.edit()
            .putInt("progress_${courseCode}_${getModeSuffix()}", progress)
            .apply()
    }
    
    private fun completeCurrentTopic() {
        currentTopic?.let { topic ->
            // Mark current topic as read
            updateTopicReadStatus(topic.id, true)
            setCheckboxCheckedSilently(true)
            
            // Show completion message
            Toast.makeText(this, "Topic completed! Great job!", Toast.LENGTH_LONG).show()
            
            // If this was the last topic, show completion dialog
            if (currentTopicIndex == topics.size - 1) {
                showCourseCompletionDialog()
            }
        }
    }
    
    private fun showCourseCompletionDialog() {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎉 Course Completed!")
            .setMessage("Congratulations! You have successfully completed all topics in this course.")
            .setPositiveButton("View Progress") { _, _ ->
                // Navigate back to PrepActivity or show progress
                finish()
            }
            .setNegativeButton("Continue Reading") { _, _ ->
                // Stay on current page
            }
            .create()
        
        alertDialog.show()
    }
    
    // Method to fetch topics from API
    private fun fetchTopicsFromAPI() {
        // Convert course code to course ID (you might need to adjust this based on your database structure)
        val courseId = getCourseIdFromCode(courseCode)
        
        // Determine mode for API
        val apiMode = when (selectedMode) {
            "PASS" -> "pass"
            "MASTER" -> "master"
            else -> "all"
        }
        
        val url = "http://10.137.118.54/univault/get_topics.php?course_code=$courseId&mode=$apiMode"
        Log.d("ReadingActivity", "Fetching topics from: $url")
        
        val queue = Volley.newRequestQueue(this)
        
        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            Request.Method.GET, url,
            { body ->
                try {
                    // Try to parse as JSON array first
                    val response = org.json.JSONArray(body)
                    val fetchedTopics = mutableListOf<Topic>()
                    for (i in 0 until response.length()) {
                        val topicObject = response.getJSONObject(i)
                        val topic = Topic(
                            id = topicObject.optString("topic_id", "unknown"),
                            name = topicObject.optString("topic_name", "Unknown Topic"),
                            content = topicObject.optString("content", "No content available"),
                            isRead = false
                        )
                        fetchedTopics.add(topic)
                    }
                    if (fetchedTopics.isNotEmpty()) {
                        topics = fetchedTopics
                        loadCurrentTopic()
                        Toast.makeText(this, "Loaded ${topics.size} topics", Toast.LENGTH_SHORT).show()
                    } else {
                        showLoadingSpinner()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            showNoTopicsState("📚 No topics found for this course and mode.\n\nPlease check back later or contact your instructor.")
                            Toast.makeText(this, "No topics found", Toast.LENGTH_SHORT).show()
                        }, 2000)
                    }
                } catch (arrayEx: org.json.JSONException) {
                    // Not a JSON array; try to parse as an object and check for error
                    try {
                        val obj = org.json.JSONObject(body)
                        val errorMsg = obj.optString("error", "No topics found")
                        showLoadingSpinner()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            showNoTopicsState("📚 $errorMsg\n\nPlease check back later or contact your instructor.")
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                        }, 2000)
                    } catch (objEx: org.json.JSONException) {
                        Log.e("ReadingActivity", "Unrecognized server response")
                        showNoTopicsState("❌ Unrecognized server response. Please try again later.")
                    }
                }
            },
            { error ->
                Log.e("ReadingActivity", "Network error: ${error.message}")
                showNoTopicsState("🌐 Network Error: ${error.message}\n\nPlease check your internet connection and try again.")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {}

        queue.add(stringRequest)
 
    }
    
    // Helper method to convert course code to course ID
    // You might need to adjust this based on your database structure
    private fun getCourseIdFromCode(courseCode: String): String {
        // For now, using course code as course ID
        // You might want to create a separate API endpoint to get course ID from course code
        return courseCode
    }

    // Helper to build storage key suffix for current mode
    private fun getModeSuffix(): String {
        return when (selectedMode) {
            "PASS" -> "PASS"
            "MASTER" -> "MASTER"
            else -> "ALL"
        }
    }
 
    // Backend integration methods for progress tracking
    
    private fun updateTopicProgressToBackend(topicId: String, isRead: Boolean) {
        val url = "http://10.137.118.54/univault/update_topic_progress.php"
        
        val queue = Volley.newRequestQueue(this)
        
        val jsonObject = JSONObject().apply {
            put("student_id", getStudentId())
            put("course_code", courseCode)
            put("topic_id", topicId)
            put("is_read", isRead)
            put("mode", when (selectedMode) {
                "PASS" -> "pass"
                "MASTER" -> "master"
                else -> "all"
            })
        }
        
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                if (response.optBoolean("success", false)) {
                    Log.d("ReadingActivity", "Progress updated to backend: ${response.optString("message")}")
                } else {
                    Log.e("ReadingActivity", "Backend error: ${response.optString("error")}")
                }
            },
            { error ->
                Log.e("ReadingActivity", "Failed to update backend: ${error.message}")
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    // Helper method to get student ID (you'll need to implement this based on your login system)
    private fun getStudentId(): Int {
        // This should return the logged-in student's ID
        // You might store this in SharedPreferences after login
        val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPrefs.getInt("student_id", 0)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh topic read status
        currentTopic?.let { topic ->
            loadTopicReadStatus(topic.id)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Save any final progress
        updateCourseProgress()
    }

    private fun setCheckboxCheckedSilently(checked: Boolean) {
        markAsReadCheckbox.setOnCheckedChangeListener(null)
        if (markAsReadCheckbox.isChecked != checked) {
            markAsReadCheckbox.isChecked = checked
            updateReadStatus(checked)
        } else {
            updateReadStatus(checked)
        }
        autoCheckScheduled = false
        setupCheckboxListener()
    }

    private fun showNoTopicsState(message: String) {
        hideLoadingSpinner()
        setCheckboxCheckedSilently(false)
        markAsReadCheckbox.isEnabled = false
        readStatus.text = "No topic"
        readStatus.setTextColor(getColor(android.R.color.darker_gray))
        val html = "<html><body style='font-family: sans-serif; padding:16px; color:#555;'>${message}</body></html>"
        topicContent.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        topicProgress.progress = 0
        progressText.text = "0/0"
        previousButton.isEnabled = false
        nextButton.visibility = android.view.View.GONE
        completeTopicButton.visibility = android.view.View.GONE
    }

    // WebView handles images and CSS; ImageGetter no longer required
}
