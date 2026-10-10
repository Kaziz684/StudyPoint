package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studypoint.practicetest.QuizActivity

class MaterialSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_selection)

        val rawGrade = intent.getStringExtra("grade") ?: intent.getStringExtra("class") ?: "9"
        val gradeNum = rawGrade.filter { it.isDigit() }.ifBlank { "9" }
        val subject = intent.getStringExtra("subject") ?: ""

        // Set Header
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.headerText).text = if (subject.isNotBlank()) "$subject Materials" else "Study Material"

        // Strictly 4 Material Categories: Textbook, Notes, Past Papers, MCQs (Keybook strictly excluded)
        val materials = listOf(
            GridItem("Textbook", "Textbook", R.drawable.ic_textbook),
            GridItem("Notes", "Solved Notes", R.drawable.ic_solved_notes),
            GridItem("Past Papers", "Past Papers", R.drawable.ic_pastpaper),
            GridItem("MCQs", "MCQs Practice", R.drawable.ic_mcqs_practice)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GenericGridAdapter(materials) { item ->
            if (item.id == "MCQs") {
                val sheetName = "Class_${gradeNum}th"
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("sheetName", sheetName)
                    putExtra("subject", subject)
                    putExtra("chapter", "")
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, NotesActivity::class.java).apply {
                    putExtra("grade", gradeNum)
                    putExtra("class", gradeNum)
                    putExtra("subject", subject)
                    putExtra("material", item.id)
                }
                startActivity(intent)
            }
        }
    }
}
