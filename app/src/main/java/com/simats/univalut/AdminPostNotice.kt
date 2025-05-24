package com.simats.univalut
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AdminPostNotice : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_post_notice)

        val collegeName = intent.getStringExtra("COLLEGE_NAME")

        // Back button (top-left)
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // or finish()
        }

        // Post Notice button
        val btnPostNotice = findViewById<Button>(R.id.btnSubmitResources)
        btnPostNotice.setOnClickListener {
            // Get input values
            val noticeTitle = findViewById<EditText>(R.id.etNoticeTitle).text.toString().trim()
            val noticeDetails = findViewById<EditText>(R.id.etNoticeDetails).text.toString().trim()

            // Validate mandatory fields (title and details)
            if (noticeTitle.isEmpty() || noticeDetails.isEmpty()) {
                Toast.makeText(this, "Title and Details are mandatory!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get current date and time if not provided
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            // Optional data: handle high priority switch and attachment (if any)
            val isHighPriority = findViewById<Switch>(R.id.switchHighPriority).isChecked
            val attachmentPath = "" // You can add file upload logic here if needed

            // Call method to post notice data
            postNoticeToServer(
                noticeTitle, noticeDetails, collegeName, currentDate, currentTime, attachmentPath, isHighPriority
            )
        }

        // Cancel button
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            finish() // Or clear inputs as needed
        }
    }

    // Function to post notice to the server (replace with your actual server endpoint)
    private fun postNoticeToServer(
        title: String,
        details: String,
        college: String?,
        scheduleDate: String,
        scheduleTime: String,
        attachmentPath: String,
        isHighPriority: Boolean
    ) {
        // URL to your PHP script (change it based on your local server)
        val url = "https://api-9buk.onrender.com/post_notice.php" // Use '10.0.2.2' for Android Emulator, or your server IP

        // Create the request parameters (add more as needed)
        val params = HashMap<String, String>()
        params["title"] = title
        params["details"] = details
        params["college"] = college ?: ""
        params["schedule_date"] = scheduleDate
        params["schedule_time"] = scheduleTime
        params["attachment"] = attachmentPath
        params["is_high_priority"] = isHighPriority.toString()

        // Create the StringRequest for Volley
        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.getString("status")
                    val message = jsonResponse.getString("message")

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                    if (status == "success") {
                        finish() // Or navigate to another screen
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Request failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return params
            }
        }

        // Add request to Volley queue
        Volley.newRequestQueue(this).add(request)
    }
}
