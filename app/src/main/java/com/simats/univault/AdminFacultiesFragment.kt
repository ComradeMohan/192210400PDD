package com.simats.univault

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.util.Log
import com.android.volley.Request
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class AdminFacultiesFragment : Fragment() {

    private lateinit var collegeName: String
    private lateinit var facultyContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private val facultyViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sf = requireActivity().getSharedPreferences("user_sf", Context.MODE_PRIVATE)
        collegeName = sf.getString("college", "") ?: ""

        Log.d("AdminFaculties", "College name: $collegeName")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_admin_faculties, container, false)

        facultyContainer = view.findViewById(R.id.facultyList)
        searchEditText = view.findViewById(R.id.searchEditText)

        fetchFacultiesByCollege()
        setupSearch()

        return view
    }

    private fun fetchFacultiesByCollege() {
        val url = "http://192.168.56.1/univault/getFacultyByCollege.php?college=${collegeName}"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("AdminFaculties", "Response: $response")
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        val facultyArray = jsonObject.getJSONArray("faculty")
                        facultyContainer.removeAllViews()
                        facultyViews.clear()

                        for (i in 0 until facultyArray.length()) {
                            val faculty = facultyArray.getJSONObject(i)
                            val name = faculty.getString("name")
                            val loginId = faculty.getString("login_id")

                            val facultyView = layoutInflater.inflate(
                                R.layout.item_faculty, facultyContainer, false
                            )
                            facultyView.findViewById<TextView>(R.id.facultyName).text = name
                            facultyView.findViewById<TextView>(R.id.facultyId).text = "ID: $loginId"

                            facultyView.findViewById<TextView>(R.id.deleteButton).setOnClickListener {
                                showDeleteConfirmationDialog(name, loginId)
                            }

                            facultyContainer.addView(facultyView)
                            facultyViews.add(facultyView)
                        }
                    } else {
                        Toast.makeText(requireContext(), jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("AdminFaculties", "Network error", error)
                Toast.makeText(requireContext(), "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }
    private fun showDeleteConfirmationDialog(facultyName: String, loginId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Faculty")
            .setMessage("Are you sure you want to delete:\n\nName: $facultyName\nID: $loginId?")
            .setIcon(R.drawable.ic_warning) // Optional: Add a warning icon
            .setPositiveButton("Delete") { _, _ ->
                deleteFaculty(loginId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFaculty(loginId: String) {
        val url = "http://192.168.56.1/univault/deleteFaculty.php?login_id=$loginId"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        Toast.makeText(requireContext(), "Faculty deleted", Toast.LENGTH_SHORT).show()
                        fetchFacultiesByCollege() // Refresh list
                    } else {
                        Toast.makeText(requireContext(), jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }


    private fun setupSearch() {
        searchEditText.addTextChangedListener {
            val query = it.toString().trim()
            for (view in facultyViews) {
                if (view is ViewGroup) {
                    val nameView = view.findViewById<TextView>(R.id.facultyName)
                    val idView = view.findViewById<TextView>(R.id.facultyId)
                    val textCombined = "${nameView.text} ${idView.text}"
                    view.visibility = if (textCombined.contains(query, ignoreCase = true)) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
