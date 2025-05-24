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

        // Fetch events from backend (placeholder)


        // Add event button
        view.findViewById<Button>(R.id.buttonAddEvent).setOnClickListener {
            showAddEventDialog()
        }

        return view
    }

    private fun fetchAdminDetails(adminId: String) {
        val url = "https://api-9buk.onrender.com/getAdminDetails.php?admin_id=$adminId"
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
        val url = "https://api-9buk.onrender.com/getEvents.php?college_name=$collegeName"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    Log.d("AdminCalenderFragment", "Response: $response") // Log the entire response

                    eventList.clear() // Clear the previous event list

                    // Assuming the response contains a JSON array of events
                    val eventsArray = response.getJSONArray("data") // Adjust this based on your response structure
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

                    // Notify the adapter about the new data
                    eventAdapter.notifyDataSetChanged()

                } catch (e: Exception) {
                    Log.e("AdminCalenderFragment", "Error parsing events: ${e.message}")
                    Toast.makeText(requireContext(), "Error parsing events", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("AdminCalenderFragment", "Error: ${error.message}") // Log the error
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        // Add the request to the queue
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

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Event")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val title = editTextTitle.text.toString().trim()
                val type = editTextType.text.toString().trim()
                val description = editTextDescription.text.toString().trim()
                val startDateStr = startDateTextView.text.toString()
                val endDateStr = endDateTextView.text.toString()

                try {
                    val startDate = LocalDate.parse(startDateStr)
                    val endDate = LocalDate.parse(endDateStr)

                    val newEvent = Event(
                        title = title,
                        type = type,
                        startDate = startDate,
                        endDate = endDate,
                        description = description
                    )

                    eventList.add(newEvent)
                    eventAdapter.notifyItemInserted(eventList.size - 1)

                    addEventToBackend(newEvent)
                } catch (e: DateTimeParseException) {
                    Toast.makeText(requireContext(), "Invalid date format (yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun addEventToBackend(event: Event) {
        val url = "https://api-9buk.onrender.com/addEvent.php" // Replace with your backend URL

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
            },
            { error ->
                Toast.makeText(requireContext(), "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showDatePicker(targetTextView: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, y, m, d ->
            val date = LocalDate.of(y, m + 1, d)
            targetTextView.text = date.toString()
        }, year, month, day).show()
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
