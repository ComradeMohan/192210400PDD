package com.simats.univault

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AdminPostNotice : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var btnPostNotice: Button
    private lateinit var oldNoticesContainer: LinearLayout
    private var isPosting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_post_notice)

        val collegeName = intent.getStringExtra("COLLEGE_NAME") ?: ""
        progressBar = findViewById(R.id.progressBar1)
        btnPostNotice = findViewById(R.id.btnSubmitResources)
        oldNoticesContainer = findViewById(R.id.oldNoticesContainer)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { onBackPressed() }

        // Load old notices when screen opens
        loadOldNotices(collegeName)

        btnPostNotice.setOnClickListener {
            if (isPosting) return@setOnClickListener
            isPosting = true

            val noticeTitle = findViewById<EditText>(R.id.etNoticeTitle).text.toString().trim()
            val noticeDetails = findViewById<EditText>(R.id.etNoticeDetails).text.toString().trim()

            if (noticeTitle.isEmpty() || noticeDetails.isEmpty()) {
                Toast.makeText(this, "Title and Details are mandatory!", Toast.LENGTH_SHORT).show()
                isPosting = false
                return@setOnClickListener
            }

            btnPostNotice.isEnabled = false
            btnPostNotice.text = ""
            progressBar.visibility = View.VISIBLE

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val isHighPriority = findViewById<Switch>(R.id.switchHighPriority).isChecked
            val attachmentPath = ""

            postNoticeToServer(
                noticeTitle, noticeDetails, collegeName,
                currentDate, currentTime, attachmentPath, isHighPriority
            )
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            finish()
        }
    }

    private fun postNoticeToServer(
        title: String,
        details: String,
        college: String,
        scheduleDate: String,
        scheduleTime: String,
        attachmentPath: String,
        isHighPriority: Boolean
    ) {
        val url = "http://10.86.199.54/univault/post_notice.php"

        val params = HashMap<String, String>().apply {
            put("title", title)
            put("details", details)
            put("college", college)
            put("schedule_date", scheduleDate)
            put("schedule_time", scheduleTime)
            put("attachment", attachmentPath)
            put("is_high_priority", isHighPriority.toString())
        }

        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val status = json.getString("status")
                    val message = json.getString("message")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    if (status == "success") {
                        // Refresh old notices after posting
                        loadOldNotices(college)
                        findViewById<EditText>(R.id.etNoticeTitle).text.clear()
                        findViewById<EditText>(R.id.etNoticeDetails).text.clear()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                resetPostButton()
            },
            { error ->
                Toast.makeText(this, "Request failed: ${error.message}", Toast.LENGTH_SHORT).show()
                resetPostButton()
            }
        ) {
            override fun getParams(): Map<String, String> = params
        }

        request.retryPolicy = DefaultRetryPolicy(0, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        Volley.newRequestQueue(this).add(request)
    }

    private fun loadOldNotices(college: String) {
        val url = "http://10.86.199.54/univault/fetch_notifications.php?college_name=$college"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val notificationsArray = json.getJSONArray("notifications")
                        oldNoticesContainer.removeAllViews()

                        for (i in notificationsArray.length() - 1 downTo 0) {
                            val notice = notificationsArray.getJSONObject(i)
                            addNoticeView(
                                title = notice.getString("title"),
                                details = notice.getString("description"),
                                date = notice.getString("date")
                            )
                        }

                    } else {
                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to load: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        request.retryPolicy = DefaultRetryPolicy(5000, 1, 1f)
        Volley.newRequestQueue(this).add(request)
    }

    private fun addNoticeView(title: String, details: String, date: String) {
        val noticeView = LayoutInflater.from(this)
            .inflate(R.layout.item_notice, oldNoticesContainer, false)

        val titleView = noticeView.findViewById<TextView>(R.id.tvNoticeTitle)
        val detailView = noticeView.findViewById<TextView>(R.id.tvNoticeDetails)
        val dateView = noticeView.findViewById<TextView>(R.id.tvNoticeDate)
        val deleteBtn = noticeView.findViewById<ImageView>(R.id.btnDeleteNotice)

        titleView.text = title
        detailView.text = details
        dateView.text = "📅 $date"

        deleteBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Notice")
                .setMessage("Delete this notice?")
                .setPositiveButton("Yes") { _, _ ->
                    deleteNoticeFromServer(title)
                }
                .setNegativeButton("No", null)
                .show()
        }

        oldNoticesContainer.addView(noticeView)
    }

    private fun deleteNoticeFromServer(noticeTitle: String) {
        val url = "http://10.86.199.54/univault/delete_notice.php"

        val params = HashMap<String, String>()
        params["title"] = noticeTitle

        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    if (json.getString("status") == "success") {
                        loadOldNotices(intent.getStringExtra("COLLEGE_NAME") ?: "")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Delete failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> = params
        }

        request.retryPolicy = DefaultRetryPolicy(0, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        Volley.newRequestQueue(this).add(request)
    }


    private fun resetPostButton() {
        isPosting = false
        btnPostNotice.isEnabled = true
        btnPostNotice.text = "Post Notice"
        progressBar.visibility = View.GONE
    }
}
