package com.simats.univalut

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.simats.univalut.databinding.FragmentDepartmentCoursesBinding
import org.json.JSONObject

class DepartmentCoursesFragment : Fragment() {

    private var _binding: FragmentDepartmentCoursesBinding? = null
    private val binding get() = _binding!!

    private lateinit var courseAdapter: DeptCourseAdapter
    private val courses = mutableListOf<DeptCourse>()

    private var departmentId: String? = null
    private var departmentName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        departmentId = arguments?.getString("department_id")
        departmentName = arguments?.getString("department_name")

        if (departmentId == null) {
            Toast.makeText(requireContext(), "Missing department ID", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDepartmentCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvCourseTitle.text = "Courses for $departmentName"

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnAddCourse.setOnClickListener {
            showAddCourseDialog()
        }

        courseAdapter = DeptCourseAdapter(courses) { course ->
            confirmAndDeleteCourse(course)
        }

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourses.adapter = courseAdapter

        departmentId?.let { fetchCourses(it) }
    }

    private fun fetchCourses(departmentId: String) {
        val url = "http://10.143.152.54/univault/get_courses.php?department_id=$departmentId"
        val queue = Volley.newRequestQueue(requireContext())

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        courses.clear()
                        val dataArray = json.getJSONArray("data")
                        for (i in 0 until dataArray.length()) {
                            val obj = dataArray.getJSONObject(i)
                            courses.add(
                                DeptCourse(
                                    id = obj.getString("id"),
                                    departmentId = obj.getString("department_id"),
                                    name = obj.getString("name").trim(),
                                    credits = obj.getInt("credits")
                                )
                            )
                        }
                        courseAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(requireContext(), "No courses found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Parsing error", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(requireContext(), "Error fetching courses", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun showAddCourseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_course, null)
        val editTextCourse = dialogView.findViewById<EditText>(R.id.etCourseName)
        val buttonSubmit = dialogView.findViewById<Button>(R.id.btnSubmitCourse)

        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // ✅ Show the dialog first
        dialog.show()

        // ✅ Then set width to MATCH_PARENT
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        buttonSubmit.setOnClickListener {
            val courseName = editTextCourse.text.toString().trim()
            if (courseName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a course name", Toast.LENGTH_SHORT).show()
            } else {
                departmentId?.let { deptId ->
                    addCourse(deptId, courseName)
                }
                dialog.dismiss()
            }
        }
    }


    private fun addCourse(departmentId: String, name: String) {
        val url = "http://10.143.152.54/univault/add_course.php"
        val queue = Volley.newRequestQueue(requireContext())

        val request = object : StringRequest(Method.POST, url,
            { response ->
                Toast.makeText(requireContext(), "Course added successfully", Toast.LENGTH_SHORT).show()
                fetchCourses(departmentId)
            },
            { error ->
                Toast.makeText(requireContext(), "Failed to add course: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                return mapOf("department_id" to departmentId, "name" to name)
            }
        }
        queue.add(request)
    }

    private fun confirmAndDeleteCourse(course: DeptCourse) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete '${course.name}'?")
            .setPositiveButton("Yes") { dialog, _ ->
                deleteCourse(course.id)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteCourse(courseId: String) {
        val url = "http://10.143.152.54/univault/delete_course.php"
        val queue = Volley.newRequestQueue(requireContext())

        val request = object : StringRequest(Method.POST, url,
            {
                Toast.makeText(requireContext(), "Course deleted", Toast.LENGTH_SHORT).show()
                departmentId?.let { fetchCourses(it) }
            },
            { error ->
                Toast.makeText(requireContext(), "Failed to delete course: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                return mapOf("course_id" to courseId)
            }
        }
        queue.add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
