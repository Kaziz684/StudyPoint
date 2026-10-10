package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    
    private lateinit var otpInput: EditText
    private lateinit var tvTarget: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnResend: TextView
    private lateinit var btnVerify: Button

    private var target: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        auth = FirebaseAuth.getInstance()
        target = intent.getStringExtra("target")

        otpInput = findViewById(R.id.otpInput)
        tvTarget = findViewById(R.id.tvOtpTarget)
        tvTimer = findViewById(R.id.tvResendTimer)
        btnResend = findViewById(R.id.btnResend)
        btnVerify = findViewById(R.id.btnVerify)

        tvTarget.text = "Sent to $target"

        startPhoneVerification()

        btnVerify.setOnClickListener {
            val code = otpInput.text.toString().trim()
            if (code.length == 6) {
                verifyPhoneCode(code)
            } else {
                Toast.makeText(this, "Enter 6 digits", Toast.LENGTH_SHORT).show()
            }
        }

        btnResend.setOnClickListener {
            startResendTimer()
            startPhoneVerification()
        }

        startResendTimer()
    }

    private fun startResendTimer() {
        btnResend.visibility = View.GONE
        tvTimer.visibility = View.VISIBLE
        
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "Resend Code in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                tvTimer.visibility = View.GONE
                btnResend.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun startPhoneVerification() {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(target!!)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    val code = credential.smsCode
                    if (code != null) {
                        otpInput.setText(code)
                        verifyPhoneCode(code)
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@OtpVerificationActivity, e.message, Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    resendToken = token
                    Toast.makeText(this@OtpVerificationActivity, "Code Sent", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyPhoneCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this, HomeActivity::class.java))
                finishAffinity()
            } else {
                Toast.makeText(this, "Invalid Code", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
