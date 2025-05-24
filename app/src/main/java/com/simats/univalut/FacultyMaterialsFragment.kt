package com.simats.univalut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class FacultyMaterialsFragment : Fragment() {

    private lateinit var adapter: FacultyCourseAdapter
    private lateinit var recyclerView: RecyclerView
    private var facultyId: String? = null
    private var selectedCourseCode: String? = null
    private var collegeName: String? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_faculty_materials, container, false)

        recyclerView = view.findViewById(R.id.courseRecyclerView)
        val backButton: ImageView = view.findViewById(R.id.backButton)
        val nextButton: Button = view.findViewById(R.id.nextButton)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        facultyId = arguments?.getString("ID")
        collegeName = arguments?.getString("college_name")

        if (!collegeName.isNullOrEmpty()) {
            // Admin flow
            fetchCourses(collegeName!!)
        } else if (!facultyId.isNullOrEmpty()) {
            // Faculty flow
            fetchCollegeAndCourses(facultyId!!)
        } else {
            Toast.makeText(requireContext(), "Faculty ID or College Name missing", Toast.LENGTH_SHORT).show()
        }

        nextButton.setOnClickListener {
            if (selectedCourseCode == null) {
                Toast.makeText(requireContext(), "Please select a course first", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(requireContext(), FacultyUploadMaterial::class.java)
                intent.putExtra("COURSE_CODE", selectedCourseCode)
                intent.putExtra("COLLEGE_NAME", collegeName)  // Pass college name as well
                startActivity(intent)
            }
        }

        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return view
    }


    private fun fetchCollegeAndCourses(facultyId: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "https://api-9buk.onrender.com/get_college_by_faculty.php"

        val requestBody = JSONObject()
        requestBody.put("faculty_id", facultyId)

        val collegeRequest = object : JsonObjectRequest(Request.Method.POST, url, requestBody,
            { response ->
                if (response.getBoolean("success")) {
                    val college = response.getString("college")
                    collegeName = response.getString("college")
                    fetchCourses(college)
                } else {
                    Toast.makeText(requireContext(), "College fetch failed", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                Log.e("Volley", "Error: ${error.message}")
            }) {}

        queue.add(collegeRequest)
    }

    private fun fetchCourses(college: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val encodedCollege = Uri.encode(college)
        val url = "https://api-9buk.onrender.com/getCoursesByCollege.php?college=$encodedCollege"

        val courseRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val courseArray = response.getJSONArray("courses")
                        val courseList = mutableListOf<FacultyCourse>()
                        for (i in 0 until courseArray.length()) {
                            val courseObj = courseArray.getJSONObject(i)
                            val course = FacultyCourse(
                                courseObj.getString("course_code").trim(),
                                courseObj.getString("subject_name").trim(),
                                courseObj.getString("faculty_name").trim()
                            )
                            courseList.add(course)
                        }

                        adapter = FacultyCourseAdapter(courseList) { selectedCourse ->
                            selectedCourseCode = selectedCourse.code
                            Toast.makeText(requireContext(), "Selected: ${selectedCourse.title}", Toast.LENGTH_SHORT).show()
                        }
                        recyclerView.adapter = adapter
                    } else {
                        Toast.makeText(requireContext(), "No courses found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Parsing error", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error fetching courses", Toast.LENGTH_SHORT).show()
            })

        queue.add(courseRequest)
    }

}
