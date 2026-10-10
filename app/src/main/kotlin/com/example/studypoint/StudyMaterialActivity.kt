package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StudyMaterialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedClass = intent.getStringExtra("grade") ?: intent.getStringExtra("class") ?: "9"
        val selectedSubject = intent.getStringExtra("subject") ?: "English"

        // Redirect to MaterialSelectionActivity to present the 4 PCTB material categories:
        // Textbook, Notes, Past Papers, MCQs (Keybook strictly excluded)
        val intent = Intent(this, MaterialSelectionActivity::class.java).apply {
            putExtra("grade", selectedClass)
            putExtra("class", selectedClass)
            putExtra("subject", selectedSubject)
        }
        startActivity(intent)
        finish()
    }
}
