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
import org.json.JSONArray

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
    private var currentReadingMode: String = "light" // light, dark, sepia, high_contrast
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
        topicContent.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        // Enable pinch-to-zoom
        topicContent.settings.setSupportZoom(true)
        topicContent.settings.builtInZoomControls = false
        topicContent.settings.displayZoomControls = false

        // Enable text selection
        topicContent.isLongClickable = true
        topicContent.isFocusable = true
        topicContent.isFocusableInTouchMode = true

        // Add JavaScript interface for annotations
        topicContent.addJavascriptInterface(AnnotationBridge(), "AndroidAnnotator")

        topicContent.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectAnnotationJS()
                renderSavedAnnotations()
            }
        }

        topicProgress = findViewById(R.id.topicProgress)
        progressText = findViewById(R.id.progressText)
        markAsReadCheckbox = findViewById(R.id.markAsReadCheckbox)
        readStatus = findViewById(R.id.readStatus)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        completeTopicButton = findViewById(R.id.completeTopicButton)
        loadingSpinnerLayout = findViewById(R.id.loadingSpinnerLayout)
        studyTimeText = findViewById(R.id.studyTimeText)

        // Setup scroll listener for progress tracking
        setupScrollListener()
    }

    private fun initializeEnhancedFeatures() {
        // Initialize TTS
        initializeTTS()

        // Initialize scale gesture detector for pinch-to-zoom
        initializeScaleGestureDetector()

        // Setup enhanced UI controls
        setupReadingControls()

        // Load saved preferences
        loadReadingPreferences()

        // Load bookmarks
        loadBookmarks()

        // Load annotations
        loadAnnotations()
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
            }
        }
    }

    private fun initializeScaleGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                currentZoomLevel *= scaleFactor
                currentZoomLevel = currentZoomLevel.coerceIn(0.5f, 3.0f)

                // Apply zoom to WebView using textZoom
                val textZoom = (currentZoomLevel * 100).toInt()
                topicContent.settings.textZoom = textZoom
                updateZoomButtons()

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

    // Annotation persistence
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

    // Enhanced Reading Features
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
        val js = """
            var searchTerm = '$query';
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
                    2 -> exportAnnotations()
                    3 -> importAnnotations()
                    4 -> showAboutDialog()
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
                Enhanced Reading Activity v2.0
                
                Features:
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


    // JavaScript Interface for Annotations
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

    // Inject JavaScript for Annotations
    private fun injectAnnotationJS() {
        val js = """
            (function() {
                if (window.__annotatorInstalled) return;
                window.__annotatorInstalled = true;

                function getSelectedOffsets() {
                    var selection = window.getSelection();
                    if (!selection || selection.rangeCount === 0) return null;
                    var range = selection.getRangeAt(0);
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
                        currentOffset += node.nodeValue.length;
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
                        var nodeEnd = currentOffset + node.nodeValue.length;
                        var s = Math.max(start, nodeStart);
                        var e = Math.min(end, nodeEnd);
                        if (e > s) {
                            ranges.push({node: node, s: s - nodeStart, e: e - nodeStart});
                        }
                        currentOffset += nodeEnd;
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
                            window.AndroidAnnotator.onAnnotationTapped(id);
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
                    }
                };
            })();
        """.trimIndent()
        topicContent.evaluateJavascript(js, null)
    }

    private fun renderSavedAnnotations() {
        annotations.filter { it.contentId == currentContentId() }.forEach { ann ->
            val js = """
                AnnotatorJS.applyAnnotation('${ann.id}', '${ann.type}', ${ann.start}, ${ann.end}, '${ann.color ?: ""}', '${ann.noteText ?: ""}');
            """
            topicContent.evaluateJavascript(js, null)
        }
    }

    private fun handleSelectionAction(itemId: Int) {
        topicContent.evaluateJavascript("AnnotatorJS.getSelectedOffsets();") { result ->
            try {
                if (result == "null") return@evaluateJavascript
                val json = JSONObject(result)
                val start = json.getInt("start")
                val end = json.getInt("end")
                val text = json.getString("text")
                if (end <= start || text.isBlank()) return@evaluateJavascript

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
                            topicContent.evaluateJavascript(
                                "AnnotatorJS.applyAnnotation('$annotationId', 'note', $start, $end, '', '$noteText');",
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
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
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

    private fun hasUserScrolledToMarkAsRead(): Boolean {
        return hasScrolledToMarkAsRead
    }

    private fun showLoadingSpinner() {
        loadingSpinnerLayout.visibility = View.VISIBLE
        topicContent.visibility = View.GONE
    }

    private fun hideLoadingSpinner() {
        loadingSpinnerLayout.visibility = View.GONE
        topicContent.visibility = View.VISIBLE
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

        Log.d("ReadingActivity", "Saved course data - Code: $courseCode, Name: $courseTitle, Mode: $selectedMode")
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
    }

    private fun setupCheckboxListener() {
        markAsReadCheckbox.setOnCheckedChangeListener { _, isChecked ->
            currentTopic?.let { topic ->
                if (isChecked) {
                    updateTopicReadStatus(topic.id, true)
                    updateReadStatus(true)
                    Log.d("ReadingActivity", "User marked topic as read: ${topic.name}")
                } else {
                    updateTopicReadStatus(topic.id, false)
                    updateReadStatus(false)
                    hasScrolledToMarkAsRead = false
                    Log.d("ReadingActivity", "User manually unchecked topic: ${topic.name}")
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
                }, 2000)
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
            previousButton.layoutParams = (previousButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
            completeTopicButton.layoutParams = (completeTopicButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
        } else {
            nextButton.visibility = View.VISIBLE
            nextButton.isEnabled = currentTopicIndex < topics.size - 1
            completeTopicButton.visibility = View.GONE
            previousButton.layoutParams = (previousButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
            nextButton.layoutParams = (nextButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
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
            .setPositiveButton("View Progress") { _, _ ->
                finish()
            }
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

        val url = "http://192.168.56.1/univault/get_topics.php?course_code=$courseId&mode=$apiMode"
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
                        showLoadingSpinner()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            showNoTopicsState("📚 No topics found for this course and mode.\n\nPlease check back later or contact your instructor.")
                            Toast.makeText(this, "No topics found", Toast.LENGTH_SHORT).show()
                        }, 2000)
                    }
                } catch (arrayEx: JSONException) {
                    try {
                        val obj = JSONObject(body)
                        val errorMsg = obj.optString("error", "No topics found")
                        showLoadingSpinner()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            showNoTopicsState("📚 $errorMsg\n\nPlease check back later or contact your instructor.")
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                        }, 2000)
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
        val url = "http://192.168.56.1/univault/update_topic_progress.php"

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
    }

    private fun updateStudyTime() {
        if (sessionStartTime > 0) {
            val currentTime = System.currentTimeMillis()
            val sessionDuration = currentTime - sessionStartTime

            if (sessionDuration > 0) {
                totalStudyTimeMillis += sessionDuration
                saveStudyTime()

                // Assuming HomeFragment1 is defined elsewhere
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
        val url = "http://192.168.56.1/univault/save_study_time.php"

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

        val url = "http://192.168.56.1/univault/get_study_time.php?student_id=$studentId&course_code=$courseCode&mode=$apiMode"

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
}