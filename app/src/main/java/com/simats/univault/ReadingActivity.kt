package com.simats.univault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import android.graphics.drawable.GradientDrawable
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.ScaleGestureDetector
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.text.InputType
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Color
import android.view.ActionMode
import android.webkit.WebChromeClient
import org.json.JSONArray
import android.widget.PopupWindow
import android.graphics.Typeface
import android.widget.FrameLayout
import android.widget.ImageView
import android.graphics.drawable.Drawable
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.ColorFilter
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.InputMethodManager

class ReadingActivity : AppCompatActivity() {

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    // Scroll tracking variables
    private var hasScrolledToMarkAsRead = false
    private var markAsReadSectionY = 0
    private var autoCheckScheduled = false

    // UI Elements
    private lateinit var courseName: TextView
    private lateinit var topicName: TextView
    private lateinit var topicContent: WebView
    private lateinit var topicProgress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var markAsReadCheckbox: CheckBox
    private lateinit var readStatus: TextView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var completeTopicButton: Button
    private lateinit var loadingSpinnerLayout: LinearLayout
    private lateinit var studyTimeText: TextView
    private lateinit var notesButton: ImageButton

    // Enhanced UI Controls
    private lateinit var readingControlsLayout: LinearLayout
    private lateinit var zoomInButton: ImageButton
    private lateinit var zoomOutButton: ImageButton
    private lateinit var zoomResetButton: ImageButton
    private lateinit var fontSizeButton: ImageButton
    private lateinit var fontFamilyButton: ImageButton
    private lateinit var readingModeButton: ImageButton
    private lateinit var ttsButton: ImageButton
    private lateinit var bookmarkButton: ImageButton
    private lateinit var searchButton: ImageButton
    private lateinit var settingsButton: ImageButton

    // Data
    private var courseCode: String = ""
    private var courseTitle: String = ""
    private var collegeName: String = ""
    private var selectedMode: String = ""
    private var currentTopicIndex: Int = 0
    private var topics: List<Topic> = emptyList()
    private var currentTopic: Topic? = null

    // Time tracking variables
    private var sessionStartTime: Long = 0
    private var totalStudyTimeMillis: Long = 0

    // Enhanced reading features
    private var currentZoomLevel: Float = 1.0f
    private var currentFontSize: Int = 16
    private var currentFontFamily: String = "Arial"
    private var currentReadingMode: String = "light"
    private var isTtsEnabled: Boolean = false
    private var tts: TextToSpeech? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var bookmarks: MutableSet<String> = mutableSetOf()
    private var searchQuery: String = ""
    private var isSearchMode: Boolean = false

    // Simplified Annotation Features
    private var currentHighlightColor: String = "#FFFF00" // Yellow
    private var currentUnderlineColor: String = "#FF0000" // Red
    private var annotations: MutableList<Annotation> = mutableListOf()

    // Text Selection Features
    private var selectionToolbar: PopupWindow? = null
    private var currentSelection: Triple<Int, Int, String>? = null // start, end, text

    // Notes Features
    private var topicNotes: MutableMap<String, String> = mutableMapOf()
    private var notesPopup: PopupWindow? = null

    // Data class for Topic
    data class Topic(
        val id: String,
        val name: String,
        val content: String,
        val isRead: Boolean = false
    )

    // Simplified Annotation model
    data class Annotation(
        val id: String,
        val contentId: String,
        val type: String, // highlight, note, underline
        val start: Int,
        val end: Int,
        val color: String? = null,
        val noteText: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading)

        // Initialize UI elements
        initializeViews()

        // Initialize enhanced features
        initializeEnhancedFeatures()

        // Get data from intent
        getIntentData()

        // Set up click listeners
        setupClickListeners()

        // Load topics for the course
        loadTopics()
    }

    private fun initializeViews() {
        courseName = findViewById(R.id.courseName)
        topicName = findViewById(R.id.topicName)
        topicContent = findViewById(R.id.topicWebView)
        topicContent.settings.loadsImagesAutomatically = true
        topicContent.settings.javaScriptEnabled = true
        topicContent.settings.defaultTextEncodingName = "utf-8"
        topicContent.settings.domStorageEnabled = true
        topicContent.settings.builtInZoomControls = false
        topicContent.settings.displayZoomControls = false
        topicContent.settings.useWideViewPort = true
        topicContent.settings.loadWithOverviewMode = true
        topicContent.settings.setSupportZoom(true)
        topicContent.settings.textZoom = 100
        topicContent.setInitialScale(0)
        topicContent.setBackgroundColor(Color.TRANSPARENT)

        // Simplified text selection configuration
        topicContent.isLongClickable = true
        topicContent.isFocusable = true
        topicContent.isFocusableInTouchMode = true

        // Essential WebView settings for text selection
        topicContent.settings.setSupportMultipleWindows(false)
        topicContent.settings.javaScriptCanOpenWindowsAutomatically = false
        topicContent.settings.allowFileAccess = true
        topicContent.settings.allowContentAccess = true

        // Enable text selection - critical settings
        topicContent.settings.setSupportZoom(true)
        topicContent.settings.setBuiltInZoomControls(false)
        topicContent.settings.setDisplayZoomControls(false)
        topicContent.settings.setUseWideViewPort(true)
        topicContent.settings.setLoadWithOverviewMode(true)

        // Enable hardware acceleration
        topicContent.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

        // Add JavaScript interface for annotations
        topicContent.addJavascriptInterface(AnnotationBridge(), "AndroidAnnotator")
        // Set WebViewClient and WebChromeClient for handling page load and selection
        topicContent.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("ReadingActivity", "Page finished loading, enabling text selection")

                // Initialize text selection
                injectAnnotationJS()
                renderSavedAnnotations()
                applyReadingPreferences()

                // Enable text selection
                enableTextSelection()

                // Test text selection
                testTextSelection()

                // Re-enable after delays to handle rendering latency
                topicContent.postDelayed({
                    enableTextSelection()
                    testTextSelection()
                }, 1000)

                topicContent.postDelayed({
                    enableTextSelection()
                    testTextSelection()
                }, 2000)

                topicContent.postDelayed({
                    enableTextSelection()
                    testTextSelection()
                }, 3000)
            }
        }

        // Set WebChromeClient for better text selection support
        topicContent.webChromeClient = object : WebChromeClient() {
            // WebChromeClient doesn't have onSelectionChanged method
            // Text selection is handled through JavaScript interface
        }

        // Set up text selection with custom floating toolbar
        setupTextSelection()

        // Add JavaScript interface for text selection
        topicContent.addJavascriptInterface(TextSelectionBridge(), "TextSelection")

        topicProgress = findViewById(R.id.topicProgress)
        progressText = findViewById(R.id.progressText)
        markAsReadCheckbox = findViewById(R.id.markAsReadCheckbox)
        readStatus = findViewById(R.id.readStatus)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        completeTopicButton = findViewById(R.id.completeTopicButton)
        loadingSpinnerLayout = findViewById(R.id.loadingSpinnerLayout)
        studyTimeText = findViewById(R.id.studyTimeText)
        notesButton = findViewById(R.id.notesButton)

        // Setup scroll listener for progress tracking
        setupScrollListener()
    }
    private fun initializeEnhancedFeatures() {
        initializeTTS()
        initializeScaleGestureDetector()
        setupReadingControls()
        loadReadingPreferences()
        loadBookmarks()
        loadAnnotations()
        loadTopicNotes()
    }
    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        runOnUiThread {
                            if (::ttsButton.isInitialized) {
                                ttsButton.setColorFilter(ContextCompat.getColor(this@ReadingActivity, android.R.color.holo_green_dark))
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        runOnUiThread {
                            if (::ttsButton.isInitialized) {
                                ttsButton.setColorFilter(null)
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        runOnUiThread {
                            if (::ttsButton.isInitialized) {
                                ttsButton.setColorFilter(ContextCompat.getColor(this@ReadingActivity, android.R.color.holo_red_dark))
                            }
                        }
                    }
                })
            } else {
                Log.e("ReadingActivity", "TTS initialization failed: status $status")
            }
        }
    }

    private fun initializeScaleGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                currentZoomLevel *= scaleFactor
                currentZoomLevel = currentZoomLevel.coerceIn(0.5f, 3.0f)
                val textZoom = (currentZoomLevel * 100).toInt()
                topicContent.settings.textZoom = textZoom
                updateZoomButtons()
                saveReadingPreferences()
                return true
            }
        })

        topicContent.setOnTouchListener { _, event ->
            scaleGestureDetector?.onTouchEvent(event) ?: false
        }
    }

    private fun loadReadingPreferences() {
        val prefs = getSharedPreferences("ReadingPreferences", MODE_PRIVATE)
        currentFontSize = prefs.getInt("font_size", 16)
        currentFontFamily = prefs.getString("font_family", "Arial") ?: "Arial"
        currentReadingMode = prefs.getString("reading_mode", "light") ?: "light"
        currentZoomLevel = prefs.getFloat("zoom_level", 1.0f)
        applyReadingPreferences()
    }

    private fun saveReadingPreferences() {
        val prefs = getSharedPreferences("ReadingPreferences", MODE_PRIVATE)
        prefs.edit().apply {
            putInt("font_size", currentFontSize)
            putString("font_family", currentFontFamily)
            putString("reading_mode", currentReadingMode)
            putFloat("zoom_level", currentZoomLevel)
        }.apply()
    }

    private fun applyReadingPreferences() {
        updateWebViewStyling()
        applyReadingMode(currentReadingMode)
        val textZoom = (currentZoomLevel * 100).toInt()
        topicContent.settings.textZoom = textZoom
        updateZoomButtons()
    }

    private fun setupReadingControls() {
        val root = findViewById<ViewGroup>(android.R.id.content)

        readingControlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F0FFFFFF"))
                cornerRadius = 12.dpToPx().toFloat()
                setStroke(1.dpToPx(), Color.parseColor("#E0E0E0"))
            }
            elevation = 8f
        }

        zoomInButton = createControlButton(R.drawable.ic_menu_zoom_in, "Zoom In") { zoomIn() }
        zoomOutButton = createControlButton(R.drawable.ic_menu_zoom_out, "Zoom Out") { zoomOut() }
        zoomResetButton = createControlButton(R.drawable.ic_menu_revert, "Reset Zoom") { resetZoom() }
        fontSizeButton = createControlButton(R.drawable.ic_menu_edit, "Font Size") { showFontSizeDialog() }
        fontFamilyButton = createControlButton(R.drawable.ic_format_size, "Font Family") { showFontFamilyDialog() }
        readingModeButton = createControlButton(R.drawable.ic_menu_view, "Reading Mode") { showReadingModeDialog() }
        ttsButton = createControlButton(android.R.drawable.ic_media_play, "Text-to-Speech") { toggleTTS() }
        bookmarkButton = createControlButton(android.R.drawable.ic_menu_slideshow, "Bookmark") { toggleBookmark() }
        searchButton = createControlButton(android.R.drawable.ic_menu_search, "Search") { showSearchDialog() }
        settingsButton = createControlButton(android.R.drawable.ic_menu_preferences, "Settings") { showSettingsDialog() }

        readingControlsLayout.addView(zoomInButton)
        readingControlsLayout.addView(zoomOutButton)
        readingControlsLayout.addView(zoomResetButton)
        readingControlsLayout.addView(fontSizeButton)
        readingControlsLayout.addView(fontFamilyButton)
        readingControlsLayout.addView(readingModeButton)
        readingControlsLayout.addView(ttsButton)
        readingControlsLayout.addView(bookmarkButton)
        readingControlsLayout.addView(searchButton)
        readingControlsLayout.addView(settingsButton)

        root.addView(readingControlsLayout, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        readingControlsLayout.y = 0f
        readingControlsLayout.visibility = View.VISIBLE
    }

    private fun createControlButton(iconRes: Int, contentDesc: String, onClick: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            setImageResource(iconRes)
            this.contentDescription = contentDesc
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFFFFF"))
                cornerRadius = 8.dpToPx().toFloat()
                setStroke(1.dpToPx(), Color.parseColor("#E0E0E0"))
            }
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx()).apply {
                marginStart = 4.dpToPx()
                marginEnd = 4.dpToPx()
            }
        }
    }

    private fun loadBookmarks() {
        val prefs = getSharedPreferences("Bookmarks", MODE_PRIVATE)
        val bookmarkSet = prefs.getStringSet("bookmarks", emptySet()) ?: emptySet()
        bookmarks.addAll(bookmarkSet)
        updateBookmarkButton()
    }

    private fun saveBookmarks() {
        val prefs = getSharedPreferences("Bookmarks", MODE_PRIVATE)
        prefs.edit().putStringSet("bookmarks", bookmarks).apply()
    }

    private fun loadAnnotations() {
        val prefs = getSharedPreferences("AnnotationsPrefs", MODE_PRIVATE)
        val json = prefs.getString("ann_${currentContentId()}", "[]")
        try {
            val arr = JSONArray(json)
            annotations.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                annotations.add(
                    Annotation(
                        id = obj.getString("id"),
                        contentId = obj.getString("contentId"),
                        type = obj.getString("type"),
                        start = obj.getInt("start"),
                        end = obj.getInt("end"),
                        color = obj.optString("color", null),
                        noteText = obj.optString("noteText", null)
                    )
                )
            }
        } catch (e: JSONException) {
            Log.e("ReadingActivity", "Failed to load annotations: ${e.message}")
            Toast.makeText(this, "Error loading annotations", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAnnotations() {
        val prefs = getSharedPreferences("AnnotationsPrefs", MODE_PRIVATE)
        val arr = JSONArray()
        annotations.forEach { ann ->
            val obj = JSONObject().apply {
                put("id", ann.id)
                put("contentId", ann.contentId)
                put("type", ann.type)
                put("start", ann.start)
                put("end", ann.end)
                if (ann.color != null) put("color", ann.color)
                if (ann.noteText != null) put("noteText", ann.noteText)
            }
            arr.put(obj)
        }
        prefs.edit().putString("ann_${currentContentId()}", arr.toString()).apply()
    }

    private fun currentContentId(): String {
        val topicId = currentTopic?.id ?: "unknown"
        return "${courseCode}|${selectedMode}|${topicId}"
    }

    private fun generateAnnotationId(): String = "ann_${System.currentTimeMillis()}_${(0..9999).random()}"

    private fun zoomIn() {
        currentZoomLevel = (currentZoomLevel * 1.2f).coerceAtMost(3.0f)
        val textZoom = (currentZoomLevel * 100).toInt()
        topicContent.settings.textZoom = textZoom
        updateZoomButtons()
        saveReadingPreferences()
    }

    private fun zoomOut() {
        currentZoomLevel = (currentZoomLevel / 1.2f).coerceAtLeast(0.5f)
        val textZoom = (currentZoomLevel * 100).toInt()
        topicContent.settings.textZoom = textZoom
        updateZoomButtons()
        saveReadingPreferences()
    }

    private fun resetZoom() {
        currentZoomLevel = 1.0f
        topicContent.settings.textZoom = 100
        updateZoomButtons()
        saveReadingPreferences()
    }

    private fun updateZoomButtons() {
        if (::zoomInButton.isInitialized && ::zoomOutButton.isInitialized && ::zoomResetButton.isInitialized) {
            zoomInButton.isEnabled = currentZoomLevel < 3.0f
            zoomOutButton.isEnabled = currentZoomLevel > 0.5f
            zoomResetButton.isEnabled = currentZoomLevel != 1.0f
        }
    }

    private fun showFontSizeDialog() {
        val sizes = arrayOf("12", "14", "16", "18", "20", "22", "24", "28", "32")
        val currentSizeIndex = sizes.indexOf(currentFontSize.toString())

        AlertDialog.Builder(this)
            .setTitle("Font Size")
            .setSingleChoiceItems(sizes, currentSizeIndex) { dialog, which ->
                currentFontSize = sizes[which].toInt()
                updateWebViewStyling()
                saveReadingPreferences()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFontFamilyDialog() {
        val families = arrayOf("Arial", "Times New Roman", "Georgia", "Verdana", "Helvetica", "Courier New", "Monospace")
        val currentFamilyIndex = families.indexOf(currentFontFamily)

        AlertDialog.Builder(this)
            .setTitle("Font Family")
            .setSingleChoiceItems(families, currentFamilyIndex) { dialog, which ->
                currentFontFamily = families[which]
                updateWebViewStyling()
                saveReadingPreferences()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReadingModeDialog() {
        val modes = arrayOf("Light", "Dark", "Sepia", "High Contrast")
        val currentModeIndex = when (currentReadingMode) {
            "light" -> 0
            "dark" -> 1
            "sepia" -> 2
            "high_contrast" -> 3
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("Reading Mode")
            .setSingleChoiceItems(modes, currentModeIndex) { dialog, which ->
                currentReadingMode = when (which) {
                    0 -> "light"
                    1 -> "dark"
                    2 -> "sepia"
                    3 -> "high_contrast"
                    else -> "light"
                }
                applyReadingMode(currentReadingMode)
                saveReadingPreferences()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyReadingMode(mode: String) {
        val js = when (mode) {
            "dark" -> """
                document.body.style.backgroundColor = '#1a1a1a';
                document.body.style.color = '#ffffff';
                document.querySelectorAll('h1, h2, h3, h4, h5, h6').forEach(h => h.style.color = '#4CAF50');
                document.querySelectorAll('pre, code').forEach(el => {
                    el.style.backgroundColor = '#2d2d2d';
                    el.style.color = '#f8f8f2';
                });
            """
            "sepia" -> """
                document.body.style.backgroundColor = '#f4f1ea';
                document.body.style.color = '#5c4b37';
                document.querySelectorAll('h1, h2, h3, h4, h5, h6').forEach(h => h.style.color = '#8b4513');
            """
            "high_contrast" -> """
                document.body.style.backgroundColor = '#000000';
                document.body.style.color = '#ffffff';
                document.querySelectorAll('h1, h2, h3, h4, h5, h6').forEach(h => h.style.color = '#ffff00');
                document.querySelectorAll('a').forEach(a => a.style.color = '#00ffff');
            """
            else -> """
                document.body.style.backgroundColor = '#ffffff';
                document.body.style.color = '#333333';
                document.querySelectorAll('h1, h2, h3, h4, h5, h6').forEach(h => h.style.color = '#4CAF50');
            """
        }
        topicContent.evaluateJavascript(js, null)
    }

    private fun updateWebViewStyling() {
        val js = """
            document.body.style.fontSize = '${currentFontSize}px';
            document.body.style.fontFamily = '$currentFontFamily, sans-serif';
            document.body.style.userSelect = 'text';
            document.body.style.webkitUserSelect = 'text';
            document.body.style.mozUserSelect = 'text';
            document.body.style.msUserSelect = 'text';
            document.body.style.cursor = 'text';
            
            // Ensure all text elements are selectable
            var allElements = document.querySelectorAll('*');
            for (var i = 0; i < allElements.length; i++) {
                allElements[i].style.userSelect = 'text';
                allElements[i].style.webkitUserSelect = 'text';
                allElements[i].style.mozUserSelect = 'text';
                allElements[i].style.msUserSelect = 'text';
            }
        """
        topicContent.evaluateJavascript(js, null)
    }

    private fun enableTextSelection() {
        val js = """
            console.log('Enabling simple text selection...');
            
            // Simple text selection enable
            document.body.style.userSelect = 'text';
            document.body.style.webkitUserSelect = 'text';
            document.body.style.mozUserSelect = 'text';
            document.body.style.msUserSelect = 'text';
            document.body.style.webkitTouchCallout = 'default';
            document.body.style.webkitTapHighlightColor = 'rgba(76, 175, 80, 0.3)';
            document.body.style.cursor = 'text';
            
            // Enable on all elements
            var allElements = document.querySelectorAll('*');
            for (var i = 0; i < allElements.length; i++) {
                var element = allElements[i];
                element.style.userSelect = 'text';
                element.style.webkitUserSelect = 'text';
                element.style.mozUserSelect = 'text';
                element.style.msUserSelect = 'text';
                element.style.webkitTouchCallout = 'default';
                element.style.webkitTapHighlightColor = 'rgba(76, 175, 80, 0.3)';
                
                // Make text elements selectable
                if (['P', 'DIV', 'SPAN', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI', 'TD', 'TH'].includes(element.tagName)) {
                    element.style.cursor = 'text';
                    element.style.minHeight = '24px';
                }
            }
            
            // Re-initialize if available
            if (window.AnnotatorJS && window.AnnotatorJS.enableSelection) {
                window.AnnotatorJS.enableSelection();
            }
            
            console.log('Simple text selection enabled on', allElements.length, 'elements');
        """
        topicContent.evaluateJavascript(js, null)
    }

    private fun clearAutomaticSelections() {
        val js = """
            console.log('Clearing automatic selections...');
            var selection = window.getSelection();
            if (selection) {
                selection.removeAllRanges();
                console.log('Cleared automatic selections');
            }
        """
        topicContent.evaluateJavascript(js, null)
    }

    private fun testTextSelection() {
        val js = """
            console.log('Testing text selection...');
            console.log('Document ready state:', document.readyState);
            console.log('Body content length:', document.body ? document.body.innerHTML.length : 'No body');
            console.log('Selection API available:', typeof window.getSelection);
            
            // Test if text selection is working
            var testElement = document.querySelector('p, div, span, h1, h2, h3, h4, h5, h6');
            if (testElement) {
                console.log('Found text element:', testElement.tagName);
                console.log('Element user-select:', window.getComputedStyle(testElement).userSelect);
                console.log('Element webkit-user-select:', window.getComputedStyle(testElement).webkitUserSelect);
            }
            
            // Clear any existing selections
            var selection = window.getSelection();
            if (selection) {
                selection.removeAllRanges();
                console.log('Cleared any existing selections');
            }
            
            console.log('Text selection test complete - try selecting text now');
        """
        topicContent.evaluateJavascript(js, null)
    }

    private fun toggleTTS() {
        if (isTtsEnabled) {
            stopTTS()
        } else {
            startTTS()
        }
    }

    private fun startTTS() {
        currentTopic?.let { topic ->
            val text = topic.content.replace(Regex("<[^>]*>"), " ")
            if (text.isNotBlank()) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reading_utterance")
                isTtsEnabled = true
                if (::ttsButton.isInitialized) {
                    ttsButton.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                }
                Toast.makeText(this, "Text-to-Speech started", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("ReadingActivity", "No text available for TTS")
                Toast.makeText(this, "No text to read", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopTTS() {
        tts?.stop()
        isTtsEnabled = false
        if (::ttsButton.isInitialized) {
            ttsButton.setColorFilter(null)
        }
        Toast.makeText(this, "Text-to-Speech stopped", Toast.LENGTH_SHORT).show()
    }

    private fun toggleBookmark() {
        currentTopic?.let { topic ->
            val bookmarkKey = "${courseCode}_${topic.id}"
            if (bookmarks.contains(bookmarkKey)) {
                bookmarks.remove(bookmarkKey)
                Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
            } else {
                bookmarks.add(bookmarkKey)
                Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show()
            }
            updateBookmarkButton()
            saveBookmarks()
        }
    }

    private fun updateBookmarkButton() {
        if (::bookmarkButton.isInitialized) {
            currentTopic?.let { topic ->
                val bookmarkKey = "${courseCode}_${topic.id}"
                if (bookmarks.contains(bookmarkKey)) {
                    bookmarkButton.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                } else {
                    bookmarkButton.setColorFilter(null)
                }
            }
        }
    }

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = "Search in content..."
            setText(searchQuery)
        }

        AlertDialog.Builder(this)
            .setTitle("Search Content")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                searchQuery = input.text.toString()
                if (searchQuery.isNotBlank()) {
                    searchInContent(searchQuery)
                }
            }
            .setNegativeButton("Clear") { _, _ ->
                clearSearch()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun searchInContent(query: String) {
        val escapedQuery = query.replace("'", "\\'")
        val js = """
            var searchTerm = '$escapedQuery';   
            var content = document.body.innerHTML;
            var regex = new RegExp('(' + searchTerm + ')', 'gi');
            var highlightedContent = content.replace(regex, '<mark style="background-color: yellow; color: black;">$1</mark>');
            document.body.innerHTML = highlightedContent;
            
            var firstMatch = document.querySelector('mark');
            if (firstMatch) {
                firstMatch.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        """
        topicContent.evaluateJavascript(js, null)
        isSearchMode = true
        Toast.makeText(this, "Search results highlighted", Toast.LENGTH_SHORT).show()
    }

    private fun clearSearch() {
        val js = """
            var marks = document.querySelectorAll('mark');
            marks.forEach(function(mark) {
                var parent = mark.parentNode;
                parent.replaceChild(document.createTextNode(mark.textContent), mark);
                parent.normalize();
            });
        """
        topicContent.evaluateJavascript(js, null)
        searchQuery = ""
        isSearchMode = false
        Toast.makeText(this, "Search cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showSettingsDialog() {
        val options = arrayOf(
            "Toggle Reading Controls",
            "Reset All Settings",
            "Test Word Selection",
            "Export Annotations",
            "Import Annotations",
            "About"
        )

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleReadingControls()
                    1 -> resetAllSettings()
                    2 -> testWordSelection()
                    3 -> exportAnnotations()
                    4 -> importAnnotations()
                    5 -> showAboutDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleReadingControls() {
        readingControlsLayout.visibility = if (readingControlsLayout.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun resetAllSettings() {
        AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("This will reset all reading preferences, bookmarks, and annotations. Are you sure?")
            .setPositiveButton("Reset") { _, _ ->
                getSharedPreferences("ReadingPreferences", MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences("Bookmarks", MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences("AnnotationsPrefs", MODE_PRIVATE).edit().clear().apply()

                currentFontSize = 16
                currentFontFamily = "Arial"
                currentReadingMode = "light"
                currentZoomLevel = 1.0f
                bookmarks.clear()
                annotations.clear()

                applyReadingPreferences()
                updateBookmarkButton()
                topicContent.reload()

                Toast.makeText(this, "All settings reset", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun testWordSelection() {
        val js = """
            console.log('🧪 Testing word selection functionality...');
            
            // Show test instructions
            var testInstructions = document.createElement('div');
            testInstructions.id = 'word-selection-test';
            testInstructions.style.cssText = 
                'position: fixed;' +
                'top: 50%;' +
                'left: 50%;' +
                'transform: translate(-50%, -50%);' +
                'background: linear-gradient(135deg, #FF9800, #F57C00);' +
                'color: white;' +
                'padding: 20px 25px;' +
                'border-radius: 15px;' +
                'box-shadow: 0 8px 25px rgba(0,0,0,0.3);' +
                'z-index: 10000;' +
                'font-family: Arial, sans-serif;' +
                'font-size: 16px;' +
                'font-weight: bold;' +
                'text-align: center;' +
                'max-width: 90%;' +
                'animation: testPulse 2s infinite;';
            
            testInstructions.innerHTML = 
                '<div style="margin-bottom: 10px;">🧪 Word Selection Test</div>' +
                '<div style="font-size: 14px; opacity: 0.9; line-height: 1.5;">' +
                'Try these actions on any text:<br><br>' +
                '• Double-click a word<br>' +
                '• Long-press (500ms) on a word<br>' +
                '• Look for the green feedback popup<br>' +
                '• Use the toolbar to highlight/underline' +
                '</div>' +
                '<div style="margin-top: 15px; font-size: 12px; opacity: 0.8;">' +
                'This message will auto-hide in 8 seconds' +
                '</div>';
            
            // Add CSS animation
            if (!document.getElementById('test-pulse-styles')) {
                var style = document.createElement('style');
                style.id = 'test-pulse-styles';
                style.textContent = `
                    @keyframes testPulse {
                        0% { transform: translate(-50%, -50%) scale(1); }
                        50% { transform: translate(-50%, -50%) scale(1.05); }
                        100% { transform: translate(-50%, -50%) scale(1); }
                    }
                `;
                document.head.appendChild(style);
            }
            
            document.body.appendChild(testInstructions);
            
            // Auto-hide after 8 seconds
            setTimeout(function() {
                if (testInstructions && testInstructions.parentNode) {
                    testInstructions.style.animation = 'testPulse 0.5s ease-out reverse';
                    setTimeout(function() {
                        if (testInstructions && testInstructions.parentNode) {
                            testInstructions.parentNode.removeChild(testInstructions);
                        }
                    }, 500);
                }
            }, 8000);
            
            console.log('✅ Word selection test instructions displayed');
        """
        topicContent.evaluateJavascript(js, null)
        Log.d("ReadingActivity", "📄 Word selection test initiated")
    }

    private fun exportAnnotations() {
        Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun importAnnotations() {
        Toast.makeText(this, "Import feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About Reading Activity")
            .setMessage("""
                Enhanced Reading Activity v2.1
                
                Features:
                • Text selection with highlight, underline, and note options
                • Zoom controls with pinch-to-zoom
                • Font size and family selection
                • Multiple reading modes (Light, Dark, Sepia, High Contrast)
                • Text-to-Speech support
                • Bookmarking system
                • Content search
                • Simple annotation tools (highlight, underline, notes)
                • Responsive UI controls
                
                Developed for UniValut
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    // Text Selection Methods
    private fun setupTextSelection() {
        createSelectionToolbar()

        // Simple touch handling for text selection
        topicContent.setOnTouchListener { _, event ->
            scaleGestureDetector?.onTouchEvent(event) ?: false
        }
    }

    private fun createSelectionToolbar() {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 12.dpToPx())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFFFFFFF"))
                cornerRadius = 20.dpToPx().toFloat()
                setStroke(2.dpToPx(), Color.parseColor("#4CAF50"))
            }
            elevation = 32f
        }

        fun createSelectionButton(iconRes: Int, desc: String, onClick: () -> Unit): ImageButton {
            return ImageButton(context).apply {
                setImageResource(iconRes)
                contentDescription = desc
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F5F5F5"))
                    cornerRadius = 12.dpToPx().toFloat()
                    setStroke(2.dpToPx(), Color.parseColor("#4CAF50"))
                }
                setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
                setColorFilter(Color.parseColor("#4CAF50"))
                setOnClickListener {
                    onClick()
                    hideSelectionToolbar()
                }
                layoutParams = LinearLayout.LayoutParams(56.dpToPx(), 56.dpToPx()).apply {
                    marginStart = 8.dpToPx()
                    marginEnd = 8.dpToPx()
                }
            }
        }

        // Create selection action buttons - Only Highlight and Underline
        val highlightButton = createSelectionButton(android.R.drawable.ic_menu_set_as, "Highlight") {
            applyHighlight()
        }
        val underlineButton = createSelectionButton(android.R.drawable.ic_menu_edit, "Underline") {
            applyUnderline()
        }

        container.addView(highlightButton)
        container.addView(underlineButton)

        selectionToolbar = PopupWindow(container, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = 20f
            isOutsideTouchable = true
        }
    }

    private fun showSelectionToolbar(x: Int, y: Int) {
        val root = findViewById<View>(android.R.id.content)
        root.post {
            val pw = selectionToolbar ?: return@post
            if (pw.isShowing) pw.dismiss()

            // Convert WebView coordinates to screen coordinates
            val webViewLocation = IntArray(2)
            topicContent.getLocationOnScreen(webViewLocation)
            val screenX = webViewLocation[0] + x
            val screenY = webViewLocation[1] + y - 60.dpToPx() // Position above selection

            pw.showAtLocation(root, Gravity.TOP or Gravity.START,
                screenX.coerceAtLeast(0), screenY.coerceAtLeast(0))
        }
    }

    private fun hideSelectionToolbar() {
        selectionToolbar?.dismiss()
    }

    private fun applyHighlight() {
        currentSelection?.let { (start, end, text) ->
            val annotation = Annotation(
                id = generateAnnotationId(),
                contentId = currentContentId(),
                type = "highlight",
                start = start,
                end = end,
                color = currentHighlightColor
            )
            annotations.add(annotation)
            saveAnnotations()
            applyAnnotationToWebView(annotation)
            Toast.makeText(this, "Text highlighted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyUnderline() {
        currentSelection?.let { (start, end, text) ->
            val annotation = Annotation(
                id = generateAnnotationId(),
                contentId = currentContentId(),
                type = "underline",
                start = start,
                end = end,
                color = currentUnderlineColor
            )
            annotations.add(annotation)
            saveAnnotations()
            applyAnnotationToWebView(annotation)
            Toast.makeText(this, "Text underlined", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addNoteToSelection() {
        currentSelection?.let { (start, end, text) ->
            showNoteDialog("") { noteText ->
                val annotation = Annotation(
                    id = generateAnnotationId(),
                    contentId = currentContentId(),
                    type = "note",
                    start = start,
                    end = end,
                    noteText = noteText
                )
                annotations.add(annotation)
                saveAnnotations()
                applyAnnotationToWebView(annotation)
                Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copySelection() {
        currentSelection?.let { (_, _, text) ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Selected text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyAnnotationToWebView(annotation: Annotation) {
        val js = when (annotation.type) {
            "highlight" -> "AnnotatorJS.applyAnnotation('${annotation.id}', 'highlight', ${annotation.start}, ${annotation.end}, '${annotation.color}', '');"
            "underline" -> "AnnotatorJS.applyAnnotation('${annotation.id}', 'underline', ${annotation.start}, ${annotation.end}, '${annotation.color}', '');"
            "note" -> "AnnotatorJS.applyAnnotation('${annotation.id}', 'note', ${annotation.start}, ${annotation.end}, '', '${annotation.noteText}');"
            else -> ""
        }
        topicContent.evaluateJavascript(js, null)
    }

    inner class AnnotationBridge {
        @JavascriptInterface
        fun onAnnotationTapped(id: String) {
            runOnUiThread {
                val annotation = annotations.find { it.id == id }
                if (annotation != null) {
                    when (annotation.type) {
                        "note" -> showNoteDialog(annotation.noteText ?: "") { newNote ->
                            val updated = annotation.copy(noteText = newNote)
                            annotations[annotations.indexOf(annotation)] = updated
                            saveAnnotations()
                            topicContent.reload()
                        }
                        else -> showAnnotationEditDialog(annotation)
                    }
                }
            }
        }
    }

    // JavaScript interface for text selection
    inner class TextSelectionBridge {
        @JavascriptInterface
        fun onTextSelected(start: Int, end: Int, text: String, x: Float, y: Float) {
            runOnUiThread {
                Log.d("TextSelection", "Text selected: start=$start, end=$end, text='$text'")
                if (text.trim().isNotEmpty()) {
                    currentSelection = Triple(start, end, text)
                    showSelectionToolbar(x.toInt(), y.toInt())
                }
            }
        }

        @JavascriptInterface
        fun onSelectionCleared() {
            runOnUiThread {
                Log.d("TextSelection", "Selection cleared")
                currentSelection = null
                hideSelectionToolbar()
            }
        }

        @JavascriptInterface
        fun onSelectionChanged(start: Int, end: Int, text: String, x: Float, y: Float) {
            runOnUiThread {
                Log.d("TextSelection", "Selection changed: start=$start, end=$end, text='$text'")
                if (text.trim().isNotEmpty()) {
                    currentSelection = Triple(start, end, text)
                    showSelectionToolbar(x.toInt(), y.toInt())
                } else {
                    currentSelection = null
                    hideSelectionToolbar()
                }
            }
        }
    }

    private fun injectAnnotationJS() {
        val js = """
            (function() {
                if (window.__annotatorInstalled) return;
                window.__annotatorInstalled = true;
                
                console.log('Enhanced text selection script loaded');
                
                // Text selection detection
                var selectionTimeout;
                var lastSelection = '';
                var longPressTimer = null;
                var isLongPressing = false;
                
                function handleSelection() {
                    clearTimeout(selectionTimeout);
                    selectionTimeout = setTimeout(function() {
                        var selection = window.getSelection();
                        var selectedText = selection ? selection.toString().trim() : '';
                        
                        if (selectedText && selectedText.length > 0 && selectedText !== lastSelection) {
                            lastSelection = selectedText;
                            var range = selection.getRangeAt(0);
                            var rect = range.getBoundingClientRect();
                            var start = getTextOffset(range.startContainer, range.startOffset);
                            var end = getTextOffset(range.endContainer, range.endOffset);
                            
                            console.log('Text selected:', selectedText);
                            
                            if (selectedText.length > 1 && window.TextSelection) {
                                window.TextSelection.onTextSelected(start, end, selectedText, rect.left + rect.width/2, rect.top);
                            }
                        } else if (!selectedText && lastSelection) {
                            lastSelection = '';
                            if (window.TextSelection) {
                                window.TextSelection.onSelectionCleared();
                            }
                        }
                    }, 100);
                }
                
                // Function to select word at position
                function selectWordAtPosition(x, y) {
                    var element = document.elementFromPoint(x, y);
                    if (!element) return;
                    
                    // Find the text node
                    var textNode = null;
                    var walker = document.createTreeWalker(
                        element,
                        NodeFilter.SHOW_TEXT,
                        null,
                        false
                    );
                    
                    var node;
                    while (node = walker.nextNode()) {
                        if (node.nodeValue && node.nodeValue.trim().length > 0) {
                            textNode = node;
                            break;
                        }
                    }
                    
                    if (!textNode) return;
                    
                    // Get the word at the click position
                    var range = document.createRange();
                    var text = textNode.nodeValue;
                    var textOffset = 0;
                    
                    // Calculate offset within the text node
                    var tempRange = document.createRange();
                    tempRange.setStart(textNode, 0);
                    tempRange.setEnd(textNode, textNode.nodeValue.length);
                    var rect = tempRange.getBoundingClientRect();
                    
                    // Find word boundaries
                    var wordStart = 0;
                    var wordEnd = text.length;
                    
                    // Find the start of the word
                    for (var i = 0; i < text.length; i++) {
                        if (/\w/.test(text[i])) {
                            wordStart = i;
                            break;
                        }
                    }
                    
                    // Find the end of the word
                    for (var i = wordStart; i < text.length; i++) {
                        if (!/\w/.test(text[i])) {
                            wordEnd = i;
                            break;
                        }
                    }
                    
                    if (wordStart < wordEnd) {
                        // Select the word
                        range.setStart(textNode, wordStart);
                        range.setEnd(textNode, wordEnd);
                        
                        var selection = window.getSelection();
                        selection.removeAllRanges();
                        selection.addRange(range);
                        
                        // Trigger selection event
                        var selectedText = text.substring(wordStart, wordEnd);
                        var rect = range.getBoundingClientRect();
                        var start = getTextOffset(range.startContainer, range.startOffset);
                        var end = getTextOffset(range.endContainer, range.endOffset);
                        
                        console.log('Word selected via click:', selectedText);
                        
                        // Show visual feedback
                        showWordSelectionFeedback(selectedText, rect.left + rect.width/2, rect.top);
                        
                        if (window.TextSelection) {
                            window.TextSelection.onTextSelected(start, end, selectedText, rect.left + rect.width/2, rect.top);
                        }
                    }
                }
                
                // Double click handler
                function handleDoubleClick(event) {
                    event.preventDefault();
                    console.log('Double click detected at:', event.clientX, event.clientY);
                    selectWordAtPosition(event.clientX, event.clientY);
                }
                
                // Long press handlers
                function handleTouchStart(event) {
                    if (longPressTimer) {
                        clearTimeout(longPressTimer);
                    }
                    
                    isLongPressing = false;
                    longPressTimer = setTimeout(function() {
                        isLongPressing = true;
                        console.log('Long press detected at:', event.touches[0].clientX, event.touches[0].clientY);
                        selectWordAtPosition(event.touches[0].clientX, event.touches[0].clientY);
                    }, 500); // 500ms for long press
                }
                
                function handleTouchEnd(event) {
                    if (longPressTimer) {
                        clearTimeout(longPressTimer);
                        longPressTimer = null;
                    }
                    isLongPressing = false;
                }
                
                function handleTouchMove(event) {
                    if (longPressTimer) {
                        clearTimeout(longPressTimer);
                        longPressTimer = null;
                    }
                }
                
                // Mouse long press handler
                function handleMouseDown(event) {
                    if (longPressTimer) {
                        clearTimeout(longPressTimer);
                    }
                    
                    isLongPressing = false;
                    longPressTimer = setTimeout(function() {
                        isLongPressing = true;
                        console.log('Mouse long press detected at:', event.clientX, event.clientY);
                        selectWordAtPosition(event.clientX, event.clientY);
                    }, 500); // 500ms for long press
                }
                
                function handleMouseUp(event) {
                    if (longPressTimer) {
                        clearTimeout(longPressTimer);
                        longPressTimer = null;
                    }
                    isLongPressing = false;
                }
                
                // Event listeners
                document.addEventListener('mouseup', handleSelection);
                document.addEventListener('touchend', handleSelection);
                document.addEventListener('selectionchange', handleSelection);
                
                // Visual feedback for word selection
                function showWordSelectionFeedback(text, x, y) {
                    // Remove any existing feedback
                    var existingFeedback = document.getElementById('word-selection-feedback');
                    if (existingFeedback) {
                        existingFeedback.remove();
                    }
                    
                    // Create feedback element
                    var feedback = document.createElement('div');
                    feedback.id = 'word-selection-feedback';
                    feedback.style.cssText = 
                        'position: fixed;' +
                        'left: ' + x + 'px;' +
                        'top: ' + (y - 40) + 'px;' +
                        'background: linear-gradient(135deg, #4CAF50, #45a049);' +
                        'color: white;' +
                        'padding: 8px 12px;' +
                        'border-radius: 20px;' +
                        'font-size: 14px;' +
                        'font-weight: bold;' +
                        'z-index: 10000;' +
                        'box-shadow: 0 4px 12px rgba(0,0,0,0.3);' +
                        'transform: translateX(-50%);' +
                        'animation: wordSelectBounce 0.3s ease-out;' +
                        'pointer-events: none;' +
                        'max-width: 200px;' +
                        'text-align: center;' +
                        'word-wrap: break-word;';
                    
                    feedback.innerHTML = 
                        '<div style="font-size: 12px; opacity: 0.9; margin-bottom: 2px;">Word Selected</div>' +
                        '<div style="font-size: 16px;">"' + text + '"</div>';
                    
                    // Add CSS animation
                    if (!document.getElementById('word-selection-styles')) {
                        var style = document.createElement('style');
                        style.id = 'word-selection-styles';
                        style.textContent = `
                            @keyframes wordSelectBounce {
                                0% { transform: translateX(-50%) scale(0.8); opacity: 0; }
                                50% { transform: translateX(-50%) scale(1.1); opacity: 1; }
                                100% { transform: translateX(-50%) scale(1); opacity: 1; }
                            }
                        `;
                        document.head.appendChild(style);
                    }
                    
                    document.body.appendChild(feedback);
                    
                    // Auto-remove after 2 seconds
                    setTimeout(function() {
                        if (feedback && feedback.parentNode) {
                            feedback.style.animation = 'wordSelectBounce 0.3s ease-out reverse';
                            setTimeout(function() {
                                if (feedback && feedback.parentNode) {
                                    feedback.parentNode.removeChild(feedback);
                                }
                            }, 300);
                        }
                    }, 2000);
                }
                
                // Double click and long press listeners
                document.addEventListener('dblclick', handleDoubleClick);
                document.addEventListener('touchstart', handleTouchStart);
                document.addEventListener('touchend', handleTouchEnd);
                document.addEventListener('touchmove', handleTouchMove);
                document.addEventListener('mousedown', handleMouseDown);
                document.addEventListener('mouseup', handleMouseUp);
                
                // Enable text selection on all elements
                function enableSelectionOnAll() {
                    var allElements = document.querySelectorAll('*');
                    for (var i = 0; i < allElements.length; i++) {
                        var element = allElements[i];
                        element.style.userSelect = 'text';
                        element.style.webkitUserSelect = 'text';
                        element.style.mozUserSelect = 'text';
                        element.style.msUserSelect = 'text';
                        element.style.webkitTouchCallout = 'default';
                        element.style.webkitTapHighlightColor = 'rgba(76, 175, 80, 0.3)';
                        
                        if (['P', 'DIV', 'SPAN', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI', 'TD', 'TH'].includes(element.tagName)) {
                            element.style.cursor = 'text';
                            element.style.minHeight = '24px';
                        }
                    }
                    console.log('Text selection enabled on', allElements.length, 'elements');
                }
                
                // Enable immediately
                enableSelectionOnAll();
                setTimeout(enableSelectionOnAll, 500);
                setTimeout(enableSelectionOnAll, 1000);
                
                // Show instructions for word selection
                setTimeout(function() {
                    showWordSelectionInstructions();
                }, 2000);
                
                // Function to show word selection instructions
                function showWordSelectionInstructions() {
                    // Check if instructions already shown
                    if (document.getElementById('word-selection-instructions')) return;
                    
                    var instructions = document.createElement('div');
                    instructions.id = 'word-selection-instructions';
                    instructions.style.cssText = 
                        'position: fixed;' +
                        'top: 20px;' +
                        'left: 50%;' +
                        'transform: translateX(-50%);' +
                        'background: linear-gradient(135deg, #2196F3, #1976D2);' +
                        'color: white;' +
                        'padding: 15px 20px;' +
                        'border-radius: 25px;' +
                        'box-shadow: 0 8px 25px rgba(0,0,0,0.3);' +
                        'z-index: 10000;' +
                        'font-family: Arial, sans-serif;' +
                        'font-size: 14px;' +
                        'font-weight: bold;' +
                        'text-align: center;' +
                        'max-width: 90%;' +
                        'animation: slideDown 0.5s ease-out;';
                    
                    instructions.innerHTML = 
                        '<div style="margin-bottom: 8px;">🎯 Word Selection Tips</div>' +
                        '<div style="font-size: 12px; opacity: 0.9; line-height: 1.4;">' +
                        '• Double-click any word to select it<br>' +
                        '• Long-press (500ms) to select a word<br>' +
                        '• Use the toolbar to highlight or underline' +
                        '</div>';
                    
                    // Add CSS animation if not exists
                    if (!document.getElementById('slide-down-styles')) {
                        var style = document.createElement('style');
                        style.id = 'slide-down-styles';
                        style.textContent = `
                            @keyframes slideDown {
                                from { transform: translateX(-50%) translateY(-100%); opacity: 0; }
                                to { transform: translateX(-50%) translateY(0); opacity: 1; }
                            }
                        `;
                        document.head.appendChild(style);
                    }
                    
                    document.body.appendChild(instructions);
                    
                    // Auto-hide after 6 seconds
                    setTimeout(function() {
                        if (instructions && instructions.parentNode) {
                            instructions.style.animation = 'slideDown 0.5s ease-out reverse';
                            setTimeout(function() {
                                if (instructions && instructions.parentNode) {
                                    instructions.parentNode.removeChild(instructions);
                                }
                            }, 500);
                        }
                    }, 6000);
                }
                
                function getTextOffset(node, offset) {
                    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
                    var total = 0;
                    var current;
                    while (current = walker.nextNode()) {
                        if (current === node) {
                            return total + offset;
                        }
                        total += current.nodeValue ? current.nodeValue.length : 0;
                    }
                    return total;
                }

                function getSelectedOffsets() {
                    var selection = window.getSelection();
                    if (!selection || selection.rangeCount === 0) return null;
                    var range = selection.getRangeAt(0);
                    if (!range.toString().trim()) return null;
                    
                    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
                    var start = 0, end = 0;
                    var currentOffset = 0;
                    var foundStart = false;

                    while (walker.nextNode()) {
                        var node = walker.currentNode;
                        if (!foundStart && node === range.startContainer) {
                            start = currentOffset + range.startOffset;
                            foundStart = true;
                        }
                        if (node === range.endContainer) {
                            end = currentOffset + range.endOffset;
                            break;
                        }
                        currentOffset += node.nodeValue ? node.nodeValue.length : 0;
                    }
                    return {start: start, end: end, text: selection.toString()};
                }

                function applyAnnotation(id, type, start, end, color, noteText) {
                    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
                    var currentOffset = 0;
                    var ranges = [];
                    
                    while (walker.nextNode()) {
                        var node = walker.currentNode;
                        var nodeStart = currentOffset;
                        var nodeEnd = currentOffset + (node.nodeValue ? node.nodeValue.length : 0);
                        var s = Math.max(start, nodeStart);
                        var e = Math.min(end, nodeEnd);
                        
                        if (e > s) {
                            ranges.push({node: node, s: s - nodeStart, e: e - nodeStart});
                        }
                        currentOffset = nodeEnd;
                        if (currentOffset >= end) break;
                    }

                    for (var i = ranges.length - 1; i >= 0; i--) {
                        var r = ranges[i];
                        var node = r.node;
                        var text = node.nodeValue;
                        var before = document.createTextNode(text.substring(0, r.s));
                        var middle = document.createTextNode(text.substring(r.s, r.e));
                        var after = document.createTextNode(text.substring(r.e));
                        
                        var span = document.createElement('span');
                        span.setAttribute('data-ann-id', id);
                        span.setAttribute('data-ann-type', type);
                        
                        var style = '';
                        if (type === 'highlight') {
                            style = 'background-color:' + (color || '#FFFF00') + ';padding:2px;border-radius:3px;';
                        } else if (type === 'underline') {
                            style = 'text-decoration:underline;text-decoration-color:' + (color || '#FF0000') + ';text-decoration-thickness:2px;';
                        } else if (type === 'note') {
                            style = 'border-bottom:2px dotted #90caf9;background-color:rgba(144,202,249,0.1);padding:2px;';
                            if (noteText) span.setAttribute('title', noteText);
                        }
                        
                        span.style.cssText = style + 'cursor:pointer;';
                        span.onclick = function() {
                            if (window.AndroidAnnotator) {
                                window.AndroidAnnotator.onAnnotationTapped(id);
                            }
                        };
                        
                        node.parentNode.insertBefore(before, node);
                        node.parentNode.insertBefore(span, node);
                        span.appendChild(middle);
                        node.parentNode.insertBefore(after, node);
                        node.parentNode.removeChild(node);
                    }
                }
                
                window.AnnotatorJS = {
                    getSelectedOffsets: function() {
                        var offsets = getSelectedOffsets();
                        return offsets ? JSON.stringify(offsets) : 'null';
                    },
                    applyAnnotation: function(id, type, start, end, color, noteText) {
                        applyAnnotation(id, type, start, end, color, noteText);
                    },
                    enableSelection: function() {
                        enableSelectionOnAll();
                    }
                };
                
                // Observe for DOM changes to re-enable selection
                var observer = new MutationObserver(function(mutations) {
                    enableSelectionOnAll();
                    console.log('DOM changed, re-enabled text selection');
                });
                observer.observe(document.body, { childList: true, subtree: true, characterData: true });
                
                console.log('Simple text selection system initialized with mutation observer');
            })();
        """.trimIndent()
        topicContent.evaluateJavascript(js, null)
    }

    private fun renderSavedAnnotations() {
        annotations.filter { it.contentId == currentContentId() }.forEach { ann ->
            val escapedNoteText = ann.noteText?.replace("'", "\\'") ?: ""
            val js = """
                AnnotatorJS.applyAnnotation('${ann.id}', '${ann.type}', ${ann.start}, ${ann.end}, '${ann.color ?: ""}', '$escapedNoteText');
            """
            topicContent.evaluateJavascript(js, null)
        }
    }

    private fun handleSelectionAction(itemId: Int) {
        topicContent.evaluateJavascript("AnnotatorJS.getSelectedOffsets();") { result ->
            try {
                if (result == "null") {
                    Log.w("ReadingActivity", "No valid text selection")
                    Toast.makeText(this, "Please select some text", Toast.LENGTH_SHORT).show()
                    return@evaluateJavascript
                }
                val json = JSONObject(result)
                val start = json.getInt("start")
                val end = json.getInt("end")
                val text = json.getString("text")
                if (end <= start || text.isBlank()) {
                    Log.w("ReadingActivity", "Invalid selection range or empty text: start=$start, end=$end, text='$text'")
                    Toast.makeText(this, "Invalid selection", Toast.LENGTH_SHORT).show()
                    return@evaluateJavascript
                }

                val annotationId = generateAnnotationId()
                when (itemId) {
                    1001 -> { // Highlight
                        val ann = Annotation(annotationId, currentContentId(), "highlight", start, end, currentHighlightColor)
                        annotations.add(ann)
                        topicContent.evaluateJavascript(
                            "AnnotatorJS.applyAnnotation('$annotationId', 'highlight', $start, $end, '$currentHighlightColor', '');",
                            null
                        )
                        saveAnnotations()
                        Toast.makeText(this, "Text highlighted", Toast.LENGTH_SHORT).show()
                    }
                    1002 -> { // Underline
                        val ann = Annotation(annotationId, currentContentId(), "underline", start, end, currentUnderlineColor)
                        annotations.add(ann)
                        topicContent.evaluateJavascript(
                            "AnnotatorJS.applyAnnotation('$annotationId', 'underline', $start, $end, '$currentUnderlineColor', '');",
                            null
                        )
                        saveAnnotations()
                        Toast.makeText(this, "Text underlined", Toast.LENGTH_SHORT).show()
                    }
                    1003 -> { // Add Note
                        showNoteDialog("") { noteText ->
                            val ann = Annotation(annotationId, currentContentId(), "note", start, end, null, noteText)
                            annotations.add(ann)
                            val escapedNoteText = noteText.replace("'", "\\'")
                            topicContent.evaluateJavascript(
                                "AnnotatorJS.applyAnnotation('$annotationId', 'note', $start, $end, '', '$escapedNoteText');",
                                null
                            )
                            saveAnnotations()
                            Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1004 -> { // Copy
                        copyToClipboard(text)
                    }
                }
                topicContent.evaluateJavascript("window.getSelection().removeAllRanges();", null)
            } catch (e: JSONException) {
                Log.e("ReadingActivity", "Failed to parse selection: ${e.message}")
                Toast.makeText(this, "Error processing selection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNoteDialog(initial: String, onSave: (String) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(initial)
            hint = "Type your note..."
        }
        AlertDialog.Builder(this)
            .setTitle("Add/Edit Note")
            .setView(input)
            .setPositiveButton("Save") { _, _ -> onSave(input.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAnnotationEditDialog(annotation: Annotation) {
        val options = if (annotation.type == "highlight") {
            arrayOf("Change Color", "Delete")
        } else {
            arrayOf("Delete")
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Annotation")
            .setItems(options) { _, which ->
                when {
                    which == 0 && annotation.type == "highlight" -> {
                        showColorPicker { color ->
                            val updated = annotation.copy(color = color)
                            annotations[annotations.indexOf(annotation)] = updated
                            saveAnnotations()
                            topicContent.reload()
                        }
                    }
                    which == 0 || (which == 1 && annotation.type == "highlight") -> {
                        annotations.remove(annotation)
                        saveAnnotations()
                        topicContent.reload()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showColorPicker(onPicked: (String) -> Unit) {
        val colors = arrayOf("#FFFF00", "#FF9800", "#4CAF50", "#2196F3", "#E91E63")
        val labels = arrayOf("Yellow", "Orange", "Green", "Blue", "Pink")
        AlertDialog.Builder(this)
            .setTitle("Pick Color")
            .setItems(labels) { _, which -> onPicked(colors[which]) }
            .show()
    }

    private fun copyToClipboard(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("selection", text))
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun setupScrollListener() {
        findViewById<ScrollView>(R.id.contentScrollView)?.viewTreeObserver?.addOnGlobalLayoutListener {
            markAsReadSectionY = markAsReadCheckbox.top - (findViewById<ScrollView>(R.id.contentScrollView)?.height ?: 0) + 100.dpToPx()
        }

        findViewById<ScrollView>(R.id.contentScrollView)?.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val scrollView = findViewById<ScrollView>(R.id.contentScrollView)
            val maxScrollY = scrollView?.getChildAt(0)?.height?.minus(scrollView.height) ?: 0

            if ((scrollY >= markAsReadSectionY || scrollY >= maxScrollY - 50) && !hasScrolledToMarkAsRead) {
                hasScrolledToMarkAsRead = true
            }
        }
    }

    private fun getIntentData() {
        courseCode = intent.getStringExtra("courseCode") ?: ""
        courseTitle = intent.getStringExtra("courseName") ?: "Unknown Course"
        collegeName = intent.getStringExtra("collegeName") ?: "Unknown College"
        selectedMode = intent.getStringExtra("mode") ?: "UNKNOWN"

        courseName.text = "$courseTitle - $selectedMode Mode"
        saveLastStudiedCourse()
    }

    private fun saveLastStudiedCourse() {
        val sharedPrefs = getSharedPreferences("LastStudied", MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("last_course_code", courseCode)
            putString("last_course_name", courseTitle)
            putString("last_mode", selectedMode)
            putLong("last_study_time", System.currentTimeMillis())
        }.apply()
    }

    private fun setupClickListeners() {
        setupCheckboxListener()
        previousButton.setOnClickListener {
            if (currentTopicIndex > 0) {
                currentTopicIndex--
                loadCurrentTopic()
            }
        }
        nextButton.setOnClickListener {
            if (currentTopicIndex < topics.size - 1) {
                currentTopicIndex++
                loadCurrentTopic()
            }
        }
        completeTopicButton.setOnClickListener {
            completeCurrentTopic()
        }
        notesButton.setOnClickListener {
            showNotesDialog()
        }
    }

    private fun setupCheckboxListener() {
        markAsReadCheckbox.setOnCheckedChangeListener { _, isChecked ->
            currentTopic?.let { topic ->
                if (isChecked) {
                    updateTopicReadStatus(topic.id, true)
                    updateReadStatus(true)
                } else {
                    updateTopicReadStatus(topic.id, false)
                    updateReadStatus(false)
                    hasScrolledToMarkAsRead = false
                }
            }
        }
    }

    private fun loadTopics() {
        topicContent.loadDataWithBaseURL(
            null,
            "<html><head><meta charset=\"UTF-8\"></head><body>🔄 Loading topics from server...</body></html>",
            "text/html; charset=UTF-8",
            "utf-8",
            null
        )
        fetchTopicsFromAPI()
    }

    private fun loadCurrentTopic() {
        if (currentTopicIndex >= 0 && currentTopicIndex < topics.size) {
            currentTopic = topics[currentTopicIndex]
            currentTopic?.let { topic ->
                setCheckboxCheckedSilently(false)
                showLoadingSpinner()

                topicName.text = topic.name

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val html = """
                        <html>
                        <head>
                          <meta charset="UTF-8" />
                          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes" />
                          <style>
                            body { 
                              font-family: $currentFontFamily, sans-serif; 
                              font-size: ${currentFontSize}px;
                              line-height: 1.6; 
                              color: #333; 
                              width: 100%; 
                              margin: 0 auto; 
                              padding: 8px; 
                              box-sizing: border-box;
                              transition: all 0.3s ease;
                              user-select: text !important;
                              -webkit-user-select: text !important;
                              -moz-user-select: text !important;
                              -ms-user-select: text !important;
                              cursor: text;
                            }
                            h1, h2, h3, h4, h5, h6 { 
                              color: #4CAF50; 
                              border-bottom: 2px solid #ddd; 
                              padding-bottom: 10px; 
                              margin-top: 30px;
                              font-weight: bold;
                            }
                            h1 { font-size: 1.8em; }
                            h2 { font-size: 1.5em; }
                            h3 { font-size: 1.3em; }
                            h4 { font-size: 1.1em; }
                            ul, ol { 
                              list-style-type: disc; 
                              padding-left: 20px; 
                              margin: 15px 0;
                            }
                            li { margin: 8px 0; }
                            li b { color: #555; font-weight: bold; }
                            p { margin: 15px 0; text-align: justify; }
                            hr { border: 0; height: 1px; background: #ddd; margin: 20px 0; }
                            .image-container { 
                              text-align: center; 
                              margin: 20px 0; 
                            }
                            .image-container img { 
                              max-width: 100%; 
                              height: auto; 
                              border-radius: 8px;
                              box-shadow: 0 4px 8px rgba(0,0,0,0.1);
                            }
                            pre { 
                              white-space: pre-wrap; 
                              word-wrap: break-word; 
                              font-family: 'Courier New', monospace; 
                              background-color: #f5f5f5; 
                              padding: 15px; 
                              border-radius: 8px; 
                              border: 1px solid #ddd; 
                              width: 100%; 
                              box-sizing: border-box; 
                              margin: 10px 0; 
                              font-size: 14px; 
                              overflow-x: auto;
                              box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                            }
                            .plain-text { 
                              font-family: 'Courier New', monospace; 
                              white-space: pre-wrap; 
                              word-wrap: break-word; 
                              background-color: #f9f9f9; 
                              padding: 15px; 
                              border-radius: 8px; 
                              border: 1px solid #ddd; 
                              line-height: 1.5; 
                              font-size: 14px; 
                              width: 100%; 
                              box-sizing: border-box; 
                              overflow-x: auto;
                              box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                            }
                            table { 
                              border-collapse: collapse; 
                              width: 100%; 
                              overflow-x: auto; 
                              display: block; 
                              margin: 15px 0;
                              border-radius: 8px;
                              overflow: hidden;
                              box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                            }
                            table, th, td { 
                              border: 1px solid #ddd; 
                              padding: 12px 8px; 
                            }
                            th {
                              background-color: #f5f5f5;
                              font-weight: bold;
                              color: #333;
                            }
                            tr:nth-child(even) { background-color: #f9f9f9; }
                            tr:hover { background-color: #f0f0f0; }
                            img { max-width: 100%; height: auto; border-radius: 4px; }
                            code { 
                              background-color: #f5f5f5; 
                              padding: 2px 6px; 
                              border-radius: 4px; 
                              font-family: 'Courier New', monospace;
                              font-size: 0.9em;
                              border: 1px solid #e0e0e0;
                            }
                            blockquote {
                              border-left: 4px solid #4CAF50;
                              margin: 20px 0;
                              padding: 10px 20px;
                              background-color: #f9f9f9;
                              border-radius: 0 8px 8px 0;
                              font-style: italic;
                            }
                            [data-ann-id] { 
                              cursor: pointer; 
                              transition: all 0.3s ease; 
                              border-radius: 4px;
                              position: relative;
                            }
                            [data-ann-id]:hover { 
                              transform: scale(1.02); 
                              box-shadow: 0 4px 12px rgba(0,0,0,0.2);
                            }
                            [data-ann-type="highlight"] { 
                              background-color: #FFFF00 !important; 
                              padding: 2px; 
                              border-radius: 3px;
                            }
                            [data-ann-type="underline"] { 
                              text-decoration: underline !important; 
                              text-decoration-color: #FF0000 !important; 
                              text-decoration-thickness: 2px !important;
                            }
                            [data-ann-type="note"] {
                              border-bottom: 2px dotted #90caf9 !important;
                              background-color: rgba(144,202,249,0.1) !important;
                              padding: 2px;
                            }
                            mark {
                              background-color: #ffeb3b !important;
                              color: #000 !important;
                              padding: 2px 4px;
                              border-radius: 3px;
                              font-weight: bold;
                            }
                            
                            /* Enhanced text selection styling */
                            ::selection {
                              background-color: #4CAF50 !important;
                              color: white !important;
                              text-shadow: none !important;
                            }
                            
                            ::-moz-selection {
                              background-color: #4CAF50 !important;
                              color: white !important;
                              text-shadow: none !important;
                            }
                            
                            /* Make sure all text is selectable with enhanced properties */
                            * {
                              user-select: text !important;
                              -webkit-user-select: text !important;
                              -moz-user-select: text !important;
                              -ms-user-select: text !important;
                              -webkit-touch-callout: default !important;
                              -webkit-tap-highlight-color: transparent !important;
                              -webkit-user-drag: none !important;
                              -khtml-user-select: text !important;
                            }
                            
                            /* Ensure text elements are selectable with better interaction */
                            p, div, span, h1, h2, h3, h4, h5, h6, li, td, th, pre, code, blockquote {
                              user-select: text !important;
                              -webkit-user-select: text !important;
                              -moz-user-select: text !important;
                              -ms-user-select: text !important;
                              cursor: text !important;
                              -webkit-touch-callout: default !important;
                              -webkit-tap-highlight-color: transparent !important;
                              min-height: 20px !important;
                              position: relative !important;
                            }
                            
                            /* Enhanced selection for better click handling */
                            p:hover, div:hover, span:hover, h1:hover, h2:hover, h3:hover, h4:hover, h5:hover, h6:hover {
                              background-color: rgba(76, 175, 80, 0.05) !important;
                              transition: background-color 0.2s ease !important;
                            }
                            
                            /* Enhanced text selection for mobile */
                            @media (max-width: 768px) {
                              p, div, span, h1, h2, h3, h4, h5, h6, li, td, th, pre, code, blockquote {
                                -webkit-touch-callout: default !important;
                                -webkit-user-select: text !important;
                                -moz-user-select: text !important;
                                -ms-user-select: text !important;
                                user-select: text !important;
                                touch-action: manipulation !important;
                                min-height: 32px !important;
                                padding: 4px !important;
                                margin: 2px 0 !important;
                                cursor: text !important;
                                -webkit-tap-highlight-color: rgba(76, 175, 80, 0.2) !important;
                              }
                              
                              /* Make text more selectable on mobile */
                              body {
                                -webkit-touch-callout: default !important;
                                -webkit-user-select: text !important;
                                -webkit-tap-highlight-color: transparent !important;
                                touch-action: manipulation !important;
                              }
                              
                              /* Enhanced selection highlighting for mobile */
                              ::selection {
                                background-color: #4CAF50 !important;
                                color: white !important;
                                text-shadow: none !important;
                              }
                              
                              ::-moz-selection {
                                background-color: #4CAF50 !important;
                                color: white !important;
                                text-shadow: none !important;
                              }
                              
                              /* Better touch targets */
                              p, div, span {
                                min-height: 40px !important;
                                line-height: 1.6 !important;
                                padding: 8px 4px !important;
                              }
                              
                              h1, h2, h3, h4, h5, h6 {
                                min-height: 36px !important;
                                padding: 6px 4px !important;
                              }
                            }
                            @media (max-width: 768px) {
                              body { padding: 4px; }
                              h1 { font-size: 1.5em; }
                              h2 { font-size: 1.3em; }
                              h3 { font-size: 1.2em; }
                              pre, .plain-text { font-size: 12px; padding: 10px; }
                              table { font-size: 14px; }
                            }
                            .focus-mode {
                              max-width: 800px;
                              margin: 0 auto;
                              padding: 20px;
                            }
                            @media print {
                              body { font-size: 12pt; line-height: 1.4; }
                              [data-ann-id] { box-shadow: none !important; }
                              .no-print { display: none; }
                            }
                          </style>
                        </head>
                        <body class="focus-mode">${wrapPlainTextIfNeeded(topic.content)}</body>
                        </html>
                    """.trimIndent()
                    topicContent.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "utf-8", null)

                    hideLoadingSpinner()

                    findViewById<ScrollView>(R.id.contentScrollView)?.post {
                        findViewById<ScrollView>(R.id.contentScrollView)?.smoothScrollTo(0, 0)
                    }

                    hasScrolledToMarkAsRead = false
                    autoCheckScheduled = false

                    updateProgress()
                    updateNavigationButtons()
                    loadTopicReadStatus(topic.id)
                    loadAnnotations()
                    renderSavedAnnotations()
                    updateNotesButtonAppearance()

                    // Additional delays to ensure text selection works after content latency
                    topicContent.postDelayed({
                        injectAnnotationJS()
                        enableTextSelection()
                        testTextSelection()
                    }, 500)

                    topicContent.postDelayed({
                        injectAnnotationJS()
                        enableTextSelection()
                        testTextSelection()
                    }, 1500)

                    topicContent.postDelayed({
                        injectAnnotationJS()
                        enableTextSelection()
                        testTextSelection()
                    }, 2500)
                }, 1000) // Reduced delay for better UX
            }
        }
    }

    private fun updateProgress() {
        if (topics.isEmpty()) {
            topicProgress.progress = 0
            progressText.text = "0/0"
            return
        }

        val sharedPrefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        var completedTopics = 0
        topics.forEach { topic ->
            val isRead = sharedPrefs.getBoolean("topic_${courseCode}_${getModeSuffix()}_${topic.id}", false)
            if (isRead) completedTopics++
        }

        val progressPercent = (completedTopics * 100) / topics.size
        topicProgress.progress = progressPercent
        progressText.text = "${completedTopics}/${topics.size}"
    }

    private fun updateNavigationButtons() {
        previousButton.isEnabled = currentTopicIndex > 0
        if (currentTopicIndex == topics.size - 1) {
            nextButton.visibility = View.GONE
            completeTopicButton.visibility = View.VISIBLE
        } else {
            nextButton.visibility = View.VISIBLE
            nextButton.isEnabled = currentTopicIndex < topics.size - 1
            completeTopicButton.visibility = View.GONE
        }
    }

    private fun loadTopicReadStatus(topicId: String) {
        val sharedPrefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        val isRead = sharedPrefs.getBoolean("topic_${courseCode}_${getModeSuffix()}_${topicId}", false)
        setCheckboxCheckedSilently(isRead)
    }

    private fun updateTopicReadStatus(topicId: String, isRead: Boolean) {
        val sharedPrefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("topic_${courseCode}_${getModeSuffix()}_${topicId}", isRead).apply()
        updateTopicProgressToBackend(topicId, isRead)
        updateCourseProgress()
        updateProgress()
    }

    private fun updateReadStatus(isRead: Boolean) {
        if (isRead) {
            readStatus.text = "✓ Completed"
            readStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            readStatus.text = "Not read yet"
            readStatus.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    private fun updateCourseProgress() {
        val sharedPrefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        if (topics.isEmpty()) {
            val courseProgressPrefs = getSharedPreferences("CourseProgress", MODE_PRIVATE)
            courseProgressPrefs.edit()
                .putInt("progress_${courseCode}_${getModeSuffix()}", 0)
                .apply()
            return
        }
        var completedTopics = 0

        topics.forEach { topic ->
            val isRead = sharedPrefs.getBoolean("topic_${courseCode}_${getModeSuffix()}_${topic.id}", false)
            if (isRead) completedTopics++
        }

        val progress = (completedTopics * 100) / topics.size
        val courseProgressPrefs = getSharedPreferences("CourseProgress", MODE_PRIVATE)
        courseProgressPrefs.edit()
            .putInt("progress_${courseCode}_${getModeSuffix()}", progress)
            .apply()
    }

    private fun completeCurrentTopic() {
        currentTopic?.let { topic ->
            updateTopicReadStatus(topic.id, true)
            setCheckboxCheckedSilently(true)
            Toast.makeText(this, "Topic completed! Great job!", Toast.LENGTH_LONG).show()
            if (currentTopicIndex == topics.size - 1) {
                showCourseCompletionDialog()
            }
        }
    }

    private fun showCourseCompletionDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎉 Course Completed!")
            .setMessage("Congratulations! You have successfully completed all topics in this course.")
            .setPositiveButton("View Progress") { _, _ -> finish() }
            .setNegativeButton("Continue Reading") { _, _ -> }
            .create()
            .show()
    }

    private fun fetchTopicsFromAPI() {
        val courseId = getCourseIdFromCode(courseCode)
        val apiMode = when (selectedMode) {
            "PASS" -> "pass"
            "MASTER" -> "master"
            else -> "all"
        }

        val url = "http://10.86.199.54/univault/get_topics.php?course_code=$courseId&mode=$apiMode"
        Log.d("ReadingActivity", "Fetching topics from: $url")

        val queue = Volley.newRequestQueue(this)
        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            Request.Method.GET, url,
            { body ->
                try {
                    val response = JSONArray(body)
                    val fetchedTopics = mutableListOf<Topic>()
                    for (i in 0 until response.length()) {
                        val topicObject = response.getJSONObject(i)
                        val topic = Topic(
                            id = topicObject.optString("topic_id", "unknown"),
                            name = topicObject.optString("topic_name", "Unknown Topic"),
                            content = topicObject.optString("content", "No content available"),
                            isRead = false
                        )
                        fetchedTopics.add(topic)
                    }
                    if (fetchedTopics.isNotEmpty()) {
                        topics = fetchedTopics
                        seedTopicProgressDefaults()
                        saveTotalTopicCount(topics.size)
                        loadCurrentTopic()
                        Toast.makeText(this, "Loaded ${topics.size} topics", Toast.LENGTH_SHORT).show()
                    } else {
                        showNoTopicsState("📚 No topics found for this course and mode.\n\nPlease check back later or contact your instructor.")
                    }
                } catch (arrayEx: JSONException) {
                    try {
                        val obj = JSONObject(body)
                        val errorMsg = obj.optString("error", "No topics found")
                        showNoTopicsState("📚 $errorMsg\n\nPlease check back later or contact your instructor.")
                    } catch (objEx: JSONException) {
                        Log.e("ReadingActivity", "Unrecognized server response")
                        showNoTopicsState("❌ Unrecognized server response. Please try again later.")
                    }
                }
            },
            { error ->
                Log.e("ReadingActivity", "Network error: ${error.message}")
                showNoTopicsState("🌐 Network Error: ${error.message}\n\nPlease check your internet connection and try again.")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {}
        queue.add(stringRequest)
    }

    private fun seedTopicProgressDefaults() {
        if (topics.isEmpty()) return
        val prefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        val editor = prefs.edit()
        val modeSuffix = getModeSuffix()
        var seededAny = false
        topics.forEach { topic ->
            val key = "topic_${courseCode}_${modeSuffix}_${topic.id}"
            if (!prefs.contains(key)) {
                editor.putBoolean(key, false)
                seededAny = true
            }
        }
        if (seededAny) editor.apply()
    }

    private fun getCourseIdFromCode(courseCode: String): String {
        return courseCode
    }

    private fun getModeSuffix(): String {
        return when (selectedMode) {
            "PASS" -> "PASS"
            "MASTER" -> "MASTER"
            else -> "ALL"
        }
    }

    private fun saveTotalTopicCount(total: Int) {
        val totalsPrefs = getSharedPreferences("TopicTotals", MODE_PRIVATE)
        val modeSuffix = getModeSuffix()
        totalsPrefs.edit()
            .putInt("total_${courseCode}_${modeSuffix}", total)
            .apply()
    }

    private fun updateTopicProgressToBackend(topicId: String, isRead: Boolean) {
        val url = "http://10.86.199.54/univault/update_topic_progress.php"
        val queue = Volley.newRequestQueue(this)
        val jsonObject = JSONObject().apply {
            put("student_id", getStudentId())
            put("course_code", courseCode)
            put("topic_id", topicId)
            put("is_read", isRead)
            put("mode", when (selectedMode) {
                "PASS" -> "pass"
                "MASTER" -> "master"
                else -> "all"
            })
        }
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                if (response.optBoolean("success", false)) {
                    Log.d("ReadingActivity", "Progress updated to backend: ${response.optString("message")}")
                } else {
                    Log.e("ReadingActivity", "Backend error: ${response.optString("error")}")
                }
            },
            { error ->
                Log.e("ReadingActivity", "Failed to update backend: ${error.message}")
            }
        )
        queue.add(jsonObjectRequest)
    }

    private fun getStudentId(): Int {
        val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPrefs.getInt("student_id", 0)
    }

    override fun onResume() {
        super.onResume()
        sessionStartTime = System.currentTimeMillis()
        currentTopic?.let { topic ->
            loadTopicReadStatus(topic.id)
        }
        loadTotalStudyTime()
        updateStudyTimeDisplay()

        // Re-enable text selection when activity resumes
        topicContent.postDelayed({
            enableTextSelection()
            Log.d("ReadingActivity", "Text selection re-enabled on resume")
        }, 500)

        topicContent.postDelayed({
            enableTextSelection()
            Log.d("ReadingActivity", "Text selection re-enabled on resume (delayed)")
        }, 1500)
    }

    override fun onPause() {
        super.onPause()
        updateStudyTime()
        updateStudyTimeDisplay()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateCourseProgress()
        updateStudyTime()
        tts?.stop()
        tts?.shutdown()
        saveReadingPreferences()
        saveAnnotations()
        saveTopicNotes() // Add this line to save topic notes
    }

    private fun updateStudyTime() {
        if (sessionStartTime > 0) {
            val currentTime = System.currentTimeMillis()
            val sessionDuration = currentTime - sessionStartTime
            if (sessionDuration > 0) {
                totalStudyTimeMillis += sessionDuration
                saveStudyTime()
                HomeFragment1.saveReadingSession(this, sessionDuration, courseCode)
            }
            sessionStartTime = 0
        }
    }

    private fun saveStudyTime() {
        val sharedPreferences = getSharedPreferences("study_time_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putLong("total_study_time_$courseCode", totalStudyTimeMillis)
            .apply()
        saveStudyTimeToBackend()
    }

    private fun loadTotalStudyTime() {
        val sharedPreferences = getSharedPreferences("study_time_prefs", Context.MODE_PRIVATE)
        totalStudyTimeMillis = sharedPreferences.getLong("total_study_time_$courseCode", 0)
        loadStudyTimeFromBackend()
    }

    private fun getTotalStudyTimeInHours(): Float {
        return (totalStudyTimeMillis / (1000.0f * 60.0f * 60.0f)).toFloat()
    }

    private fun updateStudyTimeDisplay() {
        val hours = getTotalStudyTimeInHours()
        val minutes = (hours * 60).toInt()
        if (minutes < 60) {
            studyTimeText.text = "Study time: $minutes min"
        } else {
            val displayHours = minutes / 60
            val displayMinutes = minutes % 60
            studyTimeText.text = "Study time: $displayHours h $displayMinutes min"
        }
    }

    private fun setCheckboxCheckedSilently(checked: Boolean) {
        markAsReadCheckbox.setOnCheckedChangeListener(null)
        if (markAsReadCheckbox.isChecked != checked) {
            markAsReadCheckbox.isChecked = checked
            updateReadStatus(checked)
        } else {
            updateReadStatus(checked)
        }
        autoCheckScheduled = false
        setupCheckboxListener()
    }

    private fun showLoadingSpinner() {
        loadingSpinnerLayout.visibility = View.VISIBLE
        topicContent.visibility = View.GONE
    }

    private fun hideLoadingSpinner() {
        loadingSpinnerLayout.visibility = View.GONE
        topicContent.visibility = View.VISIBLE
    }

    private fun showNoTopicsState(message: String) {
        hideLoadingSpinner()
        setCheckboxCheckedSilently(false)
        markAsReadCheckbox.isEnabled = false
        readStatus.text = "No topic"
        readStatus.setTextColor(getColor(android.R.color.darker_gray))
        val html = "<html><body style='font-family: sans-serif; padding:16px; color:#555;'>${message}</body></html>"
        topicContent.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        topicProgress.progress = 0
        progressText.text = "0/0"
        previousButton.isEnabled = false
        nextButton.visibility = View.GONE
        completeTopicButton.visibility = View.GONE
    }

    private fun wrapPlainTextIfNeeded(content: String): String {
        val containsHtmlTags = content.contains(Regex("<[^>]*>"))
        return if (!containsHtmlTags) {
            "<div class='plain-text'>$content</div>"
        } else {
            content
        }
    }

    private fun saveStudyTimeToBackend() {
        val url = "http://10.86.199.54/univault/save_study_time.php"
        val queue = Volley.newRequestQueue(this)
        val jsonObject = JSONObject().apply {
            put("student_id", getStudentId())
            put("course_code", courseCode)
            put("study_time_millis", totalStudyTimeMillis)
            put("mode", when (selectedMode) {
                "PASS" -> "pass"
                "MASTER" -> "master"
                else -> "all"
            })
        }
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                if (response.optBoolean("success", false)) {
                    Log.d("ReadingActivity", "Study time saved to backend: ${response.optString("message")}")
                } else {
                    Log.e("ReadingActivity", "Backend error saving study time: ${response.optString("error")}")
                }
            },
            { error ->
                Log.e("ReadingActivity", "Failed to save study time to backend: ${error.message}")
            }
        )
        queue.add(jsonObjectRequest)
    }

    private fun loadStudyTimeFromBackend() {
        val studentId = getStudentId()
        if (studentId <= 0) {
            Log.w("ReadingActivity", "Invalid student ID, cannot load study time from backend")
            return
        }

        val apiMode = when (selectedMode) {
            "PASS" -> "pass"
            "MASTER" -> "master"
            else -> "all"
        }

        val url = "http://10.86.199.54/univault/get_study_time.php?student_id=$studentId&course_code=$courseCode&mode=$apiMode"
        val queue = Volley.newRequestQueue(this)
        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optBoolean("success", false)) {
                        val backendStudyTime = jsonResponse.optLong("total_study_time_millis", 0)
                        if (backendStudyTime > totalStudyTimeMillis) {
                            totalStudyTimeMillis = backendStudyTime
                            val sharedPreferences = getSharedPreferences("study_time_prefs", Context.MODE_PRIVATE)
                            sharedPreferences.edit()
                                .putLong("total_study_time_$courseCode", totalStudyTimeMillis)
                                .apply()
                            runOnUiThread {
                                updateStudyTimeDisplay()
                            }
                        }
                        Log.d("ReadingActivity", "Study time loaded from backend: ${jsonResponse.optString("formatted_time")}")
                    } else {
                        Log.w("ReadingActivity", "Backend response: ${jsonResponse.optString("error", "Unknown error")}")
                    }
                } catch (e: JSONException) {
                    Log.e("ReadingActivity", "Error parsing study time response: ${e.message}")
                }
            },
            { error ->
                Log.e("ReadingActivity", "Failed to load study time from backend: ${error.message}")
            }
        ) {}
        queue.add(stringRequest)
    }

    // ==================== NOTES FUNCTIONALITY ====================

    private fun loadTopicNotes() {
        val prefs = getSharedPreferences("TopicNotes", MODE_PRIVATE)
        val notesData = prefs.all
        topicNotes.clear()
        
        for ((key, value) in notesData) {
            if (value is String) {
                topicNotes[key] = value
            }
        }
        
        updateNotesButtonAppearance()
        Log.d("ReadingActivity", "Loaded ${topicNotes.size} topic notes from cache")
    }

    private fun saveTopicNotes() {
        val prefs = getSharedPreferences("TopicNotes", MODE_PRIVATE)
        val editor = prefs.edit()
        
        for ((key, value) in topicNotes) {
            editor.putString(key, value)
        }
        
        editor.apply()
        Log.d("ReadingActivity", "Saved ${topicNotes.size} topic notes to cache")
    }

    private fun getCurrentTopicNotesKey(): String {
        val topicId = currentTopic?.id ?: "unknown"
        return "${courseCode}_${selectedMode}_${topicId}"
    }

    private fun getCurrentTopicNotes(): String {
        val key = getCurrentTopicNotesKey()
        return topicNotes[key] ?: ""
    }

    private fun saveCurrentTopicNotes(notes: String) {
        val key = getCurrentTopicNotesKey()
        if (notes.trim().isEmpty()) {
            topicNotes.remove(key)
        } else {
            topicNotes[key] = notes
        }
        saveTopicNotes()
        updateNotesButtonAppearance()
    }

    private fun updateNotesButtonAppearance() {
        if (::notesButton.isInitialized) {
            val hasNotes = getCurrentTopicNotes().isNotEmpty()
            if (hasNotes) {
                notesButton.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                notesButton.alpha = 1.0f
            } else {
                notesButton.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
                notesButton.alpha = 0.7f
            }
        }
    }

    private fun showNotesDialog() {
        if (notesPopup?.isShowing == true) {
            notesPopup?.dismiss()
            return
        }

        val context = this
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        
        // Create main container with animated notepad design
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 20.dpToPx(), 24.dpToPx(), 20.dpToPx())
            background = GradientDrawable().apply {
                // Notepad-like gradient background
                colors = intArrayOf(
                    Color.parseColor("#FEFEFE"),
                    Color.parseColor("#F8F8F0")
                )
                gradientType = GradientDrawable.LINEAR_GRADIENT
                cornerRadius = 16.dpToPx().toFloat()
                setStroke(2.dpToPx(), Color.parseColor("#E0E0E0"))
            }
            elevation = 20f
        }

        // Header with notepad styling
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 16.dpToPx())
        }

        val noteIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_edit)
            setColorFilter(Color.parseColor("#FF9800"))
            layoutParams = LinearLayout.LayoutParams(32.dpToPx(), 32.dpToPx()).apply {
                marginEnd = 12.dpToPx()
            }
        }

        val titleText = TextView(context).apply {
            text = "✏️ Topic Notes"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#F5F5F5"))
                setStroke(1.dpToPx(), Color.parseColor("#E0E0E0"))
            }
            setColorFilter(Color.parseColor("#757575"))
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            layoutParams = LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx())
            setOnClickListener {
                hideNotesDialog()
            }
        }

        header.addView(noteIcon)
        header.addView(titleText)
        header.addView(closeButton)

        // Topic name display
        val topicLabel = TextView(context).apply {
            text = "📖 ${currentTopic?.name ?: "Current Topic"}"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 0, 0, 12.dpToPx())
            setTypeface(null, Typeface.ITALIC)
        }

        // Ruled lines background for notepad effect
        val notesContainer = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FEFEFE"))
                cornerRadius = 12.dpToPx().toFloat()
                setStroke(1.dpToPx(), Color.parseColor("#E8E8E8"))
            }
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        // Create ruled lines background
        val ruledBackground = View(context).apply {
            background = object : Drawable() {
                override fun draw(canvas: Canvas) {
                    val paint = Paint().apply {
                        color = Color.parseColor("#E8F4FD")
                        strokeWidth = 1.dpToPx().toFloat()
                        isAntiAlias = true
                    }
                    val lineHeight = 24.dpToPx().toFloat()
                    var y = lineHeight
                    while (y < bounds.height()) {
                        canvas.drawLine(0f, y, bounds.width().toFloat(), y, paint)
                        y += lineHeight
                    }
                    // Left margin line
                    paint.color = Color.parseColor("#FFE0E0")
                    paint.strokeWidth = 2.dpToPx().toFloat()
                    canvas.drawLine(40.dpToPx().toFloat(), 0f, 40.dpToPx().toFloat(), bounds.height().toFloat(), paint)
                }
                
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: ColorFilter?) {}
                override fun getOpacity(): Int = PixelFormat.OPAQUE
            }
        }

        // Notes EditText with notepad styling
        val notesEditText = EditText(context).apply {
            setText(getCurrentTopicNotes())
            hint = "Write your notes here...\n\n• Key points\n• Important concepts\n• Questions to review\n• Personal insights"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = Gravity.TOP or Gravity.START
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setHintTextColor(Color.parseColor("#999999"))
            background = null
            setPadding(44.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx()) // Left padding for margin line
            setLineSpacing(8.dpToPx().toFloat(), 1.0f)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                300.dpToPx()
            )
            
            // Custom selection handles color
            try {
                val field = TextView::class.java.getDeclaredField("mCursorDrawableRes")
                field.isAccessible = true
                field.set(this, android.R.drawable.editbox_background)
            } catch (e: Exception) {
                Log.w("ReadingActivity", "Could not customize cursor: ${e.message}")
            }
        }

        notesContainer.addView(ruledBackground)
        notesContainer.addView(notesEditText)

        // Action buttons
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16.dpToPx(), 0, 0)
        }

        fun createActionButton(text: String, bgColor: String, textColor: String, onClick: () -> Unit): Button {
            return Button(context).apply {
                this.text = text
                textSize = 14f
                setTextColor(Color.parseColor(textColor))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(bgColor))
                    cornerRadius = 20.dpToPx().toFloat()
                    setStroke(1.dpToPx(), Color.parseColor("#E0E0E0"))
                }
                setPadding(24.dpToPx(), 12.dpToPx(), 24.dpToPx(), 12.dpToPx())
                setOnClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                ).apply {
                    marginStart = 8.dpToPx()
                    marginEnd = 8.dpToPx()
                }
                elevation = 4f
            }
        }

        val saveButton = createActionButton(
            "💾 Save", "#4CAF50", "#FFFFFF"
        ) {
            val notes = notesEditText.text.toString()
            saveCurrentTopicNotes(notes)
            Toast.makeText(context, "Notes saved! 📝", Toast.LENGTH_SHORT).show()
            hideNotesDialog()
        }

        val clearButton = createActionButton(
            "🗑️ Clear", "#FF5722", "#FFFFFF"
        ) {
            AlertDialog.Builder(context)
                .setTitle("Clear Notes")
                .setMessage("Are you sure you want to clear all notes for this topic?")
                .setPositiveButton("Clear") { _, _ ->
                    notesEditText.setText("")
                    saveCurrentTopicNotes("")
                    Toast.makeText(context, "Notes cleared! 🗑️", Toast.LENGTH_SHORT).show()
                    hideNotesDialog()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val cancelButton = createActionButton(
            "❌ Cancel", "#757575", "#FFFFFF"
        ) {
            hideNotesDialog()
        }

        buttonContainer.addView(saveButton)
        buttonContainer.addView(clearButton)
        buttonContainer.addView(cancelButton)

        // Assemble the dialog
        container.addView(header)
        container.addView(topicLabel)
        container.addView(notesContainer)
        container.addView(buttonContainer)

        // Create and show popup with animation
        notesPopup = PopupWindow(
            container,
            (rootView.width * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 20f
            isOutsideTouchable = true
            isFocusable = true
            animationStyle = android.R.style.Animation_Dialog
        }

        // Show with slide-in animation
        rootView.post {
            container.alpha = 0f
            container.scaleX = 0.8f
            container.scaleY = 0.8f
            
            notesPopup?.showAtLocation(
                rootView,
                Gravity.CENTER,
                0,
                0
            )
            
            // Animate in
            container.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
                
            // Focus on EditText and show keyboard
            notesEditText.requestFocus()
            notesEditText.postDelayed({
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(notesEditText, InputMethodManager.SHOW_IMPLICIT)
            }, 200)
        }
        
        Log.d("ReadingActivity", "Notes dialog shown for topic: ${currentTopic?.name}")
    }

    private fun hideNotesDialog() {
        notesPopup?.let { popup ->
            if (popup.isShowing) {
                val contentView = popup.contentView
                contentView.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(200)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        popup.dismiss()
                        notesPopup = null
                    }
                    .start()
            }
        }
        
        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = currentFocus
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
        
        Log.d("ReadingActivity", "Notes dialog hidden")
    }
}