package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MaterialSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_material_selection)

        val grade = intent.getStringExtra("grade") ?: ""
        val subject = intent.getStringExtra("subject") ?: ""

        fun go(material: String) {
            val i = Intent(this, NotesActivity::class.java)
            i.putExtra("grade", grade)
            i.putExtra("subject", subject)
            i.putExtra("material", material)
            startActivity(i)
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNotes).setOnClickListener { go("Notes") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHandwritten).setOnClickListener { go("Handwritten Notes") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBook).setOnClickListener { go("Book") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnKeyBook).setOnClickListener { go("Key Book") }
    }
}
