package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Email verification has been REMOVED from StudyPoint's auth flow.
 * Accounts are active immediately after sign-up / sign-in.
 *
 * This activity is kept only as a safe pass-through so that no old
 * link, shortcut or saved intent can ever trap a user on a
 * verification screen: it forwards straight to Home when signed in,
 * or to Login when signed out.
 */
class EmailVerificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        } catch (e: Exception) {
            try {
                startActivity(Intent(this, LoginActivity::class.java))
            } catch (ignored: Exception) {
            }
        }
        finish()
    }
}
