package com.simats.univalut

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ProfileFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var departmentTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var regNoTextView: TextView
    private lateinit var yearTextView: TextView
    private lateinit var joinedDateTextView: TextView
    private lateinit var academicRecordsButton: LinearLayout
    private lateinit var helpSupportButton: LinearLayout
    private lateinit var settingsButton: LinearLayout
    private lateinit var logoutButton: LinearLayout

    private var studentID: String? = null
    private var department: String? = null
    private var collegeName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_student_profile, container, false)
    }

    companion object {
        fun newInstance(studentID: String): ProfileFragment {
            val fragment = ProfileFragment()
            val args = Bundle()
            args.putString("studentID", studentID)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        profileImageView = view.findViewById(R.id.profileImageView)
        nameTextView = view.findViewById(R.id.nameTextView)
        statusTextView = view.findViewById(R.id.statusTextView)
        departmentTextView = view.findViewById(R.id.departmentTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        phoneTextView = view.findViewById(R.id.phoneTextView)
        regNoTextView = view.findViewById(R.id.regNoTextView)
        yearTextView = view.findViewById(R.id.yearTextView)
        academicRecordsButton = view.findViewById(R.id.academicRecordsButton)
        helpSupportButton = view.findViewById(R.id.helpSupportButton)
        settingsButton = view.findViewById(R.id.settingsButton)
        logoutButton = view.findViewById(R.id.logoutButton)

        // Set default placeholder (e.g., 'A')
        profileImageView.setImageDrawable(getLetterDrawable('A'))

        // Set data
        nameTextView.text = ""
        statusTextView.text = "Student"
        departmentTextView.text = "Dept: "
        emailTextView.text = "MAIL"
        phoneTextView.text = "Dept: CSE"
        regNoTextView.text = "Reg No: 192XXXXX"
        yearTextView.text = "Year X"

        val studentID = arguments?.getString("studentID")
        if (studentID != null) {
            fetchStudentData(studentID)
        } else {
            Toast.makeText(requireContext(), "Student ID not found", Toast.LENGTH_SHORT).show()
        }

        // Button click listeners
        academicRecordsButton.setOnClickListener {
            if (studentID != null && department != null && collegeName != null) {
                val intent = Intent(requireContext(), AcadmicRecordActivity::class.java).apply {
                    putExtra("studentID", studentID)
                    putExtra("department", department)
                    putExtra("collegeName", collegeName)
                }
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Data is still loading...", Toast.LENGTH_SHORT).show()
            }
        }

        helpSupportButton.setOnClickListener {
            val changePasswordFragment = ChangePasswordFragment()
            val bundle = Bundle()
            bundle.putString("ID", studentID)
            bundle.putString("userType", "student")
            changePasswordFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, changePasswordFragment)
                .addToBackStack(null)
                .commit()
        }
//changes made in online mode
        settingsButton.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_feedback, null)
            val etFeedback = dialogView.findViewById<EditText>(R.id.etFeedback)
            val tvUserId = dialogView.findViewById<TextView>(R.id.tvUserId)
            // Set user ID
            tvUserId.text = "User ID: ${studentID ?: "Unknown"}"

            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setTitle("Submit Feedback")
                .setView(dialogView)
                .setPositiveButton("Submit") { _, _ ->
                    val feedback = etFeedback.text.toString().trim()
                    val userId = studentID ?: ""

                    if (feedback.isEmpty()) {
                        Toast.makeText(requireContext(), "Feedback cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Send feedback using OkHttp
                    val client = OkHttpClient()
                    val requestBody = FormBody.Builder()
                        .add("user_id", userId)
                        .add("feedback", feedback)
                        .add("college", collegeName ?: "Unknown")
                        .build()

                    val request = Request.Builder()
                        .url("http://192.168.205.54/univault/submit_feedback.php") // Replace with your actual URL
                        .post(requestBody)
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            activity?.runOnUiThread {
                                Toast.makeText(requireContext(), "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body?.string()
                            activity?.runOnUiThread {
                                if (response.isSuccessful && responseBody?.contains("success") == true) {
                                    Toast.makeText(requireContext(), "Feedback submitted successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Server error: $responseBody", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    })
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }


        logoutButton.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
            val btnLogout = dialogView.findViewById<Button>(R.id.btnLogout)

            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnLogout.setOnClickListener {
                dialog.dismiss()
                val sharedPreferences = requireContext().getSharedPreferences("user_sf", Context.MODE_PRIVATE)
                sharedPreferences.edit().clear().apply()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        }

    }

    private fun fetchStudentData(studentNumber: String) {
        val client = OkHttpClient()
        val url = "http://192.168.205.54/univault/get_student.php?student_number=$studentNumber"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                activity?.runOnUiThread {
                    if (responseData != null) {
                        parseAndDisplayData(responseData)
                    }
                }
            }
        })
    }

    private fun parseAndDisplayData(jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            if (jsonObject.getBoolean("success")) {
                val studentData = jsonObject.getJSONObject("data")

                val fullName = studentData.getString("full_name")
                nameTextView.text = fullName
                regNoTextView.text = "Reg No: ${studentData.getString("student_number")}"
                phoneTextView.text = "Dept: ${studentData.getString("department")}"
                emailTextView.text = studentData.getString("email")
                yearTextView.text = "${studentData.getString("year_of_study")}"
                departmentTextView.text = studentData.get("department").toString()

                // Set profile image as first letter of name
                if (fullName.isNotEmpty()) {
                    val firstLetter = fullName[0].uppercaseChar()
                    profileImageView.setImageDrawable(getLetterDrawable(firstLetter))
                }

                studentID = studentData.getString("student_number")
                department = studentData.getString("department")
                collegeName = studentData.getString("college")

            } else {
                Toast.makeText(requireContext(), "Student not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error parsing data", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to create a circular drawable with the given letter
    private fun getLetterDrawable(letter: Char, size: Int = 120): BitmapDrawable {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a colored circle
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = ContextCompat.getColor(requireContext(), R.color.blue_focus) // Use your color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw the letter
        paint.color = Color.WHITE
        paint.textSize = size * 0.5f
        paint.textAlign = Paint.Align.CENTER
        val fontMetrics = paint.fontMetrics
        val x = size / 2f
        val y = size / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2
        canvas.drawText(letter.toString(), x, y, paint)

        return BitmapDrawable(resources, bitmap)
    }
}
