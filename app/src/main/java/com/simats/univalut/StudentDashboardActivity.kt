package com.simats.univalut

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import com.simats.univalut.databinding.ActivityStudentDashboardBinding

// Make sure all your fragment classes are imported correctly.
// For example:



class StudentDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudentDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val ID = intent.getStringExtra("ID")
        Log.d("StudentDashboardActivity", "Received ID: $ID")

        // Default fragment on initial load
        if (savedInstanceState == null) {
            // Use a simple fade-in for the very first fragment
            replaceFragment(HomeFragment1.newInstance(ID ?: ""), R.anim.fade_in, 0, 0, 0)
        }

        binding.bottomNavigationView.setOnItemSelectedListener {
            // Get the ID of the currently active fragment to determine animation direction
            val currentFragmentTag = supportFragmentManager.findFragmentById(R.id.fragment_container)?.tag
            val currentItemId = binding.bottomNavigationView.selectedItemId

            when (it.itemId) {
                R.id.nav_home -> {
                    if (currentItemId != R.id.nav_home) { // Only animate if changing tab
                        val enterAnim = if (currentItemId == R.id.nav_courses || currentItemId == R.id.nav_schedule || currentItemId == R.id.nav_profile) R.anim.slide_in_left else R.anim.fade_in // Slide left from right tabs
                        val exitAnim = if (currentItemId == R.id.nav_courses || currentItemId == R.id.nav_schedule || currentItemId == R.id.nav_profile) R.anim.slide_out_right else 0 // Slide right to exit
                        replaceFragment(HomeFragment1.newInstance(ID ?: ""), enterAnim, exitAnim, R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
                R.id.nav_courses -> {
                    if (currentItemId != R.id.nav_courses) {
                        val enterAnim = if (currentItemId == R.id.nav_home) R.anim.slide_in_right else R.anim.slide_in_left // Slide right from home, left from others
                        val exitAnim = if (currentItemId == R.id.nav_home) R.anim.slide_out_left else R.anim.slide_out_right // Slide left to exit from home, right from others
                        replaceFragment(CourseFragment.newInstance(ID ?: ""), enterAnim, exitAnim, R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }
                R.id.nav_schedule -> {
                    if (currentItemId != R.id.nav_schedule) {
                        val enterAnim = if (currentItemId == R.id.nav_profile) R.anim.slide_in_left else R.anim.slide_in_right // Slide left from profile, right from others
                        val exitAnim = if (currentItemId == R.id.nav_profile) R.anim.slide_out_right else R.anim.slide_out_left // Slide right from profile, left from others
                        replaceFragment(StudentCalenderFragment.newInstance(ID ?: ""), enterAnim, exitAnim, R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }
                R.id.nav_profile -> {
                    if (currentItemId != R.id.nav_profile) {
                        val enterAnim = if (currentItemId == R.id.nav_home || currentItemId == R.id.nav_courses || currentItemId == R.id.nav_schedule) R.anim.slide_in_right else R.anim.fade_in // Always slide in right
                        val exitAnim = if (currentItemId == R.id.nav_home || currentItemId == R.id.nav_courses || currentItemId == R.id.nav_schedule) R.anim.slide_out_left else 0 // Always slide out left
                        replaceFragment(ProfileFragment.newInstance(ID ?: ""), enterAnim, exitAnim, R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }
            }
            true
        }
    }

    // Function to replace fragments with optional animations and manage back stack
    private fun replaceFragment(
        fragment: Fragment,
        enterAnim: Int = 0,    // Animation for the new fragment entering
        exitAnim: Int = 0,     // Animation for the current fragment exiting
        popEnterAnim: Int = 0, // Animation for a fragment entering when popping back stack
        popExitAnim: Int = 0,  // Animation for a fragment exiting when popping back stack
        addToBackStack: Boolean = false // Set to true if you want proper back stack navigation for this fragment
    ) {
        val transaction = supportFragmentManager.beginTransaction()

        // Apply custom animations if provided
        if (enterAnim != 0 || exitAnim != 0 || popEnterAnim != 0 || popExitAnim != 0) {
            transaction.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
        }

        transaction.replace(R.id.fragment_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null) // You can give a unique name here if needed for specific popTo backstack
        }

        transaction.commit()
    }

    private var backPressedTime: Long = 0
    private val backPressInterval: Long = 2000

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // If not on the HomeFragment1, navigate to it with a "slide back" animation
        if (currentFragment != null && currentFragment !is HomeFragment1) {
            val ID = intent.getStringExtra("ID") ?: ""
            // When going back to Home, it should slide in from the left, and the current fragment should slide out to the right
            replaceFragment(HomeFragment1.newInstance(ID), R.anim.slide_in_left, R.anim.slide_out_right, 0, 0)
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
        } else {
            // If already on HomeFragment1, handle the double-tap to exit
            if (backPressedTime + backPressInterval > System.currentTimeMillis()) {
                super.onBackPressed() // Calls the default back press behavior (exit app)
                finishAffinity() // Closes all activities in the current task
            } else {
                backPressedTime = System.currentTimeMillis()
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }
}