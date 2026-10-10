package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 3 seconds delay for Splash Screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, 3000)
    }

    private fun checkAuthAndNavigate() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Check if user is verified via email OR logged in via phone
            if (currentUser.isEmailVerified || currentUser.phoneNumber != null) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // User logged in via email but NOT verified
                startActivity(Intent(this, EmailVerificationActivity::class.java))
            }
        } else {
            // User is not signed in, go to Login screen
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}
