package com.simats.univault

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.simats.univault.databinding.FragmentAdminHomeBinding
import org.json.JSONObject

class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var context: FragmentActivity
    private var adminId: String? = null
    private var collegeName: String? = null
    private var collegeId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        context = activity ?: requireActivity()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adminId = arguments?.getString("admin_id")
        adminId?.let { fetchAdminDetails(it) }

        binding.tvTotalStudents.text = "0"  // Placeholder for student count
        binding.rvRecentActivity.layoutManager = LinearLayoutManager(context)
        binding.btnViewStudents.setOnClickListener {
            val fragment = FacultyStudentsFragment().apply {
                arguments = Bundle().apply {
                    putString("college_id", collegeId)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.faculty.setOnClickListener {
            val fragment = AdminFacultiesFragment().apply {
                arguments = Bundle().apply {
                    putString("college_id", collegeId)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.btnAddDept.setOnClickListener {
            val fragment = ManageDepartmentFragment().apply {
                arguments = Bundle().apply {
                    putString("admin_id", adminId)
                    putString("college_id", collegeId)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }


        binding.btnAddFaculty.setOnClickListener {
            showAddFacultyDialog()
        }

        binding.btnPostNotice.setOnClickListener {
            val college = collegeName ?: return@setOnClickListener
            val intent = Intent(context, AdminPostNotice::class.java).apply {
                putExtra("COLLEGE_NAME", college)
            }
            startActivity(intent)
        }

        binding.btnUploadFiles.setOnClickListener {
            val fragment = FacultyMaterialsFragment().apply {
                arguments = Bundle().apply {
                    putString("college_name", collegeName)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
    private fun showAddDepartmentDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_department, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Department")
            .setView(dialogView)
            .create()

        val etDeptName = dialogView.findViewById<EditText>(R.id.etDeptName)
        val btnSubmitDept = dialogView.findViewById<Button>(R.id.btnSubmitDept)

        btnSubmitDept.setOnClickListener {
            val deptName = etDeptName.text.toString().trim()
            if (deptName.isEmpty() || collegeName.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please enter department name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addDepartmentToServer(collegeName!!, deptName)
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun addDepartmentToServer(college: String, departmentName: String) {
        fetchCollegeIdByName(college) { collegeId ->
            if (collegeId == null) {
                Toast.makeText(requireContext(), "Invalid college name", Toast.LENGTH_SHORT).show()
                return@fetchCollegeIdByName
            }

            val url = "http://+/univault/add_department.php"

            val request = object : StringRequest(Request.Method.POST, url,
                { response ->
                    Toast.makeText(requireContext(), "Department added successfully", Toast.LENGTH_SHORT).show()
                },
                { error ->
                    Toast.makeText(requireContext(), "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }) {

                override fun getParams(): Map<String, String> {
                    return mapOf(
                        "college_id" to collegeId,
                        "name" to departmentName
                    )
                }
            }

            Volley.newRequestQueue(requireContext()).add(request)
        }
    }



    private fun fetchCollegeIdByName(collegeName: String, onResult: (String?) -> Unit) {
        val url = "http://10.86.199.54/univault/get_college_id.php" // Replace IP if testing on device
        val params = HashMap<String, String>()
        params["college_name"] = collegeName

        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        collegeId = json.getString("college_id")
                        onResult(collegeId)
                    } else {
                        Toast.makeText(requireContext(), "College not found", Toast.LENGTH_SHORT).show()
                        onResult(null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Parsing error", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }) {

            override fun getParams(): Map<String, String> {
                return params
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun fetchAdminDetails(adminId: String) {
        val url = "http://10.86.199.54/univault/getAdminDetails.php?admin_id=$adminId"
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (!isAdded || _binding == null) return@JsonObjectRequest
                val safeBinding = _binding!!
                try {

                    val name = response.getString("name")
                    collegeName = response.getString("college")
                    val studentCount = response.getInt("student_count")
                    val facultyCount = response.getInt("faculty_count")
                    safeBinding.tvTitle.text = "$name - $collegeName"
                    safeBinding.tvTotalStudents.text = studentCount.toString()
                    safeBinding.tvTotalFaculty.text = facultyCount.toString()
                    collegeName?.let {
                        fetchLatestNotice(it)
                        fetchFeedbacks()
                    }
                    collegeName?.let {
                        fetchCollegeIdByName(it) { id ->
                            collegeId = id
                            // Do something if needed after getting ID
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error parsing admin details", Toast.LENGTH_SHORT).show()
                }
            },
            { if (!isAdded || _binding == null) return@JsonObjectRequest
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun fetchLatestNotice(college: String) {
        val url = "http://10.86.199.54/univault/get_latest_notice.php?college=$college"
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (!isAdded || _binding == null) return@JsonObjectRequest
                val safeBinding = _binding!!
                if (response.getBoolean("success")) {
                    safeBinding.tvNoticeTitle.text = response.getString("title")
                    safeBinding.tvNoticeDescription.text = response.getString("description")
                } else {
                    safeBinding.tvNoticeTitle.text = "No Notices"
                    safeBinding.tvNoticeDescription.text = ""
                }
            },
            {
                if (!isAdded || _binding == null) return@JsonObjectRequest
                val safeBinding = _binding!!
                safeBinding.tvNoticeTitle.text = "Error"
                safeBinding.tvNoticeDescription.text = "Failed to fetch notice."
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun fetchFeedbacks() {
        val college = collegeName ?: return  // Skip if college name is null
        val url = "http://10.86.199.54/univault/get_feedbacks.php?college=$college"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (!isAdded || _binding == null) return@JsonObjectRequest
                val safeBinding = _binding!!

                if (response.getBoolean("success")) {
                    val feedbackList = mutableListOf<Feedback>()
                    val dataArray = response.getJSONArray("data")
                    for (i in 0 until dataArray.length()) {
                        val obj = dataArray.getJSONObject(i)
                        feedbackList.add(
                            Feedback(
                                user_id = obj.getString("user_id"),
                                feedback = obj.getString("feedback"),
                                created_at = obj.getString("created_at")
                            )
                        )
                    }
                    safeBinding.rvRecentActivity.adapter = FeedbackAdapter(feedbackList)
                } else {
                    Toast.makeText(requireContext(), "No feedbacks found", Toast.LENGTH_SHORT).show()
                }
            },
            {
                if (!isAdded || _binding == null) return@JsonObjectRequest
                Toast.makeText(requireContext(), "Error loading feedbacks", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }


    private fun showAddFacultyDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_faculty, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Faculty")
            .setView(dialogView)
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etLoginId = dialogView.findViewById<EditText>(R.id.etLoginId)
        val etCollege = dialogView.findViewById<EditText>(R.id.etCollege)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmitFaculty)

        etCollege.setText(collegeName)
        etPassword.setText("welcome")
        etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        btnSubmit.isEnabled = false

        getNextFacultyId { newId ->
            etLoginId.setText(newId)
            btnSubmit.isEnabled = true
        }

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val loginId = etLoginId.text.toString().trim()
            val college = etCollege.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            postFacultyToServer(name, email, phone, college, loginId, "welcome")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getNextFacultyId(callback: (String) -> Unit) {
        val college = collegeName ?: run {
            callback("FAC001")
            return
        }
        val url = "http://10.86.199.54/univault/getNextFacultyId.php?college=${college.replace(" ", "%20")}"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    callback(response.getString("next_id"))
                } catch (e: Exception) {
                    callback("FAC001")
                }
            },
            {
                callback("FAC001")
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }


    private fun postFacultyToServer(
        name: String, email: String, phone: String,
        college: String, loginId: String, password: String
    ) {
        val url = "http://10.86.199.54/univault/faculty_register.php"
        val params = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("phone_number", phone)
            put("college", college)
            put("login_id", loginId)
            put("password", password)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, params,
            {
                Toast.makeText(requireContext(), "Faculty added successfully", Toast.LENGTH_SHORT).show()
            },
            {
                Toast.makeText(requireContext(), "Failed to add faculty: ${it.message}", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(adminId: String): AdminHomeFragment {
            val fragment = AdminHomeFragment()
            fragment.arguments = Bundle().apply {
                putString("admin_id", adminId)
            }
            return fragment
        }
    }
}
