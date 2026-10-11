package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Shows sections (Urdu/English) or chapters (Sciences) for a subject.
 * Flow: Subject -> Sections/Chapters -> Materials -> PDFs
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
        val mode = intent.getStringExtra("mode") ?: "sections" // "sections", "content", "chapters"

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        val items: List<StudySection>
        val header: String

        when (mode) {
            "sections" -> {
                // Urdu/English top-level sections
                items = SubjectStructure.getSections(gradeNum, subject)
                header = "$subject Sections"
            }
            "content" -> {
                // Content inside a section (e.g. ghazals list, grammar topics)
                items = SubjectStructure.getSectionContent(gradeNum, subject, sectionId)
                header = if (sectionTitle.isNotBlank()) sectionTitle else "$subject"
            }
            "chapters" -> {
                // Science chapters (shown when Notes is tapped)
                items = SubjectStructure.getScienceChapters(gradeNum, subject)
                header = "$subject Chapters"
            }
            else -> {
                items = emptyList()
                header = subject
            }
        }

        findViewById<TextView>(R.id.headerText).text = header

        val gridItems = items.map { s ->
            val icon = when (s.type) {
                "chapter" -> R.drawable.ic_solved_notes
                else -> R.drawable.ic_textbook
            }
            val displayTitle = if (s.titleUrdu.isNotBlank()) "${s.title}\n${s.titleUrdu}" else s.title
            GridItem(s.id, displayTitle, icon)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GenericGridAdapter(gridItems) { item ->
            when (mode) {
                "sections" -> {
                    // Go deeper: show content of this section
                    val intent = Intent(this, SectionsActivity::class.java).apply {
                        putExtra("grade", gradeNum)
                        putExtra("class", gradeNum)
                        putExtra("subject", subject)
                        putExtra("sectionId", item.id)
                        putExtra("sectionTitle", item.title)
                        putExtra("mode", "content")
                    }
                    startActivity(intent)
                }
                "content", "chapters" -> {
                    // Show materials for this specific chapter/section item
                    val intent = Intent(this, MaterialSelectionActivity::class.java).apply {
                        putExtra("grade", gradeNum)
                        putExtra("class", gradeNum)
                        putExtra("subject", subject)
                        putExtra("chapter", item.id)
                        putExtra("chapterTitle", item.title)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}
