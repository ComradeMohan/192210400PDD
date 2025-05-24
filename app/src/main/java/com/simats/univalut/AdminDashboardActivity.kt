package com.simats.univalut

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.simats.univalut.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Correct binding usage
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ID = intent.getStringExtra("ID")
        // Set default fragment
        if (savedInstanceState == null) {
            replaceFragment(AdminHomeFragment.newInstance(ID ?: ""))
        }



        // Set bottom navigation listener
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(AdminHomeFragment.newInstance(ID ?: ""))
                R.id.nav_courses -> replaceFragment(AdminCoursesFragment.newInstance(ID ?: ""))

                R.id.nav_calender -> replaceFragment(AdminCalenderFragment.newInstance(ID ?: ""))
                R.id.nav_profile -> replaceFragment(AdminProfileFragment.newInstance(ID ?: ""))
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private var backPressedTime: Long = 0
    private val backPressInterval: Long = 2000 // 2 seconds

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment !is AdminHomeFragment) {
            // Navigate to Home Fragment if not already there
            val ID = intent.getStringExtra("ID") ?: ""
            replaceFragment(AdminHomeFragment.newInstance(ID))
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
