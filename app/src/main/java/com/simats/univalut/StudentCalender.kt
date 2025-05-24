package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView // Import if you need to interact with it


class StudentCalender : AppCompatActivity() {

    // You might declare variables here to hold references to your views
    // For example, if you plan to interact with the ScrollView or the LinearLayouts:
    // private lateinit var scrollView: ScrollView
    // private lateinit var eventsTitle: TextView
    // private lateinit var eventsContainer: LinearLayout // Or RecyclerView

    // If you add a CalendarView widget later, you'd declare it here:
    // private lateinit var calendarView: YourCalendarViewWidget // Replace YourCalendarViewWidget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to your layout file
        setContentView(R.layout.student_calender) // Make sure this matches your XML filename

        // --- Code to add later for Calendar View ---
        // Once you add a CalendarView widget to your XML,
        // get a reference to it here:
        // calendarView = findViewById(R.id.your_calendar_view_id)

        // Then, add the logic to:
        // 1. Get today's date
        // 2. Set the calendar view to display the current month/year
        // 3. Highlight today's date on the calendar
        // 4. Set up listeners for date selection if needed (to update events list)
        // This part heavily depends on the CalendarView library or custom implementation you use.
        // Example (conceptual):
        // val today = Calendar.getInstance()
        // calendarView.setDate(today, true, true) // Example using a hypothetical method

        // --- Code to add later for Events List ---
        // Get references to your events related views:
        // eventsTitle = findViewById(R.id.eventsTitle) // Make sure you add IDs in XML if needed
        // eventsContainer = findViewById(R.id.eventsContainer) // Make sure you add IDs in XML if needed

        // Add logic to:
        // 1. Fetch event data (e.g., from a database or API)
        // 2. Filter events for the currently selected date
        // 3. Dynamically add or update the views in the events container (LinearLayout or RecyclerView)
        // This would likely involve creating a list of event objects and binding them to UI elements.

        // Example of getting a reference to the BottomNavigationView (if you need to interact with it)
        // val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        // Set up listeners or initial state for the bottom navigation here if needed.

        // Example: Set a title for the Activity (optional)
        // supportActionBar?.title = "Calendar"

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigationView.selectedItemId = R.id.nav_schedule

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, StudentActivity::class.java))
                    // You're already in StudentActivity, maybe refresh or do nothing
                    true
                }

                R.id.nav_courses -> {
                    startActivity(Intent(this, StudentCourses::class.java))
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, StudentProfileActivity::class.java))
                    true
                }

                R.id.nav_schedule -> {
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, StudentProfileActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }

    // You might add helper functions here for fetching/displaying events
    // private fun updateEventsList(date: CalendarDay) {
    //     // Logic to get events for 'date' and update UI
    // }
}