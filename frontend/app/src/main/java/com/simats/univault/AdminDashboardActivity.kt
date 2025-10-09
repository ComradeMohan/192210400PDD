package com.simats.univault

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.simats.univault.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private var backPressedTime: Long = 0
    private val backPressInterval: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ID = intent.getStringExtra("ID")
        val rootView = findViewById<View>(android.R.id.content)

        // Add keyboard visibility listener to hide bottom navigation when keyboard is open
        rootView.setKeyboardVisibilityListener { isKeyboardVisible ->
            binding.bottomNavigationView.visibility = if (isKeyboardVisible) View.GONE else View.VISIBLE
        }

        // Default fragment
        if (savedInstanceState == null) {
            replaceFragment(AdminHomeFragment.newInstance(ID ?: ""), R.anim.fade_in, 0, 0, 0)
        }

        // Bottom navigation selection logic
        binding.bottomNavigationView.setOnItemSelectedListener {
            val currentItemId = binding.bottomNavigationView.selectedItemId
            when (it.itemId) {
                R.id.nav_home -> {
                    if (currentItemId != R.id.nav_home) {
                        val enterAnim = if (currentItemId == R.id.nav_courses || currentItemId == R.id.nav_calender || currentItemId == R.id.nav_profile) R.anim.slide_in_left else R.anim.fade_in
                        val exitAnim = if (currentItemId == R.id.nav_courses || currentItemId == R.id.nav_calender || currentItemId == R.id.nav_profile) R.anim.slide_out_right else 0
                        replaceFragment(AdminHomeFragment.newInstance(ID ?: ""), enterAnim, exitAnim, R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
                R.id.nav_courses -> {
                    if (currentItemId != R.id.nav_courses) {
                        val enterAnim = if (currentItemId == R.id.nav_home) R.anim.slide_in_right else R.anim.slide_in_left
                        val exitAnim = if (currentItemId == R.id.nav_home) R.anim.slide_out_left else R.anim.slide_out_right
                        replaceFragment(AdminCoursesFragment.newInstance(ID ?: ""), enterAnim, exitAnim, R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }
                R.id.nav_calender -> {
                    if (currentItemId != R.id.nav_calender) {
                        val enterAnim = if (currentItemId == R.id.nav_profile) R.anim.slide_in_left else R.anim.slide_in_right
                        val exitAnim = if (currentItemId == R.id.nav_profile) R.anim.slide_out_right else R.anim.slide_out_left
                        replaceFragment(AdminCalenderFragment.newInstance(ID ?: ""), enterAnim, exitAnim, R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }
                R.id.nav_profile -> {
                    if (currentItemId != R.id.nav_profile) {
                        val enterAnim = R.anim.slide_in_right
                        val exitAnim = R.anim.slide_out_left
                        replaceFragment(AdminProfileFragment.newInstance(ID ?: ""), enterAnim, exitAnim, R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }
            }
            true
        }
    }

    // Extension function to set keyboard visibility listener
    private fun View.setKeyboardVisibilityListener(onKeyboardVisibilityChanged: (Boolean) -> Unit) {
        val rootView = this
        var wasKeyboardVisible = false

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height
            val isKeyboardVisible = heightDiff > rootView.rootView.height * 0.15

            if (isKeyboardVisible != wasKeyboardVisible) {
                wasKeyboardVisible = isKeyboardVisible
                onKeyboardVisibilityChanged(isKeyboardVisible)
            }
        }
    }

    // Reusable fragment replacement method with optional animation and back stack control
    private fun replaceFragment(
        fragment: Fragment,
        enterAnim: Int = 0,
        exitAnim: Int = 0,
        popEnterAnim: Int = 0,
        popExitAnim: Int = 0,
        addToBackStack: Boolean = false
    ) {
        val transaction = supportFragmentManager.beginTransaction()
        if (enterAnim != 0 || exitAnim != 0 || popEnterAnim != 0 || popExitAnim != 0) {
            transaction.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
        }
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBackStack) transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val ID = intent.getStringExtra("ID") ?: ""

        if (currentFragment !is AdminHomeFragment) {
            replaceFragment(AdminHomeFragment.newInstance(ID), R.anim.slide_in_left, R.anim.slide_out_right, 0, 0)
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