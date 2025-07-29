package com.simats.univalut

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AdminPostNotice : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var btnPostNotice: Button
    private var isPosting = false // Flag to prevent double posts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_post_notice)

        val collegeName = intent.getStringExtra("COLLEGE_NAME")
        progressBar = findViewById(R.id.progressBar1)
        btnPostNotice = findViewById(R.id.btnSubmitResources)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { onBackPressed() }

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
        college: String?,
        scheduleDate: String,
        scheduleTime: String,
        attachmentPath: String,
        isHighPriority: Boolean
    ) {
        val url = "http://10.143.152.54/univault/post_notice.php"

        val params = HashMap<String, String>().apply {
            put("title", title)
            put("details", details)
            put("college", college ?: "")
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
                    if (status == "success") finish()
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

        // Prevent retries if request times out
        request.retryPolicy = DefaultRetryPolicy(
            0, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun resetPostButton() {
        isPosting = false
        btnPostNotice.isEnabled = true
        btnPostNotice.text = "Post Notice"
        progressBar.visibility = View.GONE
    }
}
