package com.simats.univault

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.simats.univault.databinding.ActivityPdfViewerBinding

class PdfViewerActivity : AppCompatActivity() {

    lateinit var binding: ActivityPdfViewerBinding
    lateinit var url: String
    var hasRendered = false
    private var animationStartTime = 0L
    private val minAnimationDuration = 2000L // Show animation for at least 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loadingAnimation.setAnimation(R.raw.loading)
        binding.loadingAnimation.playAnimation()
        binding.loadingAnimation.visibility = View.VISIBLE
        animationStartTime = System.currentTimeMillis()

        url = intent.getStringExtra("pdf_url") ?: ""

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        if (url.isNotEmpty()) {
            binding.pdfView.initWithUrl(
                url = url,
                lifecycleCoroutineScope = lifecycleScope,
                lifecycle = lifecycle
            )

            // Check every second for max 10s if it rendered something
            val checkInterval = 1000L
            val maxTimeout = 10000L
            var elapsed = 0L

            val handler = Handler(Looper.getMainLooper())
            val checkRenderRunnable = object : Runnable {
                override fun run() {
                    val currentTime = System.currentTimeMillis()
                    val animationElapsed = currentTime - animationStartTime

                    // Check if PDF view has reasonable dimensions (indicating content is loaded)
                    if (binding.pdfView.height > 100 && binding.pdfView.width > 100) {
                        // PDF is rendered, but check if minimum animation time has passed
                        if (animationElapsed >= minAnimationDuration) {
                            hideLoading()
                            hasRendered = true
                        } else {
                            // Wait for remaining animation time
                            val remainingTime = minAnimationDuration - animationElapsed
                            handler.postDelayed({
                                hideLoading()
                                hasRendered = true
                            }, remainingTime)
                        }
                    } else if (elapsed < maxTimeout) {
                        elapsed += checkInterval
                        handler.postDelayed(this, checkInterval)
                    } else {
                        hideLoading()
                        Toast.makeText(this@PdfViewerActivity, "Failed to load PDF.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            // Start checking after a small delay to let animation show
            handler.postDelayed(checkRenderRunnable, 2000L) // Wait 2 seconds before first check
        } else {
            hideLoading()
            Toast.makeText(this, "No PDF URL provided.", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideLoading() {
        binding.loadingAnimation.cancelAnimation()
        binding.loadingAnimation.visibility = View.GONE
    }
}