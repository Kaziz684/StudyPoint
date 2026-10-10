package com.example.studypoint

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class BoardWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var centerProgressBar: ProgressBar
    private lateinit var errorLayout: LinearLayout
    private lateinit var btnRetry: MaterialButton
    private var webUrl: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board_web_view)

        val title = intent.getStringExtra("title") ?: "Board Website"
        webUrl = intent.getStringExtra("url")

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.headerText).text = title

        webView = findViewById(R.id.boardWebView)
        progressBar = findViewById(R.id.webViewProgress)
        centerProgressBar = findViewById(R.id.centerProgressBar)
        errorLayout = findViewById(R.id.errorLayout)
        btnRetry = findViewById(R.id.btnRetry)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
                centerProgressBar.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE

                // Start 10 seconds timeout timer
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                timeoutRunnable = Runnable {
                    showErrorState()
                }.also { handler.postDelayed(it, 10000) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                centerProgressBar.visibility = View.GONE
                timeoutRunnable?.let { handler.removeCallbacks(it) }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    showErrorState()
                }
            }
        }

        webView.webChromeClient = WebChromeClient()

        btnRetry.setOnClickListener {
            errorLayout.visibility = View.GONE
            centerProgressBar.visibility = View.VISIBLE
            webUrl?.let { webView.loadUrl(it) }
        }

        loadWebsite()
    }

    private fun loadWebsite() {
        if (!webUrl.isNullOrEmpty()) {
            errorLayout.visibility = View.GONE
            centerProgressBar.visibility = View.VISIBLE
            webView.loadUrl(webUrl!!)
        } else {
            showErrorState()
        }
    }

    private fun showErrorState() {
        progressBar.visibility = View.GONE
        centerProgressBar.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
        timeoutRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        webView.destroy()
        super.onDestroy()
    }
}
