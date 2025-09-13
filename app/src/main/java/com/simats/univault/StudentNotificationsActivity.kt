package com.simats.univault

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StudentNotificationsActivity : AppCompatActivity() {

    private lateinit var tvCollegeName: TextView // TextView to show the college name
    private lateinit var notificationsContainer: LinearLayout // Container to hold notification views

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_notifications)

        // Initialize views
        tvCollegeName = findViewById(R.id.tvCollegeName)
        notificationsContainer = findViewById(R.id.notifications_container)

        // Get the college name from the Intent
        var collegeName = intent.getStringExtra("college")
        if (collegeName.isNullOrEmpty()) {
            val sf = getSharedPreferences("user_sf", Context.MODE_PRIVATE)
            collegeName = sf.getString("collegeName", null)
        }
        // Set the college name to the TextView
        tvCollegeName.text = collegeName ?: "College not found"

        // Fetch notices from PHP backend
        fetchNotices(collegeName ?: "")
    }
    //changed in online mode
    private fun fetchNotices(collegeName: String) {
        val url = "http://10.86.199.54/univault/fetch_notices.php?college=$collegeName"

        Thread {
            try {
                val urlConnection = URL(url).openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.connect()

                val inputStream = urlConnection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                val jsonResponse = JSONArray(response.toString().trim())

                // Step 1: Convert JSONArray to a MutableList of JSONObject
                val noticeList = mutableListOf<JSONObject>()
                for (i in 0 until jsonResponse.length()) {
                    noticeList.add(jsonResponse.getJSONObject(i))
                }

                // Step 2: Sort the list based on schedule_date + schedule_time (latest first)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                noticeList.sortByDescending {
                    val dateTimeStr = it.getString("schedule_date") + " " + it.getString("schedule_time")
                    LocalDateTime.parse(dateTimeStr, formatter)
                }

                runOnUiThread {
                    notificationsContainer.removeAllViews()

                    if (noticeList.isNotEmpty()) {
                        for (notice in noticeList) {
                            val title = notice.getString("title")
                            val description = notice.getString("description")
                            val date = notice.getString("schedule_date")
                            val time = notice.getString("schedule_time")

                            val notificationView = LayoutInflater.from(this)
                                .inflate(R.layout.notification_item, notificationsContainer, false)

                            val tvNoticeTitle: TextView = notificationView.findViewById(R.id.tvNoticeTitle)
                            val tvNoticeDescription: TextView = notificationView.findViewById(R.id.tvNoticeDescription)

                            tvNoticeTitle.text = title
                            tvNoticeDescription.text = "$description\n$date $time"

                            notificationsContainer.addView(notificationView)
                        }
                    } else {
                        Toast.makeText(applicationContext, "No notices found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Error fetching notices: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
