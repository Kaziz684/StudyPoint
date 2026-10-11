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

        // 5 Material Categories: Textbook, Notes, Past Papers, Guess Papers, MCQs
        val materials = listOf(
            GridItem("Textbook", "Textbook", R.drawable.ic_textbook),
            GridItem("Notes", "Solved Notes", R.drawable.ic_solved_notes),
            GridItem("Past Papers", "Past Papers", R.drawable.ic_pastpaper),
            GridItem("Guess Papers", "Guess Papers", R.drawable.ic_pastpaper),
            GridItem("MCQs", "MCQs Practice", R.drawable.ic_mcqs_practice)
        )

        val chapter = intent.getStringExtra("chapter") ?: ""
        val chapterTitle = intent.getStringExtra("chapterTitle") ?: ""

        // Update header if we're inside a chapter/section
        if (chapterTitle.isNotBlank()) {
            findViewById<TextView>(R.id.headerText).text = chapterTitle
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GenericGridAdapter(materials) { item ->
            if (item.id == "MCQs") {
                val sheetName = "Class_${gradeNum}th"
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("sheetName", sheetName)
                    putExtra("subject", subject)
                    putExtra("chapter", chapter)
                }
                startActivity(intent)
            } else if (item.id == "Notes" && SubjectStructure.hasChapterNotes(subject) && chapter.isBlank()) {
                // Sciences: show chapter list when Notes is tapped
                val intent = Intent(this, SectionsActivity::class.java).apply {
                    putExtra("grade", gradeNum)
                    putExtra("class", gradeNum)
                    putExtra("subject", subject)
                    putExtra("mode", "chapters")
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, NotesActivity::class.java).apply {
                    putExtra("grade", gradeNum)
                    putExtra("class", gradeNum)
                    putExtra("subject", subject)
                    putExtra("material", item.id)
                    putExtra("chapter", chapter)
                    putExtra("chapterTitle", chapterTitle)
                }
                startActivity(intent)
            }
        }
    }
}
