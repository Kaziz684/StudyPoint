package com.example.studypoint

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AppUpdateManager {
    const val GITHUB_USER = "shaheen-computer"
    const val GITHUB_REPO = "StudyPoint"
    const val CURRENT_VERSION = "1.0"
    private const val TAG = "AppUpdateManager"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    fun checkUpdateManually(context: Context) {
        val url = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest"
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .build()

        Toast.makeText(context, "Checking for latest updates...", Toast.LENGTH_SHORT).show()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Could not check for updates. Check internet connection.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "You are using the latest version ($CURRENT_VERSION)", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    try {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val tagName = json.optString("tag_name", "").trim()
                        val cleanTag = tagName.removePrefix("v").removePrefix("V").trim()
                        val cleanCurrent = CURRENT_VERSION.removePrefix("v").removePrefix("V").trim()

                        if (isNewer(cleanTag, cleanCurrent)) {
                            val assets = json.optJSONArray("assets")
                            var downloadUrl = ""
                            if (assets != null && assets.length() > 0) {
                                downloadUrl = assets.getJSONObject(0).optString("browser_download_url", "")
                            }
                            if (downloadUrl.isBlank()) {
                                downloadUrl = json.optString("html_url", "https://github.com/$GITHUB_USER/$GITHUB_REPO/releases/latest")
                            }

                            Handler(Looper.getMainLooper()).post {
                                AlertDialog.Builder(context)
                                    .setTitle("Update Available! 🚀")
                                    .setMessage("A new version of StudyPoint ($tagName) is ready for download.\n\nPlease update to enjoy new lectures, solved notes, and performance improvements.")
                                    .setPositiveButton("Update Now") { _, _ ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to launch update URL: ${e.message}")
                                        }
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "You are using the latest version of StudyPoint ($CURRENT_VERSION) 🎉", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "You are using the latest version ($CURRENT_VERSION)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun isNewer(latest: String, current: String): Boolean {
        if (latest.isBlank() || current.isBlank()) return false
        return try {
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val length = maxOf(latestParts.size, currentParts.size)

            for (i in 0 until length) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            latest != current
        }
    }
}
