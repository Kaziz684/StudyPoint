package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SubjectsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subjects)

        val grade = intent.getStringExtra("grade") ?: ""

        fun go(subject: String) {
            val i = Intent(this, MaterialSelectionActivity::class.java)
            i.putExtra("grade", grade)
            i.putExtra("subject", subject)
            startActivity(i)
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMath).setOnClickListener { go("Mathematics") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPhysics).setOnClickListener { go("Physics") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnChem).setOnClickListener { go("Chemistry") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBio).setOnClickListener { go("Biology") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCS).setOnClickListener { go("Computer") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEnglish).setOnClickListener { go("English") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUrdu).setOnClickListener { go("Urdu") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnIsl).setOnClickListener { go("Islamiat") }
    }
}
