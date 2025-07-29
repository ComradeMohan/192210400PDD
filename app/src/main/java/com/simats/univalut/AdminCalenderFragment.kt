package com.simats.univalut

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
import com.simats.univalut.databinding.FragmentAdminCalenderBinding
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*

class AdminCalenderFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private val eventList = mutableListOf<Event>()
    private lateinit var startDateTextView: TextView
    private lateinit var endDateTextView: TextView

    private var adminId: String? = null
    private var collegeName: String? = null

    // Flag to prevent double add
    private var isAddingEvent = false
    private lateinit var csvPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_calender, container, false)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        eventAdapter = EventAdapter(eventList)
        recyclerView.adapter = eventAdapter

        // Get admin ID and fetch college details
        adminId = arguments?.getString("admin_id")
        adminId?.let { fetchAdminDetails(it) }

        // Add event button
        view.findViewById<Button>(R.id.buttonAddEvent).setOnClickListener {
            showAddEventDialog()
        }
        view.findViewById<Button>(R.id.buttonAddSupply).setOnClickListener {
            csvPickerLauncher.launch("text/csv")
        }


        return view
    }

    private fun fetchAdminDetails(adminId: String) {
        val url = "http://10.143.152.54/univault/getAdminDetails.php?admin_id=$adminId"
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    collegeName = response.getString("college")
                    Toast.makeText(requireContext(), "College: $collegeName", Toast.LENGTH_SHORT).show()
                    fetchEvents()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error parsing admin details", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun fetchEvents() {
        val url = "http://10.143.152.54/univault/getEvents.php?college_name=$collegeName"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    Log.d("AdminCalenderFragment", "Response: $response")

                    eventList.clear()

                    val eventsArray = response.getJSONArray("data")
                    for (i in 0 until eventsArray.length()) {
                        val eventObj = eventsArray.getJSONObject(i)
                        val title = eventObj.getString("title")
                        val type = eventObj.getString("type")
                        val startDate = LocalDate.parse(eventObj.getString("start_date"))
                        val endDate = LocalDate.parse(eventObj.getString("end_date"))
                        val description = eventObj.getString("description")

                        val event = Event(title, type, startDate, endDate, description)
                        eventList.add(event)
                    }

                    eventAdapter.notifyDataSetChanged()

                } catch (e: Exception) {
                    Log.e("AdminCalenderFragment", "Error parsing events: ${e.message}")
                    Toast.makeText(requireContext(), "Error parsing events", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("AdminCalenderFragment", "Error: ${error.message}")
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        alertDialog.setOnShowListener {
            val addButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            addButton.setOnClickListener {
                val title = editTextTitle.text.toString().trim()
                val type = editTextType.text.toString().trim()
                val description = editTextDescription.text.toString().trim()
                val startDateStr = startDateTextView.text.toString()
                val endDateStr = endDateTextView.text.toString()

                if (title.isEmpty() || type.isEmpty() || description.isEmpty() || startDateStr == "Pick Start Date" || endDateStr == "Pick End Date") {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                try {
                    val startDate = LocalDate.parse(startDateStr)
                    val endDate = LocalDate.parse(endDateStr)

                    val newEvent = Event(title, type, startDate, endDate, description)
                    addEventToBackend(newEvent) {
                        alertDialog.dismiss()
                    }
                } catch (e: DateTimeParseException) {
                    Toast.makeText(requireContext(), "Invalid date format (yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        alertDialog.show()
    }

    private fun addEventToBackend(event: Event, callback: () -> Unit) {
        val url = "http://10.143.152.54/univault/addEvent.php"

        val params = HashMap<String, String>().apply {
            put("title", event.title)
            put("type", event.type)
            put("description", event.description)
            put("start_date", event.startDate.toString())
            put("end_date", event.endDate.toString())
            put("college_name", collegeName ?: "")
        }

        val jsonObject = JSONObject(params as Map<*, *>)

        val request = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            { response ->
                val status = response.optString("status")
                val message = response.optString("message")
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                if (status == "success") {
                    fetchEvents()
                }
                callback()
            },
            { error ->
                Toast.makeText(requireContext(), "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                callback()
            }
        )

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

        // Rule 1: Start date must be today or future
        if (targetTextView == startDateTextView) {
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        }

        // Rule 2: End date must be >= selected start date
        if (targetTextView == endDateTextView && startDateTextView.text.isNotEmpty()) {
            try {
                val selectedStart = LocalDate.parse(startDateTextView.text.toString())
                val startMillis = Calendar.getInstance().apply {
                    set(selectedStart.year, selectedStart.monthValue - 1, selectedStart.dayOfMonth)
                }.timeInMillis
                datePickerDialog.datePicker.minDate = startMillis
            } catch (e: Exception) {
                // fallback to today's date
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
