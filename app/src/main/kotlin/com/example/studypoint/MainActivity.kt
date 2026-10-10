package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check authentication status
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User is signed in, go to home screen
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // User is not signed in, go to login screen
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}