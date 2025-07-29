package com.simats.univalut

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splashLogo = findViewById<ImageView>(R.id.splashLogo)
        val appName = findViewById<TextView>(R.id.appName)
        val caption = findViewById<TextView>(R.id.caption)

        // Option 1: Dramatic entrance with floating effect
        startDramaticAnimation(splashLogo, appName, caption)

        // Option 2: Simple and elegant (uncomment to use instead)
        // startElegantAnimation(splashLogo, appName, caption)

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            startTransitionToMain()
        }, 3000) // Increased to 3 seconds for better animation viewing
    }

    private fun startDramaticAnimation(logo: ImageView, appName: TextView, caption: TextView) {
        // Logo dramatic entrance + floating
        val logoEntrance = AnimationUtils.loadAnimation(this, R.anim.logo_entrance)
        val logoFloat = AnimationUtils.loadAnimation(this, R.anim.logo_floating)

        logo.startAnimation(logoEntrance)

        // Start floating animation after entrance
        Handler(Looper.getMainLooper()).postDelayed({
            logo.startAnimation(logoFloat)
        }, 1100)

        // App name typewriter effect
        val appNameAnim = AnimationUtils.loadAnimation(this, R.anim.app_name_typewriter)
        appName.startAnimation(appNameAnim)

        // Caption slide up
        val captionAnim = AnimationUtils.loadAnimation(this, R.anim.caption_slide_up)
        caption.startAnimation(captionAnim)
    }

    private fun startElegantAnimation(logo: ImageView, appName: TextView, caption: TextView) {
        // Simple bounce in for logo
        val logoBounce = AnimationUtils.loadAnimation(this, R.anim.logo_bounce_in)
        logo.startAnimation(logoBounce)

        // Text reveal effect
        val textReveal = AnimationUtils.loadAnimation(this, R.anim.text_reveal)
        appName.startAnimation(textReveal)

        // Caption with delay
        val captionFade = AnimationUtils.loadAnimation(this, R.anim.caption_slide_up)
        caption.startAnimation(captionFade)
    }

    private fun startTransitionToMain() {
        // Smooth exit animation
//        val splashExit = AnimationUtils.loadAnimation(this, R.anim.splash_exit)
//        findViewById<LinearLayout>(R.id.rootLayout).startAnimation(splashExit)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 300)
    }
}


//package com.simats.univalut
//
//import android.content.Intent
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.view.animation.AnimationUtils
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//
//class SplashActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_splash)
//
//        val splashLogo = findViewById<ImageView>(R.id.splashLogo)
//        val appName = findViewById<TextView>(R.id.appName)
//        val caption = findViewById<TextView>(R.id.caption)
//
//        // Load animations
//        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.float_logo)
//        val textAppearAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
//
//        // Start animations
//        splashLogo.startAnimation(logoAnim)
//        appName.startAnimation(textAppearAnim)
//        caption.startAnimation(textAppearAnim)
//
//        // Navigate after delay
//        Handler(Looper.getMainLooper()).postDelayed({
//            startActivity(Intent(this, MainActivity::class.java))
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
//            finish()
//        }, 2500)
//    }
//}
//
