package com.simats.univalut

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.simats.univalut.databinding.ActivityFacultyDashboardBinding

class FacultyDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFacultyDashboardBinding
    private var facultyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use ViewBinding to inflate the layout
        binding = ActivityFacultyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        facultyId = intent.getStringExtra("ID")

        // Set default fragment (HomeFragment)
        if (savedInstanceState == null) {
            val fragment = FacultyHomeFragment()
            val bundle = Bundle()
            bundle.putString("ID", facultyId)  // Passing faculty name to fragment
            fragment.arguments = bundle
            replaceFragment(fragment)
        }

        // Set BottomNavigationView item selected listener
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (currentFragment !is FacultyHomeFragment) {
                        val homeFragment = FacultyHomeFragment()
                        val bundle = Bundle()
                        bundle.putString("ID", facultyId)
                        homeFragment.arguments = bundle
                        replaceFragment(homeFragment)
                    }
                }
                R.id.nav_profile ->{
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (currentFragment !is FacultyProfileFragment) {
                        val profileFragment = FacultyProfileFragment()
                        val bundle = Bundle()
                        bundle.putString("ID", facultyId)
                        profileFragment.arguments = bundle
                        replaceFragment(profileFragment)
                    }
                }
                R.id.nav_materials ->{
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (currentFragment !is FacultyMaterialsFragment) {
                        val materialFragment = FacultyMaterialsFragment()
                        val bundle = Bundle()
                        bundle.putString("ID", facultyId)
                        materialFragment.arguments = bundle
                        replaceFragment(materialFragment)
                    }
                }
                R.id.nav_students ->{
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (currentFragment !is FacultyStudentsFragment) {
                        val studentsFragment = FacultyStudentsFragment()
                        val bundle = Bundle()
                        bundle.putString("ID", facultyId)
                        studentsFragment.arguments = bundle
                        replaceFragment(studentsFragment)
                    }
                }
                else -> false
            }
            true
        }
    }

    // Function to replace the current fragment with the selected one
    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }


    // Override back button to handle fragment back stack
    private var backPressedTime: Long = 0
    private val backPressInterval: Long = 2000 // 2 seconds

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment !is FacultyHomeFragment) {
            // Navigate to Home Fragment if not already there
            val ID = intent.getStringExtra("ID") ?: ""
            replaceFragment(FacultyHomeFragment.newInstance(ID))
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
