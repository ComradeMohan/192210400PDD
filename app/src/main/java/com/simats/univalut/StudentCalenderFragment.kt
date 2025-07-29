package com.simats.univalut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
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
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter

    private val fullEventList = mutableListOf<Event>()     // All events fetched from server
    private val filteredEventList = mutableListOf<Event>() // Events shown in RecyclerView

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
        val view = inflater.inflate(R.layout.fragment_student_calender, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        eventAdapter = EventAdapter(filteredEventList)
        recyclerView.adapter = eventAdapter
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            collegeName?.let {
                fetchEvents(it)
            } ?: run {
                studentID?.let { fetchStudentName(it) }
            }
        }

        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            filterEventsByDate(selectedDate)
        }

        collegeName?.let {
            fetchEvents(it)
        } ?: run {
            studentID?.let { fetchStudentName(it) }
        }

        return view
    }

    private fun filterEventsByDate(date: LocalDate) {
        val filtered = fullEventList.filter {
            !date.isBefore(it.startDate) && !date.isAfter(it.endDate)
        }
        filteredEventList.clear()
        filteredEventList.addAll(filtered)
        eventAdapter.notifyDataSetChanged()
        swipeRefresh.isRefreshing = false
        Toast.makeText(requireContext(), "Events loaded", Toast.LENGTH_SHORT).show()
        val emptyText = view?.findViewById<TextView>(R.id.textEmpty)
        emptyText?.visibility = if (filteredEventList.isEmpty()) View.VISIBLE else View.GONE

    }

    private fun fetchStudentName(studentID: String) {
        val url = "http://10.143.152.54/univault/fetch_student_name.php?studentID=$studentID"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val success = response.getBoolean("success")
                if (success) {
                    val name = response.getString("name")
                    collegeName = response.getString("college")
                    Toast.makeText(requireContext(), "Student: $name, College: $collegeName", Toast.LENGTH_SHORT).show()
                    collegeName?.let { fetchEvents(it) }
                } else {
                    Toast.makeText(requireContext(), "Student not found", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error fetching student data: ${error.message}", Toast.LENGTH_SHORT).show()
                swipeRefresh.isRefreshing = false
            }
        )

        Volley.newRequestQueue(requireContext()).add(jsonObjectRequest)
    }

    private fun fetchEvents(collegeName: String) {
        val url = "http://10.143.152.54/univault/getEvents.php?college_name=$collegeName"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    fullEventList.clear()
                    filteredEventList.clear()

                    val eventsArray: JSONArray = response.getJSONArray("data")
                    for (i in 0 until eventsArray.length()) {
                        val eventObj = eventsArray.getJSONObject(i)

                        val title = eventObj.getString("title")
                        val type = eventObj.getString("type")
                        val startDate = LocalDate.parse(eventObj.getString("start_date"))
                        val endDate = LocalDate.parse(eventObj.getString("end_date"))
                        val description = eventObj.getString("description")

                        val event = Event(title, type, startDate, endDate, description)
                        fullEventList.add(event)
                    }

                    filteredEventList.addAll(fullEventList)
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
        swipeRefresh.isRefreshing = false

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
