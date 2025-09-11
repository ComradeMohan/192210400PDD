package com.simats.univault

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import android.util.Log
import com.simats.univault.ReadingActivity

class PrepActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var courseTitle: TextView
    private lateinit var courseDescription: TextView
    private lateinit var passProgress: ProgressBar
    private lateinit var passProgressPercent: TextView
    private lateinit var masterProgress: ProgressBar
    private lateinit var masterProgressPercent: TextView
    private lateinit var passModeCard: LinearLayout
    private lateinit var masterModeCard: LinearLayout
    
    // Progress tracking (per mode)
    private var passProgressValue = 0
    private var masterProgressValue = 0
    private var selectedMode: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prep)
        
        // Initialize UI elements
        initializeViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Load course data
        loadCourseData()
        
        // Update progress
        updateProgress()
    }
    
    private fun initializeViews() {
        courseTitle = findViewById(R.id.courseTitle)
        courseDescription = findViewById(R.id.courseDescription)
        passProgress = findViewById(R.id.passProgress)
        passProgressPercent = findViewById(R.id.passProgressPercent)
        masterProgress = findViewById(R.id.masterProgress)
        masterProgressPercent = findViewById(R.id.masterProgressPercent)
        passModeCard = findViewById(R.id.passModeCard)
        masterModeCard = findViewById(R.id.masterModeCard)
    }
    
    private fun setupClickListeners() {
        // Pass Mode Card Click Listener
        passModeCard.setOnClickListener {
            selectedMode = "PASS"
            highlightSelectedMode(passModeCard, masterModeCard)
            showModeDetails("Pass Mode", "Quick revision with essential concepts and easy questions. Perfect for passing the exam with minimal effort.")
        }
        
        // Master Mode Card Click Listener
        masterModeCard.setOnClickListener {
            selectedMode = "MASTER"
            highlightSelectedMode(masterModeCard, passModeCard)
            showModeDetails("Master Mode", "Comprehensive preparation with advanced concepts, challenging questions, and deep analysis. Achieve 100/100 mastery level.")
        }
    }
    
    private fun highlightSelectedMode(selectedCard: LinearLayout, unselectedCard: LinearLayout) {
        if (selectedMode != null) {
            // Highlight selected card
            selectedCard.alpha = 1.0f
            selectedCard.elevation = 8f
            
            // Unhighlight other card
            unselectedCard.alpha = 0.7f
            unselectedCard.elevation = 2f
        } else {
            // Reset both cards to normal state
            selectedCard.alpha = 1.0f
            selectedCard.elevation = 2f
            unselectedCard.alpha = 1.0f
            unselectedCard.elevation = 2f
        }
    }
    
    private fun showModeDetails(modeName: String, description: String) {
        // Show mode selection dialog with start button
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎯 $modeName Selected")
            .setMessage(description)
            .setPositiveButton("Start Learning") { _, _ ->
                // Start the selected mode
                startSelectedMode()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Reset selection
                selectedMode = null
                highlightSelectedMode(passModeCard, masterModeCard)
            }
            .create()
        
        alertDialog.show()
    }
    
    private fun loadCourseData() {
        // Load course information from intent
        val courseCode = intent.getStringExtra("courseCode") ?: "Unknown Course"
        val courseName = intent.getStringExtra("courseName") ?: "Fundamental Analysis"
        val collegeName = intent.getStringExtra("collegeName") ?: "Unknown College"
        
        // Set course title
        courseTitle.text = courseName
        
        // Set initial description with course details
        courseDescription.text = "Course Code: $courseCode\nCollege: $collegeName\n\n🔄 Fetching course description from server..."
        
        // Fetch course description from API
        fetchCourseDescription(courseCode, courseName, collegeName)
        
        // Load progress from shared preferences or database
        loadProgressFromStorage()
    }
    
    private fun fetchCourseDescription(courseCode: String, courseName: String, collegeName: String) {
        // Construct the API URL
        val url = "http://10.235.18.54/univault/get_course_description.php?course_code=$courseCode"
        Log.d("PrepActivity", "Fetching course description from: $url")
        val queue = Volley.newRequestQueue(this)
        
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    // Check if response contains course data
                    if (response.has("course_id") || response.has("course_name") || response.has("description")) {
                        val apiCourseName = response.optString("course_name", courseName)
                        val description = response.optString("description", "No description available")
                        
                        // Update the course description with API data
                        courseDescription.text = "Course Code: $courseCode\nCollege: $collegeName\n\n$description"
                        
                        // Update course title if API provides different name
                        if (apiCourseName.isNotEmpty() && apiCourseName != "null") {
                            courseTitle.text = apiCourseName
                        }
                        
                        Toast.makeText(this, "Course description loaded successfully", Toast.LENGTH_SHORT).show()
                        
                    } else {
                        // Show API response for debugging
                        courseDescription.text = "Course Code: $courseCode\nCollege: $collegeName\n\nAPI Response: ${response.toString()}"
                        Toast.makeText(this, "API response format unexpected", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    // Show error details for debugging
                    courseDescription.text = "Course Code: $courseCode\nCollege: $collegeName\n\nError parsing API response: ${e.message}"
                    Toast.makeText(this, "Error parsing course description: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                // Show network error details for debugging
                courseDescription.text = "Course Code: $courseCode\nCollege: $collegeName\n\nNetwork Error: ${error.message}\n\nPlease check your internet connection and try again."
                Toast.makeText(this, "Error fetching course description: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    private fun loadProgressFromStorage() {
        val courseCode = intent.getStringExtra("courseCode") ?: "unknown"
        val sharedPrefs = getSharedPreferences("CourseProgress", MODE_PRIVATE)
        passProgressValue = sharedPrefs.getInt("progress_${courseCode}_PASS", 0)
        masterProgressValue = sharedPrefs.getInt("progress_${courseCode}_MASTER", 0)
    }
    
    private fun updateProgress() {
        // Update per-mode progress bars and labels
        passProgress.progress = passProgressValue
        passProgressPercent.text = "${passProgressValue}% Completed"

        masterProgress.progress = masterProgressValue
        masterProgressPercent.text = "${masterProgressValue}% Completed"

        // Save progress to storage
        saveProgressToStorage()
    }
    
    private fun saveProgressToStorage() {
        val courseCode = intent.getStringExtra("courseCode") ?: "unknown"
        val sharedPrefs = getSharedPreferences("CourseProgress", MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt("progress_${courseCode}_PASS", passProgressValue)
            .putInt("progress_${courseCode}_MASTER", masterProgressValue)
            .apply()
    }
    
    // Methods to increment per-mode progress (called when user completes topics/questions)
    fun incrementPassProgress(increment: Int) {
        passProgressValue = (passProgressValue + increment).coerceAtMost(100)
        updateProgress()
    }

    fun incrementMasterProgress(increment: Int) {
        masterProgressValue = (masterProgressValue + increment).coerceAtMost(100)
        updateProgress()
    }
    
    // Method to start the selected mode
    fun startSelectedMode() {
        when (selectedMode) {
            "PASS" -> startPassMode()
            "MASTER" -> startMasterMode()
            else -> {
                Toast.makeText(this, "Please select a mode first", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startPassMode() {
        // Navigate to Reading Activity with Pass Mode
        val intent = Intent(this, ReadingActivity::class.java).apply {
            putExtra("courseCode", this@PrepActivity.intent.getStringExtra("courseCode"))
            putExtra("courseName", courseTitle.text.toString())
            putExtra("collegeName", this@PrepActivity.intent.getStringExtra("collegeName"))
            putExtra("mode", "PASS")
        }
        startActivity(intent)
    }
    
    private fun startMasterMode() {
        // Navigate to Reading Activity with Master Mode
        val intent = Intent(this, ReadingActivity::class.java).apply {
            putExtra("courseCode", this@PrepActivity.intent.getStringExtra("courseCode"))
            putExtra("courseName", courseTitle.text.toString())
            putExtra("collegeName", this@PrepActivity.intent.getStringExtra("collegeName"))
            putExtra("mode", "MASTER")
        }
        startActivity(intent)
    }
    
    // Method to reset progress
    fun resetProgress() {
        passProgressValue = 0
        masterProgressValue = 0
        updateProgress()
        Toast.makeText(this, "Progress reset for both modes", Toast.LENGTH_SHORT).show()
    }
    
    // Method to get current overall progress (derived)
    fun getCurrentProgress(): Int {
        return maxOf(passProgressValue, masterProgressValue)
    }

    fun getPassProgress(): Int = passProgressValue
    fun getMasterProgress(): Int = masterProgressValue
    
    // Method to get selected mode
    fun getSelectedMode(): String? {
        return selectedMode
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh progress when returning to this activity
        loadProgressFromStorage()
        updateProgress()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Save any final progress
        saveProgressToStorage()
    }
}

// Placeholder activities for Pass and Master modes
class PassModeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set layout for Pass Mode
        // setContentView(R.layout.activity_pass_mode)
        
        val courseCode = intent.getStringExtra("courseCode") ?: "Unknown Course"
        val courseName = intent.getStringExtra("courseName") ?: "Unknown Course"
        val collegeName = intent.getStringExtra("collegeName") ?: "Unknown College"
        val mode = intent.getStringExtra("mode") ?: "PASS"
        
        Toast.makeText(this, "Starting $mode for $courseName ($courseCode) at $collegeName", Toast.LENGTH_SHORT).show()
        
        // TODO: Implement Pass Mode functionality
        // - Show easy questions
        // - Quick revision content
        // - Progress tracking for Pass Mode
    }
}

class MasterModeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set layout for Master Mode
        // setContentView(R.layout.activity_master_mode)
        
        val courseCode = intent.getStringExtra("courseCode") ?: "Unknown Course"
        val courseName = intent.getStringExtra("courseName") ?: "Unknown Course"
        val collegeName = intent.getStringExtra("collegeName") ?: "Unknown College"
        val mode = intent.getStringExtra("mode") ?: "MASTER"
        
        Toast.makeText(this, "Starting $mode for $courseName ($courseCode) at $collegeName", Toast.LENGTH_SHORT).show()
        
        // TODO: Implement Master Mode functionality
        // - Show advanced questions
        // - Comprehensive content
        // - Progress tracking for Master Mode
    }
}