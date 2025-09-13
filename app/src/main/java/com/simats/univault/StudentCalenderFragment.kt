package com.simats.univault

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek

class StudentCalenderFragment : Fragment() {

    private var studentID: String? = null
    private var collegeName: String? = null
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private lateinit var monthYearText: TextView
    private lateinit var calendarGrid: GridLayout
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton

    private val fullEventList = mutableListOf<Event>()
    private val filteredEventList = mutableListOf<Event>()
    private var currentMonth: YearMonth = YearMonth.now()
    private var selectedDate: LocalDate = LocalDate.now()
    private val today: LocalDate = LocalDate.now()

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
        monthYearText = view.findViewById(R.id.monthYearText)
        calendarGrid = view.findViewById(R.id.calendarGrid)
        prevMonthButton = view.findViewById(R.id.prevMonthButton)
        nextMonthButton = view.findViewById(R.id.nextMonthButton)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        eventAdapter = EventAdapter(filteredEventList, requireContext())
        recyclerView.adapter = eventAdapter

        swipeRefresh.setOnRefreshListener {
            collegeName?.let { fetchEvents(it) } ?: studentID?.let { fetchStudentName(it) }
        }

        prevMonthButton.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            calendarGrid.post { updateCalendar() }
        }

        nextMonthButton.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            calendarGrid.post { updateCalendar() }
        }

        // Set calendar grid to 7 columns (for days of week)
        calendarGrid.columnCount = 7

        // Initialize calendar after view is laid out
        calendarGrid.post {
            updateCalendar()
            // Filter events for today's date initially
            filterEventsByDate(selectedDate)
        }

        // Load events
        collegeName?.let { fetchEvents(it) } ?: studentID?.let { fetchStudentName(it) }

        return view
    }

    private fun updateCalendar() {
        // Wait for grid to be measured
        if (calendarGrid.width == 0) {
            calendarGrid.post { updateCalendar() }
            return
        }

        // Set month/year text
        monthYearText.text = "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.year}"

        calendarGrid.removeAllViews()

        val firstDayOfMonth = LocalDate.of(currentMonth.year, currentMonth.month, 1)
        val daysInMonth = currentMonth.lengthOfMonth()

        // Calculate which day of week the month starts on (0=Sunday, 1=Monday, etc.)
        val firstDayOfWeek = when (firstDayOfMonth.dayOfWeek) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
        }

        // Get colors
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val textColor = ContextCompat.getColor(requireContext(), typedValue.resourceId)

        // Calculate margins and cell dimensions
        val marginDp = 1f
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginDp,
            resources.displayMetrics
        ).toInt()

        val totalWidth = calendarGrid.width - calendarGrid.paddingLeft - calendarGrid.paddingRight
        val cellWidth = (totalWidth / 7f).toInt()
        val cellHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            48f,
            resources.displayMetrics
        ).toInt()

        // Add empty cells for days before the first day of month
        for (i in 0 until firstDayOfWeek) {
            val emptyView = View(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellWidth
                    height = cellHeight
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }
            }
            calendarGrid.addView(emptyView)
        }

        // Add day buttons
        for (day in 1..daysInMonth) {
            val date = LocalDate.of(currentMonth.year, currentMonth.month, day)
            val isToday = date == today
            val isSelected = date == selectedDate

            val dayButton = Button(requireContext()).apply {
                text = day.toString()
                textSize = 14f
                setPadding(0, 0, 0, 0)

                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellWidth
                    height = cellHeight
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }

                if (isToday || isSelected) {
                    // Create circular background for today/selected date
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(requireContext(), R.color.blue_focus))
                    }
                    background = drawable
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                } else {
                    setTextColor(textColor)
                    background = null
                }

                // Check if date has events
                val hasEvents = fullEventList.any { event ->
                    !date.isBefore(event.startDate) && !date.isAfter(event.endDate)
                }

                if (hasEvents && !isToday && !isSelected) {
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(requireContext(), R.color.blue_focus))
                        alpha = 50
                    }
                    background = drawable
                }
                setOnClickListener {
                    selectedDate = date
                    calendarGrid.post { updateCalendar() }
                    filterEventsByDate(date)
                }
            }
            calendarGrid.addView(dayButton)
        }
    }

    private fun filterEventsByDate(date: LocalDate) {
        val filtered = fullEventList.filter { event ->
            // Check if the selected date falls within the event's date range
            !date.isBefore(event.startDate) && !date.isAfter(event.endDate)
        }

        filteredEventList.clear()
        filteredEventList.addAll(filtered)
        eventAdapter.notifyDataSetChanged()

        // Update empty state
        val emptyText = view?.findViewById<TextView>(R.id.textEmpty)
        emptyText?.visibility = if (filteredEventList.isEmpty()) View.VISIBLE else View.GONE

        // Show feedback message
        val eventCount = filteredEventList.size
        val message = if (eventCount > 0) {
            "$eventCount event${if (eventCount == 1) "" else "s"} found"
        } else {
            "No events on ${date.dayOfMonth}/${date.monthValue}/${date.year}"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchStudentName(studentID: String) {
        swipeRefresh.isRefreshing = true
        val url = "http://192.168.56.1/univault/fetch_student_name.php?studentID=$studentID"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val success = response.getBoolean("success")
                    if (success) {
                        val name = response.getString("name")
                        collegeName = response.getString("college")
                        Toast.makeText(requireContext(), "Welcome $name", Toast.LENGTH_SHORT).show()
                        collegeName?.let { fetchEvents(it) }
                    } else {
                        Toast.makeText(requireContext(), "Student not found", Toast.LENGTH_SHORT).show()
                        swipeRefresh.isRefreshing = false
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error parsing student data", Toast.LENGTH_SHORT).show()
                    swipeRefresh.isRefreshing = false
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message ?: "Network error"}", Toast.LENGTH_SHORT).show()
                swipeRefresh.isRefreshing = false
            }
        )

        Volley.newRequestQueue(requireContext()).add(jsonObjectRequest)
    }

    private fun fetchEvents(collegeName: String) {
        swipeRefresh.isRefreshing = true
        val url = "http://192.168.56.1/univault/getEvents.php?college_name=${collegeName.replace(" ", "%20")}"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    fullEventList.clear()

                    if (response.has("data")) {
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

                        Toast.makeText(requireContext(), "${fullEventList.size} events loaded", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "No events found", Toast.LENGTH_SHORT).show()
                    }

                    // Update calendar to show event indicators
                    calendarGrid.post { updateCalendar() }

                    // Filter events for currently selected date
                    filterEventsByDate(selectedDate)

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error parsing events: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    swipeRefresh.isRefreshing = false
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Network error: ${error.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                swipeRefresh.isRefreshing = false
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