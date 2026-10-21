package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_splash)
        } catch (_: Throwable) {
            // If even the splash layout fails, fall back to Login.
            try {
                startActivity(Intent(this, LoginActivity::class.java))
            } catch (ignored: Throwable) {
            }
            finish()
            return
        }

        // If the previous launch crashed, show its recorded report FIRST
        // (before navigating anywhere) so a screenshot of this dialog
        // finally reveals the exact cause. Continue resumes normal flow.
        val crashFile = java.io.File(filesDir, "last_crash.txt")
        if (crashFile.exists()) {
            val report = try {
                crashFile.readText()
            } catch (_: Throwable) {
                "Could not read the crash report."
            }
            AlertDialog.Builder(this)
                .setTitle("Last crash report")
                .setMessage(
                    "App pichhli dafa is error se band hui thi. " +
                        "Is screen ka screenshot bhej dein.\n\n" +
                        report.take(2000)
                )
                .setCancelable(false)
                .setPositiveButton("Continue") { _, _ ->
                    try {
                        crashFile.delete()
                    } catch (ignored: Throwable) {
                    }
                    startSplashFlow()
                }
                .show()
            return
        }
        startSplashFlow()
    }

    private fun startSplashFlow() {
        // Entrance animation: fade + scale
        val content = findViewById<LinearLayout>(R.id.splashContent)
        content.alpha = 0f
        content.scaleX = 0.92f
        content.scaleY = 0.92f
        content.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(900).start()

        // 3 seconds delay for Splash Screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, 3000)
    }

    private fun checkAuthAndNavigate() {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Signed in (email, Google or phone) — no verification gate.
                startActivity(Intent(this, RoleGateActivity::class.java))
            } else {
                // User is not signed in, go to Login screen
                startActivity(Intent(this, LoginActivity::class.java))
            }
        } catch (_: Throwable) {
            // Never crash on the splash screen; fall back to Login.
            try {
                startActivity(Intent(this, LoginActivity::class.java))
            } catch (ignored: Throwable) {
            }
        }
        finish()
    }
}
