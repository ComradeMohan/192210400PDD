package com.simats.univalut

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class FacultyStudentsFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var studentsListLayout: LinearLayout
    private var allStudents = mutableListOf<Pair<String, String>>() // name, number
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

        val facultyId = arguments?.getString("ID")
        if (facultyId != null) {
            Log.d("DEBUG", "Faculty ID: $facultyId")
            // Fetch the college name of the faculty first
            fetchCollegeName(facultyId)
        } else {
            Log.e("DEBUG", "No faculty ID passed in arguments")
        }
    }

    // Function to fetch the college name from the server
    private fun fetchCollegeName(facultyId: String) {
        val url = "https://api-9buk.onrender.com/get_faculty_name.php?facultyId=$facultyId"

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
        val url = "https://api-9buk.onrender.com/fetch_students_by_college.php?college=$college"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getBoolean("success")) {
                    val students = response.getJSONArray("students")
                    allStudents.clear()

                    for (i in 0 until students.length()) {
                        val student = students.getJSONObject(i)
                        val name = student.getString("full_name")
                        val number = student.getString("student_number")
                        allStudents.add(Pair(name, number))
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
                    it.second.contains(query, ignoreCase = true)
                }
                displayStudents(filtered)
            }
        })
    }

    // Displaying the list of students in the UI
    private fun displayStudents(students: List<Pair<String, String>>) {
        studentsListLayout.removeAllViews()
        for ((name, number) in students) {
            val view = layoutInflater.inflate(R.layout.student_item_layout, studentsListLayout, false)

            view.findViewById<TextView>(R.id.studentName).text = name
            view.findViewById<TextView>(R.id.studentId).text = number

            val studentImageView = view.findViewById<ImageView>(R.id.studentImage)

            if (name.isNotEmpty()) {
                val firstLetter = name[0].uppercaseChar()
                studentImageView.setImageDrawable(getLetterDrawable(firstLetter))
            } else {
                // Optionally set a default drawable or clear image
                studentImageView.setImageDrawable(null)
            }

            studentsListLayout.addView(view)
        }
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
