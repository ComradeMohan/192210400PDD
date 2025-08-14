package com.simats.univault

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ManageDepartmentsFragment : Fragment() {

    private lateinit var etDeptName: EditText
    private lateinit var btnAddDepartment: View
    private lateinit var rvDepartments: RecyclerView

    private val departments = mutableListOf<Department>()
    private lateinit var adapter: DepartmentsAdapter

    // Replace with your backend URLsa
    private val baseUrl = "http://10.169.48.54/univault"
    private val collegeName = "Your College" // You may want to pass this dynamically

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_manage_departments, container, false)
        etDeptName = root.findViewById(R.id.etDeptName)
        btnAddDepartment = root.findViewById(R.id.btnAddDepartment)
        rvDepartments = root.findViewById(R.id.rvDepartments)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvDepartments.layoutManager = LinearLayoutManager(requireContext())
        adapter = DepartmentsAdapter(departments,
            onDeleteDept = { dept -> confirmDeleteDepartment(dept) },
            onManageCourses = { dept -> showManageCoursesDialog(dept) }
        )
        rvDepartments.adapter = adapter

        btnAddDepartment.setOnClickListener {
            val deptName = etDeptName.text.toString().trim()
            if (deptName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter department name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addDepartment(deptName)
        }

        fetchDepartments()
    }

    private fun fetchDepartments() {
        val url = "$baseUrl/get_departments.php?college=${collegeName.replace(" ", "%20")}"
        val request = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    departments.clear()
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val data = json.getJSONArray("departments")
                        for (i in 0 until data.length()) {
                            val obj = data.getJSONObject(i)
                            val dept = Department(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                courses = mutableListOf()
                            )
                            departments.add(dept)
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch departments", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error parsing departments", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun addDepartment(name: String) {
        val url = "$baseUrl/add_department.php"
        val request = object : StringRequest(Method.POST, url,
            { response ->
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    Toast.makeText(requireContext(), "Department added", Toast.LENGTH_SHORT).show()
                    etDeptName.setText("")
                    fetchDepartments()
                } else {
                    Toast.makeText(requireContext(), "Failed to add department", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "college_name" to collegeName,
                    "department_name" to name
                )
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun confirmDeleteDepartment(department: Department) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Department")
            .setMessage("Are you sure you want to delete the department '${department.name}'? This will also delete all its courses.")
            .setPositiveButton("Delete") { _, _ ->
                deleteDepartment(department)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDepartment(department: Department) {
        val url = "$baseUrl/delete_department.php"
        val request = object : StringRequest(Method.POST, url,
            { response ->
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    Toast.makeText(requireContext(), "Department deleted", Toast.LENGTH_SHORT).show()
                    fetchDepartments()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete department", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                return mapOf("department_id" to department.id)
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    // Show dialog to manage courses for a department
    private fun showManageCoursesDialog(department: Department) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manage_courses, null)
        val etCourseName = dialogView.findViewById<EditText>(R.id.etCourseName)
        val btnAddCourse = dialogView.findViewById<View>(R.id.btnAddCourse)
        val rvCourses = dialogView.findViewById<RecyclerView>(R.id.rvCourses)

        val courses = mutableListOf<Course>()
        val coursesAdapter = CoursesAdapter(courses,
            onDeleteCourse = { course -> confirmDeleteCourse(course, department) }
        )
        rvCourses.layoutManager = LinearLayoutManager(requireContext())
        rvCourses.adapter = coursesAdapter

        // Fetch courses of this department from server
        fetchCourses(department.id) { fetchedCourses ->
            courses.clear()
            courses.addAll(fetchedCourses)
            coursesAdapter.notifyDataSetChanged()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Manage Courses - ${department.name}")
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()

        btnAddCourse.setOnClickListener {
            val courseName = etCourseName.text.toString().trim()
            if (courseName.isEmpty()) {
                Toast.makeText(requireContext(), "Enter course name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addCourse(department.id, courseName) {
                fetchCourses(department.id) { updatedCourses ->
                    courses.clear()
                    courses.addAll(updatedCourses)
                    coursesAdapter.notifyDataSetChanged()
                    etCourseName.setText("")
                }
            }
        }

        dialog.show()
    }

    private fun fetchCourses(departmentId: String, onResult: (List<Course>) -> Unit) {
        val url = "$baseUrl/get_courses.php?department_id=$departmentId"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val list = mutableListOf<Course>()
                    if (json.getBoolean("success")) {
                        val data = json.getJSONArray("courses")
                        for (i in 0 until data.length()) {
                            val obj = data.getJSONObject(i)
                            list.add(
                                Course(
                                    id = obj.getString("id"),
                                    name = obj.getString("name")
                                )
                            )
                        }
                        onResult(list)
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch courses", Toast.LENGTH_SHORT).show()
                        onResult(emptyList())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error parsing courses", Toast.LENGTH_SHORT).show()
                    onResult(emptyList())
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun addCourse(departmentId: String, courseName: String, onSuccess: () -> Unit) {
        val url = "$baseUrl/add_course.php"
        val request = object : StringRequest(Method.POST, url,
            { response ->
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    Toast.makeText(requireContext(), "Course added", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(requireContext(), "Failed to add course", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "department_id" to departmentId,
                    "course_name" to courseName
                )
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun confirmDeleteCourse(course: Course, department: Department) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete the course '${course.name}' from '${department.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCourse(course)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCourse(course: Course) {
        val url = "$baseUrl/delete_course.php"
        val request = object : StringRequest(Method.POST, url,
            { response ->
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    Toast.makeText(requireContext(), "Course deleted", Toast.LENGTH_SHORT).show()
                    fetchDepartments() // Refresh departments & courses UI
                } else {
                    Toast.makeText(requireContext(), "Failed to delete course", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                return mapOf("course_id" to course.id)
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    data class Department(val id: String, val name: String, val courses: MutableList<Course>)
    data class Course(val id: String, val name: String)

    // RecyclerView Adapter for Departments
    class DepartmentsAdapter(
        private val departments: List<Department>,
        private val onDeleteDept: (Department) -> Unit,
        private val onManageCourses: (Department) -> Unit
    ) : RecyclerView.Adapter<DepartmentsAdapter.DeptViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeptViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return DeptViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeptViewHolder, position: Int) {
            val dept = departments[position]
            holder.bind(dept, onDeleteDept, onManageCourses)
        }

        override fun getItemCount() = departments.size

        class DeptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val text1 = itemView.findViewById<TextView>(android.R.id.text1)
            private val text2 = itemView.findViewById<TextView>(android.R.id.text2)

            fun bind(dept: Department, onDeleteDept: (Department) -> Unit, onManageCourses: (Department) -> Unit) {
                text1.text = dept.name
                text2.text = "Courses: ${dept.courses.size}"
                itemView.setOnClickListener {
                    onManageCourses(dept)
                }
                itemView.setOnLongClickListener {
                    onDeleteDept(dept)
                    true
                }
            }
        }
    }

    // RecyclerView Adapter for Courses inside dialog
    class CoursesAdapter(
        private val courses: List<Course>,
        private val onDeleteCourse: (Course) -> Unit
    ) : RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return CourseViewHolder(view)
        }

        override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
            val course = courses[position]
            holder.bind(course, onDeleteCourse)
        }

        override fun getItemCount() = courses.size

        class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val text1 = itemView.findViewById<TextView>(android.R.id.text1)
            fun bind(course: Course, onDeleteCourse: (Course) -> Unit) {
                text1.text = course.name
                itemView.setOnLongClickListener {
                    onDeleteCourse(course)
                    true
                }
            }
        }
    }
}
