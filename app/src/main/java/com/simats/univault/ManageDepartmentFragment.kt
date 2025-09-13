package com.simats.univault

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.view.animation.AnimationUtils
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

        deptAdapter = DepartmentAdapter(departments,
            onClick = { dept ->
                val bundle = Bundle().apply {
                    putString("department_id", dept.id)
                    putString("department_name", dept.name)
                }
                val fragment = DepartmentCoursesFragment()
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDelete = { dept -> confirmAndDeleteDepartment(dept) }
        )

        binding.rvDepartments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDepartments.adapter = deptAdapter

        // Load the animation controller
        binding.rvDepartments.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation)
        binding.rvDepartments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDepartments.adapter = deptAdapter

// Set layout animation for department list
        val animationController = AnimationUtils.loadLayoutAnimation(
            requireContext(),
            R.anim.layout_animation_fall_down
        )
        binding.rvDepartments.layoutAnimation = animationController

        courseAdapter = DeptCourseAdapter(courses,
            onDelete = { course -> confirmAndDeleteCourse(course) }
        )

        binding.btnAddDepartment.setOnClickListener { showAddDepartmentDialog() }

        collegeId?.let { fetchDepartments(it) }
    }

    private fun fetchDepartments(collegeId: String) {
        val url = "http://192.168.56.1/univault/get_departments.php?college_id=$collegeId"
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

                        // ✅ Sort the departments alphabetically by name (case-insensitive)
                        departments.sortBy { it.name.lowercase() }

                        deptAdapter.notifyDataSetChanged()
                        binding.rvDepartments.scheduleLayoutAnimation()
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
        val url = "http://192.168.56.1/univault/add_department.php"
        val queue = Volley.newRequestQueue(requireContext())

        val request = object : StringRequest(Method.POST, url,
            {
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
        val url = "http://192.168.56.1/univault/delete_department.php"
        val queue = Volley.newRequestQueue(requireContext())

        val request = object : StringRequest(Method.POST, url,
            {
                Toast.makeText(requireContext(), "Department deleted", Toast.LENGTH_SHORT).show()
                collegeId?.let { fetchDepartments(it) }
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
        val url = "http://192.168.56.1/univault/delete_course.php"
        val queue = Volley.newRequestQueue(requireContext())

        val request = object : StringRequest(Method.POST, url,
            {
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

    private fun fetchCourses(departmentId: String) {
        val url = "http://192.168.56.1/univault/get_courses.php?department_id=$departmentId"
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
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to parse courses", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(requireContext(), "Error loading courses", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Department(val id: String, val name: String)
}
