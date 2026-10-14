package com.example.studypoint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Firebase Auth instance
        auth = FirebaseAuth.getInstance()

        // If user is not signed in -> LoginActivity
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Show user's name or email
        val name = auth.currentUser?.displayName ?: auth.currentUser?.email
        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tvWelcomeUser).text = "Welcome, $name!"

        // 🔹 Start Button -> Grade Selection
        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener {
            startActivity(Intent(this, GradeSelectionActivity::class.java))
        }

        // 🔹 Logout Button
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "You have been logged out!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // 🔹 Feedback Button -> Email
        findViewById<MaterialButton>(R.id.btnFeedback).setOnClickListener {
            val email = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("alsamadacadmey6844@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Feedback for StudyPoint App")
                putExtra(
                    Intent.EXTRA_TEXT,
                    """
                        Dear StudyPoint Team,
                        
                        I have been using the StudyPoint app and I would like to share my feedback:
                        
                        [Write your feedback here...]
                        
                        Regards,
                        [Your Name]
                    """.trimIndent()
                )
            }
            startActivity(email)
        }

        // 🔹 WhatsApp Channel Button
        findViewById<MaterialButton>(R.id.btnWhatsapp).setOnClickListener {
            val url = "https://whatsapp.com/channel/0029Vah4pV3Au3aVHGzJL805"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // 🔹 Share App Button
        findViewById<MaterialButton>(R.id.btnShare).setOnClickListener {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    " Check out the StudyPoint app for Notes, Past Papers, and MCQs! Download now: https://kaziz684.github.io/StudyPoint/ 🚀"
                )
            }
            startActivity(Intent.createChooser(share, "Share via"))
        }
    }

    // 🔹 Menu Create (Logout Option)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // 🔹 Logout action
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                Toast.makeText(this, "You have been logged out!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}