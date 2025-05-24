package com.simats.univalut

import CourseFragment
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.simats.univalut.databinding.FragmentFacultyHomeBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class FacultyHomeFragment : Fragment() {

    private var facultyId: String? = null
    private var collegeName: String? = null

    private lateinit var tvNoticeTitle: TextView
    private lateinit var tvNoticeDescription: TextView

    private var _binding: FragmentFacultyHomeBinding? = null
    private val binding get() = _binding!!


    private lateinit var editTextQuickNote: EditText
    private lateinit var buttonSaveNote: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_faculty_home, container, false)


        editTextQuickNote = view.findViewById(R.id.editTextQuickNote)
        buttonSaveNote = view.findViewById(R.id.buttonSaveNote)

        // Load saved note


        buttonSaveNote.setOnClickListener {
            val note = editTextQuickNote.text.toString().trim()
            saveQuickNote(note)
            Toast.makeText(requireContext(), "Note saved", Toast.LENGTH_SHORT).show()
        }

        facultyId = arguments?.getString("ID")
        val facultyNameTextView: TextView = view.findViewById(R.id.facultyNameTextView)

        tvNoticeTitle = view.findViewById(R.id.tvNoticeTitle)
        tvNoticeDescription = view.findViewById(R.id.tvNoticeDescription)

        facultyId?.let {
            FacultyNameTask(facultyNameTextView, this).execute(it)
        }

        val notificationIcon: View = view.findViewById(R.id.notificationIcon)
        notificationIcon.setOnClickListener {
            val intent = Intent(requireContext(), StudentNotificationsActivity::class.java)
            intent.putExtra("college", collegeName)
            startActivity(intent)
        }

        val uploadButton: LinearLayout = view.findViewById(R.id.uploadResourcesButton)
        uploadButton.setOnClickListener {
            if (collegeName != null) {
                val courseFragment = CourseFragment.newInstanceWithCollegeName(collegeName!!)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, courseFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(requireContext(), "College name not available", Toast.LENGTH_SHORT).show()
            }
        }
        loadQuickNote()
        val sendAnnounce: LinearLayout = view.findViewById(R.id.sendAnnouncementButton)
        sendAnnounce.setOnClickListener {
            val calendar = if (!collegeName.isNullOrEmpty()) {
                // If collegeName is available, pass it directly
                StudentCalenderFragment.newInstance(studentID = "", collegeName = collegeName)
            } else {
                // If collegeName is not available, pass only studentID
                StudentCalenderFragment.newInstance(studentID = facultyId ?: "")
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, calendar)
                .addToBackStack(null)
                .commit()

        }


        return view
    }
    private fun saveQuickNote(note: String) {
        val sharedPref = requireContext().getSharedPreferences("faculty_prefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("quick_note_${facultyId}", note)
            apply()
        }
    }

    private fun loadQuickNote() {
        val sharedPref = requireContext().getSharedPreferences("faculty_prefs", android.content.Context.MODE_PRIVATE)
        val savedNote = sharedPref.getString("quick_note_${facultyId}", "")
        editTextQuickNote.setText(savedNote)
    }

    private fun fetchLatestNotice(college: String) {
        val url = "https://api-9buk.onrender.com/get_latest_notice.php?college=$college"
        val ctx = context ?: return
        val queue = Volley.newRequestQueue(ctx)

        val jsonObjectRequest = JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            { response ->
                if (!isAdded) return@JsonObjectRequest
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

    private class FacultyNameTask(
        private val facultyNameTextView: TextView,
        private val fragment: FacultyHomeFragment
    ) : AsyncTask<String, Void, Pair<String, String>?>() {

        override fun doInBackground(vararg params: String?): Pair<String, String>? {
            val facultyId = params[0] ?: return null
            val urlString = "https://api-9buk.onrender.com/get_faculty_name.php?facultyId=$facultyId"
            return try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonResponse = JSONObject(response.toString())
                if (jsonResponse.getBoolean("success")) {
                    val facultyName = jsonResponse.getString("name")
                    val collegeName = jsonResponse.getString("college")
                    Pair(facultyName, collegeName)
                } else null
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: Pair<String, String>?) {
            super.onPostExecute(result)
            if (result != null) {
                val (facultyName, collegeName) = result
                facultyNameTextView.text = facultyName
                fragment.collegeName = collegeName
                fragment.fetchLatestNotice(collegeName)
            } else {
                Toast.makeText(facultyNameTextView.context, "Failed to fetch faculty name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun newInstance(id: String): Fragment {
            val fragment = FacultyHomeFragment()
            val args = Bundle()
            args.putString("ID", id)
            fragment.arguments = args
            return fragment
        }
    }
}
