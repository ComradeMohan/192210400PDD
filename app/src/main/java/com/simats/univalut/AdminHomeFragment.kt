package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.simats.univalut.databinding.FragmentAdminHomeBinding
import org.json.JSONObject

class AdminHomeFragment : Fragment() {

    private var adminId: String? = null
    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var context: FragmentActivity
    private var collegeName: String? = null



    private lateinit var tvNoticeTitle: TextView
    private lateinit var tvNoticeDescription: TextView

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

        // Initialize views safely here
        tvNoticeTitle = view.findViewById(R.id.tvNoticeTitle)
        tvNoticeDescription = view.findViewById(R.id.tvNoticeDescription)

        adminId = arguments?.getString("admin_id")  // fixed key

        adminId?.let { fetchAdminDetails(it) }

        binding.tvTotalStudents.text = "0"

        binding.rvRecentActivity.layoutManager = LinearLayoutManager(context)
        binding.rvRecentActivity.adapter = RecentActivityAdapter(getDummyActivityList())

        binding.btnAddStudent.setOnClickListener {
            Toast.makeText(requireContext(), "Add Student clicked", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddFaculty.setOnClickListener {
            Toast.makeText(requireContext(), "Add Faculty clicked", Toast.LENGTH_SHORT).show()
            showAddFacultyDialog()
        }

        binding.btnPostNotice.setOnClickListener {
            val collegeName = binding.tvTitle.text.toString().split(" - ").last()
            val intent = Intent(context, AdminPostNotice::class.java)
            intent.putExtra("COLLEGE_NAME", collegeName)
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

    private fun fetchLatestNotice(college: String) {
        val url = "https://api-9buk.onrender.com/get_latest_notice.php?college=$college"
        val ctx = context ?: return  // Safely get context or return if fragment is not attached
        val queue = Volley.newRequestQueue(ctx)

        val jsonObjectRequest = JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            { response ->
                if (!isAdded) return@JsonObjectRequest  // Fragment no longer valid
                val success = response.getBoolean("success")
                if (success) {
                    val title = response.getString("title")
                    val description = response.getString("description")
                    tvNoticeTitle.text = title
                    tvNoticeDescription.text = description
                } else {
                    tvNoticeTitle.text = "No Notices"
                    tvNoticeDescription.text = ""
                }
            },
            {
                if (!isAdded) return@JsonObjectRequest
                tvNoticeTitle.text = "Error"
                tvNoticeDescription.text = "Failed to fetch notice."
            }
        )
        queue.add(jsonObjectRequest)
    }
    private fun fetchAdminDetails(adminId: String) {
        val url = "https://api-9buk.onrender.com/getAdminDetails.php?admin_id=$adminId"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    val name = response.getString("name")
                    val college = response.getString("college")
                    collegeName = college
                    collegeName?.let { fetchLatestNotice(it) }
                    binding.tvTitle.text = "$name - $college"
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error parsing admin details", Toast.LENGTH_SHORT).show()

                }
            },
            { error ->
                if (!isAdded) return@JsonObjectRequest
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(requireContext()).add(jsonObjectRequest)
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
            val collegeName = etCollege.text.toString().trim()
            val password = "welcome"

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            postFacultyToServer(name, email, phone, collegeName, loginId, password)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun postFacultyToServer(name: String, email: String, phone: String, college: String, loginId: String, password: String) {
        val url = "https://api-9buk.onrender.com/faculty_register.php"

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

    private fun getNextFacultyId(callback: (String) -> Unit) {
        val url = "https://api-9buk.onrender.com/getNextFacultyId.php"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    val nextId = response.getString("next_id")
                    callback(nextId)
                } catch (e: Exception) {
                    callback("SSE001")
                }
            },
            {
                callback("SSE001")
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun getDummyActivityList(): List<String> {
        return listOf(
            "Student John added to CS101",
            "Notice posted: Midterm postponed",
            "Dr. Smith uploaded lecture slides",
            "Faculty Jane added to ME201"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(adminId: String): Fragment {
            val fragment = AdminHomeFragment()
            val args = Bundle()
            args.putString("admin_id", adminId) // fixed key
            fragment.arguments = args
            return fragment
        }
    }
}
