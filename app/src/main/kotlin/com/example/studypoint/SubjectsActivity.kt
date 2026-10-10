package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SubjectsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_selection)

        val rawGrade = intent.getStringExtra("grade") ?: intent.getStringExtra("class") ?: "9"
        val gradeNum = rawGrade.filter { it.isDigit() }.ifBlank { "9" }

        // Set Header
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.headerText).text = "Class ${gradeNum}th Subjects"

        // PCTB Curriculum Rules:
        // - All Classes (9th, 10th, 11th, 12th): Must include "Tarjuma-tul-Quran" (ترجمۃ القرآن) as a dedicated subject section.
        // - 9th & 11th Classes: Must include "Islamiyat" (اسلامیات) compulsory.
        // - 10th & 12th Classes: REMOVE "Islamiyat" completely and REPLACE/ADD "Pak Studies" (پاکستان اسٹڈیز).
        val isMatricOrInterSecondYear = (gradeNum == "10" || gradeNum == "12")

        val subjects = mutableListOf(
            GridItem("English", "English", R.drawable.ic_urdu_english),
            GridItem("Urdu", "Urdu", R.drawable.ic_urdu_english),
            GridItem("Mathematics", "Mathematics", R.drawable.ic_mathematics),
            GridItem("Physics", "Physics", R.drawable.ic_physics),
            GridItem("Chemistry", "Chemistry", R.drawable.ic_chemistry),
            GridItem("Biology", "Biology", R.drawable.ic_biology),
            GridItem("Computer Science", "Computer Science", R.drawable.ic_computer)
        )

        if (isMatricOrInterSecondYear) {
            // 10th & 12th Classes: Add Pak Studies, remove Islamiyat
            subjects.add(GridItem("Pak Studies", "Pak Studies", R.drawable.ic_pak_studies))
        } else {
            // 9th & 11th Classes: Add Islamiyat compulsory, omit Pak Studies
            subjects.add(GridItem("Islamiyat", "Islamiyat", R.drawable.ic_islamiyat))
        }

        // All Classes (9th, 10th, 11th, 12th): Dedicated Tarjuma-tul-Quran section
        subjects.add(GridItem("Tarjuma-tul-Quran", "Tarjuma-tul-Quran", R.drawable.ic_islamiyat))

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GenericGridAdapter(subjects) { item ->
            val intent = Intent(this, MaterialSelectionActivity::class.java)
            intent.putExtra("grade", gradeNum)
            intent.putExtra("class", gradeNum)
            intent.putExtra("subject", item.id)
            startActivity(intent)
        }
    }
}
