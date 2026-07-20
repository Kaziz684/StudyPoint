package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GradeSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grade_selection)

        fun go(grade: String) {
            val i = Intent(this, SubjectsActivity::class.java)
            i.putExtra("grade", grade)
            startActivity(i)
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClass9).setOnClickListener { go("9") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClass10).setOnClickListener { go("10") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClass11).setOnClickListener { go("11") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClass12).setOnClickListener { go("12") }
    }
}
