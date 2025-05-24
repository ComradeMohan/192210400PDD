package com.simats.univalut

import android.content.Intent // Import Intent for navigation
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class FacultyProfile : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var notification: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to your Faculty Profile layout file
        setContentView(R.layout.faculty_profile) // Make sure this matches your XML filename

        notification = findViewById(R.id.academicRecordsButton)
        notification.setOnClickListener {
            startActivity(Intent(this, StudentNotificationsActivity::class.java))
            finish()
        }


        // Get a reference to the BottomNavigationView from the layout
        bottomNavigationView = findViewById(R.id.bottomNavigationView)


        // Set the listener to handle item selections in the    bottom navigation bar
        bottomNavigationView.setOnItemSelectedListener { item ->
            // 'item' represents the menu item that was selected
            when (item.itemId) {
                R.id.nav_home -> {
                    // Handle Home navigation
                    startActivity(Intent(this,FacultyDashboard::class.java))
                    finish() // Optional: finish current activity if you don't want to go back to it
                    true // Indicate that the item selection was handled
                }
                R.id.nav_students -> {
                    // Handle Students navigation
                    startActivity(Intent(this, StudentsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_materials -> {
                    // Handle Materials navigation
                    startActivity(Intent(this, CourseSelectionActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    // Handle Profile navigation (This is the current screen, maybe do nothing or refresh)
                    true
                }
                // Add cases for any other menu items in your faculty_bottom_nav_menu
                // R.id.some_other_item -> {
                //     // Handle other item
                //     true
                // }
                else -> false // Return false if the item ID is not handled
            }
        }

        // Optional: Set the initially selected item in the bottom navigation when the Activity starts.
        // This makes the icon for the current screen appear selected.
        // Replace R.id.nav_profile with the ID corresponding to the current screen's menu item.
        bottomNavigationView.selectedItemId = R.id.nav_profile

        // --- Your other Activity initialization code goes here ---
        // Get references to other UI elements in your profile layout (TextViews, ImageViews, LinearLayouts)
        // Populate the profile data (name, status, department, contact info) from your data source
        // Set up click listeners for the clickable LinearLayouts (Notifications, Help, Settings, Logout)
    }

    // You might override onResume() or other lifecycle methods to handle
    // re-selecting the correct item when navigating back to this Activity.
}