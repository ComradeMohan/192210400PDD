package com.simats.univault

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.simats.univault.databinding.FragmentManageDepartmentBinding
import org.json.JSONObject

class ManageDepartmentFragment : Fragment() {

    private var _binding: FragmentManageDepartmentBinding? = null
    private val binding get() = _binding!!

    private var adminId: String? = null
    private var collegeId: String? = null

    private var selectedDepartmentId: String? = null
    private var departments = mutableListOf<Department>()
    private var courses = mutableListOf<DeptCourse>()


    private lateinit var deptAdapter: DepartmentAdapter
    private lateinit var courseAdapter: DeptCourseAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adminId = arguments?.getString("admin_id")
        collegeId = arguments?.getString("college_id")

        if (adminId == null || collegeId == null) {
            Toast.makeText(requireContext(), "Missing admin or college ID", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageDepartmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Departments RecyclerView
        deptAdapter = DepartmentAdapter(departments,
            onClick = { dept ->
                val bundle = Bundle().apply {
                    putString("department_id", dept.id)
                    putString("department_name", dept.name)
                }

                val fragment = DepartmentCoursesFragment()
                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment) // Replace with your container ID
                    .addToBackStack(null)
                    .commit()
            },
            onDelete = { dept -> confirmAndDeleteDepartment(dept) }
        )

        binding.rvDepartments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDepartments.adapter = deptAdapter

        // Setup Courses RecyclerView
        courseAdapter = DeptCourseAdapter(courses,
            onDelete = { course ->
                confirmAndDeleteCourse(course)
            }
        )
//        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
//        binding.rvCourses.adapter = courseAdapter

        // Button listeners
        binding.btnAddDepartment.setOnClickListener { showAddDepartmentDialog() }

      //  binding.btnAddCourse.setOnClickListener { showAddCourseDialog() }

        // Fetch departments initially
        collegeId?.let { fetchDepartments(it) }
    }

    private fun fetchDepartments(collegeId: String) {
        val url = "http://10.143.152.54/univault/get_departments.php?college_id=$collegeId"
        val queue = Volley.newRequestQueue(requireContext())

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        departments.clear()
                        val dataArray = json.getJSONArray("departments")
                        for (i in 0 until dataArray.length()) {
                            val obj = dataArray.getJSONObject(i)
                            departments.add(
                                Department(
                                    id = obj.getString("id"),
                                    name = obj.getString("name")
                                )
                            )
                        }
                        deptAdapter.notifyDataSetChanged()

                        // Clear courses UI until department selected
//                        courses.clear()
//                        courseAdapter.notifyDataSetChanged()
//                        binding.tvCoursesTitle.visibility = View.GONE
//                        binding.rvCourses.visibility = View.GONE
//                        binding.btnAddCourse.visibility = View.GONE
                    } else {
                        Toast.makeText(requireContext(), "No departments found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to parse departments", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(requireContext(), "Error loading departments", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

    private fun showAddDepartmentDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Department")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val deptName = input.text.toString().trim()
            if (deptName.isEmpty() || collegeId.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please enter a department name", Toast.LENGTH_SHORT).show()
            } else {
                addDepartment(deptName)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun addDepartment(name: String) {
        val url = "http://10.143.152.54/univault/add_department.php"
        val queue = Volley.newRequestQueue(requireContext())

        val request = object : StringRequest(Method.POST, url,
            { response ->
                Toast.makeText(requireContext(), "Department added successfully", Toast.LENGTH_SHORT).show()
                collegeId?.let { fetchDepartments(it) }
            },
            { error ->
                Toast.makeText(requireContext(), "Failed to add department: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {

            override fun getParams(): Map<String, String> {
                return mapOf("college_id" to collegeId!!, "name" to name)
            }
        }
        queue.add(request)
    }

    private fun confirmAndDeleteDepartment(dept: Department) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Department")
            .setMessage("Are you sure you want to delete the department '${dept.name}'?")
            .setPositiveButton("Yes") { dialog, _ ->
                deleteDepartment(dept.id)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteDepartment(deptId: String) {
        val url = "http://10.143.152.54/univault/delete_department.php"
        val queue = Volley.newRequestQueue(requireContext())

        val request = object : StringRequest(Method.POST, url,
            { response ->
                Toast.makeText(requireContext(), "Department deleted", Toast.LENGTH_SHORT).show()
                collegeId?.let { fetchDepartments(it) }
                // Also clear courses UI
//                courses.clear()
//                courseAdapter.notifyDataSetChanged()
//                binding.tvCoursesTitle.visibility = View.GONE
//                binding.rvCourses.visibility = View.GONE
//                binding.btnAddCourse.visibility = View.GONE
            },
            { error ->
                Toast.makeText(requireContext(), "Failed to delete department: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {

            override fun getParams(): Map<String, String> {
                return mapOf("department_id" to deptId)
            }
        }
        queue.add(request)
    }

//    private fun showCoursesForDepartment(departmentId: String) {
//        binding.tvCoursesTitle.visibility = View.VISIBLE
//        binding.rvCourses.visibility = View.VISIBLE
//        binding.btnAddCourse.visibility = View.VISIBLE
//
//        fetchCourses(departmentId)
//    }

    private fun fetchCourses(departmentId: String) {
        val url = "http://10.143.152.54/univault/get_courses.php?department_id=$departmentId"
        val queue = Volley.newRequestQueue(requireContext())

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        courses.clear()  // make sure 'courses' is a MutableList<DeptCourse>
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
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to parse courses", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(requireContext(), "Error loading courses", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }


    private fun showAddCourseDialog() {
        if (selectedDepartmentId == null) {
            Toast.makeText(requireContext(), "Select a department first", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Course")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val courseName = input.text.toString().trim()
            if (courseName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a course name", Toast.LENGTH_SHORT).show()
            } else {
                addCourse(selectedDepartmentId!!, courseName)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
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
            .setMessage("Are you sure you want to delete the course '${course.name}'?")
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
            { response ->
                Toast.makeText(requireContext(), "Course deleted", Toast.LENGTH_SHORT).show()
                selectedDepartmentId?.let { fetchCourses(it) }
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

    // Data classes for Departments and Courses
    data class Department(val id: String, val name: String)
    //data class Course(val id: String, val name: String)
}
