package com.example.studypoint

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import java.net.URL
import kotlin.concurrent.thread
import java.io.InputStream
import java.net.HttpURLConnection

class PdfViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)

        Log.d("StudyPoint", "PdfViewActivity started")

        val pdfView = findViewById<PDFView>(R.id.pdfView)

        // NotesActivity se aaya hua link
        val pdfUrl = intent.getStringExtra("pdfUrl")
        Log.d("StudyPoint", "Received PDF URL: $pdfUrl")

        if (!pdfUrl.isNullOrEmpty()) {
            if (pdfUrl.contains("REPLACE_WITH_") || pdfUrl.contains("SAMPLE_ID")) {
                val errorMsg = "Please replace placeholder URL with actual Google Drive file ID"
                Log.e("StudyPoint", "Error: $errorMsg")
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                return
            }

            Toast.makeText(this, "Loading PDF...", Toast.LENGTH_SHORT).show()
            Log.d("StudyPoint", "Starting PDF download...")

            thread {
                try {
                    val directUrl = convertToDirectDownloadUrl(pdfUrl)
                    Log.d("StudyPoint", "Direct URL: $directUrl")

                    val connection = URL(directUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000 // Increased timeout
                    connection.readTimeout = 45000    // Increased timeout
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                    connection.setRequestProperty("Accept", "application/pdf,*/*")
                    connection.setRequestProperty("Cache-Control", "no-cache")

                    Log.d("StudyPoint", "Connecting to URL...")
                    val responseCode = connection.responseCode
                    Log.d("StudyPoint", "Response code: $responseCode")

                    if (responseCode == 200) {
                        val input: InputStream = connection.inputStream
                        Log.d("StudyPoint", "Got input stream, loading PDF...")

                        runOnUiThread {
                            pdfView.fromStream(input)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .spacing(8)
                                .onLoad { nbPages ->
                                    val successMsg = "PDF loaded successfully ($nbPages pages)"
                                    Log.d("StudyPoint", successMsg)
                                    Toast.makeText(this@PdfViewActivity, successMsg, Toast.LENGTH_SHORT).show()
                                }
                                .onError { throwable ->
                                    val errorMsg = "Error loading PDF: ${throwable.message}"
                                    Log.e("StudyPoint", "PDF Error: $errorMsg", throwable)
                                    Toast.makeText(this@PdfViewActivity, errorMsg, Toast.LENGTH_LONG).show()
                                }
                                .load()
                        }
                    } else {
                        throw Exception("HTTP $responseCode: ${connection.responseMessage}")
                    }
                } catch (e: Exception) {
                    Log.e("StudyPoint", "Exception: ${e.message}", e)
                    runOnUiThread {
                        val errorMsg = when {
                            e.message?.contains("404") == true -> "PDF file not found. Check if Google Drive link is public."
                            e.message?.contains("403") == true -> "Access denied. Make sure PDF is shared publicly."
                            e.message?.contains("timeout") == true -> "Connection timeout. Check your internet connection."
                            else -> "Failed to load PDF: ${e.message}"
                        }
                        Log.e("StudyPoint", "Showing error: $errorMsg")
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            val errorMsg = "No PDF URL provided"
            Log.e("StudyPoint", "Error: $errorMsg")
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertToDirectDownloadUrl(url: String): String {
        return when {
            // Already a direct download URL
            url.contains("drive.google.com/uc?export=download") -> url

            // Convert sharing URL to direct download
            url.contains("drive.google.com/file/d/") -> {
                val fileId = url.substringAfter("/d/").substringBefore("/")
                "https://drive.google.com/uc?export=download&id=$fileId"
            }

            // Convert view URL to direct download
            url.contains("drive.google.com/open?id=") -> {
                val fileId = url.substringAfter("id=")
                "https://drive.google.com/uc?export=download&id=$fileId"
            }

            // Return as-is for other URLs
            else -> url
        }
    }
}
