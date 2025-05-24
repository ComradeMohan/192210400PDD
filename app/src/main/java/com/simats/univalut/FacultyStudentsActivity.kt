package com.simats.univalut
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class FacultyStudentsActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var studentsListLayout: LinearLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.favulty_students)

        searchEditText = findViewById(R.id.searchEditText)
        studentsListLayout = findViewById(R.id.studentsList)
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
        bottomNavigationView.selectedItemId = R.id.nav_students
        // Example: Search filter logic (this will only affect real dynamic content)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // You can implement filter logic here when using real data
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })



        // TODO: Replace this part with dynamic data loading from Firebase, API, or local database
        populateDummyStudents()
    }

    private fun populateDummyStudents() {
        // Static entries already present in XML, dynamic loading example goes here if needed
        // You can inflate layouts or add views programmatically here
    }
}