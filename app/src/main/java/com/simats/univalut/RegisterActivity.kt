package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
    private val collegeMap = mutableMapOf<String, String>()  // Maps college name → ID

    //update
    private lateinit var createAccountButton1: Button
    private lateinit var progressBar: ProgressBar


    private val collegeDomainMap = mapOf(
        "Saveetha School of Engineering" to "saveetha",
        "Panmialar Engineering College" to "panimalar",
        "Saveetha Engineering College" to "sec"
        // Add other colleges and their domains here
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val fullNameInput = findViewById<EditText>(R.id.fullNameInput)
        val studentNumberInput = findViewById<EditText>(R.id.studentNumberInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        departmentAutoCompleteTextView = findViewById(R.id.departmentAutoCompleteTextView)
        yearOfStudyAutoCompleteTextView = findViewById(R.id.yearOfStudyAutoCompleteTextView)
        collegeAutoCompleteTextView = findViewById(R.id.collegeAutoCompleteTextView)

        val createAccountButton = findViewById<Button>(R.id.createAccountButton)
        val alreadyHaveAccountTextView = findViewById<TextView>(R.id.alreadyHaveAccountTextView)


        //update

        createAccountButton1 = findViewById(R.id.createAccountButton)
        progressBar = findViewById(R.id.progressBar)



        val years = arrayOf("First Year", "Second Year", "Third Year", "Fourth Year")
        yearOfStudyAutoCompleteTextView.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years))

        // Fetch college list
        fetchCollegeList()

        createAccountButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val studentNumber = studentNumberInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            val department = departmentAutoCompleteTextView.text.toString().trim()
            val yearOfStudy = yearOfStudyAutoCompleteTextView.text.toString().trim()
            val college = collegeAutoCompleteTextView.text.toString().trim()

            if (fullName.isEmpty() || studentNumber.isEmpty() || email.isEmpty() || password.isEmpty() ||
                confirmPassword.isEmpty() || department.isEmpty() || yearOfStudy.isEmpty() || college.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
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

    private fun fetchCollegeList() {
        val url = "http://10.143.152.54/univault/get_colleges.php"

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

                            // When user selects a college, fetch its departments
                            collegeAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                                val selectedCollege = colleges[position]
                                selectedCollegeId = collegeMap[selectedCollege]
                                departmentAutoCompleteTextView.setText("") // Clear previous department
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
        val url = "http://10.143.152.54/univault/fetch_departments_by_college.php?college_id=$collegeId"

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
        val url = "http://10.143.152.54/univault/register-smtp.php"

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
                                Toast.makeText(this@RegisterActivity, "Registration successful Verify your gmail!", Toast.LENGTH_SHORT).show()
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
