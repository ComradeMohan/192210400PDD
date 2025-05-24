package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splashLogo = findViewById<ImageView>(R.id.splashLogo)
        val appName = findViewById<TextView>(R.id.appName)
        val caption = findViewById<TextView>(R.id.caption)

        // Load animations
        val floatLogoAnim = AnimationUtils.loadAnimation(this, R.anim.float_logo)
        val slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Start animations
        splashLogo.startAnimation(floatLogoAnim)
        appName.startAnimation(slideUpAnim)
        caption.startAnimation(fadeInAnim)

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2500)
    }
}
