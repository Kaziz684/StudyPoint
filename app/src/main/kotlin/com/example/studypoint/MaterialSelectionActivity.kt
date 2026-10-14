package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studypoint.practicetest.QuizActivity
import com.example.studypoint.practicetest.PaperGeneratorActivity
import com.example.studypoint.practicetest.TestGeneratorActivity

class MaterialSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_selection)

        val rawGrade = intent.getStringExtra("grade") ?: intent.getStringExtra("class") ?: "9"
        val gradeNum = rawGrade.filter { it.isDigit() }.ifBlank { "9" }
        val subject = intent.getStringExtra("subject") ?: ""
        val subjectLower = subject.lowercase()

        // Set Header
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.headerText).text = if (subject.isNotBlank()) "$subject" else "Study Material"

        // Build materials list based on subject type
        val materials = mutableListOf(
            GridItem("Textbook", "Textbook", R.drawable.ic_textbook),
            GridItem("Notes", "Solved Notes", R.drawable.ic_solved_notes),
            GridItem("Past Papers", "Past Papers", R.drawable.ic_pastpaper),
            GridItem("Guess Papers", "Guess Papers", R.drawable.ic_pastpaper),
            GridItem("MCQs", "MCQs Practice", R.drawable.ic_mcqs_practice),
            GridItem("TestGenerator", "Generate MCQs Test", R.drawable.ic_mcqs_practice),
            GridItem("PaperGenerator", "Paper Generator", R.drawable.ic_mcqs_practice)
        )

        // English: add Grammar as a material
        if (subjectLower.contains("english")) {
            materials.add(GridItem("Grammar", "Grammar", R.drawable.ic_solved_notes))
        }

        // Urdu: add 3 sections as materials
        if (subjectLower.contains("urdu")) {
            materials.add(GridItem("urdu_ghazal", "حصہ غزل", R.drawable.ic_textbook))
            materials.add(GridItem("urdu_nazm", "حصہ نظم", R.drawable.ic_textbook))
            materials.add(GridItem("urdu_asbaq", "اسباق", R.drawable.ic_textbook))
            materials.add(GridItem("urdu_farhang", "فرہنگ", R.drawable.ic_textbook))
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GenericGridAdapter(materials) { item ->
            when {
                item.id == "MCQs" -> {
                    val sheetName = "Class_${gradeNum}th"
                    val intent = Intent(this, QuizActivity::class.java).apply {
                        putExtra("sheetName", sheetName)
                        putExtra("subject", subject)
                        putExtra("chapter", "")
                    }
                    startActivity(intent)
                }
                item.id == "PaperGenerator" -> {
                    val intent = Intent(this, PaperGeneratorActivity::class.java).apply {
                        putExtra("grade", gradeNum)
                        putExtra("subject", subject)
                    }
                    startActivity(intent)
                }
                item.id == "TestGenerator" -> {
                    val intent = Intent(this, TestGeneratorActivity::class.java).apply {
                        putExtra("grade", gradeNum)
                        putExtra("subject", subject)
                    }
                    startActivity(intent)
                }
                item.id == "Notes" && SubjectStructure.hasChapterNotes(subject) -> {
                    // Sciences: Notes -> chapter list -> DIRECT to content (no materials again)
                    val intent = Intent(this, SectionsActivity::class.java).apply {
                        putExtra("grade", gradeNum)
                        putExtra("class", gradeNum)
                        putExtra("subject", subject)
                        putExtra("mode", "chapters")
                    }
                    startActivity(intent)
                }
                item.id == "Grammar" -> {
                    // English Grammar -> topic list -> DIRECT to content
                    val intent = Intent(this, SectionsActivity::class.java).apply {
                        putExtra("grade", gradeNum)
                        putExtra("class", gradeNum)
                        putExtra("subject", subject)
                        putExtra("mode", "content")
                        putExtra("sectionId", "eng_grammar")
                        putExtra("sectionTitle", "Grammar")
                    }
                    startActivity(intent)
                }
                item.id.startsWith("urdu_") -> {
                    // Urdu sections -> content list -> DIRECT to content
                    val intent = Intent(this, SectionsActivity::class.java).apply {
                        putExtra("grade", gradeNum)
                        putExtra("class", gradeNum)
                        putExtra("subject", subject)
                        putExtra("mode", "content")
                        putExtra("sectionId", item.id)
                        putExtra("sectionTitle", item.title)
                    }
                    startActivity(intent)
                }
                else -> {
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
}
