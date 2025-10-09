package com.simats.univault

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class CalendarActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter
    private val allEvents = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_calender) // Your main layout

        recyclerView = findViewById(R.id.rvEvents)
        adapter = EventAdapter(mutableListOf(), this)
        // Using mutable list for dynamic updates
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Example: Add some sample events

        // Set default events for today
        val today = LocalDate.now()
        updateEventsForDate(today)

        // Calendar date selection logic
        // If using MaterialCalendarView:
        // val calendarView = findViewById<MaterialCalendarView>(R.id.calendarView)
        // calendarView.setOnDateChangedListener { _, date, _ ->
        //     val selectedDate = LocalDate.of(date.year, date.month + 1, date.day)
        //     updateEventsForDate(selectedDate)
        // }

        // If using your own calendar, call updateEventsForDate(selectedDate) on date click
    }

    private fun updateEventsForDate(date: LocalDate) {
        val filtered = allEvents.filter {
            !date.isBefore(it.startDate) && !date.isAfter(it.endDate)
        }
        adapter.updateEvents(filtered)
    }
}
