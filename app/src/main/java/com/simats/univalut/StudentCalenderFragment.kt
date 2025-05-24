package com.simats.univalut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import java.time.LocalDate

class StudentCalenderFragment : Fragment() {

    private var studentID: String? = null
    private var collegeName: String? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private val eventList = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            studentID = it.getString("studentID")
            collegeName = it.getString("college_name")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_student_calender, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        eventAdapter = EventAdapter(eventList)
        recyclerView.adapter = eventAdapter

        // Fetch events directly if collegeName is available
        collegeName?.let {
            fetchEvents(it)
        } ?: run {
            // If collegeName is not available, fetch student name and then events
            studentID?.let { fetchStudentName(it) }
        }

        return view
    }

    private fun fetchStudentName(studentID: String) {
        val url = "http://192.168.203.54/univault/fetch_student_name.php?studentID=$studentID" // Adjust the URL

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val success = response.getBoolean("success")
                if (success) {
                    val name = response.getString("name")
                    collegeName = response.getString("college")
                    // Log the name and college for debugging purposes
                    Toast.makeText(requireContext(), "Student: $name, College: $collegeName", Toast.LENGTH_SHORT).show()

                    // Fetch events after getting the college name
                    collegeName?.let { fetchEvents(it) }
                } else {
                    Toast.makeText(requireContext(), "Student not found", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error fetching student data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(jsonObjectRequest)
    }

    private fun fetchEvents(collegeName: String) {
        val url = "http://192.168.203.54/univault/getEvents.php?college_name=$collegeName" // Adjust the URL

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    eventList.clear() // Clear any previous data

                    // Assuming the response contains a JSON array under "data" key
                    val eventsArray: JSONArray = response.getJSONArray("data")
                    for (i in 0 until eventsArray.length()) {
                        val eventObj = eventsArray.getJSONObject(i)

                        // Parse event details from the backend response
                        val title = eventObj.getString("title")
                        val type = eventObj.getString("type")
                        val startDate = LocalDate.parse(eventObj.getString("start_date"))
                        val endDate = LocalDate.parse(eventObj.getString("end_date"))
                        val description = eventObj.getString("description")

                        // Add event to the list
                        val event = Event(title, type, startDate, endDate, description)
                        eventList.add(event)
                    }

                    // Notify the adapter to update the UI
                    eventAdapter.notifyDataSetChanged()
                    Toast.makeText(requireContext(), "Events loaded", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error parsing events", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error fetching events: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(jsonObjectRequest)
    }

    companion object {
        @JvmStatic
        fun newInstance(studentID: String, collegeName: String? = null) =
            StudentCalenderFragment().apply {
                arguments = Bundle().apply {
                    putString("studentID", studentID)
                    putString("college_name", collegeName)
                }
            }
    }
}

