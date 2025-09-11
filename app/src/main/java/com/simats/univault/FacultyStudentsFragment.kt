package com.simats.univault

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.net.HttpURLConnection
import java.net.URL

class FacultyStudentsFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var studentsListLayout: LinearLayout
    private var allStudents = mutableListOf<Triple<String, String, String>>() // name, number, department
    // name, number, department
    // name, number
    private var collegeName: String? = null // To store the college name

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_faculty_students, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchEditText = view.findViewById(R.id.searchEditText)
        studentsListLayout = view.findViewById(R.id.studentsList)

        val sf = requireContext().getSharedPreferences("user_sf", AppCompatActivity.MODE_PRIVATE)
        val facultyId = sf.getString("userID", null)
        collegeName = sf.getString("college", null)

        if (!facultyId.isNullOrEmpty() && !collegeName.isNullOrEmpty()) {
            Log.d("DEBUG", "Faculty ID from SharedPreferences: $facultyId")
            Log.d("DEBUG", "College name from SharedPreferences: $collegeName")

            // Fetch students directly using stored college name
            fetchStudentsByCollege(collegeName!!)
        } else {
            Log.e("DEBUG", "Faculty ID or College name missing in SharedPreferences")
            Toast.makeText(requireContext(), "Login details missing. Please re-login.", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to fetch the college name from the server
    private fun fetchCollegeName(facultyId: String) {
        val url = "http://10.235.18.54/univault/get_faculty_name.php?facultyId=$facultyId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getBoolean("success")) {
                    collegeName = response.getString("college")
                    Log.d("DEBUG", "College name fetched: $collegeName")
                    fetchStudentsByCollege(collegeName!!)
                } else {
                    Log.e("DEBUG", "Failed to fetch college name")
                }
            },
            { error ->
                Log.e("NetworkError", "Error fetching college name: ${error.message}")
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // Function to fetch the list of students from the college
    private fun fetchStudentsByCollege(college: String) {
        val url = "http://10.235.18.54/univault/fetch_students_by_college.php?college=$college"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getBoolean("success")) {
                    val students = response.getJSONArray("students")
                    allStudents.clear()

                    for (i in 0 until students.length()) {
                        val student = students.getJSONObject(i)
                        val name = student.getString("full_name")
                        val number = student.getString("student_number")
                        val department = student.getString("department")
                        allStudents.add(Triple(name, number, department))

                    }

                    Log.d("DEBUG", "Fetched students: $allStudents")
                    displayStudents(allStudents)
                    setupSearch()
                } else {
                    Log.e("DEBUG", "Failed to fetch students")
                }
            },
            { error ->
                Log.e("NetworkError", "Error fetching students: ${error.message}")
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // Setting up the search functionality
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val filtered = allStudents.filter {
                    it.second.contains(query, ignoreCase = true) ||
                            it.first.contains(query, ignoreCase = true) ||
                            it.third.contains(query, ignoreCase = true)
                }

                displayStudents(filtered)
            }
        })
    }

    // Displaying the list of students in the UI
    private fun displayStudents(students: List<Triple<String, String, String>>) {
        studentsListLayout.removeAllViews()
        for ((name, number, department) in students) {
            val view = layoutInflater.inflate(R.layout.student_item_layout, studentsListLayout, false)

            view.findViewById<TextView>(R.id.studentName).text = name
            view.findViewById<TextView>(R.id.studentId).text = number
            view.findViewById<TextView>(R.id.studentDepartment).text = department

            val studentImageView = view.findViewById<ImageView>(R.id.studentImage)
            if (name.isNotEmpty()) {
                val firstLetter = name[0].uppercaseChar()
                studentImageView.setImageDrawable(getLetterDrawable(firstLetter))
            }

            view.setOnClickListener {
                val intent = Intent(requireContext(), AcadmicRecordActivity::class.java)
                intent.putExtra("studentID", number)
                intent.putExtra("department", department)
                intent.putExtra("collegeName", collegeName)
                startActivity(intent)
            }

            studentsListLayout.addView(view)
        }
    }

    private fun fetchStudentDepartment(studentNumber: String, callback: (String?) -> Unit) {
        Thread {
            var department: String? = null
            try {
                val url = URL("http://10.235.18.54/univault/get_student.php?student_number=$studentNumber")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = connection.inputStream
                    val response = stream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        val studentData = jsonObject.getJSONObject("data")
                        department = studentData.getString("department")
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            activity?.runOnUiThread { callback(department) }
        }.start()
    }

    private fun getLetterDrawable(letter: Char, size: Int = 80): BitmapDrawable {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = ContextCompat.getColor(requireContext(), R.color.blue_focus) // Your desired circle color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = Color.WHITE
        paint.textSize = size * 0.5f
        paint.textAlign = Paint.Align.CENTER
        val fontMetrics = paint.fontMetrics
        val x = size / 2f
        val y = size / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2
        canvas.drawText(letter.toString(), x, y, paint)

        return BitmapDrawable(resources, bitmap)
    }



    companion object {
        fun newInstance(facultyId: String): FacultyStudentsFragment {
            val fragment = FacultyStudentsFragment()
            fragment.arguments = Bundle().apply {
                putString("ID", facultyId)
            }
            return fragment
        }
    }
}
