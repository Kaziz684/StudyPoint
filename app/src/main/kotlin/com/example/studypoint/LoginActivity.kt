package com.example.studypoint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isSignupMode = false

    // CAPTCHA answers
    private var captchaLoginAnswer = 0
    private var captchaSignupAnswer = 0

    // Views
    private lateinit var tabLogin: TextView
    private lateinit var tabSignup: TextView
    private lateinit var loginContainer: LinearLayout
    private lateinit var signupContainer: LinearLayout
    private lateinit var loadingOverlay: FrameLayout

    // Login views
    private lateinit var etLoginEmail: TextInputEditText
    private lateinit var etLoginPassword: TextInputEditText
    private lateinit var tilLoginPassword: TextInputLayout
    private lateinit var cbRemember: CheckBox
    private lateinit var tvForgot: TextView
    private lateinit var tvCaptchaLogin: TextView
    private lateinit var etCaptchaLogin: EditText

    // Signup views
    private lateinit var etSignupName: TextInputEditText
    private lateinit var etSignupEmail: TextInputEditText
    private lateinit var etSignupPassword: TextInputEditText
    private lateinit var etSignupConfirm: TextInputEditText
    private lateinit var tilSignupPassword: TextInputLayout
    private lateinit var passwordStrength: ProgressBar
    private lateinit var tvStrengthLabel: TextView
    private lateinit var tvCaptchaSignup: TextView
    private lateinit var etCaptchaSignup: EditText
    private lateinit var cbTerms: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Auto-login check
        val prefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
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

        initViews()
        setupTabs()
        setupPasswordToggles()
        setupCaptcha()
        setupPasswordStrength()
        setupButtons()

        // Restore remembered email
        val rememberedEmail = prefs.getString("remembered_email", "")
        if (!rememberedEmail.isNullOrEmpty()) {
            etLoginEmail.setText(rememberedEmail)
            cbRemember.isChecked = true
        }
    }

    private fun initViews() {
        tabLogin = findViewById(R.id.tabLogin)
        tabSignup = findViewById(R.id.tabSignup)
        loginContainer = findViewById(R.id.loginContainer)
        signupContainer = findViewById(R.id.signupContainer)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        etLoginEmail = findViewById(R.id.etLoginEmail)
        etLoginPassword = findViewById(R.id.etLoginPassword)
        tilLoginPassword = findViewById(R.id.tilLoginPassword)
        cbRemember = findViewById(R.id.cbRemember)
        tvForgot = findViewById(R.id.tvForgot)
        tvCaptchaLogin = findViewById(R.id.tvCaptchaLogin)
        etCaptchaLogin = findViewById(R.id.etCaptchaLogin)

        etSignupName = findViewById(R.id.etSignupName)
        etSignupEmail = findViewById(R.id.etSignupEmail)
        etSignupPassword = findViewById(R.id.etSignupPassword)
        etSignupConfirm = findViewById(R.id.etSignupConfirm)
        tilSignupPassword = findViewById(R.id.tilSignupPassword)
        passwordStrength = findViewById(R.id.passwordStrength)
        tvStrengthLabel = findViewById(R.id.tvStrengthLabel)
        tvCaptchaSignup = findViewById(R.id.tvCaptchaSignup)
        etCaptchaSignup = findViewById(R.id.etCaptchaSignup)
        cbTerms = findViewById(R.id.cbTerms)
    }

    private fun setupTabs() {
        tabLogin.setOnClickListener { switchTab(false) }
        tabSignup.setOnClickListener { switchTab(true) }
        findViewById<TextView>(R.id.tvGoLogin).setOnClickListener { switchTab(false) }
    }

    private fun switchTab(toSignup: Boolean) {
        isSignupMode = toSignup

        if (toSignup) {
            tabSignup.setBackgroundResource(R.drawable.tab_selected_bg)
            tabSignup.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            tabSignup.setTypeface(null, android.graphics.Typeface.BOLD)
            tabLogin.background = null
            tabLogin.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            tabLogin.setTypeface(null, android.graphics.Typeface.NORMAL)

            loginContainer.visibility = View.GONE
            signupContainer.visibility = View.VISIBLE
            signupContainer.alpha = 0f
            signupContainer.animate().alpha(1f).setDuration(300).start()
        } else {
            tabLogin.setBackgroundResource(R.drawable.tab_selected_bg)
            tabLogin.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            tabLogin.setTypeface(null, android.graphics.Typeface.BOLD)
            tabSignup.background = null
            tabSignup.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            tabSignup.setTypeface(null, android.graphics.Typeface.NORMAL)

            signupContainer.visibility = View.GONE
            loginContainer.visibility = View.VISIBLE
            loginContainer.alpha = 0f
            loginContainer.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun setupPasswordToggles() {
        var loginVisible = false
        tilLoginPassword.setEndIconOnClickListener {
            loginVisible = !loginVisible
            etLoginPassword.transformationMethod = if (loginVisible)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            etLoginPassword.setSelection(etLoginPassword.text?.length ?: 0)
            tilLoginPassword.endIconDrawable = ContextCompat.getDrawable(
                this, if (loginVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
        }

        var signupVisible = false
        tilSignupPassword.setEndIconOnClickListener {
            signupVisible = !signupVisible
            etSignupPassword.transformationMethod = if (signupVisible)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            etSignupPassword.setSelection(etSignupPassword.text?.length ?: 0)
            tilSignupPassword.endIconDrawable = ContextCompat.getDrawable(
                this, if (signupVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
        }
    }

    private fun setupCaptcha() {
        generateCaptchaLogin()
        generateCaptchaSignup()

        findViewById<ImageButton>(R.id.btnRefreshCaptchaLogin).setOnClickListener {
            generateCaptchaLogin()
        }
        findViewById<ImageButton>(R.id.btnRefreshCaptchaSignup).setOnClickListener {
            generateCaptchaSignup()
        }
    }

    private fun generateCaptchaLogin() {
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)
        captchaLoginAnswer = a + b
        tvCaptchaLogin.text = "$a + $b = ?"
        etCaptchaLogin.text.clear()
    }

    private fun generateCaptchaSignup() {
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)
        captchaSignupAnswer = a + b
        tvCaptchaSignup.text = "$a + $b = ?"
        etCaptchaSignup.text.clear()
    }

    private fun setupPasswordStrength() {
        etSignupPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateStrength(s.toString())
            }
        })
    }

    private fun updateStrength(password: String) {
        var score = 0
        if (password.length >= 6) score += 25
        if (password.length >= 8) score += 25
        if (password.any { it.isDigit() }) score += 25
        if (password.any { !it.isLetterOrDigit() } || password.any { it.isUpperCase() }) score += 25

        passwordStrength.progress = score

        val (label, color) = when {
            password.isEmpty() -> "" to android.R.color.darker_gray
            score < 50 -> "Weak" to android.R.color.holo_red_light
            score < 75 -> "Medium" to android.R.color.holo_orange_light
            else -> "Strong" to android.R.color.holo_green_light
        }
        tvStrengthLabel.text = label
        if (label.isNotEmpty()) {
            tvStrengthLabel.setTextColor(ContextCompat.getColor(this, color))
        }
        val tintColor = when {
            password.isEmpty() -> "#22FFFFFF"
            score < 50 -> "#FF5252"
            score < 75 -> "#FF9800"
            else -> "#4CAF50"
        }
        passwordStrength.progressTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(tintColor)
        )
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnLogin).setOnClickListener { handleLogin() }
        findViewById<Button>(R.id.btnSignup).setOnClickListener { handleSignup() }

        tvForgot.setOnClickListener { handleForgotPassword() }

        findViewById<Button>(R.id.btnGoogle).setOnClickListener {
            Toast.makeText(this, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnPhoneOtp).setOnClickListener {
            showPhoneDialog()
        }
    }

    private fun handleLogin() {
        val email = etLoginEmail.text.toString().trim()
        val password = etLoginPassword.text.toString().trim()
        val captchaAnswer = etCaptchaLogin.text.toString().trim()

        if (email.isEmpty()) {
            etLoginEmail.error = "Enter email or mobile number"
            return
        }
        if (password.length < 6) {
            etLoginPassword.error = "Password must be 6+ characters"
            return
        }
        if (captchaAnswer.isEmpty() || captchaAnswer.toIntOrNull() != captchaLoginAnswer) {
            etCaptchaLogin.error = "Wrong answer, try again"
            generateCaptchaLogin()
            return
        }

        // Save remembered email
        val prefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        if (cbRemember.isChecked) {
            prefs.edit().putString("remembered_email", email).apply()
        } else {
            prefs.edit().remove("remembered_email").apply()
        }

        // If it's a phone number, go to OTP
        if (email.all { it.isDigit() } && email.length >= 10) {
            startPhoneAuth("+92$email")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.error = "Enter a valid email address"
            return
        }

        showLoading(true)
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            showLoading(false)
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
                generateCaptchaLogin()
            }
        }
    }

    private fun handleSignup() {
        val name = etSignupName.text.toString().trim()
        val email = etSignupEmail.text.toString().trim()
        val password = etSignupPassword.text.toString()
        val confirm = etSignupConfirm.text.toString()
        val captchaAnswer = etCaptchaSignup.text.toString().trim()

        if (name.isEmpty()) {
            etSignupName.error = "Enter your full name"
            return
        }
        if (email.isEmpty()) {
            etSignupEmail.error = "Enter email or phone number"
            return
        }
        if (password.length < 6) {
            etSignupPassword.error = "Password must be 6+ characters"
            return
        }
        if (password != confirm) {
            etSignupConfirm.error = "Passwords do not match"
            return
        }
        if (captchaAnswer.isEmpty() || captchaAnswer.toIntOrNull() != captchaSignupAnswer) {
            etCaptchaSignup.error = "Wrong answer, try again"
            generateCaptchaSignup()
            return
        }
        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Please accept Terms & Conditions", Toast.LENGTH_SHORT).show()
            return
        }

        // Phone number signup -> OTP
        if (email.all { it.isDigit() } && email.length >= 10) {
            Toast.makeText(this, "Use Phone OTP login for mobile numbers", Toast.LENGTH_LONG).show()
            switchTab(false)
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etSignupEmail.error = "Enter a valid email address"
            return
        }

        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    user.sendEmailVerification().addOnCompleteListener {
                        showLoading(false)
                        Toast.makeText(this, "Account created! Verify your email.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, EmailVerificationActivity::class.java))
                        finish()
                    }
                }
            } else {
                showLoading(false)
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                generateCaptchaSignup()
            }
        }
    }

    private fun handleForgotPassword() {
        val email = etLoginEmail.text.toString().trim()
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter your email above first", Toast.LENGTH_SHORT).show()
            return
        }
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPhoneDialog() {
        val input = EditText(this).apply {
            hint = "03XXXXXXXXX"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(32, 24, 32, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Phone OTP Login")
            .setMessage("Enter your mobile number")
            .setView(input)
            .setPositiveButton("Send OTP") { _, _ ->
                val phone = input.text.toString().trim()
                if (phone.length >= 10) {
                    val formatted = if (phone.startsWith("0")) "+92${phone.substring(1)}" else "+92$phone"
                    startPhoneAuth(formatted)
                } else {
                    Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startPhoneAuth(phoneNumber: String) {
        val intent = Intent(this, OtpVerificationActivity::class.java)
        intent.putExtra("type", "phone")
        intent.putExtra("target", phoneNumber)
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
}
