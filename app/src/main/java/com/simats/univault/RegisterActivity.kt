package com.simats.univault

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var collegeAutoCompleteTextView: AutoCompleteTextView
    private lateinit var departmentAutoCompleteTextView: AutoCompleteTextView
    private lateinit var yearOfStudyAutoCompleteTextView: AutoCompleteTextView

    private var selectedCollegeId: String? = null
    private val collegeMap = mutableMapOf<String, String>()

    private lateinit var createAccountButton1: Button
    private lateinit var progressBar: ProgressBar

    private val RC_GOOGLE_REGISTER = 9001
    private lateinit var googleSignInClient: GoogleSignInClient

    private val collegeDomainMap = mapOf(
        "Saveetha School of Engineering" to "saveetha",
        "Panmialar Engineering College" to "panimalar",
        "Saveetha Engineering College" to "sec"
    )
    override fun onResume() {
        super.onResume()
        googleSignInClient.signOut()
    }
    private fun generateStrongPassword(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()_-+=<>?"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val fullNameInput = findViewById<EditText>(R.id.fullNameInput)
        val studentNumberInput = findViewById<EditText>(R.id.studentNumberInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        departmentAutoCompleteTextView = findViewById(R.id.departmentAutoCompleteTextView)
        yearOfStudyAutoCompleteTextView = findViewById(R.id.yearOfStudyAutoCompleteTextView)
        collegeAutoCompleteTextView = findViewById(R.id.collegeAutoCompleteTextView)
        val createAccountButton = findViewById<Button>(R.id.createAccountButton)
        val alreadyHaveAccountTextView = findViewById<TextView>(R.id.alreadyHaveAccountTextView)

        createAccountButton1 = createAccountButton
        progressBar = findViewById(R.id.progressBar)

        // Google Sign-In setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<LinearLayout>(R.id.googleLoginButton).setOnClickListener {
            val intent = googleSignInClient.signInIntent
            googleSignInClient.signOut().addOnCompleteListener {
                startActivityForResult(intent, RC_GOOGLE_REGISTER)
            }
        }

        val years = arrayOf("First Year", "Second Year", "Third Year", "Fourth Year")
        yearOfStudyAutoCompleteTextView.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years))

        fetchCollegeList()

        createAccountButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val studentNumber = studentNumberInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val department = departmentAutoCompleteTextView.text.toString().trim()
            val yearOfStudy = yearOfStudyAutoCompleteTextView.text.toString().trim()
            val college = collegeAutoCompleteTextView.text.toString().trim()

            if (fullName.isEmpty() || studentNumber.isEmpty() || email.isEmpty() || password.isEmpty() ||
                 department.isEmpty() || yearOfStudy.isEmpty() || college.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                val requiredDomain = collegeDomainMap[college]
                if (requiredDomain != null && !email.contains(requiredDomain, ignoreCase = true)) {
                    emailInput.error = "Email must contain \"$requiredDomain\""
                    emailInput.requestFocus()
                    return@setOnClickListener
                }

                registerUser(fullName, studentNumber, email, password, department, yearOfStudy, college)
            }
        }

        alreadyHaveAccountTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_REGISTER) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email ?: ""
                val name = account?.displayName ?: ""

                if (email.endsWith("@gmail.com", ignoreCase = true)) {
                    Toast.makeText(this, "Please use your college email, not Gmail.", Toast.LENGTH_LONG).show()
                    return
                }

                val password = generateStrongPassword()

                val emailInput = findViewById<EditText>(R.id.emailInput)
                val fullNameInput = findViewById<EditText>(R.id.fullNameInput)
                val passwordInput = findViewById<EditText>(R.id.passwordInput)





                passwordInput.setText(password)


// Make both fields visible (unmasked)
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD


// Move cursor to the end
                passwordInput.setSelection(password.length)


                // Focus on student number field and show keyboard
                val studentNumberInput = findViewById<EditText>(R.id.studentNumberInput)
                studentNumberInput.requestFocus()

                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(studentNumberInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)


                emailInput.setText(email)
                fullNameInput.setText(name)
                passwordInput.setText(password)

                // Copy to clipboard
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Password", password)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show()

            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                Log.e("GOOGLE_REGISTER", "Error code: ${e.statusCode}", e)
            }
        }
    }


    private fun fetchCollegeList() {
        val url = "http://192.168.43.209/univault/get_colleges.php"

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Failed to fetch colleges: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val colleges = mutableListOf<String>()
                val responseBody = response.body?.string()

                Log.d("CollegeResponse", "Raw response: $responseBody")
                try {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        val jsonArray = jsonResponse.getJSONArray("colleges")
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val id = obj.getString("id")
                            val name = obj.getString("name")
                            colleges.add(name)
                            collegeMap[name] = id
                        }

                        runOnUiThread {
                            val adapter = ArrayAdapter(this@RegisterActivity, android.R.layout.simple_dropdown_item_1line, colleges)
                            collegeAutoCompleteTextView.setAdapter(adapter)

                            collegeAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                                val selectedCollege = colleges[position]
                                selectedCollegeId = collegeMap[selectedCollege]
                                departmentAutoCompleteTextView.setText("")
                                selectedCollegeId?.let { fetchDepartments(it) }
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@RegisterActivity, "Failed to fetch colleges", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Error parsing colleges: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Error parsing colleges", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun fetchDepartments(collegeId: String) {
        val url = "http://192.168.43.209/univault/fetch_departments_by_college.php?college_id=$collegeId"

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Failed to fetch departments: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val departments = mutableListOf<String>()
                val responseBody = response.body?.string()
                try {
                    val jsonResponse = JSONArray(responseBody)

                    for (i in 0 until jsonResponse.length()) {
                        val obj = jsonResponse.getJSONObject(i)
                        val name = obj.getString("name")
                        departments.add(name)
                    }

                    runOnUiThread {
                        val adapter = ArrayAdapter(this@RegisterActivity, android.R.layout.simple_dropdown_item_1line, departments)
                        departmentAutoCompleteTextView.setAdapter(adapter)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Error parsing departments", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun registerUser(
        fullName: String,
        studentNumber: String,
        email: String,
        password: String,
        department: String,
        yearOfStudy: String,
        college: String
    ) {
        val url = "http://192.168.43.209/univault/register-smtp.php"

        val json = JSONObject().apply {
            put("full_name", fullName)
            put("student_number", studentNumber)
            put("email", email)
            put("password", password)
            put("department", department)
            put("year_of_study", yearOfStudy)
            put("college", college)
        }

        createAccountButton1.text = ""
        progressBar.visibility = View.VISIBLE
        createAccountButton1.isEnabled = false

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                                resetButtonState()
                                Toast.makeText(this@RegisterActivity, "Registration successful! Verify your college mail.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                finish()
                            } else {
                                val message = jsonResponse.optString("message", "Registration failed")
                                resetButtonState()
                                Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            resetButtonState()
                            Toast.makeText(this@RegisterActivity, "Invalid server response", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        resetButtonState()
                        Toast.makeText(this@RegisterActivity, "Server error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    fun resetButtonState() {
        progressBar.visibility = View.GONE
        createAccountButton1.text = "Create Account"
        createAccountButton1.isEnabled = true
    }
}
