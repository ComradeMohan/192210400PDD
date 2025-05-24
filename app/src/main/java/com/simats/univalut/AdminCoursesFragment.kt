package com.simats.univalut

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AdminCoursesFragment : Fragment() {
    private var adminId: String? = null
    private var collegeName: String? = null

    private lateinit var btnAddCourse: Button
    private lateinit var layoutAddCourseForm: LinearLayout
    private lateinit var btnSaveCourse: Button
    private lateinit var coursesListLayout: LinearLayout

    private lateinit var etCourseCode: EditText
    private lateinit var etSubjectName: EditText
    private lateinit var etStrength: EditText
    private lateinit var spinnerFaculty: Spinner

    private var facultyList: MutableList<String> = mutableListOf("Select Faculty")
    private var isFormVisible = false

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_courses, container, false)

        // UI Initialization
        btnAddCourse = view.findViewById(R.id.btnAddCourse)
        layoutAddCourseForm = view.findViewById(R.id.layoutAddCourseForm)
        btnSaveCourse = view.findViewById(R.id.btnSaveCourse)
        coursesListLayout = view.findViewById(R.id.coursesListLayout)

        etCourseCode = view.findViewById(R.id.etCourseCode)
        etSubjectName = view.findViewById(R.id.etSubjectName)
        etStrength = view.findViewById(R.id.etStrength)
        spinnerFaculty = view.findViewById(R.id.spinnerFaculty)

        adminId = arguments?.getString("adminId")

        adminId?.let {
            fetchAdminDetails(it)
        }

        btnAddCourse.setOnClickListener {
            isFormVisible = !isFormVisible
            layoutAddCourseForm.visibility = if (isFormVisible) View.VISIBLE else View.GONE
        }

        btnSaveCourse.setOnClickListener {
            saveCourseForm()
        }

        return view
    }

    private fun fetchAdminDetails(adminId: String) {
        val url = "https://api-9buk.onrender.com/getAdminDetails.php?admin_id=$adminId"
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    val name = response.getString("name")
                    collegeName = response.getString("college")
                    Toast.makeText(requireContext(), "Welcome $name from $collegeName", Toast.LENGTH_SHORT).show()
                    collegeName?.let {
                        loadCoursesForCollege(it)
                        loadFacultyForCollege(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error fetching admin details", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun loadCoursesForCollege(college: String) {
        val encodedCollege = URLEncoder.encode(college, StandardCharsets.UTF_8.name())
        val url = "https://api-9buk.onrender.com/getCoursesByCollege.php?college=$encodedCollege"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val coursesJsonArray = response.getJSONArray("courses")
                        populateCourseList(coursesJsonArray)
                    } else {
                        Toast.makeText(requireContext(), "No courses found for $college", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error loading courses", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun loadFacultyForCollege(college: String) {
        val url = "https://api-9buk.onrender.com/getFacultyByCollege.php?college=$collegeName"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        // Clear previous faculty list and add default option
                        facultyList.clear()
                        facultyList.add("Select Faculty")

                        // Parse faculty array
                        val facultyArray = response.getJSONArray("faculty")
                        for (i in 0 until facultyArray.length()) {
                            val facultyName = facultyArray.getString(i)
                            facultyList.add(facultyName)
                        }


                        // Create custom adapter for spinner
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            facultyList
                        )

                        // Customize the dropdown view
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                        // Set the adapter to the spinner
                        spinnerFaculty.adapter = adapter

                        // Optional: Add a selection listener to handle faculty selection
                        spinnerFaculty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                // First item is "Select Faculty", so skip it
                                if (position > 0) {
                                    val selectedFaculty = facultyList[position]
                                    // You can add additional logic here if needed
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                // Optional: Handle case where nothing is selected
                            }
                        }

                        // Enable the Add Course button now that faculty are loaded
                        btnAddCourse.isEnabled = true

                        // Log the faculty list for debugging
                        Log.d("AdminCoursesFragment", "Faculty List: $facultyList")
                    } else {
                        Toast.makeText(requireContext(), "No faculty found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error processing faculty data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error loading faculty: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        // Add the request to the RequestQueue
        Volley.newRequestQueue(requireContext()).add(request)
    }


    private fun populateCourseList(coursesJsonArray: JSONArray) {
        coursesListLayout.removeAllViews()

        for (i in 0 until coursesJsonArray.length()) {
            val course = coursesJsonArray.getJSONObject(i)
            val courseCardView = LayoutInflater.from(requireContext()).inflate(R.layout.course_card_item, coursesListLayout, false)

            val courseCodeTextView = courseCardView.findViewById<TextView>(R.id.tvCourseCode)
            val subjectNameTextView = courseCardView.findViewById<TextView>(R.id.tvSubjectName)
            val facultyNameTextView = courseCardView.findViewById<TextView>(R.id.tvFacultyName)

            courseCodeTextView.text = course.getString("course_code")
            subjectNameTextView.text = course.getString("subject_name")
            facultyNameTextView.text = course.getString("faculty_name")

            coursesListLayout.addView(courseCardView)
        }
    }

    private fun saveCourseForm() {
        val code = etCourseCode.text.toString().trim()
        val subject = etSubjectName.text.toString().trim()
        val strengthStr = etStrength.text.toString().trim()
        val faculty = spinnerFaculty.selectedItem.toString()

        if (code.isBlank() || subject.isBlank() || strengthStr.isBlank() || faculty == "Select Faculty") {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val strength = try {
            strengthStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Strength must be a valid number", Toast.LENGTH_SHORT).show()
            return
        }

        val courseData = JSONObject().apply {
            put("course_code", code)
            put("subject_name", subject)
            put("faculty", faculty)
            put("strength", strength)
            put("college", collegeName)
        }

        val url = "https://api-9buk.onrender.com/addCourse.php"
        val request = JsonObjectRequest(Request.Method.POST, url, courseData,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        Toast.makeText(requireContext(), "Course added successfully", Toast.LENGTH_SHORT).show()
                        clearForm()
                        collegeName?.let { loadCoursesForCollege(it) }
                        layoutAddCourseForm.visibility = View.GONE
                        isFormVisible = false
                    } else {
                        Toast.makeText(requireContext(), response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error saving course", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun clearForm() {
        etCourseCode.text.clear()
        etSubjectName.text.clear()
        etStrength.text.clear()
        spinnerFaculty.setSelection(0)
    }

    companion object {
        fun newInstance(adminId: String): AdminCoursesFragment {
            val fragment = AdminCoursesFragment()
            val args = Bundle()
            args.putString("adminId", adminId)
            fragment.arguments = args
            return fragment
        }
    }
}
