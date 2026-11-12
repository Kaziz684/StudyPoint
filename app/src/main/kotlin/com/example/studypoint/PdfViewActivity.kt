package com.example.studypoint

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class PdfViewActivity : AppCompatActivity() {

    private lateinit var pdfView: PDFView
    private lateinit var layoutStandardToolbar: View
    private lateinit var layoutPageSearch: View
    private lateinit var btnBack: ImageButton
    private lateinit var headerText: TextView
    private lateinit var tvPageIndicator: TextView
    private lateinit var btnJumpToPage: ImageButton
    private lateinit var btnMore: ImageButton
    private lateinit var btnAssistant: ImageButton
    private lateinit var btnReadingView: ImageButton

    private lateinit var btnCloseSearch: ImageButton
    private lateinit var etPageNumber: EditText
    private lateinit var btnGoToPage: MaterialButton

    private lateinit var layoutProgress: View
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressText: TextView
    private lateinit var sepiaOverlay: View

    private var totalPages: Int = 0

    // Open document context (used by actions sheet + AI assistant)
    private var docTitle: String = "PDF Document"
    private var docGrade: String = ""
    private var docSubject: String = ""
    private var docRawUrl: String = ""
    private var currentPdfFile: File? = null

    private var actionsSheet: BottomSheetDialog? = null
    private var aiSheet: BottomSheetDialog? = null
    private var aiMessagesContainer: LinearLayout? = null
    private var aiMessagesScroll: ScrollView? = null

    // ---- Reading view settings (persisted in "pdf_reader_settings", apply to EVERY PDF) ----
    private var zoomPercent: Int = 100
    private var readingTheme: String = THEME_DEFAULT
    private var pageMode: String = MODE_VERTICAL
    private var scrollSpeed: Int = 3

    private var readingSheet: BottomSheetDialog? = null
    private var autoScrollRunning: Boolean = false
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null
    private var autoScrollStuckTicks: Int = 0

    // Views inside the open Reading view sheet (null when the sheet is closed)
    private var sheetZoomValue: TextView? = null
    private var sheetPlayButton: MaterialButton? = null
    private var sheetSpeedValue: TextView? = null
    private var themeCards: List<View> = emptyList()
    private var modeRows: List<View> = emptyList()

    companion object {
        private const val READER_PREFS = "pdf_reader_settings"
        private const val KEY_ZOOM = "zoom_percent"
        private const val KEY_THEME = "theme"
        private const val KEY_PAGE_MODE = "page_mode"
        private const val KEY_SCROLL_SPEED = "scroll_speed"

        private const val THEME_DEFAULT = "default"
        private const val THEME_NIGHT = "night"
        private const val THEME_SEPIA = "sepia"

        private const val MODE_VERTICAL = "vertical"
        private const val MODE_HORIZONTAL = "horizontal"

        private const val ZOOM_MIN = 50
        private const val ZOOM_MAX = 300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)

        initViews()
        loadReaderSettings()
        setupListeners()

        val rawUrl = intent.getStringExtra("pdfUrl") ?: ""
        val title = intent.getStringExtra("title") ?: "PDF Document"
        val grade = intent.getStringExtra("grade") ?: ""
        val subject = intent.getStringExtra("subject") ?: ""

        docTitle = title
        docGrade = grade
        docSubject = subject
        docRawUrl = rawUrl

        headerText.text = title
        tvPageIndicator.text = "Loading..."

        if (rawUrl.isBlank()) {
            Toast.makeText(this, "Invalid PDF Link", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (grade.isNotBlank() || subject.isNotBlank()) {
            RecentReadManager.addRecent(this, RecentItem(title, grade, subject, rawUrl))
        }

        // Direct Drive Link conversion and fast background loading
        val directUrl = getDirectDriveLink(rawUrl)
        loadPdfWithCache(directUrl)
    }

    private fun initViews() {
        pdfView = findViewById(R.id.pdfView)
        layoutStandardToolbar = findViewById(R.id.layoutStandardToolbar)
        layoutPageSearch = findViewById(R.id.layoutPageSearch)
        btnBack = findViewById(R.id.btnBack)
        headerText = findViewById(R.id.headerText)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)
        btnJumpToPage = findViewById(R.id.btnJumpToPage)
        btnMore = findViewById(R.id.btnMore)
        btnAssistant = findViewById(R.id.btnAssistant)
        btnReadingView = findViewById(R.id.btnReadingView)

        btnCloseSearch = findViewById(R.id.btnCloseSearch)
        etPageNumber = findViewById(R.id.etPageNumber)
        btnGoToPage = findViewById(R.id.btnGoToPage)

        layoutProgress = findViewById(R.id.layoutProgress)
        progressBar = findViewById(R.id.progressBar)
        tvProgressText = findViewById(R.id.tvProgressText)
        sepiaOverlay = findViewById(R.id.sepiaOverlay)
    }

    /** Loads the persisted Reading view settings (zoom / theme / page mode / scroll speed). */
    private fun loadReaderSettings() {
        val prefs = getSharedPreferences(READER_PREFS, Context.MODE_PRIVATE)
        zoomPercent = prefs.getInt(KEY_ZOOM, 100).coerceIn(ZOOM_MIN, ZOOM_MAX)
        readingTheme = prefs.getString(KEY_THEME, THEME_DEFAULT) ?: THEME_DEFAULT
        pageMode = prefs.getString(KEY_PAGE_MODE, MODE_VERTICAL) ?: MODE_VERTICAL
        scrollSpeed = prefs.getInt(KEY_SCROLL_SPEED, 3).coerceIn(1, 5)
        sepiaOverlay.visibility = if (readingTheme == THEME_SEPIA) View.VISIBLE else View.GONE
    }

    private fun saveReaderSetting(key: String, value: Any) {
        val editor = getSharedPreferences(READER_PREFS, Context.MODE_PRIVATE).edit()
        when (value) {
            is Int -> editor.putInt(key, value)
            is String -> editor.putString(key, value)
        }
        editor.apply()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // Open Jump to Page input
        btnJumpToPage.setOnClickListener {
            openJumpSearch()
        }

        // More options: Bookmark / Share / Image / Last Read / Collection
        btnMore.setOnClickListener {
            showActionsSheet()
        }

        // AI assistant chat sheet for this document
        btnAssistant.setOnClickListener {
            showAiSheet()
        }

        // Reading view: zoom / theme / page mode / auto-scroll
        btnReadingView.setOnClickListener {
            showReadingSheet()
        }

        // Clicking page indicator also triggers Jump to Page
        tvPageIndicator.setOnClickListener {
            if (totalPages > 0) {
                openJumpSearch()
            }
        }

        // Close Jump to Page input
        btnCloseSearch.setOnClickListener {
            closeJumpSearch()
        }

        // Go button clicked
        btnGoToPage.setOnClickListener {
            performJumpToPage()
        }

        // Keyboard Action (Go/Done/Enter)
        etPageNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                performJumpToPage()
                true
            } else {
                false
            }
        }

        // Back press handling: sheets first, then search mode, then finish
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (readingSheet?.isShowing == true) {
                    readingSheet?.dismiss()
                } else if (actionsSheet?.isShowing == true) {
                    actionsSheet?.dismiss()
                } else if (aiSheet?.isShowing == true) {
                    aiSheet?.dismiss()
                } else if (layoutPageSearch.visibility == View.VISIBLE) {
                    closeJumpSearch()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    /** A touch on the PDF itself stops auto-scroll (touches on the sheet do not). */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (autoScrollRunning && readingSheet?.isShowing != true &&
            ev.action == MotionEvent.ACTION_DOWN
        ) {
            val loc = IntArray(2)
            pdfView.getLocationOnScreen(loc)
            if (ev.rawX >= loc[0] && ev.rawX <= loc[0] + pdfView.width &&
                ev.rawY >= loc[1] && ev.rawY <= loc[1] + pdfView.height
            ) {
                stopAutoScroll()
                Toast.makeText(this, "Auto-scroll stopped", Toast.LENGTH_SHORT).show()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onPause() {
        super.onPause()
        if (autoScrollRunning) stopAutoScroll()
    }

    override fun onDestroy() {
        autoScrollHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // ================= Actions bottom sheet =================

    private fun showActionsSheet() {
        val dialog = BottomSheetDialog(this)
        // Dark card in ALL themes with full-white text: the previous white
        // background was overridden by the system dark theme on some devices,
        // so the sheet now hardcodes its own dark background + white text.
        val sheetBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#1E293B"))
            cornerRadii = floatArrayOf(
                dp(16).toFloat(), dp(16).toFloat(),
                dp(16).toFloat(), dp(16).toFloat(),
                0f, 0f, 0f, 0f
            )
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = sheetBg
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        // Drag handle
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#64748B"))
                cornerRadius = dp(2).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(12)
            }
        }
        container.addView(handle)

        val titleView = TextView(this).apply {
            text = docTitle
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        container.addView(titleView)
        container.addView(TextView(this).apply {
            text = if (totalPages > 0) {
                "Page ${pdfView.currentPage + 1} of $totalPages"
            } else {
                "Loading document..."
            }
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(0, dp(2), 0, dp(10))
        })

        val bookmarked = RecentReadManager.isBookmarked(this, docRawUrl)
        addSheetRow(
            container,
            R.drawable.ic_bookmark,
            if (bookmarked) "Remove Bookmark" else "Bookmark"
        ) {
            toggleBookmark()
            dialog.dismiss()
        }
        addSheetRow(container, android.R.drawable.ic_menu_share, "Share App") {
            shareApp()
            dialog.dismiss()
        }
        addSheetRow(container, android.R.drawable.ic_menu_gallery, "Save / Share Page as Image") {
            sharePageAsImage()
            dialog.dismiss()
        }
        addSheetRow(container, android.R.drawable.ic_menu_save, "Set Last Read") {
            setLastRead()
            dialog.dismiss()
        }
        addSheetRow(container, android.R.drawable.ic_menu_add, "Add to Collection") {
            dialog.dismiss()
            showCollectionDialog()
        }

        dialog.setContentView(container)
        actionsSheet = dialog
        // Apply the same dark background to the sheet frame itself (after show,
        // so it is the last writer and the system theme cannot override it).
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background = sheetBg
        }
        dialog.show()
    }

    private fun addSheetRow(
        container: LinearLayout,
        iconRes: Int,
        label: String,
        onClick: () -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            isClickable = true
            isFocusable = true
            val outValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            setOnClickListener { onClick() }
        }
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            val size = dp(22)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setColorFilter(Color.parseColor("#10B981"))
        }
        val text = TextView(this).apply {
            text = label
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(16), 0, 0, 0)
        }
        row.addView(icon)
        row.addView(text)
        container.addView(row)
    }

    // ================= Reading view bottom sheet =================
    // Zoom + theme + page mode + auto-scroll for EVERY PDF in the app.
    // NOTE: fonts inside a PDF cannot be swapped — the text is baked into
    // the page image. Zoom is the real equivalent of a font-size control.

    private fun showReadingSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#1E293B"))
            cornerRadii = floatArrayOf(
                dp(16).toFloat(), dp(16).toFloat(),
                dp(16).toFloat(), dp(16).toFloat(),
                0f, 0f, 0f, 0f
            )
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = sheetBg
            setPadding(dp(16), dp(12), dp(16), dp(20))
        }

        // Drag handle
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#64748B"))
                cornerRadius = dp(2).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(12)
            }
        }
        container.addView(handle)

        container.addView(TextView(this).apply {
            text = "Reading view"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        container.addView(TextView(this).apply {
            text = "Applies to all PDFs"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(2), 0, dp(14))
        })

        // ---- Zoom ----
        val zoomHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        zoomHeader.addView(TextView(this).apply {
            text = "ZOOM"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#94A3B8"))
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val zoomValueView = TextView(this).apply {
            text = "$zoomPercent%"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
        }
        zoomHeader.addView(zoomValueView)
        container.addView(zoomHeader)
        sheetZoomValue = zoomValueView

        val zoomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        val zoomSeek = SeekBar(this).apply {
            max = ZOOM_MAX - ZOOM_MIN
            progress = zoomPercent - ZOOM_MIN
            progressTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            thumbTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(8)
                marginEnd = dp(8)
            }
        }
        val btnZoomOut = roundButton("−")
        val btnZoomIn = roundButton("+")
        btnZoomOut.setOnClickListener {
            val next = (zoomPercent - 10).coerceIn(ZOOM_MIN, ZOOM_MAX)
            zoomSeek.progress = next - ZOOM_MIN
            applyZoom(next, animate = true)
        }
        btnZoomIn.setOnClickListener {
            val next = (zoomPercent + 10).coerceIn(ZOOM_MIN, ZOOM_MAX)
            zoomSeek.progress = next - ZOOM_MIN
            applyZoom(next, animate = true)
        }
        zoomSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) sheetZoomValue?.text = "${progress + ZOOM_MIN}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                applyZoom((seekBar?.progress ?: 0) + ZOOM_MIN, animate = true)
            }
        })
        zoomRow.addView(btnZoomOut)
        zoomRow.addView(zoomSeek)
        zoomRow.addView(btnZoomIn)
        container.addView(zoomRow)
        container.addView(TextView(this).apply {
            text = "PDF text is fixed — zoom makes it bigger."
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(4), 0, dp(10))
        })

        // ---- Reading theme ----
        addSectionLabel(container, "READING THEME")
        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(10))
        }
        val themes = listOf(
            Triple(THEME_DEFAULT, "Default", Color.WHITE),
            Triple(THEME_NIGHT, "Night", Color.BLACK),
            Triple(THEME_SEPIA, "Sepia", Color.parseColor("#F2D8A0"))
        )
        val cards = mutableListOf<View>()
        for ((value, label, circleColor) in themes) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                tag = value
                setPadding(dp(8), dp(12), dp(8), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
                isClickable = true
                isFocusable = true
            }
            val circle = View(this).apply {
                background = circleDrawable(
                    circleColor,
                    if (value == THEME_NIGHT) Color.parseColor("#64748B") else null
                )
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            }
            val labelView = TextView(this).apply {
                text = label
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(0, dp(8), 0, 0)
            }
            card.addView(circle)
            card.addView(labelView)
            card.setOnClickListener { applyTheme(value) }
            themeRow.addView(card)
            cards.add(card)
        }
        themeCards = cards
        container.addView(themeRow)

        // ---- Page mode ----
        addSectionLabel(container, "PAGE MODE")
        val rows = mutableListOf<View>()
        val modes = listOf(
            MODE_VERTICAL to ("Vertical scroll" to "Continuous flow, page after page"),
            MODE_HORIZONTAL to ("Horizontal swipe" to "One page at a time")
        )
        for ((value, texts) in modes) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                tag = value
                setPadding(dp(12), dp(10), dp(12), dp(10))
                isClickable = true
                isFocusable = true
                val outValue = TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
            }
            val textsLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textsLayout.addView(TextView(this).apply {
                text = texts.first
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            textsLayout.addView(TextView(this).apply {
                text = texts.second
                textSize = 12f
                setTextColor(Color.parseColor("#94A3B8"))
            })
            val check = TextView(this).apply {
                tag = "check"
                text = "✓"
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#10B981"))
            }
            row.addView(textsLayout)
            row.addView(check)
            row.setOnClickListener { applyPageMode(value) }
            container.addView(row)
            rows.add(row)
        }
        modeRows = rows
        container.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(10)
            )
        })

        // ---- Auto-scroll ----
        addSectionLabel(container, "AUTO-SCROLL")
        val scrollRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        val playButton = MaterialButton(this).apply {
            text = if (autoScrollRunning) "❚❚ Pause" else "▶ Play"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            cornerRadius = dp(10)
            minWidth = dp(96)
            setPadding(dp(8), 0, dp(8), 0)
        }
        playButton.setOnClickListener { toggleAutoScroll() }
        sheetPlayButton = playButton
        val speedSeek = SeekBar(this).apply {
            max = 4
            progress = scrollSpeed - 1
            progressTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            thumbTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10)
                marginEnd = dp(10)
            }
        }
        val speedValueView = TextView(this).apply {
            text = speedLabel()
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END
            }
            gravity = Gravity.END
        }
        sheetSpeedValue = speedValueView
        speedSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) sheetSpeedValue?.text = speedLabelFor(progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                scrollSpeed = ((seekBar?.progress ?: 0) + 1).coerceIn(1, 5)
                saveReaderSetting(KEY_SCROLL_SPEED, scrollSpeed)
                sheetSpeedValue?.text = speedLabel()
            }
        })
        scrollRow.addView(playButton)
        scrollRow.addView(speedSeek)
        scrollRow.addView(speedValueView)
        container.addView(scrollRow)

        container.addView(TextView(this).apply {
            text = "Fonts can't change inside a PDF — the text is part of the page. " +
                "Zoom, themes, page mode and auto-scroll work on every PDF."
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(14), 0, 0)
        })

        dialog.setContentView(container)
        readingSheet = dialog
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background = sheetBg
        }
        dialog.setOnDismissListener {
            readingSheet = null
            sheetZoomValue = null
            sheetPlayButton = null
            sheetSpeedValue = null
            themeCards = emptyList()
            modeRows = emptyList()
        }
        dialog.show()
        refreshReadingSheetSelection()
    }

    private fun addSectionLabel(container: LinearLayout, text: String) {
        container.addView(TextView(this).apply {
            this.text = text
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#94A3B8"))
            letterSpacing = 0.08f
            setPadding(0, dp(4), 0, 0)
        })
    }

    private fun roundButton(label: String): MaterialButton {
        return MaterialButton(this).apply {
            text = label
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#334155"))
            cornerRadius = dp(24)
            minWidth = dp(48)
            minimumWidth = dp(48)
            minimumHeight = dp(48)
            setPadding(0, 0, 0, 0)
            insetTop = 0
            insetBottom = 0
        }
    }

    private fun circleDrawable(fillColor: Int, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }
    }

    private fun themeCardBg(selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(Color.parseColor(if (selected) "#134E4A" else "#0F172A"))
            if (selected) setStroke(dp(2), Color.parseColor("#10B981"))
        }
    }

    private fun modeRowBg(selected: Boolean): GradientDrawable? {
        if (!selected) return null
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(Color.parseColor("#134E4A"))
        }
    }

    private fun refreshReadingSheetSelection() {
        for (card in themeCards) {
            card.background = themeCardBg(card.tag as? String == readingTheme)
        }
        for (row in modeRows) {
            val selected = row.tag as? String == pageMode
            row.background = modeRowBg(selected)
            row.findViewWithTag<TextView>("check")?.visibility =
                if (selected) View.VISIBLE else View.INVISIBLE
        }
        sheetZoomValue?.text = "$zoomPercent%"
        sheetSpeedValue?.text = speedLabel()
        refreshAutoScrollUi()
    }

    private fun refreshAutoScrollUi() {
        sheetPlayButton?.text = if (autoScrollRunning) "❚❚ Pause" else "▶ Play"
    }

    private fun speedLabel(): String = speedLabelFor(scrollSpeed)

    private fun speedLabelFor(speed: Int): String = when (speed) {
        1 -> "Slow"
        2 -> "Moderate"
        3 -> "Medium"
        4 -> "Quick"
        else -> "Fast"
    }

    /** Applies a reading theme live (night mode inverts the PDF; sepia is a warm tint overlay). */
    private fun applyTheme(theme: String) {
        readingTheme = theme
        saveReaderSetting(KEY_THEME, theme)
        sepiaOverlay.visibility = if (theme == THEME_SEPIA) View.VISIBLE else View.GONE
        pdfView.setNightMode(theme == THEME_NIGHT)
        pdfView.invalidate()
        refreshReadingSheetSelection()
    }

    /** Applies zoom live (100% = fit to screen width). Persists for every PDF. */
    private fun applyZoom(percent: Int, animate: Boolean) {
        zoomPercent = percent.coerceIn(ZOOM_MIN, ZOOM_MAX)
        saveReaderSetting(KEY_ZOOM, zoomPercent)
        sheetZoomValue?.text = "$zoomPercent%"
        if (totalPages > 0) {
            // NOTE: minZoom stays at the library default (1.0) on purpose —
            // lowering it would change double-tap reset to 50%. The slider sets
            // zoom directly (zoomTo/zoomWithAnimation do not clamp), so 50% works.
            pdfView.setMaxZoom(3.0f)
            if (animate) {
                pdfView.zoomWithAnimation(zoomPercent / 100f)
            } else {
                pdfView.zoomTo(zoomPercent / 100f)
                pdfView.loadPages()
            }
        }
    }

    /** Applies the page mode (reloads the document at the current page). */
    private fun applyPageMode(mode: String) {
        if (mode == pageMode) {
            refreshReadingSheetSelection()
            return
        }
        pageMode = mode
        saveReaderSetting(KEY_PAGE_MODE, mode)
        refreshReadingSheetSelection()
        val file = currentPdfFile
        if (file != null && totalPages > 0) {
            renderPdfFromFile(file, startPage = pdfView.currentPage)
            Toast.makeText(
                this,
                if (mode == MODE_HORIZONTAL) "Horizontal swipe — page by page" else "Vertical scroll",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleAutoScroll() {
        if (autoScrollRunning) {
            stopAutoScroll()
            return
        }
        if (totalPages <= 0) {
            Toast.makeText(this, "PDF is still loading...", Toast.LENGTH_SHORT).show()
            return
        }
        startAutoScroll()
    }

    private fun startAutoScroll() {
        if (totalPages <= 0 || autoScrollRunning) return
        autoScrollRunning = true
        autoScrollStuckTicks = 0
        val runnable = object : Runnable {
            override fun run() {
                if (!autoScrollRunning) return
                if (pageMode == MODE_HORIZONTAL) {
                    if (pdfView.currentPage >= totalPages - 1) {
                        stopAutoScroll()
                        Toast.makeText(this@PdfViewActivity, "Reached the last page", Toast.LENGTH_SHORT).show()
                        return
                    }
                    pdfView.jumpTo(pdfView.currentPage + 1, true)
                    autoScrollHandler.postDelayed(this, 8000L - scrollSpeed * 1000L)
                } else {
                    val before = pdfView.currentYOffset
                    // 30..90 dp per second, in small 50 ms steps.
                    val dpPerSecond = 15f + scrollSpeed * 15f
                    val stepPx = dpPerSecond * 0.05f * resources.displayMetrics.density
                    pdfView.moveRelativeTo(0f, -stepPx)
                    val after = pdfView.currentYOffset
                    if (pdfView.currentPage >= totalPages - 1 &&
                        Math.abs(after - before) < 0.5f
                    ) {
                        autoScrollStuckTicks++
                        if (autoScrollStuckTicks >= 3) {
                            stopAutoScroll()
                            Toast.makeText(this@PdfViewActivity, "Reached the end", Toast.LENGTH_SHORT).show()
                            return
                        }
                    } else {
                        autoScrollStuckTicks = 0
                    }
                    autoScrollHandler.postDelayed(this, 50)
                }
            }
        }
        autoScrollRunnable = runnable
        autoScrollHandler.post(runnable)
        refreshAutoScrollUi()
    }

    private fun stopAutoScroll() {
        if (!autoScrollRunning) return
        autoScrollRunning = false
        autoScrollRunnable?.let { autoScrollHandler.removeCallbacks(it) }
        autoScrollRunnable = null
        refreshAutoScrollUi()
    }

    private fun currentPageNumber(): Int {
        return if (totalPages > 0) pdfView.currentPage + 1 else 0
    }

    private fun toggleBookmark() {
        if (RecentReadManager.isBookmarked(this, docRawUrl)) {
            RecentReadManager.removeBookmark(this, docRawUrl)
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
        } else {
            val page = currentPageNumber()
            RecentReadManager.addBookmark(
                this,
                RecentItem(docTitle, docGrade, docSubject, docRawUrl, page = page)
            )
            if (page > 0) {
                Toast.makeText(this, "Bookmarked — page $page", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bookmarked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "StudyPoint")
            putExtra(Intent.EXTRA_TEXT, "StudyPoint \u2014 Free study app for Class 9 to 12 (Punjab Board): notes, textbooks, MCQs, solved questions and paper generator.\nhttps://studypointbwp.web.app")
        }
        startActivity(Intent.createChooser(intent, "Share StudyPoint"))
    }

    private fun setLastRead() {
        val page = currentPageNumber()
        if (page <= 0) {
            Toast.makeText(this, "PDF is still loading...", Toast.LENGTH_SHORT).show()
            return
        }
        RecentReadManager.saveLastPage(this, docRawUrl, page)
        Toast.makeText(this, "Last read saved — page $page", Toast.LENGTH_SHORT).show()
    }

    private fun showCollectionDialog() {
        val names = RecentReadManager.getCollectionNames(this)
        val items = (names + "Create new collection…").toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Add to Collection")
            .setItems(items) { _, which ->
                if (which < names.size) {
                    saveToCollection(names[which])
                } else {
                    showNewCollectionDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNewCollectionDialog() {
        val input = EditText(this).apply {
            hint = "Collection name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("New Collection")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Enter a collection name", Toast.LENGTH_SHORT).show()
                } else {
                    saveToCollection(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToCollection(name: String) {
        RecentReadManager.addToCollection(
            this,
            CollectionItem(
                title = docTitle,
                grade = docGrade,
                subject = docSubject,
                fileName = docRawUrl,
                page = currentPageNumber(),
                collection = name
            )
        )
        Toast.makeText(this, "Added to \"$name\"", Toast.LENGTH_SHORT).show()
    }

    private fun sharePageAsImage() {
        val file = currentPdfFile
        if (file == null || totalPages <= 0) {
            Toast.makeText(this, "PDF is still loading...", Toast.LENGTH_SHORT).show()
            return
        }
        val pageIndex = pdfView.currentPage
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val page = renderer.openPage(pageIndex)
                val targetWidth = 1080
                val scale = targetWidth.toFloat() / page.width
                val bitmap = Bitmap.createBitmap(
                    targetWidth,
                    (page.height * scale).toInt(),
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(Color.WHITE)
                val matrix = Matrix().apply { setScale(scale, scale) }
                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                val imagesDir = File(cacheDir, "images").apply { if (!exists()) mkdirs() }
                val outFile = File(imagesDir, "page_${pageIndex + 1}.png")
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(
                        this@PdfViewActivity,
                        "${packageName}.fileprovider",
                        outFile
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, "$docTitle — page ${pageIndex + 1}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Share page image"))
                }
            } catch (e: Exception) {
                Log.e("PdfViewActivity", "Page image failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PdfViewActivity,
                        "Could not create page image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ================= AI assistant sheet =================

    private fun showAiSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_ai_chat, null)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvAiSheetSubtitle)
        tvSubtitle.text = if (totalPages > 0) {
            "$docTitle • Page ${pdfView.currentPage + 1} of $totalPages"
        } else {
            docTitle
        }
        val messagesContainer = view.findViewById<LinearLayout>(R.id.aiMessagesContainer)
        val messagesScroll = view.findViewById<ScrollView>(R.id.aiMessagesScroll)
        val etQuestion = view.findViewById<EditText>(R.id.etAiQuestion)
        val btnSend = view.findViewById<MaterialButton>(R.id.btnAiSend)
        view.findViewById<ImageButton>(R.id.btnAiSheetClose).setOnClickListener {
            dialog.dismiss()
        }

        aiMessagesContainer = messagesContainer
        aiMessagesScroll = messagesScroll
        dialog.setContentView(view)
        aiSheet = dialog
        dialog.setOnDismissListener {
            aiMessagesContainer = null
            aiMessagesScroll = null
        }
        dialog.show()

        addAiMessage(
            false,
            "Ask me about \"$docTitle\" — a topic, a concept, or say \"go to page 12\" to jump there."
        )

        val sendQuestion = {
            val question = etQuestion.text.toString().trim()
            if (question.isNotEmpty()) {
                etQuestion.text?.clear()
                hideKeyboard(etQuestion)
                addAiMessage(true, question)
                handleAiQuestion(question)
            }
        }
        btnSend.setOnClickListener { sendQuestion() }
        etQuestion.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendQuestion()
                true
            } else {
                false
            }
        }
    }

    private fun addAiMessage(isUser: Boolean, text: String): View? {
        val container = aiMessagesContainer ?: return null
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(0, 0, 0, dp(8))
        }
        val bubble = TextView(this).apply {
            this.text = text
            textSize = AppSettings.chatTextSizeSp(this@PdfViewActivity).toFloat()
            setTextColor(if (isUser) Color.WHITE else Color.parseColor("#0F172A"))
            background = ContextCompat.getDrawable(this@PdfViewActivity, R.drawable.button_background)
            backgroundTintList = ColorStateList.valueOf(
                if (isUser) Color.parseColor("#2563EB") else Color.parseColor("#F1F5F9")
            )
            setPadding(dp(12), dp(10), dp(12), dp(10))
            maxWidth = (resources.displayMetrics.widthPixels * 0.85).toInt()
        }
        row.addView(bubble)
        container.addView(row)
        aiMessagesScroll?.post {
            aiMessagesScroll?.fullScroll(View.FOCUS_DOWN)
        }
        return row
    }

    /** Returns a page number for "go to page N" / "last page" / "first page", else null. */
    private fun parsePageJump(question: String): Int? {
        val q = question.trim().lowercase()
        if (q.contains("last page")) return totalPages
        if (q.contains("first page")) return 1
        val match = Regex("(?:page|pg\\.?|jump to|go to page|open page)\\s*(\\d+)").find(q)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun handleAiQuestion(question: String) {
        // Navigation answers never need the network: jump straight there.
        val jumpPage = parsePageJump(question)
        if (jumpPage != null) {
            if (totalPages <= 0) {
                addAiMessage(false, "The PDF is still loading — please try again in a moment.")
            } else if (jumpPage in 1..totalPages) {
                pdfView.jumpTo(jumpPage - 1, true)
                addAiMessage(false, "Jumped to page $jumpPage.")
            } else {
                addAiMessage(false, "That page is out of range. This document has $totalPages pages.")
            }
            return
        }

        // Anything else goes to the existing tutor engine with document context.
        // The tutor answers from its own knowledge; it never quotes document
        // text it has not read.
        val contextBuilder = StringBuilder()
        contextBuilder.append("The user is reading the document \"$docTitle\"")
        if (docSubject.isNotBlank() || docGrade.isNotBlank()) {
            contextBuilder.append(" (")
            if (docSubject.isNotBlank()) contextBuilder.append("subject: $docSubject")
            if (docSubject.isNotBlank() && docGrade.isNotBlank()) contextBuilder.append(", ")
            if (docGrade.isNotBlank()) contextBuilder.append("class: $docGrade")
            contextBuilder.append(")")
        }
        if (totalPages > 0) {
            contextBuilder.append(" and is on page ${pdfView.currentPage + 1} of $totalPages")
        }
        contextBuilder.append(". Their question: $question. Answer concisely for a student.")

        val thinkingRow = addAiMessage(false, "Thinking…")
        lifecycleScope.launch {
            try {
                val answer = AiTutorService.getAnswerRich(contextBuilder.toString())
                // Remove only this question's "Thinking…" bubble.
                if (thinkingRow != null) {
                    aiMessagesContainer?.removeView(thinkingRow)
                }
                val display = if (!answer.sourceLabel.isNullOrBlank()) {
                    "${answer.text}\n\nSource: ${answer.sourceLabel}"
                } else {
                    answer.text
                }
                addAiMessage(false, display)
            } catch (e: Exception) {
                Log.w("PdfViewActivity", "AI answer failed: ${e.message}")
                if (thinkingRow != null) {
                    aiMessagesContainer?.removeView(thinkingRow)
                }
                addAiMessage(
                    false,
                    "Sorry, I couldn't fetch an answer right now. Please check your internet connection and try again."
                )
            }
        }
    }

    private fun openJumpSearch() {
        layoutStandardToolbar.visibility = View.GONE
        layoutPageSearch.visibility = View.VISIBLE
        etPageNumber.text?.clear()
        etPageNumber.hint = if (totalPages > 0) "1 - $totalPages" else "Page number..."
        etPageNumber.requestFocus()
        showKeyboard(etPageNumber)
    }

    private fun closeJumpSearch() {
        hideKeyboard(etPageNumber)
        layoutPageSearch.visibility = View.GONE
        layoutStandardToolbar.visibility = View.VISIBLE
    }

    private fun performJumpToPage() {
        val input = etPageNumber.text?.toString()?.trim() ?: ""
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter a page number", Toast.LENGTH_SHORT).show()
            return
        }

        val pageNum = input.toIntOrNull()
        if (pageNum == null) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            return
        }

        if (totalPages <= 0) {
            Toast.makeText(this, "PDF is still loading...", Toast.LENGTH_SHORT).show()
            return
        }

        if (pageNum < 1 || pageNum > totalPages) {
            Toast.makeText(this, "Invalid page! Please enter between 1 and $totalPages", Toast.LENGTH_SHORT).show()
            return
        }

        // AndroidPdfViewer uses 0-based page index (page 1 is index 0)
        pdfView.jumpTo(pageNum - 1, true)
        closeJumpSearch()
        Toast.makeText(this, "Jumped to page $pageNum", Toast.LENGTH_SHORT).show()
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        view.postDelayed({
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, 150)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Google Drive URL converter: extracts clean file ID and converts to direct stream link
    private fun getDirectDriveLink(url: String): String {
        if (!url.contains("drive.google.com") && !url.contains("docs.google.com")) {
            return url
        }
        val fileId = when {
            url.contains("/file/d/") -> {
                url.substringAfter("/file/d/").substringBefore("/").substringBefore("?").substringBefore("&")
            }
            url.contains("id=") -> {
                url.substringAfter("id=").substringBefore("/").substringBefore("&").substringBefore("?")
            }
            else -> null
        }
        return if (!fileId.isNullOrBlank()) {
            "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
        } else {
            url
        }
    }

    // High performance loader: checks disk cache first for instant opening
    private fun loadPdfWithCache(pdfUrl: String) {
        val cachedFile = getCachedPdfFile(this, pdfUrl)

        // If valid cached copy exists, open INSTANTLY (0ms network delay, no lag)
        if (cachedFile.exists() && cachedFile.length() > 5000) {
            Log.d("PdfViewActivity", "Opening PDF from disk cache: ${cachedFile.absolutePath} (${cachedFile.length()} bytes)")
            renderPdfFromFile(cachedFile)
            return
        }

        // If not cached, show download progress overlay and fetch in background Dispatchers.IO
        layoutProgress.visibility = View.VISIBLE
        tvProgressText.text = "Downloading PDF..."

        lifecycleScope.launch(Dispatchers.IO) {
            val downloadSuccess = downloadPdfToFile(pdfUrl, cachedFile)
            withContext(Dispatchers.Main) {
                if (downloadSuccess && cachedFile.exists() && cachedFile.length() > 5000) {
                    renderPdfFromFile(cachedFile)
                } else {
                    layoutProgress.visibility = View.GONE
                    Toast.makeText(
                        this@PdfViewActivity,
                        "Failed to download PDF. Please check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Cache file naming helper using MD5 hash of URL
    private fun getCachedPdfFile(context: Context, url: String): File {
        val cacheDir = File(context.cacheDir, "pdf_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val key = try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(url.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            url.hashCode().toString().replace("-", "n")
        }
        return File(cacheDir, "doc_$key.pdf")
    }

    // Background file downloader with live progress updates and atomic rename
    private suspend fun downloadPdfToFile(urlString: String, destinationFile: File): Boolean {
        val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        return try {
            val (inputStream, totalBytes) = fetchPdfInputStreamWithLength(urlString)
            if (inputStream == null) return false

            var downloadedBytes = 0L
            val buffer = ByteArray(128 * 1024) // 32 KB high-efficiency buffer

            FileOutputStream(tempFile).use { output ->
                inputStream.use { input ->
                    var read: Int
                    var lastUpdatePercent = -1
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                            if (percent != lastUpdatePercent) {
                                lastUpdatePercent = percent
                                withContext(Dispatchers.Main) {
                                    tvProgressText.text = "Downloading: $percent%"
                                }
                            }
                        } else {
                            val mb = downloadedBytes / (1024 * 1024)
                            withContext(Dispatchers.Main) {
                                tvProgressText.text = "Downloading: $mb MB..."
                            }
                        }
                    }
                    output.flush()
                }
            }

            // Atomic rename to final cache destination
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            val renamed = tempFile.renameTo(destinationFile)
            if (!renamed) {
                tempFile.copyTo(destinationFile, overwrite = true)
                tempFile.delete()
            }
            true
        } catch (e: Exception) {
            Log.e("PdfViewActivity", "Download failed: ${e.message}", e)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            false
        }
    }

    // Optimized PDF renderer with memory caching, fast fling, and disabled heavy annotations.
    // Applies the persisted Reading view settings (theme / page mode / zoom).
    // startPage >= 0 reloads at that page (page-mode switch); -1 = first open (Last Read resume).
    private fun renderPdfFromFile(file: File, startPage: Int = -1) {
        stopAutoScroll()
        layoutProgress.visibility = View.GONE
        currentPdfFile = file
        try {
            pdfView.fromFile(file)
                .defaultPage(if (startPage >= 0) startPage else 0)
                .enableSwipe(true)
                .swipeHorizontal(pageMode == MODE_HORIZONTAL)
                .enableDoubletap(true)
                .enableAnnotationRendering(false) // Major performance boost
                .enableAntialiasing(true)
                .spacing(dp(16)) // Card-style gap between pages (px)
                .pageFitPolicy(FitPolicy.WIDTH) // Responsive screen width fit
                .pageFling(true) // Fast, smooth flick scrolling
                .pageSnap(pageMode == MODE_HORIZONTAL) // Snap per page in horizontal mode
                .nightMode(readingTheme == THEME_NIGHT)
                .scrollHandle(DefaultScrollHandle(this)) // Fast jump scrollbar handle
                .onLoad { nbPages ->
                    totalPages = nbPages
                    val current = pdfView.currentPage + 1
                    tvPageIndicator.text = "Page $current / $totalPages"
                    layoutProgress.visibility = View.GONE
                    // Restore the saved Reading view zoom (100% = fit to screen width).
                    // NOTE: minZoom stays at the library default (1.0) so double-tap
                    // reset is unchanged; the slider sets zoom directly (no clamp).
                    pdfView.setMaxZoom(3.0f)
                    if (zoomPercent != 100) {
                        pdfView.zoomTo(zoomPercent / 100f)
                        pdfView.loadPages()
                    }
                    // Resume at the explicitly saved Last Read position, if any.
                    // (Only on first open — a page-mode reload keeps its own page.)
                    if (startPage < 0) {
                        val savedPage = RecentReadManager.getLastPage(this, docRawUrl)
                        if (savedPage in 1..nbPages) {
                            pdfView.jumpTo(savedPage - 1, false)
                            if (savedPage > 1) {
                                Toast.makeText(this, "Resumed at page $savedPage", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .onPageChange { page, pageCount ->
                    totalPages = pageCount
                    val current = page + 1
                    tvPageIndicator.text = "Page $current / $totalPages"
                }
                .onError { t ->
                    Log.e("PdfViewActivity", "PDF render error: ${t.message}", t)
                    layoutProgress.visibility = View.GONE
                    Toast.makeText(this, "Error displaying PDF: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                .load()
        } catch (e: Exception) {
            Log.e("PdfViewActivity", "Failed to start PDFView: ${e.message}", e)
            layoutProgress.visibility = View.GONE
            Toast.makeText(this, "Could not open PDF file.", Toast.LENGTH_SHORT).show()
        }
    }

    // Redirect Follower with content-length extraction
    private fun fetchPdfInputStreamWithLength(urlString: String): Pair<InputStream?, Long> {
        var currentUrl = urlString
        var redirectsCount = 0

        while (redirectsCount < 5) {
            val url = URL(currentUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)")

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 ||
                status == 308) {

                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrEmpty()) {
                    Log.e("PdfViewActivity", "Redirect location was null")
                    return Pair(null, -1L)
                }
                currentUrl = location
                redirectsCount++
            } else if (status == HttpURLConnection.HTTP_OK) {
                val contentLength = connection.contentLengthLong
                return Pair(connection.inputStream, contentLength)
            } else {
                Log.e("PdfViewActivity", "HTTP Failed status code: $status")
                connection.disconnect()
                return Pair(null, -1L)
            }
        }
        return Pair(null, -1L)
    }
}
