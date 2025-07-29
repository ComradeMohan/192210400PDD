package com.simats.univalut

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var generalErrorText: TextView

    private lateinit var submitLoginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)



        val studentNumberInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.submitLoginButton)
        val signUpTextView = findViewById<TextView>(R.id.signUp)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgotPassword)

        emailInputLayout = findViewById(R.id.emailInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        generalErrorText = findViewById(R.id.generalErrorText)

        submitLoginButton = findViewById(R.id.submitLoginButton)
        progressBar = findViewById(R.id.progressBar1)

        forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }

        signUpTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            val studentNumber = studentNumberInput.text.toString().trim()
            val password = passwordInput.text.toString()

            // Clear previous errors
            emailInputLayout.error = null
            passwordInputLayout.error = null
            generalErrorText.visibility = View.GONE
            generalErrorText.text = ""

            var valid = true

            if (studentNumber.isEmpty()) {
                emailInputLayout.error = "Login ID is required"
                valid = false
            }
            if (password.isEmpty()) {
                passwordInputLayout.error = "Password is required"
                valid = false
            }

            if (valid) {
                loginUser(studentNumber, password)
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Forgot Password")

        val input = EditText(this)
        input.hint = "Enter your Login ID"
        builder.setView(input)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val userId = input.text.toString().trim()
            if (userId.isNotEmpty()) {
                sendForgotPasswordRequest(userId)
            } else {
                Toast.makeText(this, "Please enter your Login ID", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun sendForgotPasswordRequest(userId: String) {
        val url = "http://10.143.152.54/univault/forgot_password.php" // Adjust if needed

        val json = JSONObject().apply {
            put("student_number", userId)
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    generalErrorText.text = "Network error: ${e.message}"
                    generalErrorText.visibility = View.VISIBLE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val message = jsonResponse.optString("message", "Check your email for reset instructions.")
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@LoginActivity, "Invalid response from server", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Server error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun loginUser(studentNumber: String, password: String) {
        val url = "http://10.143.152.54/univault/login.php" // Adjust if needed

        val json = JSONObject().apply {
            put("student_number", studentNumber)
            put("password", password)
        }

        submitLoginButton.text = ""
        progressBar.visibility = View.VISIBLE
        submitLoginButton.isEnabled = false

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder().url(url).post(body).build()

        val name: String = studentNumber

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    generalErrorText.text = "Server error: ${e.message}"
                    generalErrorText.visibility = View.VISIBLE
                    resetButtonState()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            if (jsonResponse.getBoolean("success")) {
                                val college = jsonResponse.optString("college", "") // Get college from response

                                // Get FCM token and send it to backend
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val token = task.result
                                        Log.d("FCM_TOKEN", "Device token: $token")

                                        // Send token to backend server, also send college
                                        token?.let { sendTokenToServer(it, name, college) }
                                    } else {
                                        Log.e("FCM_TOKEN", "Failed to get token", task.exception)
                                    }
                                }

                                val userType = jsonResponse.getString("user_type")
                                val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
                                sf.edit().putBoolean("isLoggedIn", true).apply()
                                sf.edit().putString("userType", userType).apply()
                                sf.edit().putString("userID", name).apply()
                                sf.edit().putString("college", college).apply() // Save college in shared prefs


                                // Subscribe to college topic if non-empty and clean the topic string to avoid invalid chars
                                if (college.isNotEmpty()) {
                                    val sanitizedCollegeTopic = college.lowercase(Locale.getDefault())
                                        .replace("\\s+".toRegex(), "_")
                                    FirebaseMessaging.getInstance().subscribeToTopic(sanitizedCollegeTopic)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Log.d("FCM_TOPIC", "Subscribed to college topic: $sanitizedCollegeTopic")
                                            } else {
                                                Log.e("FCM_TOPIC", "Failed to subscribe to college topic", task.exception)
                                            }
                                        }
                                }

                                when (userType.lowercase(Locale.getDefault())) {
                                    "student" -> {
                                        val topic = "${college.lowercase(Locale.getDefault()).replace("\\s+".toRegex(), "_")}_students"
                                        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Log.d("FCM_TOPIC", "Subscribed to $topic")
                                                } else {
                                                    Log.e("FCM_TOPIC", "Failed to subscribe to $topic", task.exception)
                                                }
                                            }
                                        val intent = Intent(this@LoginActivity, StudentDashboardActivity::class.java)
                                        intent.putExtra("ID", name)
                                        startActivity(intent)
                                    }
                                    "faculty" -> {
                                        val topic = "${college.lowercase(Locale.getDefault()).replace("\\s+".toRegex(), "_")}_faculty"
                                        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Log.d("FCM_TOPIC", "Subscribed to $topic")
                                                } else {
                                                    Log.e("FCM_TOPIC", "Failed to subscribe to $topic", task.exception)
                                                }
                                            }
                                        val intent = Intent(this@LoginActivity, FacultyDashboardActivity::class.java)
                                        intent.putExtra("ID", name)
                                        startActivity(intent)
                                    }
                                    "admin" -> {
                                        val topic = "${college.lowercase(Locale.getDefault()).replace("\\s+".toRegex(), "_")}_admins"
                                        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Log.d("FCM_TOPIC", "Subscribed to $topic")
                                                } else {
                                                    Log.e("FCM_TOPIC", "Failed to subscribe to $topic", task.exception)
                                                }
                                            }
                                        val intent = Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                                        intent.putExtra("ID", name)
                                        startActivity(intent)
                                    }
                                }


                                finish()
                            }
                            else {
                                generalErrorText.text = jsonResponse.getString("message")
                                generalErrorText.visibility = View.VISIBLE
                                resetButtonState()
                            }
                        } catch (e: Exception) {
                            generalErrorText.text = "Invalid response from server"
                            generalErrorText.visibility = View.VISIBLE
                            resetButtonState()
                        }
                    } else {
                        generalErrorText.text = "Server error: ${response.code}"
                        generalErrorText.visibility = View.VISIBLE
                        resetButtonState()
                    }
                }
            }
        })
    }

    private fun sendTokenToServer(token: String, userId: String, college: String) {
        val url = "http://10.143.152.54/univault/save_fcm_token.php"

        val json = JSONObject().apply {
            put("user_id", userId)
            put("fcm_token", token)
            put("college", college) // Send college info
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM_TOKEN", "Failed to send token to server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("FCM_TOKEN", "Token sent to server, response code: ${response.code}")
                Log.d("FCM_TOKEN", "Response body: ${response.body?.string()}")
            }
        })
    }



    private fun resetButtonState() {
        progressBar.visibility = View.GONE
        submitLoginButton.text = "Login"
        submitLoginButton.isEnabled = true
    }
}
