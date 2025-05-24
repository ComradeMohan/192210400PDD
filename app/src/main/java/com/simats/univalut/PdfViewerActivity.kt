package com.simats.univalut

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.simats.univalut.databinding.ActivityPdfViewerBinding

class PdfViewerActivity : AppCompatActivity() {

    lateinit var binding:ActivityPdfViewerBinding

    lateinit var url:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        url = intent.getStringExtra("pdf_url") ?: ""

        if (url.isNotEmpty()){
            binding.pdfView.initWithUrl(
                url = url ,
                lifecycleCoroutineScope = lifecycleScope,
                lifecycle = lifecycle
            )

        }

    }
}
