package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val btnCheck = findViewById<Button>(R.id.btnCheckVerification)
        val btnResend = findViewById<TextView>(R.id.btnResendEmail)
        val progress = findViewById<ProgressBar>(R.id.verificationProgress)

        btnCheck.setOnClickListener {
            progress.visibility = View.VISIBLE
            user?.reload()?.addOnCompleteListener {
                progress.visibility = View.GONE
                if (user.isEmailVerified) {
                    Toast.makeText(this, "Email Verified!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnResend.setOnClickListener {
            progress.visibility = View.VISIBLE
            user?.sendEmailVerification()?.addOnCompleteListener {
                progress.visibility = View.GONE
                if (it.isSuccessful) {
                    Toast.makeText(this, "Verification link sent.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
