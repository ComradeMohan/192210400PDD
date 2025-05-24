package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class tester : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tester)

        val fullNameInput = findViewById<EditText>(R.id.fullNameInput)
        val studentNumberInput = findViewById<EditText>(R.id.studentNumberInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val departmentAutoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.departmentAutoCompleteTextView)
        val yearOfStudyAutoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.yearOfStudyAutoCompleteTextView)
        val createAccountButton = findViewById<Button>(R.id.createAccountButton)
        val alreadyHaveAccountTextView = findViewById<TextView>(R.id.alreadyHaveAccountTextView)

        // Sample data for Department and Year of Study (replace with your actual data)
        val departments = arrayOf("Computer Science", "Electronics Engineering", "Mechanical Engineering", "Information Technology")
        val years = arrayOf("First Year", "Second Year", "Third Year", "Fourth Year")

        val departmentAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments)
        departmentAutoCompleteTextView.setAdapter(departmentAdapter)

        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years)
        yearOfStudyAutoCompleteTextView.setAdapter(yearAdapter)

        createAccountButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val studentNumber = studentNumberInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            val department = departmentAutoCompleteTextView.text.toString().trim()
            val yearOfStudy = yearOfStudyAutoCompleteTextView.text.toString().trim()

            if (fullName.isEmpty() || studentNumber.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || department.isEmpty() || yearOfStudy.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                // TODO: Implement your registration logic here
                Toast.makeText(this, "Creating account for $fullName", Toast.LENGTH_SHORT).show()
                // For now, let's just go back to the login screen after a successful attempt
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish() // Optional: close the RegisterActivity so the user can't go back with the back button
            }
        }

        alreadyHaveAccountTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Optional: close the RegisterActivity
        }
    }
}