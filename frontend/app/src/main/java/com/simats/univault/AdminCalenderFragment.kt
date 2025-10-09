package com.simats.univault

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*

class AdminCalenderFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private val eventList = mutableListOf<Event>() // visible list
    private val fullEventList = mutableListOf<Event>() // full copy for filtering

    private lateinit var startDateTextView: TextView
    private lateinit var endDateTextView: TextView
    private var adminId: String? = null
    private var collegeName: String? = null
    private var isAddingEvent = false
    private lateinit var noEventsTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_calender, container, false)
        noEventsTextView = view.findViewById(R.id.noEventsTextView)

        recyclerView = view.findViewById(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        eventAdapter = EventAdapter(eventList, requireContext())
        recyclerView.adapter = eventAdapter

        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            filterEventsByDate(selectedDate)
        }

        adminId = arguments?.getString("admin_id")
        adminId?.let {
            Log.d("CalendarLog", "Fetching admin details for admin_id=$it")
            fetchAdminDetails(it)
        }

        view.findViewById<Button>(R.id.buttonAddEvent).setOnClickListener {
            showAddEventDialog()
        }

        return view
    }

    private fun filterEventsByDate(date: LocalDate) {
        Log.d("CalendarLog", "Filtering events for date: $date")
        val filteredList = fullEventList.filter { event ->
            !date.isBefore(event.startDate) && !date.isAfter(event.endDate)
        }
        Log.d("CalendarLog", "Filtered list size: ${filteredList.size}")
        eventAdapter.updateData(filteredList)

        if (filteredList.isEmpty()) {
            noEventsTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noEventsTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun fetchAdminDetails(adminId: String) {
        val url = "http://10.86.199.54/univault/getAdminDetails.php?admin_id=$adminId"
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    collegeName = response.getString("college")
                    Log.d("CalendarLog", "College name from admin details: $collegeName")
                    fetchEvents()
                } catch (e: Exception) {
                    Log.e("CalendarLog", "Error parsing admin details: ${e.message}")
                }
            },
            { error ->
                Log.e("CalendarLog", "Network error fetching admin details: ${error.message}")
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun fetchEvents() {
        val url = "http://10.86.199.54/univault/getEvents.php?college_name=$collegeName"
        Log.d("CalendarLog", "Fetching events from: $url")

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    Log.d("CalendarLog", "Event fetch response: $response")
                    eventList.clear()
                    fullEventList.clear()

                    val eventsArray = response.getJSONArray("data")
                    for (i in 0 until eventsArray.length()) {
                        val eventObj = eventsArray.getJSONObject(i)
                        val event = Event(
                            eventObj.getString("title"),
                            eventObj.getString("type"),
                            LocalDate.parse(eventObj.getString("start_date")),
                            LocalDate.parse(eventObj.getString("end_date")),
                            eventObj.getString("description")
                        )
                        eventList.add(event)
                        fullEventList.add(event)
                        Log.d("CalendarLog", "Fetched event: $event")
                    }

                    eventAdapter.notifyDataSetChanged()
                } catch (e: Exception) {
                    Log.e("CalendarLog", "Error parsing events: ${e.message}")
                }
            },
            { error ->
                Log.e("CalendarLog", "Error fetching events: ${error.message}")
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showAddEventDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)

        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val editTextType = dialogView.findViewById<EditText>(R.id.editTextType)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextDescription)
        startDateTextView = dialogView.findViewById(R.id.textViewStartDate)
        endDateTextView = dialogView.findViewById(R.id.textViewEndDate)

        startDateTextView.setOnClickListener { showDatePicker(startDateTextView) }
        endDateTextView.setOnClickListener { showDatePicker(endDateTextView) }

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Add New Event")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel") { dialog, _ ->
                isAddingEvent = false
                dialog.dismiss()
            }
            .create()

        alertDialog.setOnShowListener {
            val addButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            addButton.setOnClickListener {
                if (isAddingEvent) return@setOnClickListener
                isAddingEvent = true
                addButton.isEnabled = false

                val title = editTextTitle.text.toString().trim()
                val type = editTextType.text.toString().trim()
                val description = editTextDescription.text.toString().trim()
                val startDateStr = startDateTextView.text.toString()
                val endDateStr = endDateTextView.text.toString()

                if (title.isEmpty() || type.isEmpty() || description.isEmpty() ||
                    startDateStr == "Pick Start Date" || endDateStr == "Pick End Date") {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    addButton.isEnabled = true
                    isAddingEvent = false
                    return@setOnClickListener
                }

                try {
                    val startDate = LocalDate.parse(startDateStr)
                    val endDate = LocalDate.parse(endDateStr)

                    val newEvent = Event(title, type, startDate, endDate, description)
                    Log.d("CalendarLog", "Submitting new event: $newEvent")
                    addEventToBackend(newEvent) {
                        isAddingEvent = false
                        addButton.isEnabled = true
                        alertDialog.dismiss()
                    }
                } catch (e: DateTimeParseException) {
                    Toast.makeText(requireContext(), "Invalid date format (yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                    addButton.isEnabled = true
                    isAddingEvent = false
                }
            }
        }

        alertDialog.show()
    }

    private fun addEventToBackend(event: Event, callback: () -> Unit) {
        val url = "http://10.86.199.54/univault/addEvent.php"

        val params = hashMapOf(
            "title" to event.title,
            "type" to event.type,
            "description" to event.description,
            "start_date" to event.startDate.toString(),
            "end_date" to event.endDate.toString(),
            "college_name" to (collegeName ?: "")
        )

        val jsonObject = JSONObject(params as Map<*, *>)
        Log.d("CalendarLog", "Sending event POST data: $jsonObject")

        val request = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            { response ->
                Log.d("CalendarLog", "Add event response: $response")
                val status = response.optString("status")
                val message = response.optString("message")
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                if (status == "success" || message == "Event exists") {
                    fetchEvents()
                }

                callback()
            },
            { error ->
                Log.e("CalendarLog", "Network error during event add: ${error.message}")
                callback()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    fun deleteEvent(event: Event, callback: () -> Unit) {
        val url = "http://10.86.199.54/univault/delete_event.php"
        val params = hashMapOf(
            "title" to event.title,
            "college_name" to (collegeName ?: "")
        )

        val jsonObject = JSONObject(params as Map<*, *>)

        val request = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            { response ->
                Log.d("CalendarLog", "Delete event response: $response")
                val status = response.optString("status")
                val message = response.optString("message")
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                if (status == "success") {
                    fetchEvents()
                }

                callback()
            },
            { error ->
                Log.e("CalendarLog", "Error deleting event: ${error.message}")
                Toast.makeText(requireContext(), "Error deleting event", Toast.LENGTH_SHORT).show()
                callback()
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showDatePicker(targetTextView: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, y, m, d ->
            val date = LocalDate.of(y, m + 1, d)
            targetTextView.text = date.toString()
        }, year, month, day)

        if (targetTextView == startDateTextView) {
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        }

        if (targetTextView == endDateTextView && startDateTextView.text.isNotEmpty()) {
            try {
                val selectedStart = LocalDate.parse(startDateTextView.text.toString())
                val startMillis = Calendar.getInstance().apply {
                    set(selectedStart.year, selectedStart.monthValue - 1, selectedStart.dayOfMonth)
                }.timeInMillis
                datePickerDialog.datePicker.minDate = startMillis
            } catch (e: Exception) {
                datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            }
        }

        datePickerDialog.show()
    }

    companion object {
        fun newInstance(adminId: String): AdminCalenderFragment {
            val fragment = AdminCalenderFragment()
            val args = Bundle()
            args.putString("admin_id", adminId)
            fragment.arguments = args
            return fragment
        }
    }
}
