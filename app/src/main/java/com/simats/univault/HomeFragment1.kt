package com.simats.univault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class HomeFragment1 : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var etSearch: EditText
    private lateinit var courseCode1: TextView
    private lateinit var courseCode2: TextView

    private lateinit var tvNoticeTitle: TextView
    private lateinit var tvNoticeDescription: TextView

    private var collegeName: String? = null
    private var studentID: String? = null


    private lateinit var subjectAdapter: SubjectAdapter
    private var allSubjects: List<Subject> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentID = arguments?.getString("studentID")

        // Get student ID from arguments
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sf = requireContext().getSharedPreferences("user_sf", Context.MODE_PRIVATE)
        val savedProgress = sf.getInt("degreeProgress", 0)
        val cgpaValue = sf.getFloat("cgpaValue", 0.0F)

        val cgpaText = view.findViewById<TextView>(R.id.cgpaValue)
        val progressBar = view.findViewById<ProgressBar>(R.id.degreeProgressBar)
        val percentageText = view.findViewById<TextView>(R.id.degreeProgressPercentage)
        val predictionText = view.findViewById<TextView>(R.id.predictedCgpaTextView)

        // Set CGPA and progress
        cgpaText.text = "CGPA : %.2f".format(cgpaValue)
        progressBar.progress = savedProgress
        percentageText.text = "$savedProgress%"

        // ✅ Calculate predicted CGPA
        val roundedCgpa = cgpaValue.toInt().toFloat() // truncate decimal, e.g., 6.7 → 6
        val predictedCgpa = (roundedCgpa + cgpaValue) / 2f

        // ✅ Display predicted final CGPA
        predictionText.text = "Predicted CGPA: %.2f".format(predictedCgpa)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home1, container, false)
        etSearch = view.findViewById(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSubjects(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        // Initialize Views
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvStudentName = view.findViewById(R.id.tvStudentName)
        tvNoticeTitle = view.findViewById(R.id.tvNoticeTitle)
        tvNoticeDescription = view.findViewById(R.id.tvNoticeDescription)

        val notificationIcon: View = view.findViewById(R.id.notificationIcon)
        notificationIcon.setOnClickListener {
            val intent = Intent(requireContext(), StudentNotificationsActivity::class.java)
            intent.putExtra("college", collegeName) // Pass college name to the next activity
            startActivity(intent)
        }


        tvGreeting.text = getGreetingMessage()

        // Fetch student name and college
        studentID?.let {
            fetchStudentName(it)
        } ?: run {
            tvStudentName.text = "ID not found"
        }

        return view
    }
    private fun filterSubjects(query: String) {
        val filteredList = if (query.isEmpty()) {
            allSubjects
        } else {
            allSubjects.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        subjectAdapter.updateData(filteredList)
    }
    private fun fetchStudentName(studentID: String) {
        val url = "http://10.143.152.54/univault/fetch_student_name.php?studentID=$studentID"
        val queue = Volley.newRequestQueue(requireContext())

        val jsonObjectRequest = JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            { response ->
                if (!isAdded) return@JsonObjectRequest  // Check if fragment is still attached
                val success = response.getBoolean("success")
                if (success) {
                    val name = response.getString("name")
                    collegeName = response.getString("college")
                    val sf = requireContext().getSharedPreferences("user_sf", Context.MODE_PRIVATE)
                    sf.edit().putString("collegeName", collegeName).apply()
                    val departmentName = response.getString("dept")
                    // Check if fragment is still attached before using context
                    if (isAdded) {
                        fetchCollegeIdByName(collegeName?.toString() ?: "", requireContext()) { collegeId ->
                            if (collegeId != null) {
                                Log.d("CollegeID", "Fetched ID: $collegeId")
                                Toast.makeText(requireContext(), " college ID: $collegeId", Toast.LENGTH_SHORT).show()

                                fetchDepartmentId(collegeId, departmentName, requireContext()) { departmentId ->
                                    if (departmentId != null) {
                                        Log.d("DepartmentID", "Fetched ID: $departmentId")
                                        Toast.makeText(requireContext(), " Dept ID: $departmentId", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to fetch department ID", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(requireContext(), "Failed to fetch college ID", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    tvStudentName.text = name
                    collegeName?.let { fetchLatestNotice(it) }
                } else {
                    tvStudentName.text = "Student not found"
                }
            },
            {
                if (isAdded) {
                    Toast.makeText(context, "Error fetching student data", Toast.LENGTH_SHORT).show()
                }
            }
        )

        queue.add(jsonObjectRequest)
    }

    fun fetchCollegeIdByName(collegeName: String, context: Context, callback: (Int?) -> Unit) {
        val url = "http://10.143.152.54/univault/get_college_id.php" // Replace with your actual URL

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        val collegeId = jsonObject.getInt("college_id")
                        callback(collegeId) // Pass the college ID to the callback
                    } else {
                        Toast.makeText(context, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                        callback(null) // Return null if college not found
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Toast.makeText(context, "Request failed: ${error.message}", Toast.LENGTH_SHORT).show()
                callback(null)
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf("college_name" to collegeName)
            }
        }

        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(stringRequest)
    }
    fun fetchDepartmentId(collegeId: Int, departmentName: String, context: Context, callback: (Int?) -> Unit) {
        val url = "http://10.143.152.54/univault/get_department_id.php" // Replace with your PHP file URL

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                try {
                    // Parse the response as a JSON object
                    val jsonObject = JSONObject(response)
                    // Check for success in the response
                    if (jsonObject.getBoolean("success")) {
                        // Extract the department ID
                        val departmentId = jsonObject.getInt("department_id")
                        val sf = requireContext().getSharedPreferences("user_sf", Context.MODE_PRIVATE)
                        sf.edit().putInt("collegeId", collegeId).apply()
                        sf.edit().putInt("departmentId", departmentId).apply()

                        fetchPendingSubjects(studentID.toString(), departmentId.toString())
                        // Pass the department ID to the callback
                        callback(departmentId)
                    } else {
                        // If department is not found, show an error message
                        Toast.makeText(context, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                        // Pass null if the department ID is not found
                        callback(null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle parsing errors and show a Toast message
                    Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show()
                    // Pass null if there was an error
                    callback(null)
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                // Handle network errors
                Toast.makeText(context, "Request failed: ${error.message}", Toast.LENGTH_SHORT).show()
                // Pass null if the request failed
                callback(null)
            }
        ) {
            // Set the POST parameters to send to the backend
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "college_id" to collegeId.toString(),
                    "name" to departmentName
                )
            }
        }

        // Initialize the request queue and add the request to it
        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(stringRequest)
    }

    private fun fetchPendingSubjects(studentId: String, departmentId: String) {
        val urlStr = "http://10.143.152.54/univault/student_grades_pending.php?department_id=$departmentId&student_id=$studentId"

        Thread {
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connect()

                val response = conn.inputStream.bufferedReader().readText()
                println("API Response: $response") // Debug log

                val json = JSONObject(response)

                if (json.getBoolean("success")) {
                    val subjects = json.getJSONArray("courses")
                    val subjectList = mutableListOf<Subject>()

                    for (i in 0 until subjects.length()) {
                        val subject = subjects.getJSONObject(i)
                        val subjectId = subject.getString("id")
                        val subjectName = subject.getString("name").trim()
                        val subjectCredits = subject.getString("credits")
                        subjectList.add(Subject(subjectId, subjectName, subjectCredits))
                    }

                    requireActivity().runOnUiThread {
                        if (subjectList.isEmpty()) {
                            Toast.makeText(requireActivity(), "No pending courses found.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireActivity(), "Fetched ${subjectList.size} pending courses.", Toast.LENGTH_SHORT).show()
                        }
                        allSubjects = subjectList
                        val recyclerView: RecyclerView = requireView().findViewById(R.id.rvPendingSubjects)

                        recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        subjectAdapter = SubjectAdapter(allSubjects)
                        recyclerView.adapter = subjectAdapter
                    }
                } else {
                    val message = json.optString("message", "No data found")
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireActivity(), "Error fetching subjects", Toast.LENGTH_SHORT).show()
                }
            }
        }.start() // ✅ Don't forget to start the thread!
    }



    private fun fetchLatestNotice(college: String) {
        val url = "http://10.143.152.54/univault/get_latest_notice.php?college=$college"
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


    private fun getGreetingMessage(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            in 17..20 -> "Good Evening,"
            else -> "Good Night,"
        }
    }

    companion object {
        fun newInstance(studentID: String): HomeFragment1 {
            val fragment = HomeFragment1()
            val args = Bundle()
            args.putString("studentID", studentID)
            fragment.arguments = args
            return fragment
        }
    }
}
