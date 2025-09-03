package com.simats.univault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class HomeFragment1 : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var tvNoticeTitle: TextView
    private lateinit var tvNoticeDescription: TextView

    private var collegeName: String? = null
    private var studentID: String? = null


    private lateinit var tvTotalTimeValue: TextView
    private lateinit var tvTotalCoursesValue: TextView
    private lateinit var totalTimeCard: View
    private lateinit var totalCoursesCard: View

    private lateinit var continueStudyingSection: View
    private lateinit var continueStudyingCard: View
    private lateinit var tvContinueCourseName: TextView
    private lateinit var tvContinueProgress: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentID = arguments?.getString("studentID")

        // Get student ID from arguments
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sf = requireContext().getSharedPreferences("user_sf", Context.MODE_PRIVATE)
        val savedProgress = sf.getInt("degreeProgress", 0)
        val cgpaValue = sf.getFloat("cgpaValue", 0.0F)

        val cgpaText = view.findViewById<TextView>(R.id.cgpaValue)
        val progressBar = view.findViewById<ProgressBar>(R.id.degreeProgressBar)
        val percentageText = view.findViewById<TextView>(R.id.degreeProgressPercentage)
        val predictionText = view.findViewById<TextView>(R.id.predictedCgpaTextView)

        // Set CGPA and progress
        cgpaText.text = "CGPA : %.2f".format(cgpaValue)
        progressBar.progress = savedProgress
        percentageText.text = "$savedProgress%"

        // ✅ Calculate predicted CGPA
        val roundedCgpa = cgpaValue.toInt().toFloat() // truncate decimal, e.g., 6.7 → 6
        val predictedCgpa = (roundedCgpa + cgpaValue) / 2f

        // ✅ Display predicted final CGPA
        predictionText.text = "Predicted CGPA: %.2f".format(predictedCgpa)
        
        // Debug: Check saved course data
        debugCheckSavedCourseData()
        
        // Load study statistics
        loadStudyStatistics()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh continue learning section when returning to dashboard
        loadLastStudiedCourse()
        // Refresh study statistics
        loadStudyStatistics()
    }
    
    private fun debugCheckSavedCourseData() {
        val sharedPrefs = requireContext().getSharedPreferences("LastStudied", Context.MODE_PRIVATE)
        val courseCode = sharedPrefs.getString("last_course_code", "")
        val courseName = sharedPrefs.getString("last_course_name", "")
        val mode = sharedPrefs.getString("last_mode", "PASS")
        val studyTime = sharedPrefs.getLong("last_study_time", 0)
        
        Log.d("Debug", "=== SAVED COURSE DATA ===")
        Log.d("Debug", "Course Code: $courseCode")
        Log.d("Debug", "Course Name: $courseName")
        Log.d("Debug", "Mode: $mode")
        Log.d("Debug", "Study Time: $studyTime")
        Log.d("Debug", "========================")
        
        if (!courseCode.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Found saved course: $courseName", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No saved course found", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home1, container, false)
        
        // Initialize Views
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvStudentName = view.findViewById(R.id.tvStudentName)
        tvNoticeTitle = view.findViewById(R.id.tvNoticeTitle)
        tvNoticeDescription = view.findViewById(R.id.tvNoticeDescription)
        
        // Initialize Statistics Views
        tvTotalTimeValue = view.findViewById(R.id.tvTotalTimeValue)
        tvTotalCoursesValue = view.findViewById(R.id.tvTotalCoursesValue)
        totalTimeCard = view.findViewById(R.id.totalTimeCard)
        totalCoursesCard = view.findViewById(R.id.totalCoursesCard)
        
        // Set up Statistics card click listeners (after views are initialized)
        totalTimeCard.setOnClickListener {
            val totalTime = getTotalReadingTimeFromCache()
            val hours = (totalTime / (1000 * 60 * 60)).toInt()
            val minutes = ((totalTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
            val message = if (hours > 0) {
                "Total reading time: ${hours}h ${minutes}m"
            } else {
                "Total reading time: ${minutes}m"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
        
        totalCoursesCard.setOnClickListener {
            Toast.makeText(requireContext(), "Total courses available (from server)", Toast.LENGTH_SHORT).show()
        }

        // Initialize Continue Studying Views
        continueStudyingSection = view.findViewById(R.id.continueStudyingSection)
        continueStudyingCard = view.findViewById(R.id.continueStudyingCard)
        tvContinueCourseName = view.findViewById(R.id.tvContinueCourseName)
        tvContinueProgress = view.findViewById(R.id.tvContinueProgress)

        // Set up Continue Studying click listener
        continueStudyingCard.setOnClickListener {
            val sharedPrefs = requireContext().getSharedPreferences("LastStudied", Context.MODE_PRIVATE)
            val courseCode = sharedPrefs.getString("last_course_code", "")
            val courseName = sharedPrefs.getString("last_course_name", "")
            val mode = sharedPrefs.getString("last_mode", "PASS")
            
            if (!courseCode.isNullOrEmpty()) {
                val intent = Intent(requireContext(), ReadingActivity::class.java)
                intent.putExtra("courseCode", courseCode)
                intent.putExtra("courseName", courseName)
                intent.putExtra("collegeName", collegeName)
                intent.putExtra("mode", mode)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "No recent study session found", Toast.LENGTH_SHORT).show()
            }
        }

        val notificationIcon: View = view.findViewById(R.id.notificationIcon)
        notificationIcon.setOnClickListener {
            val intent = Intent(requireContext(), StudentNotificationsActivity::class.java)
            intent.putExtra("college", collegeName) // Pass college name to the next activity
            startActivity(intent)
        }


        tvGreeting.text = getGreetingMessage()

        // Fetch student name and college
        studentID?.let {
            fetchStudentName(it)
        } ?: run {
            tvStudentName.text = "ID not found"
        }

        return view
    }
    /**
     * Load and display study statistics
     */
    private fun loadStudyStatistics() {
        loadTotalStudyTime()
        loadTotalCoursesCount()
    }
    
    /**
     * Calculate and display total study time across all reading sessions from cache
     */
    private fun loadTotalStudyTime() {
        // Get total reading time from cache (not grouped by courses)
        val totalReadingTime = getTotalReadingTimeFromCache()
        
        // Convert to hours and minutes
        val totalHours = (totalReadingTime / (1000 * 60 * 60)).toInt()
        val totalMinutes = ((totalReadingTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        
        // Update UI
        val timeText = if (totalReadingTime == 0L) {
            "0m"
        } else if (totalHours > 0) {
            "${totalHours}h ${totalMinutes}m"
        } else {
            "${totalMinutes}m"
        }
        
        tvTotalTimeValue.text = timeText
        
        Log.d("StudyStats", "Total reading time from cache: $timeText (${totalReadingTime}ms)")
    }
    
    /**
     * Get total reading time from cache/session data across all reading activities
     */
    private fun getTotalReadingTimeFromCache(): Long {
        val context = requireContext()
        var totalTime = 0L
        
        // Method 1: Get from study_time_prefs (individual course times)
        val studyTimePrefs = context.getSharedPreferences("study_time_prefs", Context.MODE_PRIVATE)
        val studyTimeEntries = studyTimePrefs.all
        for ((key, value) in studyTimeEntries) {
            if (key.startsWith("total_study_time_") && value is Long) {
                totalTime += value
                Log.d("StudyStats", "Found study time: $key = ${value}ms")
            }
        }
        
        // Method 2: Get from general reading session cache (if exists)
        val sessionPrefs = context.getSharedPreferences("reading_sessions", Context.MODE_PRIVATE)
        val globalReadingTime = sessionPrefs.getLong("total_reading_time_millis", 0L)
        if (globalReadingTime > 0) {
            totalTime = maxOf(totalTime, globalReadingTime) // Use the larger value
            Log.d("StudyStats", "Found global reading time: ${globalReadingTime}ms")
        }
        
        // Method 3: Check for any active/recent session data
        val currentSessionPrefs = context.getSharedPreferences("current_session", Context.MODE_PRIVATE)
        val activeSessionTime = currentSessionPrefs.getLong("active_session_time", 0L)
        if (activeSessionTime > 0) {
            totalTime += activeSessionTime
            Log.d("StudyStats", "Found active session time: ${activeSessionTime}ms")
        }
        
        // Method 4: Sum up all reading activities from cache
        val readingActivitiesPrefs = context.getSharedPreferences("reading_activities", Context.MODE_PRIVATE)
        val activitiesEntries = readingActivitiesPrefs.all
        var activitiesTotal = 0L
        for ((key, value) in activitiesEntries) {
            if (key.contains("reading_time") && value is Long) {
                activitiesTotal += value
                Log.d("StudyStats", "Found reading activity: $key = ${value}ms")
            }
        }
        
        // Use the maximum of all methods to avoid double counting
        val finalTotal = maxOf(totalTime, activitiesTotal)
        
        Log.d("StudyStats", "Cache summary - Study time: ${totalTime}ms, Activities: ${activitiesTotal}ms, Final: ${finalTotal}ms")
        return finalTotal
    }
    
    /**
     * Save reading session time to cache (call this method when reading session ends)
     */
    private fun saveReadingSessionToCache(sessionDurationMillis: Long) {
        val context = requireContext()
        
        // Save to global reading time cache
        val sessionPrefs = context.getSharedPreferences("reading_sessions", Context.MODE_PRIVATE)
        val currentTotal = sessionPrefs.getLong("total_reading_time_millis", 0L)
        val newTotal = currentTotal + sessionDurationMillis
        
        sessionPrefs.edit()
            .putLong("total_reading_time_millis", newTotal)
            .putLong("last_session_time", sessionDurationMillis)
            .putLong("last_session_timestamp", System.currentTimeMillis())
            .apply()
        
        // Also save to reading activities cache with timestamp
        val activitiesPrefs = context.getSharedPreferences("reading_activities", Context.MODE_PRIVATE)
        val sessionKey = "reading_time_${System.currentTimeMillis()}"
        activitiesPrefs.edit()
            .putLong(sessionKey, sessionDurationMillis)
            .apply()
        
        Log.d("StudyStats", "Saved reading session: ${sessionDurationMillis}ms, New total: ${newTotal}ms")
        
        // Refresh the UI after saving
        loadTotalStudyTime()
    }
    
    /**
     * Clear reading session cache (for testing or reset purposes)
     */
    private fun clearReadingSessionCache() {
        val context = requireContext()
        
        // Clear all reading session caches
        context.getSharedPreferences("reading_sessions", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("reading_activities", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("current_session", Context.MODE_PRIVATE).edit().clear().apply()
        
        Log.d("StudyStats", "Cleared all reading session cache")
        
        // Refresh the UI after clearing
        loadTotalStudyTime()
    }
    
    /**
     * Calculate and display total number of courses studied
     */
    private fun loadTotalCoursesCount() {
        // First try to get count from backend API
        fetchCourseCountFromAPI()
        
        // Fallback: Calculate from local storage
        calculateLocalCourseCount()
    }
    
    /**
     * Fetch course count from backend API
     */
    private fun fetchCourseCountFromAPI() {
        val url = "http://10.137.118.54/univault/get_course_count.php"
        val queue = Volley.newRequestQueue(requireContext())
        
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val totalCourses = response.getInt("total_courses")
                        tvTotalCoursesValue.text = totalCourses.toString()
                        Log.d("StudyStats", "Total courses from API: $totalCourses")
                    } else {
                        Log.w("StudyStats", "API response indicates failure, using local calculation")
                        calculateLocalCourseCount()
                    }
                } catch (e: Exception) {
                    Log.e("StudyStats", "Error parsing course count response: ${e.message}")
                    calculateLocalCourseCount()
                }
            },
            { error ->
                Log.e("StudyStats", "Failed to fetch course count from API: ${error.message}")
                // Fallback to local calculation
                calculateLocalCourseCount()
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    /**
     * Calculate course count from local storage (fallback method)
     */
    private fun calculateLocalCourseCount() {
        val sharedPreferences = requireContext().getSharedPreferences("study_time_prefs", Context.MODE_PRIVATE)
        val lastStudiedPrefs = requireContext().getSharedPreferences("LastStudied", Context.MODE_PRIVATE)
        
        // Get unique course codes from study time preferences
        val courseCodesFromStudyTime = mutableSetOf<String>()
        val allEntries = sharedPreferences.all
        for (key in allEntries.keys) {
            if (key.startsWith("total_study_time_")) {
                val courseCode = key.removePrefix("total_study_time_")
                if (courseCode.isNotEmpty()) {
                    courseCodesFromStudyTime.add(courseCode)
                }
            }
        }
        
        // Also check if there's a last studied course not yet in study time
        val lastCourseCode = lastStudiedPrefs.getString("last_course_code", "")
        if (!lastCourseCode.isNullOrEmpty()) {
            courseCodesFromStudyTime.add(lastCourseCode)
        }
        
        val totalCourses = courseCodesFromStudyTime.size
        tvTotalCoursesValue.text = totalCourses.toString()
        
        Log.d("StudyStats", "Total courses from local: $totalCourses")
        Log.d("StudyStats", "Course codes: $courseCodesFromStudyTime")
    }

    private fun animateTyping(text: String, textView: TextView, delay: Long = 100L) {
        val handler = android.os.Handler()
        var index = 0

        handler.post(object : Runnable {
            override fun run() {
                if (index <= text.length) {
                    // Show partial text with a cursor
                    textView.text = text.substring(0, index) + "|"
                    index++
                    handler.postDelayed(this, delay)
                } else {
                    // When done typing, remove the cursor
                    textView.text = text
                }
            }
        })
    }


    private fun fetchStudentName(studentID: String) {
        val url = "http://10.137.118.54/univault/fetch_student_name.php?studentID=$studentID"
        val queue = Volley.newRequestQueue(requireContext())

        val jsonObjectRequest = JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            { response ->
                if (!isAdded) return@JsonObjectRequest  // Check if fragment is still attached
                val success = response.getBoolean("success")
                if (success) {
                    val name = response.getString("name")
                    collegeName = response.getString("college")
                    val sf = requireContext().getSharedPreferences("user_sf", Context.MODE_PRIVATE)
                    sf.edit().putString("collegeName", collegeName).apply()
                    val departmentName = response.getString("dept")
                    // Check if fragment is still attached before using context
                    if (isAdded) {
                        fetchCollegeIdByName(collegeName?.toString() ?: "", requireContext()) { collegeId ->
                            if (collegeId != null) {
                                Log.d("CollegeID", "Fetched ID: $collegeId")
                                Toast.makeText(requireContext(), " college ID: $collegeId", Toast.LENGTH_SHORT).show()

                                fetchDepartmentId(collegeId, departmentName, requireContext()) { departmentId ->
                                    if (departmentId != null) {
                                        Log.d("DepartmentID", "Fetched ID: $departmentId")
                                        Toast.makeText(requireContext(), " Dept ID: $departmentId", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to fetch department ID", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(requireContext(), "Failed to fetch college ID", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    animateTyping(name, tvStudentName)
                    collegeName?.let { fetchLatestNotice(it) }
                } else {
                    tvStudentName.text = "Student not found"
                }
            },
            {
                if (isAdded) {
                    Toast.makeText(context, "Error fetching student data", Toast.LENGTH_SHORT).show()
                }
            }
        )

        queue.add(jsonObjectRequest)
    }

    fun fetchCollegeIdByName(collegeName: String, context: Context, callback: (Int?) -> Unit) {
        val url = "http://10.137.118.54/univault/get_college_id.php" // Replace with your actual URL

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        val collegeId = jsonObject.getInt("college_id")
                        callback(collegeId) // Pass the college ID to the callback
                    } else {
                        Toast.makeText(context, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                        callback(null) // Return null if college not found
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Toast.makeText(context, "Request failed: ${error.message}", Toast.LENGTH_SHORT).show()
                callback(null)
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf("college_name" to collegeName)
            }
        }

        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(stringRequest)
    }
    fun fetchDepartmentId(collegeId: Int, departmentName: String, context: Context, callback: (Int?) -> Unit) {
        val url = "http://10.137.118.54/univault/get_department_id.php" // Replace with your PHP file URL

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                try {
                    // Parse the response as a JSON object
                    val jsonObject = JSONObject(response)
                    // Check for success in the response
                    if (jsonObject.getBoolean("success")) {
                        // Extract the department ID
                        val departmentId = jsonObject.getInt("department_id")
                        val sf = requireContext().getSharedPreferences("user_sf", Context.MODE_PRIVATE)
                        sf.edit().putInt("collegeId", collegeId).apply()
                        sf.edit().putInt("departmentId", departmentId).apply()
                        
                        // Note: Removed fetchPendingSubjects as we're now showing statistics
                        // Pass the department ID to the callback
                        callback(departmentId)
                    } else {
                        // If department is not found, show an error message
                        Toast.makeText(context, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                        // Pass null if the department ID is not found
                        callback(null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle parsing errors and show a Toast message
                    Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show()
                    // Pass null if there was an error
                    callback(null)
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                // Handle network errors
                Toast.makeText(context, "Request failed: ${error.message}", Toast.LENGTH_SHORT).show()
                // Pass null if the request failed
                callback(null)
            }
        ) {
            // Set the POST parameters to send to the backend
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "college_id" to collegeId.toString(),
                    "name" to departmentName
                )
            }
        }

        // Initialize the request queue and add the request to it
        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(stringRequest)
    }


    private fun fetchLatestNotice(college: String) {
        val url = "http://10.137.118.54/univault/get_latest_notice.php?college=$college"
        val ctx = context ?: return  // Safely get context or return if fragment is not attached
        val queue = Volley.newRequestQueue(ctx)

        val jsonObjectRequest = JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            { response ->
                if (!isAdded) return@JsonObjectRequest  // Fragment no longer valid
                val success = response.getBoolean("success")
                if (success) {
                    val title = response.getString("title")
                    val description = response.getString("description")
                    tvNoticeTitle.text = title
                    tvNoticeDescription.text = description
                } else {
                    tvNoticeTitle.text = "No Notices"
                    tvNoticeDescription.text = ""
                }
            },
            {
                if (!isAdded) return@JsonObjectRequest
                tvNoticeTitle.text = "Error"
                tvNoticeDescription.text = "Failed to fetch notice."
            }
        )
        queue.add(jsonObjectRequest)
    }


    private fun getGreetingMessage(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            in 17..20 -> "Good Evening,"
            else -> "Good Night,"
        }
    }

    private fun loadLastStudiedCourse() {
        val sharedPrefs = requireContext().getSharedPreferences("LastStudied", Context.MODE_PRIVATE)
        val courseCode = sharedPrefs.getString("last_course_code", "")
        val courseName = sharedPrefs.getString("last_course_name", "")
        val mode = sharedPrefs.getString("last_mode", "PASS")
        
        Log.d("ContinueLearning", "Checking saved course - Code: $courseCode, Name: $courseName, Mode: $mode")
        
        if (!courseCode.isNullOrEmpty() && !courseName.isNullOrEmpty()) {
            // Get progress for this course
            val topicProgressPrefs = requireContext().getSharedPreferences("TopicProgress", Context.MODE_PRIVATE)
            val modeSuffix = when (mode) {
                "PASS" -> "PASS"
                "MASTER" -> "MASTER"
                else -> "ALL"
            }
            
            // Count completed topics
            val allKeys = topicProgressPrefs.all.keys.filter { 
                it.startsWith("topic_${courseCode}_${modeSuffix}_") 
            }
            
            val totalTopics = allKeys.size
            val completedTopics = allKeys.count { topicProgressPrefs.getBoolean(it, false) }
            
            Log.d("ContinueLearning", "Found $totalTopics total topics, $completedTopics completed")
            
            // Update UI
            tvContinueCourseName.text = "$courseName - $mode Mode"
            tvContinueProgress.text = "Progress: $completedTopics/$totalTopics topics"
            
            // Show the section
            continueStudyingSection.visibility = View.VISIBLE
            Log.d("ContinueLearning", "Showing continue studying section")
        } else {
            // Hide the section if no last studied course
            continueStudyingSection.visibility = View.GONE
            Log.d("ContinueLearning", "No saved course found, hiding section")
        }
    }

    companion object {
        fun newInstance(studentID: String): HomeFragment1 {
            val fragment = HomeFragment1()
            val args = Bundle()
            args.putString("studentID", studentID)
            fragment.arguments = args
            return fragment
        }
        
        /**
         * Call this method from ReadingActivity or other activities when a reading session ends
         */
        fun saveReadingSession(context: Context, sessionDurationMillis: Long, courseCode: String? = null) {
            // Save to global reading time cache
            val sessionPrefs = context.getSharedPreferences("reading_sessions", Context.MODE_PRIVATE)
            val currentTotal = sessionPrefs.getLong("total_reading_time_millis", 0L)
            val newTotal = currentTotal + sessionDurationMillis
            
            sessionPrefs.edit()
                .putLong("total_reading_time_millis", newTotal)
                .putLong("last_session_time", sessionDurationMillis)
                .putLong("last_session_timestamp", System.currentTimeMillis())
                .putString("last_course_code", courseCode ?: "unknown")
                .apply()
            
            // Also save individual session
            val activitiesPrefs = context.getSharedPreferences("reading_activities", Context.MODE_PRIVATE)
            val sessionKey = "reading_time_${System.currentTimeMillis()}"
            activitiesPrefs.edit()
                .putLong(sessionKey, sessionDurationMillis)
                .putString("${sessionKey}_course", courseCode ?: "unknown")
                .apply()
            
            Log.d("StudyStats", "Static: Saved reading session: ${sessionDurationMillis}ms, New total: ${newTotal}ms, Course: $courseCode")
        }
        
        /**
         * Get current total reading time from cache (can be called from anywhere)
         */
        fun getTotalReadingTime(context: Context): Long {
            val sessionPrefs = context.getSharedPreferences("reading_sessions", Context.MODE_PRIVATE)
            return sessionPrefs.getLong("total_reading_time_millis", 0L)
        }
    }
}
