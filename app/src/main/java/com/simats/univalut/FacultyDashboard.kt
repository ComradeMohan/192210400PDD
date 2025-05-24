package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class FacultyDashboard : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.faculty_dashboard)
        // Link to your XML layout
        val uploadButton: LinearLayout = findViewById(R.id.uploadResourcesButton)
        uploadButton.setOnClickListener {
            val intent = Intent(this, CourseSelectionActivity::class.java)
            startActivity(intent)
        }

        var sendAnnounce: LinearLayout = findViewById(R.id.sendAnnouncementButton)
        sendAnnounce.setOnClickListener {
            val intent = Intent(this, SendAnnouncementActivity::class.java)
            startActivity(intent)
        }
        // Example: Handle "VIEW" buttons
        bottomNavigationView = findViewById(R.id.bottomNavigationView)


        // Set the listener to handle item selections in the    bottom navigation bar
        bottomNavigationView.setOnItemSelectedListener { item ->
            // 'item' represents the menu item that was selected
            when (item.itemId) {
                R.id.nav_home -> {
                    // Handle Home navigation
                     // Optional: finish current activity if you don't want to go back to it
                    true // Indicate that the item selection was handled
                }
                R.id.nav_students -> {
                    // Handle Students navigation
                    startActivity(Intent(this, FacultyStudentsActivity::class.java))
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
                    startActivity(Intent(this, FacultyProfile::class.java))
                    finish()
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
        bottomNavigationView.selectedItemId = R.id.nav_home

        // You can add similar logic for Upload, Announcements, etc.
    }
}