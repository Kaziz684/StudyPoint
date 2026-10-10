package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isPhoneMode = false
    private var isSignupMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Auto-login
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (currentUser.isEmailVerified || currentUser.phoneNumber != null) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
                return
            } else {
                startActivity(Intent(this, EmailVerificationActivity::class.java))
                finish()
                return
            }
        }

        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val emailSection = findViewById<LinearLayout>(R.id.emailSection)
        val phoneSection = findViewById<LinearLayout>(R.id.phoneSection)
        val nameLayout = findViewById<TextInputLayout>(R.id.nameLayout)
        val tvModeToggle = findViewById<TextView>(R.id.tvModeToggle)
        val btnPrimary = findViewById<Button>(R.id.btnPrimaryAction)
        
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val progress = findViewById<ProgressBar>(R.id.loginProgress)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnEmailMode) {
                    isPhoneMode = false
                    emailSection.visibility = View.VISIBLE
                    phoneSection.visibility = View.GONE
                    updateButtonText()
                } else {
                    isPhoneMode = true
                    emailSection.visibility = View.GONE
                    phoneSection.visibility = View.VISIBLE
                    btnPrimary.text = "Send OTP"
                }
            }
        }

        tvModeToggle.setOnClickListener {
            isSignupMode = !isSignupMode
            nameLayout.visibility = if (isSignupMode) View.VISIBLE else View.GONE
            tvModeToggle.text = if (isSignupMode) "Already have an account? Login" else "Don't have an account? Sign Up"
            updateButtonText()
        }

        btnPrimary.setOnClickListener {
            if (isPhoneMode) {
                val phone = phoneInput.text.toString().trim()
                if (phone.length < 10) {
                    phoneInput.error = "Enter valid number"
                    return@setOnClickListener
                }
                startPhoneAuth("+92$phone")
            } else {
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInput.error = "Enter valid Gmail"
                    return@setOnClickListener
                }
                if (password.length < 6) {
                    passwordInput.error = "Password must be 6+ chars"
                    return@setOnClickListener
                }

                if (isSignupMode) {
                    val name = nameInput.text.toString().trim()
                    if (name.isEmpty()) {
                        nameInput.error = "Enter your name"
                        return@setOnClickListener
                    }
                    performSignup(email, password, name, progress)
                } else {
                    performLogin(email, password, progress)
                }
            }
        }
    }

    private fun updateButtonText() {
        if (!isPhoneMode) {
            findViewById<Button>(R.id.btnPrimaryAction).text = if (isSignupMode) "Create Account" else "Login"
        }
    }

    private fun performSignup(email: String, pass: String, name: String, progress: ProgressBar) {
        progress.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                
                user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    user.sendEmailVerification().addOnCompleteListener {
                        progress.visibility = View.GONE
                        startActivity(Intent(this, EmailVerificationActivity::class.java))
                        finish()
                    }
                }
            } else {
                progress.visibility = View.GONE
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performLogin(email: String, pass: String, progress: ProgressBar) {
        progress.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            progress.visibility = View.GONE
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null && user.isEmailVerified) {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    user?.sendEmailVerification()
                    startActivity(Intent(this, EmailVerificationActivity::class.java))
                    finish()
                }
            } else {
                Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startPhoneAuth(phoneNumber: String) {
        val intent = Intent(this, OtpVerificationActivity::class.java)
        intent.putExtra("type", "phone")
        intent.putExtra("target", phoneNumber)
        startActivity(intent)
    }
}
