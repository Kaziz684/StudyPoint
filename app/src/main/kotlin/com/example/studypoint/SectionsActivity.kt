package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Shows chapters (Sciences), grammar topics (English), or section content (Urdu).
 * Tapping any item goes DIRECTLY to content - no materials screen again.
 */
class SectionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_selection)

        val rawGrade = intent.getStringExtra("grade") ?: intent.getStringExtra("class") ?: "9"
        val gradeNum = rawGrade.filter { it.isDigit() }.ifBlank { "9" }
        val subject = intent.getStringExtra("subject") ?: ""
        val sectionId = intent.getStringExtra("sectionId") ?: ""
        val sectionTitle = intent.getStringExtra("sectionTitle") ?: ""
        val mode = intent.getStringExtra("mode") ?: "chapters"

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        val items: List<StudySection>
        val header: String

        when (mode) {
            "chapters" -> {
                items = SubjectStructure.getScienceChapters(gradeNum, subject)
                header = "$subject - Chapters"
            }
            "content" -> {
                items = SubjectStructure.getSectionContent(gradeNum, subject, sectionId)
                header = if (sectionTitle.isNotBlank()) sectionTitle else subject
            }
            else -> {
                items = emptyList()
                header = subject
            }
        }

        findViewById<TextView>(R.id.headerText).text = header

        // Single-column list rows: full width, title wraps to 2 lines with
        // ellipsis, Urdu gloss / category-author shown as subtitle line.
        val gridItems = items.map { s ->
            GridItem(s.id, s.title, ChapterIcons.forChapter(subject, s.id, s.title), s.titleUrdu)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = GenericGridAdapter(gridItems, useListRow = true) { item ->
            // Go DIRECTLY to NotesActivity - show "Coming Soon" if no PDF linked yet
            // The chapter/section name is passed so NotesActivity can show relevant content
            val intent = Intent(this, NotesActivity::class.java).apply {
                putExtra("grade", gradeNum)
                putExtra("class", gradeNum)
                putExtra("subject", subject)
                putExtra("material", "Notes")
                putExtra("chapter", item.id)
                putExtra("chapterTitle", item.title)
            }
            startActivity(intent)
        }
    }
}
