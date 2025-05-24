package com.simats.univalut

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import com.simats.univalut.databinding.ActivityStudentDashboardBinding

class StudentDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use correct binding
        binding = ActivityStudentDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val ID = intent.getStringExtra("ID")
        Log.d("StudentDasboardActivity", "Received ID: $ID")
        // Default fragment
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment1.newInstance(ID ?: ""))
        }



        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment1.newInstance(ID ?: ""))
                R.id.nav_courses -> replaceFragment(CourseFragment.newInstance(ID ?: ""))

                R.id.nav_schedule -> replaceFragment(StudentCalenderFragment.newInstance(ID ?: ""))
                R.id.nav_profile -> replaceFragment(ProfileFragment.newInstance(ID ?: ""))
            }
            true
        }
    }

    // Function to replace fragments and manage back stack (for nested navigation)
    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)

        // Add fragment to back stack if needed (e.g., nested fragment transitions)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }
    private var backPressedTime: Long = 0
    private val backPressInterval: Long = 2000
    // Override back button behavior to manage fragment back stack
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment !is HomeFragment1) {
            // Navigate to Home Fragment if not already there
            val ID = intent.getStringExtra("ID") ?: ""
            replaceFragment(HomeFragment1.newInstance(ID))
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
        } else {
            if (backPressedTime + backPressInterval > System.currentTimeMillis()) {
                super.onBackPressed()
                finishAffinity()
            } else {
                backPressedTime = System.currentTimeMillis()
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
