package com.simats.univault

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ActionMode
import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupWindow
import androidx.core.view.doOnLayout
import org.json.JSONArray
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Build
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.WindowManager
import android.media.AudioManager
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.ForegroundColorSpan

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
    
    // Annotation/Drawing
    private lateinit var drawingOverlay: DrawingView
    private var isPenModeEnabled: Boolean = false
    private var currentPenColor: Int = Color.RED
    private var currentPenStrokeWidthPx: Float = 6f
    private var currentHighlightColorCss: String = "#fff59d" // yellow 300
    private var currentUnderlineColorCss: String = "#ff8a65" // deep orange 300
    private var currentStrikethroughColorCss: String = "#ef9a9a" // red 200
    private var floatingToolbar: PopupWindow? = null
    private var isEditToolbarVisible: Boolean = false
    private lateinit var editModeButton: ImageButton
    private var selectionDialog: androidx.appcompat.app.AlertDialog? = null
    private var selectionToolbar: PopupWindow? = null
    private var currentSelection: Triple<Int, Int, String>? = null // start, end, text
    
    // Enhanced annotation tools
    private var currentShape: String = "pen" // pen, rectangle, circle, arrow, line
    private var currentTextAnnotation: String = ""
    private var annotationOpacity: Float = 0.7f
    private var availableColors: List<Int> = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA,
        Color.CYAN, Color.parseColor("#FFA726"), Color.parseColor("#9C27B0"),
        Color.parseColor("#607D8B"), Color.parseColor("#795548")
    )
    
    // Cache keys
    private val annotationsPrefsName = "AnnotationsPrefs"
    
    // Data class for Topic
    data class Topic(
        val id: String,
        val name: String,
        val content: String,
        val isRead: Boolean = false
    )
    
    // Annotation model
    data class Annotation(
        val id: String,
        val contentId: String,
        val userId: Int,
        val type: String, // highlight | underline | note | pen
        val startOffset: Int? = null,
        val endOffset: Int? = null,
        val color: String? = null, // CSS color or hex
        val timestamp: Long = System.currentTimeMillis(),
        val noteText: String? = null,
        val penData: String? = null // stringified JSON of strokes
    )
    
    private data class Stroke(val color: Int, val widthPx: Float, val points: MutableList<PointF>)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading)
        // Allow standard text selection behavior
        
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

    // Removed onActionModeStarted override to allow standard text selection
    
    private fun initializeViews() {
        courseName = findViewById(R.id.courseName)
        topicName = findViewById(R.id.topicName)
        topicContent = findViewById(R.id.topicWebView)
        topicContent.settings.loadsImagesAutomatically = true
        topicContent.settings.javaScriptEnabled = true
        topicContent.settings.defaultTextEncodingName = "utf-8"
        topicContent.settings.domStorageEnabled = true
        topicContent.settings.builtInZoomControls = false // We'll use custom controls
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
        topicContent.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript("document.body.scrollHeight.toString()") { result ->
                    val px = result?.replace("\"", "")?.toFloatOrNull()?.times(resources.displayMetrics.density)
                    if (px != null && px > 0 && view != null) {
                        val params = view.layoutParams
                        params.height = px.toInt()
                        view.layoutParams = params
                    }
                }
                // Inject annotator JS helpers after content ready
                injectAnnotatorJavascript()
                // Render any saved annotations for this content
                renderSavedAnnotations()
            }
        }
        // Enable standard text selection behavior
        topicProgress = findViewById(R.id.topicProgress)
        progressText = findViewById(R.id.progressText)
        markAsReadCheckbox = findViewById(R.id.markAsReadCheckbox)
        readStatus = findViewById(R.id.readStatus)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        completeTopicButton = findViewById(R.id.completeTopicButton)
        loadingSpinnerLayout = findViewById(R.id.loadingSpinnerLayout)
        studyTimeText = findViewById(R.id.studyTimeText)
        
        // JS bridge for selection callbacks
        topicContent.addJavascriptInterface(SelectionBridge(), "AndroidAnnotator")
        
        // Add drawing overlay above content
        addDrawingOverlay()
        
        // Setup floating toolbar UI and Edit toggle button
        setupFloatingToolbar()
        setupSelectionToolbar()
        setupEditModeButton()
        
        // Setup scroll listener for progress tracking
        setupScrollListener()
    }
    
    private fun initializeEnhancedFeatures() {
        // Initialize TTS
        initializeTTS()
        
        // Initialize scale gesture detector for pinch-to-zoom
        initializeScaleGestureDetector()
        
        // Setup enhanced UI controls first
        setupReadingControls()
        
        // Load saved preferences after UI is ready
        loadReadingPreferences()
        
        // Load bookmarks
        loadBookmarks()
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
        
        // Override touch events to handle scale gestures
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
        
        // Apply loaded preferences
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
        // Apply font size and family
        updateWebViewStyling()
        
        // Apply reading mode
        applyReadingMode(currentReadingMode)
        
        // Apply zoom level
        val textZoom = (currentZoomLevel * 100).toInt()
        topicContent.settings.textZoom = textZoom
        updateZoomButtons()
    }
    
    private fun setupReadingControls() {
        // Create reading controls layout
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
        
        // Create control buttons
        zoomInButton = createControlButton(android.R.drawable.ic_menu_zoom, "Zoom In") { zoomIn() }
        zoomOutButton = createControlButton(android.R.drawable.ic_menu_zoom, "Zoom Out") { zoomOut() }
        zoomResetButton = createControlButton(android.R.drawable.ic_menu_revert, "Reset Zoom") { resetZoom() }
        fontSizeButton = createControlButton(android.R.drawable.ic_menu_edit, "Font Size") { showFontSizeDialog() }
        fontFamilyButton = createControlButton(android.R.drawable.ic_menu_sort_by_size, "Font Family") { showFontFamilyDialog() }
        readingModeButton = createControlButton(android.R.drawable.ic_menu_view, "Reading Mode") { showReadingModeDialog() }
        ttsButton = createControlButton(android.R.drawable.ic_media_play, "Text-to-Speech") { toggleTTS() }
        bookmarkButton = createControlButton(android.R.drawable.ic_menu_slideshow, "Bookmark") { toggleBookmark() }
        searchButton = createControlButton(android.R.drawable.ic_menu_search, "Search") { showSearchDialog() }
        settingsButton = createControlButton(android.R.drawable.ic_menu_preferences, "Settings") { showSettingsDialog() }
        
        // Add buttons to layout
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
        
        // Add to root layout
        root.addView(readingControlsLayout, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        
        // Position at top
        readingControlsLayout.y = 0f
        readingControlsLayout.visibility = View.GONE // Initially hidden
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
    
    // ===== Enhanced Reading Features =====
    
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
        
        androidx.appcompat.app.AlertDialog.Builder(this)
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
        
        androidx.appcompat.app.AlertDialog.Builder(this)
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
        
        androidx.appcompat.app.AlertDialog.Builder(this)
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
            val text = topic.content.replace(Regex("<[^>]*>"), " ") // Remove HTML tags
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
        
        androidx.appcompat.app.AlertDialog.Builder(this)
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
            
            // Scroll to first match
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
        
        androidx.appcompat.app.AlertDialog.Builder(this)
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
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("This will reset all reading preferences, bookmarks, and annotations. Are you sure?")
            .setPositiveButton("Reset") { _, _ ->
                // Reset all preferences
                getSharedPreferences("ReadingPreferences", MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences("Bookmarks", MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences(annotationsPrefsName, MODE_PRIVATE).edit().clear().apply()
                
                // Reset variables
                currentFontSize = 16
                currentFontFamily = "Arial"
                currentReadingMode = "light"
                currentZoomLevel = 1.0f
                bookmarks.clear()
                
                // Apply reset settings
                applyReadingPreferences()
                updateBookmarkButton()
                
                Toast.makeText(this, "All settings reset", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportAnnotations() {
        // Implementation for exporting annotations
        Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun importAnnotations() {
        // Implementation for importing annotations
        Toast.makeText(this, "Import feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
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
                • Advanced annotation tools
                • Responsive UI controls
                
                Developed for UniValut
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun addDrawingOverlay() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        drawingOverlay = DrawingView(this)
        drawingOverlay.visibility = View.GONE
        root.addView(
            drawingOverlay,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    private fun setupFloatingToolbar() {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12.dpToPx(), 10.dpToPx(), 12.dpToPx(), 10.dpToPx())
            // Rounded, elevated background for nicer UI
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F9FFFFFF"))
                cornerRadius = 16.dpToPx().toFloat() 
                setStroke(1.dpToPx(), Color.parseColor("#22000000"))
            }
            elevation = 12f
        }

        fun makeIconButton(iconRes: Int, contentDesc: String, onClick: () -> Unit): ImageButton {
            return ImageButton(context).apply {
                setImageResource(iconRes)
                contentDescription = contentDesc
                background = null
                setOnClickListener { onClick() }
                val lp = LinearLayout.LayoutParams(40.dpToPx(), 40.dpToPx())
                lp.marginStart = 8.dpToPx()
                lp.marginEnd = 8.dpToPx()
                layoutParams = lp
            }
        }

        // Use built-in icons for simplicity
        val btnHl = makeIconButton(android.R.drawable.ic_menu_set_as, "Highlight") { onHighlightClicked() }
        val btnUl = makeIconButton(android.R.drawable.ic_menu_edit, "Underline") { onUnderlineClicked() }
        val btnPen = makeIconButton(android.R.drawable.presence_online, "Pen Mode") { togglePenMode(); updatePenButtonState(this) }
        val btnErase = makeIconButton(android.R.drawable.ic_menu_delete, "Erase Last") { onEraseClicked() }
        val btnColor = makeIconButton(android.R.drawable.ic_menu_manage, "Change Color") { cycleColors() }
        val btnShape = makeIconButton(android.R.drawable.ic_menu_crop, "Shapes") { showShapeDialog() }
        val btnText = makeIconButton(android.R.drawable.ic_menu_agenda, "Add Text") { showTextAnnotationDialog() }
        val btnControls = makeIconButton(android.R.drawable.ic_menu_preferences, "Reading Controls") { toggleReadingControls() }

        container.addView(btnHl)
        container.addView(btnUl)
        container.addView(btnPen)
        container.addView(btnErase)
        container.addView(btnColor)
        container.addView(btnShape)
        container.addView(btnText)
        container.addView(btnControls)

        floatingToolbar = PopupWindow(container, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = 16f
            isOutsideTouchable = true
        }
    }

    private fun setupSelectionToolbar() {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 12.dpToPx())
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F5FFFFFF"))
                cornerRadius = 20.dpToPx().toFloat()
                setStroke(2.dpToPx(), Color.parseColor("#E0E0E0"))
            }
            elevation = 20f
        }

        fun makeIconButton(iconRes: Int, desc: String, handler: () -> Unit): ImageButton {
            return ImageButton(context).apply {
                setImageResource(iconRes)
                contentDescription = desc
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#FFFFFF"))
                    cornerRadius = 12.dpToPx().toFloat()
                    setStroke(1.dpToPx(), Color.parseColor("#E0E0E0"))
                }
                setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
                setOnClickListener { 
                    handler()
                    hideSelectionToolbar() 
                }
                layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx()).apply {
                    marginStart = 6.dpToPx()
                    marginEnd = 6.dpToPx()
                }
            }
        }

        val btnHighlight = makeIconButton(android.R.drawable.ic_menu_set_as, "Highlight") {
            currentSelection?.let { (start, end, text) ->
                val ann = Annotation(
                    id = generateAnnotationId(),
                    contentId = currentContentId(),
                    userId = getStudentId(),
                    type = "highlight",
                    startOffset = start,
                    endOffset = end,
                    color = currentHighlightColorCss
                )
                saveAnnotation(ann)
                applyAnnotationInWebView(ann)
                Toast.makeText(context, "✓ Text highlighted", Toast.LENGTH_SHORT).show()
                // Clear selection after annotation
                clearSelection()
            } ?: run {
                Toast.makeText(context, "Please select text first", Toast.LENGTH_SHORT).show()
            }
        }

        val btnUnderline = makeIconButton(android.R.drawable.ic_menu_edit, "Underline") {
            currentSelection?.let { (start, end, text) ->
                val ann = Annotation(
                    id = generateAnnotationId(),
                    contentId = currentContentId(),
                    userId = getStudentId(),
                    type = "underline",
                    startOffset = start,
                    endOffset = end,
                    color = currentUnderlineColorCss
                )
                saveAnnotation(ann)
                applyAnnotationInWebView(ann)
                Toast.makeText(context, "✓ Text underlined", Toast.LENGTH_SHORT).show()
                // Clear selection after annotation
                clearSelection()
            } ?: run {
                Toast.makeText(context, "Please select text first", Toast.LENGTH_SHORT).show()
            }
        }

        val btnNote = makeIconButton(android.R.drawable.ic_menu_agenda, "Add Note") {
            currentSelection?.let { (start, end, text) ->
                showNoteDialog(initial = "") { noteText ->
                    val ann = Annotation(
                        id = generateAnnotationId(),
                        contentId = currentContentId(),
                        userId = getStudentId(),
                        type = "note",
                        startOffset = start,
                        endOffset = end,
                        noteText = noteText
                    )
                    saveAnnotation(ann)
                    applyAnnotationInWebView(ann)
                    Toast.makeText(context, "Note added", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(context, "Please select text first", Toast.LENGTH_SHORT).show()
            }
        }

        container.addView(btnHighlight)
        container.addView(btnUnderline)
        container.addView(btnNote)

        selectionToolbar = PopupWindow(container, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = 16f
            isOutsideTouchable = true
        }
    }

    private fun updatePenButtonState(context: Context) {
        // No-op placeholder for future dynamic UI updates (e.g., tint when active)
    }

    private fun showToolbarAt(x: Int, y: Int) {
        val root = findViewById<View>(android.R.id.content)
        root.post {
            val pw = floatingToolbar ?: return@post
            if (!pw.isShowing) {
                pw.showAtLocation(root, Gravity.TOP or Gravity.START, (x - 40.dpToPx()).coerceAtLeast(0), (y - 80.dpToPx()).coerceAtLeast(0))
            }
        }
    }

    private fun showToolbarAtSelection(selCenterXpx: Int, selTopYpx: Int) {
        val root = findViewById<View>(android.R.id.content) ?: return
        // Convert WebView document coords to screen coords: WebView is inside the layout, so offset by WebView location
        val webLoc = IntArray(2)
        topicContent.getLocationOnScreen(webLoc)
        val screenX = webLoc[0] + selCenterXpx
        // Position slightly BELOW the selection instead of above
        val screenY = webLoc[1] + selTopYpx + 12.dpToPx()
        showSelectionToolbar(screenX, screenY)
    }

    private fun showToolbarBottomCentered() {
        val root = findViewById<View>(android.R.id.content)
        root.post {
            val pw = floatingToolbar ?: return@post
            if (!pw.isShowing) {
                pw.showAtLocation(root, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 24.dpToPx())
            }
        }
    }

    private fun hideToolbar() {
        floatingToolbar?.dismiss()
    }

    private fun showSelectionToolbar(x: Int, y: Int) {
        val root = findViewById<View>(android.R.id.content)
        root.post {
            val pw = selectionToolbar ?: return@post
            if (pw.isShowing) pw.dismiss()
            pw.showAtLocation(root, Gravity.TOP or Gravity.START, (x - pw.contentView.measuredWidth / 2).coerceAtLeast(0), (y - 56.dpToPx()).coerceAtLeast(0))
        }
    }

    private fun hideSelectionToolbar() {
        selectionToolbar?.dismiss()
    }

    private fun clearSelection() {
        topicContent.evaluateJavascript("window.getSelection().removeAllRanges();", null)
        currentSelection = null
    }

    private fun setupEditModeButton() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        editModeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_edit)
            contentDescription = "Edit / Annotate"
            background = getSelectableBackgroundBorderless()
            setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
            setOnClickListener { toggleEditToolbar() }
        }

        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(editModeButton, params)

        // Position the edit button above the Next button
        root.doOnLayout { positionEditButtonNearNext() }
        nextButton.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> positionEditButtonNearNext() }
        topicContent.viewTreeObserver.addOnScrollChangedListener { positionEditButtonNearNext() }
    }

    private fun getSelectableBackgroundBorderless(): android.graphics.drawable.Drawable? {
        val outValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        return androidx.core.content.ContextCompat.getDrawable(this, outValue.resourceId)
    }

    private fun toggleEditToolbar() {
        isEditToolbarVisible = !isEditToolbarVisible
        if (isEditToolbarVisible) {
            if (isPenModeEnabled) togglePenMode() // ensure overlay state is consistent
            showToolbarBottomCentered()
            Toast.makeText(this, "Annotation tools enabled", Toast.LENGTH_SHORT).show()
        } else {
            if (isPenModeEnabled) togglePenMode()
            hideToolbar()
            Toast.makeText(this, "Annotation tools hidden", Toast.LENGTH_SHORT).show()
        }
    }

    private fun positionEditButtonNearNext() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        if (!::nextButton.isInitialized || !::editModeButton.isInitialized) return
        if (nextButton.visibility != View.VISIBLE) return
        root.post {
            val rootLoc = IntArray(2)
            val nextLoc = IntArray(2)
            root.getLocationOnScreen(rootLoc)
            nextButton.getLocationOnScreen(nextLoc)

            val desiredX = (nextLoc[0] - rootLoc[0] + nextButton.width - editModeButton.width).coerceAtLeast(8.dpToPx())
            val desiredY = (nextLoc[1] - rootLoc[1] - editModeButton.height - 12.dpToPx()).coerceAtLeast(8.dpToPx())

            editModeButton.x = desiredX.toFloat()
            editModeButton.y = desiredY.toFloat()
        }
    }

    private fun onHighlightClicked() {
        evaluateGetSelectionOffsets { start, end, selectedText ->
            if (start != null && end != null && end > start) {
                val ann = Annotation(
                    id = generateAnnotationId(),
                    contentId = currentContentId(),
                    userId = getStudentId(),
                    type = "highlight",
                    startOffset = start,
                    endOffset = end,
                    color = currentHighlightColorCss
                )
                saveAnnotation(ann)
                applyAnnotationInWebView(ann)
            }
        }
    }

    private fun onUnderlineClicked() {
        evaluateGetSelectionOffsets { start, end, _ ->
            if (start != null && end != null && end > start) {
                val ann = Annotation(
                    id = generateAnnotationId(),
                    contentId = currentContentId(),
                    userId = getStudentId(),
                    type = "underline",
                    startOffset = start,
                    endOffset = end,
                    color = currentUnderlineColorCss
                )
                saveAnnotation(ann)
                applyAnnotationInWebView(ann)
            }
        }
    }

    private fun onEraseClicked() {
        // Erase the last annotation for simplicity
        val list = getAnnotationsForContent(currentContentId()).toMutableList()
        if (list.isNotEmpty()) {
            val removed = list.removeLast()
            persistAnnotations(currentContentId(), list)
            removeAnnotationInWebView(removed.id)
            Toast.makeText(this, "Annotation removed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePenMode() {
        isPenModeEnabled = !isPenModeEnabled
        drawingOverlay.visibility = if (isPenModeEnabled) View.VISIBLE else View.GONE
        drawingOverlay.setDrawingEnabled(isPenModeEnabled)
        if (!isPenModeEnabled) {
            // Save current strokes as a pen annotation
            val strokes = drawingOverlay.exportStrokesAsJson()
            if (strokes.isNotEmpty()) {
                val ann = Annotation(
                    id = generateAnnotationId(),
                    contentId = currentContentId(),
                    userId = getStudentId(),
                    type = "pen",
                    color = String.format("#%06X", 0xFFFFFF and currentPenColor),
                    penData = strokes
                )
                saveAnnotation(ann)
            }
            drawingOverlay.clear()
        } else {
            drawingOverlay.updatePaint(currentPenColor, currentPenStrokeWidthPx)
        }
    }

    private fun cycleColors() {
        // Simple cycle for demo: yellow -> green -> pink -> orange -> blue
        val colorsCss = listOf("#fff59d", "#a5d6a7", "#f48fb1", "#ffcc80", "#90caf9")
        val nextCss = colorsCss[(colorsCss.indexOf(currentHighlightColorCss) + 1).mod(colorsCss.size)]
        currentHighlightColorCss = nextCss
        // Also rotate pen color
        val colorsInt = listOf(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.parseColor("#FFA726"), Color.BLUE)
        val nextInt = colorsInt[(colorsInt.indexOf(currentPenColor).takeIf { it >= 0 } ?: 0 + 1).mod(colorsInt.size)]
        currentPenColor = nextInt
        drawingOverlay.updatePaint(currentPenColor, currentPenStrokeWidthPx)
        Toast.makeText(this, "Color changed", Toast.LENGTH_SHORT).show()
    }
    
    private fun showShapeDialog() {
        val shapes = arrayOf("Pen", "Rectangle", "Circle", "Arrow", "Line", "Highlighter")
        val currentShapeIndex = when (currentShape) {
            "pen" -> 0
            "rectangle" -> 1
            "circle" -> 2
            "arrow" -> 3
            "line" -> 4
            "highlighter" -> 5
            else -> 0
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Drawing Tool")
            .setSingleChoiceItems(shapes, currentShapeIndex) { dialog, which ->
                currentShape = when (which) {
                    0 -> "pen"
                    1 -> "rectangle"
                    2 -> "circle"
                    3 -> "arrow"
                    4 -> "line"
                    5 -> "highlighter"
                    else -> "pen"
                }
                updateDrawingMode()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateDrawingMode() {
        when (currentShape) {
            "highlighter" -> {
                currentPenStrokeWidthPx = 20f
                currentPenColor = Color.YELLOW
                annotationOpacity = 0.3f
            }
            "pen" -> {
                currentPenStrokeWidthPx = 6f
                annotationOpacity = 0.7f
            }
            else -> {
                currentPenStrokeWidthPx = 4f
                annotationOpacity = 0.7f
            }
        }
        drawingOverlay.updatePaint(currentPenColor, currentPenStrokeWidthPx)
        Toast.makeText(this, "Drawing mode: $currentShape", Toast.LENGTH_SHORT).show()
    }
    
    private fun showTextAnnotationDialog() {
        val input = EditText(this).apply {
            hint = "Enter text annotation..."
            setText(currentTextAnnotation)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Text Annotation")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                currentTextAnnotation = input.text.toString()
                if (currentTextAnnotation.isNotBlank()) {
                    addTextAnnotation(currentTextAnnotation)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addTextAnnotation(text: String) {
        // This would add a text annotation at the current position
        // For now, we'll show a toast and could implement a floating text overlay
        Toast.makeText(this, "Text annotation: $text", Toast.LENGTH_SHORT).show()
        
        // You could implement a floating text view that follows touch
        // or add it to the drawing overlay
    }

    private fun currentContentId(): String {
        val topicId = currentTopic?.id ?: "unknown"
        return "${'$'}courseCode|${'$'}selectedMode|${'$'}topicId"
    }

    private fun generateAnnotationId(): String = "ann_${'$'}{System.currentTimeMillis()}_${'$'}{(0..9999).random()}"

    // ===== Annotations persistence =====
    private fun getAnnotationsForContent(contentId: String): List<Annotation> {
        val prefs = getSharedPreferences(annotationsPrefsName, MODE_PRIVATE)
        val json = prefs.getString("ann_${'$'}contentId", "[]")
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Annotation(
                    id = o.optString("id"),
                    contentId = o.optString("contentId"),
                    userId = o.optInt("userId"),
                    type = o.optString("type"),
                    startOffset = if (o.has("startOffset")) o.optInt("startOffset") else null,
                    endOffset = if (o.has("endOffset")) o.optInt("endOffset") else null,
                    color = o.optString("color", null),
                    timestamp = o.optLong("timestamp"),
                    noteText = o.optString("noteText", null),
                    penData = o.optString("penData", null)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistAnnotations(contentId: String, list: List<Annotation>) {
        val prefs = getSharedPreferences(annotationsPrefsName, MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { a ->
            val o = JSONObject()
            o.put("id", a.id)
            o.put("contentId", a.contentId)
            o.put("userId", a.userId)
            o.put("type", a.type)
            if (a.startOffset != null) o.put("startOffset", a.startOffset)
            if (a.endOffset != null) o.put("endOffset", a.endOffset)
            if (a.color != null) o.put("color", a.color)
            o.put("timestamp", a.timestamp)
            if (a.noteText != null) o.put("noteText", a.noteText)
            if (a.penData != null) o.put("penData", a.penData)
            arr.put(o)
        }
        prefs.edit().putString("ann_${'$'}contentId", arr.toString()).apply()
    }

    private fun saveAnnotation(annotation: Annotation) {
        val list = getAnnotationsForContent(annotation.contentId).toMutableList()
        list.add(annotation)
        persistAnnotations(annotation.contentId, list)
        // TODO: enqueue background sync
    }

    private fun renderSavedAnnotations() {
        val contentId = currentContentId()
        val list = getAnnotationsForContent(contentId)
        list.forEach { a ->
            if (a.type == "pen" && !a.penData.isNullOrEmpty()) {
                // For simplicity, skip immediate redraw of pen on HTML; pen is on overlay only when drawing
                // Could be implemented by a bitmap overlay cache if needed
            } else {
                applyAnnotationInWebView(a)
            }
        }
    }

    // ===== WebView JS integration =====
    private inner class SelectionBridge {
        @JavascriptInterface
        fun onSelectionChanged() {
            runOnUiThread {
                // Deprecated path: kept for compatibility if coordinates aren't available
                // no-op; precise toolbar will show via onSelectionAt
            }
        }
        @JavascriptInterface
        fun onAnnotationTapped(id: String) {
            runOnUiThread { showAnnotationEditDialog(id) }
        }
        @JavascriptInterface
        fun onSelectionAt(xPx: Float, yPx: Float) {
            runOnUiThread {
                // Capture the current selection when toolbar is shown
                evaluateGetSelectionOffsets { start, end, text ->
                    if (start != null && end != null && end > start && !text.isNullOrBlank()) {
                        currentSelection = Triple(start, end, text)
                        showToolbarAtSelection(xPx.toInt(), yPx.toInt())
                    }
                }
            }
        }
        @JavascriptInterface
        fun onSelectionCleared() {
            runOnUiThread { 
                currentSelection = null
                hideSelectionToolbar() 
            }
        }
    }

    private fun injectAnnotatorJavascript() {
        val js = """
            (function(){
              if (window.__annotatorInstalled) return; window.__annotatorInstalled = true;
              var __selDebounceTimer = null;
              var __lastSelectionText = '';
              
              function notifySelectionPosition(){
                try {
                  var sel = window.getSelection();
                  if (!sel || sel.rangeCount === 0 || sel.toString().length === 0){
                    if (window.AndroidAnnotator && AndroidAnnotator.onSelectionCleared){ 
                      AndroidAnnotator.onSelectionCleared(); 
                    }
                    return;
                  }
                  
                  var selectedText = sel.toString().trim();
                  if (selectedText.length === 0) {
                    if (window.AndroidAnnotator && AndroidAnnotator.onSelectionCleared){ 
                      AndroidAnnotator.onSelectionCleared(); 
                    }
                    return;
                  }
                  
                  // Only show toolbar if selection has changed
                  if (selectedText !== __lastSelectionText) {
                    __lastSelectionText = selectedText;
                    var r = sel.getRangeAt(0);
                    var rect = r.getBoundingClientRect();
                    var dpr = window.devicePixelRatio || 1;
                    var cx = Math.round((rect.left + rect.right) / 2 * dpr);
                    // Use bottom of selection to position toolbar BELOW the selection
                    var topY = Math.round(rect.bottom * dpr);
                    if (window.AndroidAnnotator && AndroidAnnotator.onSelectionAt){
                      AndroidAnnotator.onSelectionAt(cx, topY);
                    }
                  }
                } catch (e) { 
                  console.log('Selection error:', e);
                }
              }
              function getLinearOffset(node, offsetInNode){
                var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
                var total = 0; var current;
                while(current = walker.nextNode()){
                  if(current === node){ return total + offsetInNode; }
                  total += current.nodeValue.length;
                }
                return total;
              }
              function getSelectionOffsets(){
                var sel = window.getSelection();
                if(!sel || sel.rangeCount===0){ return null; }
                var r = sel.getRangeAt(0);
                var start = getLinearOffset(r.startContainer, r.startOffset);
                var end = getLinearOffset(r.endContainer, r.endOffset);
                if (end < start) { var t = start; start = end; end = t; }
                return {start:start, end:end, text: sel.toString()};
              }
              function wrapRangeLinear(id, start, end, style){
                // Walk text nodes and wrap the [start,end) segment in span
                var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
                var acc = 0; var n;
                var ranges = [];
                while(n = walker.nextNode()){
                  var len = n.nodeValue.length;
                  var nodeStart = acc; var nodeEnd = acc + len;
                  var s = Math.max(start, nodeStart);
                  var e = Math.min(end, nodeEnd);
                  if (e > s){
                    ranges.push({node:n, s:s-nodeStart, e:e-nodeStart});
                  }
                  acc += len;
                  if (acc >= end) break;
                }
                for (var i=0;i<ranges.length;i++){
                  var it = ranges[i];
                  var n = it.node;
                  var text = n.nodeValue;
                  var before = document.createTextNode(text.substring(0, it.s));
                  var middle = document.createTextNode(text.substring(it.s, it.e));
                  var after = document.createTextNode(text.substring(it.e));
                  var span = document.createElement('span');
                  span.setAttribute('data-ann-id', id);
                  span.setAttribute('data-ann-type', style.includes('background-color') ? 'highlight' : 'underline');
                  span.setAttribute('style', style + '; cursor: pointer; transition: all 0.2s ease;');
                  span.setAttribute('title', 'Tap to edit annotation');
                  n.parentNode.insertBefore(before, n);
                  n.parentNode.insertBefore(span, n);
                  span.appendChild(middle);
                  n.parentNode.insertBefore(after, n);
                  n.parentNode.removeChild(n);
                  // Continue with 'after' as next node in subsequent iterations
                }
              }
              window.AnnotatorJS = {
                getSelectionOffsets: function(){ return getSelectionOffsets(); },
                applyHighlight: function(id, start, end, color){
                  var style = 'background-color:'+color+'; padding: 3px 2px; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);';
                  wrapRangeLinear(id,start,end,style);
                },
                applyUnderline: function(id, start, end, color){
                  var style = 'text-decoration: underline; text-decoration-color:'+color+'; text-decoration-thickness: 3px; text-underline-offset: 3px; border-bottom: 2px solid '+color+'; padding-bottom: 1px;';
                  wrapRangeLinear(id,start,end,style);
                },
                applyStrikethrough: function(id, start, end, color){
                  var style = 'text-decoration: line-through; text-decoration-color:'+color+'; text-decoration-thickness: 2px;';
                  wrapRangeLinear(id,start,end,style);
                },
                applyNote: function(id, start, end){
                  var style = 'border-bottom: 2px dotted #90caf9;';
                  wrapRangeLinear(id,start,end,style);
                },
                removeAnnotation: function(id){
                  var spans = document.querySelectorAll('[data-ann-id="' + id + '"]');
                  spans.forEach(function(s){
                    var parent = s.parentNode;
                    while(s.firstChild){ parent.insertBefore(s.firstChild, s); }
                    parent.removeChild(s);
                    parent.normalize && parent.normalize();
                  });
                }
              };
              document.addEventListener('selectionchange', function(){
                if (__selDebounceTimer) { clearTimeout(__selDebounceTimer); }
                __selDebounceTimer = setTimeout(function(){
                  try {
                    var sel = window.getSelection();
                    if (!sel || sel.rangeCount === 0 || sel.toString().length === 0){
                      __lastSelectionText = '';
                      if (window.AndroidAnnotator && AndroidAnnotator.onSelectionCleared){ 
                        AndroidAnnotator.onSelectionCleared(); 
                      }
                    } else {
                      var selectedText = sel.toString().trim();
                      if (selectedText.length > 0) {
                        if (window.AndroidAnnotator && AndroidAnnotator.onSelectionChanged){ 
                          AndroidAnnotator.onSelectionChanged(); 
                        }
                        notifySelectionPosition();
                      } else {
                        __lastSelectionText = '';
                        if (window.AndroidAnnotator && AndroidAnnotator.onSelectionCleared){ 
                          AndroidAnnotator.onSelectionCleared(); 
                        }
                      }
                    }
                  } catch (e) {
                    console.log('Selection change error:', e);
                  }
                }, 200); // Reduced debounce time for better responsiveness
              });
              document.addEventListener('click', function(e){
                var node = e.target.closest('[data-ann-id]');
                if (node && window.AndroidAnnotator && AndroidAnnotator.onAnnotationTapped){
                  AndroidAnnotator.onAnnotationTapped(node.getAttribute('data-ann-id'));
                  e.preventDefault();
                }
              });
            })();
        """.trimIndent()
        topicContent.evaluateJavascript(js, null)
    }

    private fun removeAnnotationInWebView(annotationId: String) {
        val js = "AnnotatorJS.removeAnnotation('" + annotationId + "');"
        topicContent.evaluateJavascript(js, null)
    }

    private fun applyAnnotationInWebView(annotation: Annotation) {
        when (annotation.type) {
            "highlight" -> {
                val js = "AnnotatorJS.applyHighlight('${'$'}{annotation.id}', ${annotation.startOffset}, ${annotation.endOffset}, '${annotation.color ?: currentHighlightColorCss}');"
                topicContent.evaluateJavascript(js, null)
            }
            "underline" -> {
                val js = "AnnotatorJS.applyUnderline('${'$'}{annotation.id}', ${annotation.startOffset}, ${annotation.endOffset}, '${annotation.color ?: currentUnderlineColorCss}');"
                topicContent.evaluateJavascript(js, null)
            }
            "strike" -> {
                val js = "AnnotatorJS.applyStrikethrough('${'$'}{annotation.id}', ${annotation.startOffset}, ${annotation.endOffset}, '${annotation.color ?: currentStrikethroughColorCss}');"
                topicContent.evaluateJavascript(js, null)
            }
            "note" -> {
                val js = "AnnotatorJS.applyNote('${'$'}{annotation.id}', ${annotation.startOffset}, ${annotation.endOffset});"
                topicContent.evaluateJavascript(js, null)
            }
        }
    }

    private fun evaluateGetSelectionOffsets(callback: (start: Int?, end: Int?, selectedText: String?) -> Unit) {
        val js = "(function(){var r=AnnotatorJS.getSelectionOffsets(); return r? JSON.stringify(r): 'null';})()"
        topicContent.evaluateJavascript(js) { result ->
            try {
                if (result == null || result == "null") { callback(null, null, null); return@evaluateJavascript }
                val obj = JSONObject(result)
                val start = obj.optInt("start")
                val end = obj.optInt("end")
                val text = obj.optString("text")
                callback(start, end, text)
            } catch (e: Exception) {
                Log.e("ReadingActivity", "Failed to parse selection offsets: ${'$'}{e.message}")
                callback(null, null, null)
            }
        }
    }

    // ===== Selection actions sheet and helpers =====
    private fun showSelectionActionSheet() {
        if (selectionDialog?.isShowing == true) return
        evaluateGetSelectionOffsets { start, end, selectedText ->
            if (start == null || end == null || end <= start || selectedText.isNullOrBlank()) return@evaluateGetSelectionOffsets

            val options = arrayOf(
                "Highlight",
                "Underline",
                "Strikethrough",
                "Add Note",
                "Copy",
                "Define/Search",
                "Translate",
                "Share"
            )
            selectionDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Text actions")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showColorPicker { color ->
                            val ann = Annotation(
                                id = generateAnnotationId(),
                                contentId = currentContentId(),
                                userId = getStudentId(),
                                type = "highlight",
                                startOffset = start,
                                endOffset = end,
                                color = color
                            )
                            saveAnnotation(ann)
                            applyAnnotationInWebView(ann)
                        }
                        1 -> {
                            val ann = Annotation(
                                id = generateAnnotationId(),
                                contentId = currentContentId(),
                                userId = getStudentId(),
                                type = "underline",
                                startOffset = start,
                                endOffset = end,
                                color = currentUnderlineColorCss
                            )
                            saveAnnotation(ann)
                            applyAnnotationInWebView(ann)
                        }
                        2 -> {
                            val ann = Annotation(
                                id = generateAnnotationId(),
                                contentId = currentContentId(),
                                userId = getStudentId(),
                                type = "strike",
                                startOffset = start,
                                endOffset = end,
                                color = currentStrikethroughColorCss
                            )
                            saveAnnotation(ann)
                            applyAnnotationInWebView(ann)
                        }
                        3 -> showNoteDialog(initial = "") { noteText ->
                            val ann = Annotation(
                                id = generateAnnotationId(),
                                contentId = currentContentId(),
                                userId = getStudentId(),
                                type = "note",
                                startOffset = start,
                                endOffset = end,
                                noteText = noteText
                            )
                            saveAnnotation(ann)
                            applyAnnotationInWebView(ann)
                        }
                        4 -> copyToClipboard(selectedText)
                        5 -> openWebSearch(selectedText)
                        6 -> openTranslate(selectedText)
                        7 -> shareText(selectedText)
                    }
                }
                .setOnDismissListener { selectionDialog = null }
                .show()
        }
    }

    private fun showColorPicker(onPicked: (String) -> Unit) {
        val colors = arrayOf("#fff59d", "#a5d6a7", "#f48fb1", "#ffcc80", "#90caf9", "#ffd54f", "#80cbc4")
        val labels = arrayOf("Yellow", "Green", "Pink", "Orange", "Blue", "Amber", "Teal")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pick color")
            .setItems(labels) { _, which -> onPicked(colors[which]) }
            .show()
    }

    private fun showNoteDialog(initial: String, onSave: (String) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(initial)
            hint = "Type your note..."
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Note")
            .setView(input)
            .setPositiveButton("Save") { _, _ -> onSave(input.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyToClipboard(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("selection", text))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun openWebSearch(query: String) {
        try {
            val url = "https://www.google.com/search?q=" + Uri.encode(query)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {}
    }

    private fun openTranslate(text: String) {
        try {
            val url = "https://translate.google.com/?sl=auto&tl=en&text=" + Uri.encode(text)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {}
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share selection"))
    }

    private fun showAnnotationEditDialog(annotationId: String) {
        val list = getAnnotationsForContent(currentContentId())
        val ann = list.find { it.id == annotationId } ?: return
        val actions = when (ann.type) {
            "highlight" -> arrayOf("Change color", "Delete")
            "underline", "strike" -> arrayOf("Delete")
            "note" -> arrayOf("Edit note", "Delete")
            else -> arrayOf("Delete")
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edit annotation")
            .setItems(actions) { _, which ->
                when (ann.type) {
                    "highlight" -> {
                        if (which == 0) {
                            showColorPicker { color ->
                                val updated = ann.copy(color = color)
                                replaceAnnotation(updated)
                            }
                        } else if (which == 1) deleteAnnotation(ann)
                    }
                    "underline", "strike" -> if (which == 0) deleteAnnotation(ann)
                    "note" -> {
                        if (which == 0) {
                            showNoteDialog(initial = ann.noteText ?: "") { note ->
                                val updated = ann.copy(noteText = note)
                                replaceAnnotation(updated)
                            }
                        } else if (which == 1) deleteAnnotation(ann)
                    }
                    else -> if (which == 0) deleteAnnotation(ann)
                }
            }
            .show()
    }

    private fun replaceAnnotation(updated: Annotation) {
        val list = getAnnotationsForContent(updated.contentId).toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            list[idx] = updated
            persistAnnotations(updated.contentId, list)
            removeAnnotationInWebView(updated.id)
            applyAnnotationInWebView(updated)
        }
    }

    private fun deleteAnnotation(ann: Annotation) {
        val list = getAnnotationsForContent(ann.contentId).toMutableList()
        val idx = list.indexOfFirst { it.id == ann.id }
        if (idx >= 0) {
            list.removeAt(idx)
            persistAnnotations(ann.contentId, list)
            removeAnnotationInWebView(ann.id)
        }
    }

    // ===== Drawing overlay =====
    private inner class DrawingView(context: Context) : View(context) {
        private val strokes: MutableList<Stroke> = mutableListOf()
        private var currentPath: Path? = null
        private var currentPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentPenColor
            style = Paint.Style.STROKE
            strokeWidth = currentPenStrokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private var drawingEnabled: Boolean = false

        fun setDrawingEnabled(enabled: Boolean) { drawingEnabled = enabled }
        fun updatePaint(color: Int, widthPx: Float) {
            currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = widthPx
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        }
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!drawingEnabled) return false
            val x = event.x
            val y = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentPath = Path().apply { moveTo(x, y) }
                    strokes.add(Stroke(currentPenColor, currentPenStrokeWidthPx, mutableListOf(PointF(x, y))))
                }
                MotionEvent.ACTION_MOVE -> {
                    currentPath?.lineTo(x, y)
                    strokes.lastOrNull()?.points?.add(PointF(x, y))
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    currentPath?.lineTo(x, y)
                    strokes.lastOrNull()?.points?.add(PointF(x, y))
                    currentPath = null
                }
            }
            invalidate()
            return true
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            var idx = 0
            for (s in strokes) {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = s.color
                    style = Paint.Style.STROKE
                    strokeWidth = s.widthPx
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                var last: PointF? = null
                for (pt in s.points) {
                    if (last == null) {
                        last = pt
                        continue
                    }
                    canvas.drawLine(last.x, last.y, pt.x, pt.y, p)
                    last = pt
                }
                idx++
            }
        }
        fun clear() { strokes.clear(); invalidate() }
        fun exportStrokesAsJson(): String {
            val arr = JSONArray()
            for (s in strokes) {
                val o = JSONObject()
                o.put("color", String.format("#%06X", 0xFFFFFF and s.color))
                o.put("width", s.widthPx)
                val pts = JSONArray()
                for (pt in s.points) {
                    val p = JSONObject(); p.put("x", pt.x); p.put("y", pt.y); pts.put(p)
                }
                o.put("points", pts)
                arr.put(o)
            }
            return arr.toString()
        }
    }
    
    private fun setupScrollListener() {
        findViewById<ScrollView>(R.id.contentScrollView).viewTreeObserver.addOnGlobalLayoutListener {
            // Calculate the Y position of the mark as read section
            markAsReadSectionY = markAsReadCheckbox.top - findViewById<ScrollView>(R.id.contentScrollView).height + 100.dpToPx()
        }
        
        findViewById<ScrollView>(R.id.contentScrollView).setOnScrollChangeListener { _, _, _, scrollY, _ ->
            val scrollView = findViewById<ScrollView>(R.id.contentScrollView)
            val maxScrollY = scrollView.getChildAt(0).height - scrollView.height
            
            // Check if user has scrolled to the mark as read section OR to the end of content
            if ((scrollY >= markAsReadSectionY || scrollY >= maxScrollY - 50) && !hasScrolledToMarkAsRead) {
                // Only record that the user reached the section/end; do not auto-check
                hasScrolledToMarkAsRead = true
            }
        }
    }
    
    private fun hasUserScrolledToMarkAsRead(): Boolean {
        return hasScrolledToMarkAsRead
    }
    
    private fun showLoadingSpinner() {
        loadingSpinnerLayout.visibility = android.view.View.VISIBLE
        topicContent.visibility = android.view.View.GONE
    }
    
    private fun hideLoadingSpinner() {
        loadingSpinnerLayout.visibility = android.view.View.GONE
        topicContent.visibility = android.view.View.VISIBLE
    }
    
    private fun getIntentData() {
        courseCode = intent.getStringExtra("courseCode") ?: ""
        courseTitle = intent.getStringExtra("courseName") ?: "Unknown Course"
        collegeName = intent.getStringExtra("collegeName") ?: "Unknown College"
        selectedMode = intent.getStringExtra("mode") ?: "UNKNOWN"
        
        // Set course name with mode indicator
        courseName.text = "$courseTitle - $selectedMode Mode"
        
        // Save last studied course information
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
        // Setup checkbox listener
        setupCheckboxListener()
        
        // Previous Button
        previousButton.setOnClickListener {
            if (currentTopicIndex > 0) {
                currentTopicIndex--
                loadCurrentTopic()
            }
        }
        
        // Next Button
        nextButton.setOnClickListener {
            if (currentTopicIndex < topics.size - 1) {
                currentTopicIndex++
                loadCurrentTopic()
            }
        }
        
        // Complete Topic Button
        completeTopicButton.setOnClickListener {
            completeCurrentTopic()
        }
    }
    
    private fun setupCheckboxListener() {
        markAsReadCheckbox.setOnCheckedChangeListener { _, isChecked ->
            currentTopic?.let { topic ->
                if (isChecked) {
                    // User manually marked as read
                    updateTopicReadStatus(topic.id, true)
                    updateReadStatus(true)
                    Log.d("ReadingActivity", "User marked topic as read: ${topic.name}")
                } else {
                    // User manually unchecked - update progress and reset tracking
                    updateTopicReadStatus(topic.id, false)
                    updateReadStatus(false)
                    hasScrolledToMarkAsRead = false
                    Log.d("ReadingActivity", "User manually unchecked topic: ${topic.name}")
                }
            }
        }
    }
    
    private fun loadTopics() {
        // Show loading state
        topicContent.loadDataWithBaseURL(
            null,
            "<html><head><meta charset=\"UTF-8\"></head><body>🔄 Loading topics from server...</body></html>",
            "text/html; charset=UTF-8",
            "utf-8",
            null
        )
        
        // Fetch topics from API based on selected mode
        fetchTopicsFromAPI()
    }
    
    // Removed hardcoded topic methods - now using API
    
    private fun loadCurrentTopic() {
        if (currentTopicIndex >= 0 && currentTopicIndex < topics.size) {
            currentTopic = topics[currentTopicIndex]
            currentTopic?.let { topic ->
                // Show loading spinner first
                // Reset checkbox immediately to avoid carryover from previous topic
                setCheckboxCheckedSilently(false)
                showLoadingSpinner()
                
                // Update topic name immediately
                topicName.text = topic.name
                
                // Delay content loading by 2 seconds
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
                            li { 
                              margin: 8px 0;
                            }
                            li b { 
                              color: #555; 
                              font-weight: bold;
                            }
                            p { 
                              margin: 15px 0; 
                              text-align: justify;
                            }
                            hr { 
                              border: 0; 
                              height: 1px; 
                              background: #ddd; 
                              margin: 20px 0; 
                            }
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
                            tr:nth-child(even) {
                              background-color: #f9f9f9;
                            }
                            tr:hover {
                              background-color: #f0f0f0;
                            }
                            img { 
                              max-width: 100%; 
                              height: auto; 
                              border-radius: 4px;
                            }
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
                            /* Enhanced annotation styles */
                            [data-ann-id] { 
                              cursor: pointer; 
                              transition: all 0.3s ease; 
                              border-radius: 4px;
                              position: relative;
                            }
                            [data-ann-id]:hover { 
                              transform: scale(1.02); 
                              box-shadow: 0 4px 12px rgba(0,0,0,0.2);
                              z-index: 10;
                            }
                            [data-ann-type="highlight"] { 
                              background-color: #fff59d !important; 
                              padding: 3px 2px; 
                              border-radius: 4px; 
                              box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                              border: 1px solid rgba(255, 193, 7, 0.3);
                            }
                            [data-ann-type="underline"] { 
                              text-decoration: underline !important; 
                              text-decoration-color: #ff8a65 !important; 
                              text-decoration-thickness: 3px !important; 
                              text-underline-offset: 3px !important;
                              border-bottom: 3px solid #ff8a65 !important;
                              padding-bottom: 2px;
                            }
                            [data-ann-type="note"] {
                              border-bottom: 2px dotted #90caf9 !important;
                              background-color: rgba(144, 202, 249, 0.1) !important;
                              padding: 2px 4px;
                              border-radius: 3px;
                            }
                            /* Search highlighting */
                            mark {
                              background-color: #ffeb3b !important;
                              color: #000 !important;
                              padding: 2px 4px;
                              border-radius: 3px;
                              font-weight: bold;
                              box-shadow: 0 1px 3px rgba(0,0,0,0.2);
                            }
                            /* Responsive design */
                            @media (max-width: 768px) {
                              body { padding: 4px; }
                              h1 { font-size: 1.5em; }
                              h2 { font-size: 1.3em; }
                              h3 { font-size: 1.2em; }
                              pre, .plain-text { font-size: 12px; padding: 10px; }
                              table { font-size: 14px; }
                            }
                            /* Focus mode for better reading */
                            .focus-mode {
                              max-width: 800px;
                              margin: 0 auto;
                              padding: 20px;
                            }
                            /* Print styles */
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
                    
                    // Hide spinner and show content
                    hideLoadingSpinner()
                    
                    // Reset scroll position to top for new topic
                    findViewById<ScrollView>(R.id.contentScrollView).post {
                        findViewById<ScrollView>(R.id.contentScrollView).smoothScrollTo(0, 0)
                    }
                    
                    // Reset scroll tracking for new topic
                    hasScrolledToMarkAsRead = false
                    autoCheckScheduled = false
                    
                    // Update progress
                    updateProgress()
                    
                    // Update navigation buttons
                    updateNavigationButtons()
                    
                    // Load read status
                    loadTopicReadStatus(topic.id)
                }, 2000) // 2 second delay
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
        // Previous button
        previousButton.isEnabled = currentTopicIndex > 0
        
        // Next button - hide on last page, show on other pages
        if (currentTopicIndex == topics.size - 1) {
            nextButton.visibility = android.view.View.GONE
        } else {
            nextButton.visibility = android.view.View.VISIBLE
            nextButton.isEnabled = currentTopicIndex < topics.size - 1
        }
        
        // Complete button (show only on last topic)
        if (currentTopicIndex == topics.size - 1) {
            completeTopicButton.visibility = android.view.View.VISIBLE
            // On last topic, adjust button weights for two-button layout (Previous + Complete)
            previousButton.layoutParams = (previousButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
            completeTopicButton.layoutParams = (completeTopicButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
        } else {
            completeTopicButton.visibility = android.view.View.GONE
            // When Complete button is hidden, adjust weights for two-button layout (Previous + Next)
            previousButton.layoutParams = (previousButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
            nextButton.layoutParams = (nextButton.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
            }
        }
    }
    
    private fun loadTopicReadStatus(topicId: String) {
        // Also load from local storage as fallback
        val sharedPrefs = getSharedPreferences("" +
                "TopicProgress", MODE_PRIVATE)
        val isRead = sharedPrefs.getBoolean("topic_${courseCode}_${getModeSuffix()}_${topicId}", false)
        
        // Only update UI if backend hasn't responded yet
        if (!markAsReadCheckbox.isChecked && !markAsReadCheckbox.isChecked) {
            setCheckboxCheckedSilently(isRead)
        }
    }
    
    private fun updateTopicReadStatus(topicId: String, isRead: Boolean) {
        // Update local storage first for immediate UI feedback
        val sharedPrefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("topic_${courseCode}_${getModeSuffix()}_${topicId}", isRead).apply()
        
        // Update backend
        updateTopicProgressToBackend(topicId, isRead)
        
        // Update overall course progress
        updateCourseProgress()

        // Refresh on-screen progress based on marked-as-read
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
            // No topics → persist 0% and exit safely
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
        
        // Update per-mode progress for PrepActivity display
        val courseProgressPrefs = getSharedPreferences("CourseProgress", MODE_PRIVATE)
        courseProgressPrefs.edit()
            .putInt("progress_${courseCode}_${getModeSuffix()}", progress)
            .apply()
    }
    
    private fun completeCurrentTopic() {
        currentTopic?.let { topic ->
            // Mark current topic as read
            updateTopicReadStatus(topic.id, true)
            setCheckboxCheckedSilently(true)
            
            // Show completion message
            Toast.makeText(this, "Topic completed! Great job!", Toast.LENGTH_LONG).show()
            
            // If this was the last topic, show completion dialog
            if (currentTopicIndex == topics.size - 1) {
                showCourseCompletionDialog()
            }
        }
    }
    
    private fun showCourseCompletionDialog() {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎉 Course Completed!")
            .setMessage("Congratulations! You have successfully completed all topics in this course.")
            .setPositiveButton("View Progress") { _, _ ->
                // Navigate back to PrepActivity or show progress
                finish()
            }
            .setNegativeButton("Continue Reading") { _, _ ->
                // Stay on current page
            }
            .create()
        
        alertDialog.show()
    }
    
    // Method to fetch topics from API
    private fun fetchTopicsFromAPI() {
        // Convert course code to course ID (you might need to adjust this based on your database structure)
        val courseId = getCourseIdFromCode(courseCode)
        
        // Determine mode for API
        val apiMode = when (selectedMode) {
            "PASS" -> "pass"
            "MASTER" -> "master"
            else -> "all"
        }
        
        val url = "http://192.168.137.229/univault/get_topics.php?course_code=$courseId&mode=$apiMode"
        Log.d("ReadingActivity", "Fetching topics from: $url")
        
        val queue = Volley.newRequestQueue(this)
        
        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            Request.Method.GET, url,
            { body ->
                try {
                    // Try to parse as JSON array first
                    val response = org.json.JSONArray(body)
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
                        // Seed TopicProgress keys for all topics so HomeFragment can compute total correctly
                        seedTopicProgressDefaults()
                        // Persist total topic count for HomeFragment display
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
                } catch (arrayEx: org.json.JSONException) {
                    // Not a JSON array; try to parse as an object and check for error
                    try {
                        val obj = org.json.JSONObject(body)
                        val errorMsg = obj.optString("error", "No topics found")
                        showLoadingSpinner()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            showNoTopicsState("📚 $errorMsg\n\nPlease check back later or contact your instructor.")
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                        }, 2000)
                    } catch (objEx: org.json.JSONException) {
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

    /**
     * Ensure a boolean preference exists for each topic for the current course and mode.
     * This lets HomeFragment1 compute total topics reliably (completed/total), even before any are marked.
     */
    private fun seedTopicProgressDefaults() {
        if (topics.isEmpty()) return
        val prefs = getSharedPreferences("TopicProgress", MODE_PRIVATE)
        val editor = prefs.edit()
        val modeSuffix = getModeSuffix()
        var seededAny = false
        topics.forEach { topic ->
            val key = "topic_${'$'}{courseCode}_${'$'}{modeSuffix}_${'$'}{topic.id}"
            if (!prefs.contains(key)) {
                editor.putBoolean(key, false)
                seededAny = true
            }
        }
        if (seededAny) editor.apply()
    }
    
    // Helper method to convert course code to course ID
    // You might need to adjust this based on your database structure
    private fun getCourseIdFromCode(courseCode: String): String {
        // For now, using course code as course ID
        // You might want to create a separate API endpoint to get course ID from course code
        return courseCode
    }

    // Helper to build storage key suffix for current mode
    private fun getModeSuffix(): String {
        return when (selectedMode) {
            "PASS" -> "PASS"
            "MASTER" -> "MASTER"
            else -> "ALL"
        }
    }
    
    /**
     * Persist total number of topics for current course and mode so Home can display consistent totals.
     */
    private fun saveTotalTopicCount(total: Int) {
        val totalsPrefs = getSharedPreferences("TopicTotals", MODE_PRIVATE)
        val modeSuffix = getModeSuffix()
        totalsPrefs.edit()
            .putInt("total_${'$'}{courseCode}_${'$'}{modeSuffix}", total)
            .apply()
    }
 
    // Backend integration methods for progress tracking
    
    private fun updateTopicProgressToBackend(topicId: String, isRead: Boolean) {
        val url = "http://192.168.137.229/univault/update_topic_progress.php"
        
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
    
    // Helper method to get student ID (you'll need to implement this based on your login system)
    private fun getStudentId(): Int {
        // This should return the logged-in student's ID
        // You might store this in SharedPreferences after login
        val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPrefs.getInt("student_id", 0)
    }
    
    override fun onResume() {
        super.onResume()
        // Start time tracking
        sessionStartTime = System.currentTimeMillis()
        
        // Refresh topic read status
        currentTopic?.let { topic ->
            loadTopicReadStatus(topic.id)
        }
        
        // Load previously saved total study time
        loadTotalStudyTime()
        
        // Update study time display
        updateStudyTimeDisplay()
    }
    
    override fun onPause() {
        super.onPause()
        // Calculate and save study time when activity is paused
        updateStudyTime()
        // Update the study time display
        updateStudyTimeDisplay()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Save any final progress
        updateCourseProgress()
        // Save final study time
        updateStudyTime()
        // Clean up TTS
        tts?.stop()
        tts?.shutdown()
        // Save reading preferences
        saveReadingPreferences()
    }
    
    /**
     * Updates the total study time by calculating the time spent in the current session
     * and saves it to SharedPreferences
     */
    private fun updateStudyTime() {
        if (sessionStartTime > 0) {
            val currentTime = System.currentTimeMillis()
            val sessionDuration = currentTime - sessionStartTime
            
            // Only add positive durations to prevent errors
            if (sessionDuration > 0) {
                totalStudyTimeMillis += sessionDuration
                saveStudyTime()
                
                // Save reading session to cache for HomeFragment statistics
                HomeFragment1.saveReadingSession(this, sessionDuration, courseCode)
            }
            
            // Reset session start time
            sessionStartTime = 0
        }
    }
    
    /**
     * Saves the total study time to SharedPreferences and backend
     */
    private fun saveStudyTime() {
        val sharedPreferences = getSharedPreferences("study_time_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("total_study_time_$courseCode", totalStudyTimeMillis)
        editor.apply()
        
        // Also save to backend
        saveStudyTimeToBackend()
    }
    
    /**
     * Loads the total study time from SharedPreferences and syncs with backend
     */
    private fun loadTotalStudyTime() {
        val sharedPreferences = getSharedPreferences("study_time_prefs", Context.MODE_PRIVATE)
        totalStudyTimeMillis = sharedPreferences.getLong("total_study_time_$courseCode", 0)
        
        // Also load from backend to sync
        loadStudyTimeFromBackend()
    }
    
    /**
     * Gets the total study time in hours
     * @return Total study time in hours (with two decimal places)
     */
    private fun getTotalStudyTimeInHours(): Float {
        return (totalStudyTimeMillis / (1000.0f * 60.0f * 60.0f)).toFloat()
    }
    
    /**
     * Updates the study time display in the UI
     */
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
        nextButton.visibility = android.view.View.GONE
        completeTopicButton.visibility = android.view.View.GONE
    }
    
    /**
     * Wraps plain text content in appropriate HTML formatting for better visibility
     * Detects if content appears to be plain text and applies special styling
     */
    private fun wrapPlainTextIfNeeded(content: String): String {
        // Check if content appears to be plain text (no HTML tags)
        val containsHtmlTags = content.contains(Regex("<[^>]*>"))
        
        return if (!containsHtmlTags) {
            // This is likely plain text, wrap it in our special styling
            "<div class='plain-text'>$content</div>"
        } else {
            // This is already HTML content, return as is
            content
        }
    }

    /**
     * Saves the study time to the backend server
     */
    private fun saveStudyTimeToBackend() {
        val url = "http://192.168.137.229/univault/save_study_time.php"
        
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
                // Continue with local storage as fallback
            }
        )
        
        queue.add(jsonObjectRequest)
    }
    
    /**
     * Loads the study time from the backend server
     */
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
        
        val url = "http://192.168.137.229/univault/get_study_time.php?student_id=$studentId&course_code=$courseCode&mode=$apiMode"
        
        val queue = Volley.newRequestQueue(this)
        
        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optBoolean("success", false)) {
                        val backendStudyTime = jsonResponse.optLong("total_study_time_millis", 0)
                        
                        // Use the maximum of local and backend time to avoid data loss
                        if (backendStudyTime > totalStudyTimeMillis) {
                            totalStudyTimeMillis = backendStudyTime
                            
                            // Update local storage with backend value
                            val sharedPreferences = getSharedPreferences("study_time_prefs", Context.MODE_PRIVATE)
                            sharedPreferences.edit()
                                .putLong("total_study_time_$courseCode", totalStudyTimeMillis)
                                .apply()
                            
                            // Update display
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
                // Continue with local storage as fallback
            }
        ) {}
        
        queue.add(stringRequest)
    }

    // WebView handles images and CSS; ImageGetter no longer required
}
