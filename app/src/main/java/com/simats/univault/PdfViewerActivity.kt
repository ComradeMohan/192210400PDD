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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loadingAnimation.setAnimation(R.raw.loading)
        binding.loadingAnimation.playAnimation()
        binding.loadingAnimation.visibility = View.VISIBLE

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
                    if (binding.pdfView.height > 0) {
                        hideLoading()
                        hasRendered = true
                    } else if (elapsed < maxTimeout) {
                        elapsed += checkInterval
                        handler.postDelayed(this, checkInterval)
                    } else {
                        hideLoading()
                        Toast.makeText(this@PdfViewerActivity, "Failed to load PDF.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            handler.postDelayed(checkRenderRunnable, checkInterval)
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
