package com.simats.univault

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class LoginActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val RC_SIGN_IN = 1001
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var generalErrorText: TextView
    private lateinit var submitLoginButton: Button
    private lateinit var progressBar: ProgressBar

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val studentNumberInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.submitLoginButton)
        val googleSignInButton = findViewById<LinearLayout>(R.id.googleLoginButton)
        val signUpTextView = findViewById<TextView>(R.id.signUp)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgotPassword)

        emailInputLayout = findViewById(R.id.emailInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        generalErrorText = findViewById(R.id.generalErrorText)

        submitLoginButton = loginButton
        progressBar = findViewById(R.id.progressBar1)

        forgotPasswordTextView.setOnClickListener { showForgotPasswordDialog() }
        signUpTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Google Sign-In config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        loginButton.setOnClickListener {
            val studentNumber = studentNumberInput.text.toString().trim()
            val password = passwordInput.text.toString()

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

            if (valid) loginUser(studentNumber, password)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email ?: return

                Log.d("GOOGLE_SIGNIN_EMAIL", "Signed-in email: $email")

                // Send only email
                sendGoogleEmailToBackend(email)

            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                Log.e("GOOGLE_SIGNIN", "Error: ${e.message}")
            }
        }
    }


    private fun showForgotPasswordDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_forgot_password_bottom_sheet, null)
        dialog.setContentView(view)

        val inputLayout = view.findViewById<TextInputLayout>(R.id.forgotInputLayout)
        val inputField = view.findViewById<EditText>(R.id.forgotInput)
        val cancelBtn = view.findViewById<Button>(R.id.forgotCancel)
        val submitBtn = view.findViewById<Button>(R.id.forgotSubmit)
        val progress = view.findViewById<ProgressBar>(R.id.forgotProgress)

        inputLayout.error = null

        cancelBtn.setOnClickListener { dialog.dismiss() }

        submitBtn.setOnClickListener {
            val userId = inputField.text?.toString()?.trim().orEmpty()
            inputLayout.error = null
            if (userId.isEmpty()) {
                inputLayout.error = "Login ID is required"
                return@setOnClickListener
            }
            // Loading state
            submitBtn.isEnabled = false
            progress.visibility = View.VISIBLE
            sendForgotPasswordRequest(userId)
            // Re-enable after short delay to keep UI responsive even if network is slow
            submitBtn.postDelayed({
                if (dialog.isShowing) {
                    submitBtn.isEnabled = true
                    progress.visibility = View.GONE
                    dialog.dismiss()
                }
            }, 600)
        }

        dialog.show()
    }

    private fun sendForgotPasswordRequest(userId: String) {
        val url = "http://10.235.18.54/univault/forgot_password.php"
        val json = JSONObject().put("student_number", userId)
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    generalErrorText.text = "Network error: ${e.message}"
                    generalErrorText.visibility = View.VISIBLE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    val msg = JSONObject(response.body?.string() ?: "{}")
                        .optString("message", "Check your email for reset instructions.")
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun loginUser(studentNumber: String, password: String) {
        val url = "http://10.235.18.54/univault/login.php"
        val json = JSONObject().put("student_number", studentNumber).put("password", password)

        submitLoginButton.text = ""
        progressBar.visibility = View.VISIBLE
        submitLoginButton.isEnabled = false

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    generalErrorText.text = "Server error: ${e.message}"
                    generalErrorText.visibility = View.VISIBLE
                    resetButtonState()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            if (jsonResponse.getBoolean("success")) {
                                val college = jsonResponse.optString("college", "")
                                val userType = jsonResponse.getString("user_type")

                                val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
                                sf.edit().putBoolean("isLoggedIn", true).apply()
                                sf.edit().putString("userType", userType).apply()
                                sf.edit().putString("userID", studentNumber).apply()
                                sf.edit().putString("college", college).apply()

                                FirebaseMessaging.getInstance().token.addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        sendTokenToServer(it.result, studentNumber, college)
                                    }
                                }

                                subscribeToUserTopics(studentNumber, userType, college)

                                when (userType.lowercase(Locale.getDefault())) {
                                    "student" -> startActivity(Intent(this@LoginActivity, StudentDashboardActivity::class.java).putExtra("ID", studentNumber))
                                    "faculty" -> startActivity(Intent(this@LoginActivity, FacultyDashboardActivity::class.java).putExtra("ID", studentNumber))
                                    "admin" -> startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java).putExtra("ID", studentNumber))
                                }
                                finish()
                            } else {
                                generalErrorText.text = jsonResponse.getString("message")
                                generalErrorText.visibility = View.VISIBLE
                                resetButtonState()
                            }
                        } catch (e: Exception) {
                            generalErrorText.text = "Invalid response  from server"
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
    private fun subscribeToUserTopics(userId: String, userType: String, college: String) {
        val normalizedCollege = college.trim().lowercase(Locale.getDefault()).replace("\\s+".toRegex(), "_")
        val normalizedRole = userType.trim().lowercase(Locale.getDefault())

        val topics = listOf(
            "all_users",
            normalizedCollege,
            "${normalizedCollege}_${normalizedRole}",
            "${normalizedRole}_${userId}",
            "${normalizedCollege}_${normalizedRole}s" // plural version
        )
        val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
        sf.edit().putStringSet("fcm_topics", topics.toSet()).apply()
        for (topic in topics) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnSuccessListener { Log.d("FCM_TOPIC", "Subscribed to $topic") }
                .addOnFailureListener { Log.e("FCM_TOPIC", "Failed to subscribe to $topic") }
        }
    }

    private fun sendTokenToServer(token: String, userId: String, college: String) {
        val url = "http://10.235.18.54/univault/save_fcm_token.php"
        val json = JSONObject().put("user_id", userId).put("fcm_token", token).put("college", college)
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM_TOKEN", "Failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("FCM_TOKEN", "Sent: ${response.code}")
            }
        })
    }

    private fun sendGoogleEmailToBackend(email: String) {
        val url = "http://10.235.18.54/univault/google_login.php"
        val json = JSONObject().apply {
            put("email", email)
        }

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GOOGLE_LOGIN", "Failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("GOOGLE_LOGIN", "Response: $responseBody")

                runOnUiThread {
                    val jsonResponse = JSONObject(responseBody ?: "{}")
                    if (jsonResponse.optBoolean("success")) {
                        val studentNumber = jsonResponse.optString("student_number", "")
                        val college = jsonResponse.optString("college", "")
                        val userType = jsonResponse.optString("user_type", "student")

                        val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
                        sf.edit().putBoolean("isLoggedIn", true).apply()
                        sf.edit().putString("userType", userType).apply()
                        sf.edit().putString("userID", studentNumber).apply()
                        sf.edit().putString("college", college).apply()
                        sf.edit().putString("email", email).apply()

                        FirebaseMessaging.getInstance().token.addOnCompleteListener {
                            if (it.isSuccessful) {
                                sendTokenToServer(it.result, studentNumber, college)
                            }
                        }


                        subscribeToUserTopics(studentNumber, userType, college)

                        val intent = Intent(this@LoginActivity, StudentDashboardActivity::class.java)
                        intent.putExtra("ID", studentNumber)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, jsonResponse.optString("message"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    private fun resetButtonState() {
        progressBar.visibility = View.GONE
        submitLoginButton.text = "Login"
        submitLoginButton.isEnabled = true
    }
}
