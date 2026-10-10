package com.example.studypoint

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
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

    private lateinit var btnCloseSearch: ImageButton
    private lateinit var etPageNumber: EditText
    private lateinit var btnGoToPage: MaterialButton

    private lateinit var layoutProgress: View
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressText: TextView

    private var totalPages: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)

        initViews()
        setupListeners()

        val rawUrl = intent.getStringExtra("pdfUrl") ?: ""
        val title = intent.getStringExtra("title") ?: "PDF Document"
        val grade = intent.getStringExtra("grade") ?: ""
        val subject = intent.getStringExtra("subject") ?: ""

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

        btnCloseSearch = findViewById(R.id.btnCloseSearch)
        etPageNumber = findViewById(R.id.etPageNumber)
        btnGoToPage = findViewById(R.id.btnGoToPage)

        layoutProgress = findViewById(R.id.layoutProgress)
        progressBar = findViewById(R.id.progressBar)
        tvProgressText = findViewById(R.id.tvProgressText)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // Open Jump to Page input
        btnJumpToPage.setOnClickListener {
            openJumpSearch()
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

        // Back press handling: if search mode is open, close search first instead of finishing activity
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (layoutPageSearch.visibility == View.VISIBLE) {
                    closeJumpSearch()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
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
            "https://drive.google.com/uc?export=download&id=$fileId"
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
            val buffer = ByteArray(32 * 1024) // 32 KB high-efficiency buffer

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

    // Optimized PDF renderer with memory caching, fast fling, and disabled heavy annotations
    private fun renderPdfFromFile(file: File) {
        layoutProgress.visibility = View.GONE
        try {
            pdfView.fromFile(file)
                .defaultPage(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .enableAnnotationRendering(false) // Major performance boost
                .enableAntialiasing(true)
                .spacing(12) // Clean page margin
                .pageFitPolicy(FitPolicy.WIDTH) // Responsive screen width fit
                .pageFling(true) // Fast, smooth flick scrolling
                .pageSnap(false) // Continuous smooth flow
                .scrollHandle(DefaultScrollHandle(this)) // Fast jump scrollbar handle
                .onLoad { nbPages ->
                    totalPages = nbPages
                    val current = pdfView.currentPage + 1
                    tvPageIndicator.text = "Page $current / $totalPages"
                    layoutProgress.visibility = View.GONE
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