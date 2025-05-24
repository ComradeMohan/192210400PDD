package com.simats.univalut

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {


    private val REQUEST_CODE_POST_NOTIFICATIONS = 1001



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestNotificationPermission()

        val sf = getSharedPreferences("user_sf", MODE_PRIVATE)
        val isLoggedIn = sf.getBoolean("isLoggedIn",false)
        val userType = sf.getString("userType","")
        val userId = sf.getString("userID","")

        if (isLoggedIn){
            if (userType.equals("admin")){
                val intent = Intent(this@MainActivity, AdminDashboardActivity::class.java)
                intent.putExtra("ID", userId)
                startActivity(intent)
            }else if (userType.equals("student")){
                val intent = Intent(this@MainActivity, StudentDashboardActivity::class.java)
                intent.putExtra("ID", userId)
                startActivity(intent)
            }else if (userType.equals("faculty")){
                val intent = Intent(this@MainActivity, FacultyDashboardActivity::class.java)
                intent.putExtra("ID", userId)
                startActivity(intent)
            }
        }

        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val signUpButton = findViewById<MaterialButton>(R.id.signUpButton)

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signUpButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied. Notifications won't work.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
