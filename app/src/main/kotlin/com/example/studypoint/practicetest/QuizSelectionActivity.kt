package com.example.studypoint.practicetest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.R
import com.google.android.material.button.MaterialButton

class QuizSelectionActivity : AppCompatActivity() {

    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerSubject: Spinner
    private lateinit var spinnerChapter: Spinner
    private lateinit var btnStartQuiz: MaterialButton

    private val classes = listOf("Class_9th", "Class_10th", "Class_11th", "Class_12th")
    private val chapters = listOf("All Chapters") + (1..25).map { "Chapter $it" }

    private fun getSubjectsForClass(className: String): List<String> {
        val baseSubjects = listOf("English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science")
        return when {
            className.contains("10") || className.contains("12") -> {
                // 10th & 12th Classes: Pak Studies replaces Islamiyat; Tarjuma-tul-Quran included
                baseSubjects + listOf("Pak Studies", "Tarjuma-tul-Quran")
            }
            else -> {
                // 9th & 11th Classes: Islamiyat compulsory; Tarjuma-tul-Quran included
                baseSubjects + listOf("Islamiyat", "Tarjuma-tul-Quran")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_selection)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        spinnerClass = findViewById(R.id.spinnerClass)
        spinnerSubject = findViewById(R.id.spinnerSubject)
        spinnerChapter = findViewById(R.id.spinnerChapter)
        btnStartQuiz = findViewById(R.id.btnStartQuiz)

        val classAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, classes)
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerClass.adapter = classAdapter

        val initialSubjects = getSubjectsForClass(classes[0])
        val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, initialSubjects)
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubject.adapter = subjectAdapter

        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCls = classes[position]
                val subjectsForSelectedClass = getSubjectsForClass(selectedCls)
                val newSubjectAdapter = ArrayAdapter(this@QuizSelectionActivity, android.R.layout.simple_spinner_item, subjectsForSelectedClass)
                newSubjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSubject.adapter = newSubjectAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val chapterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, chapters)
        chapterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChapter.adapter = chapterAdapter

        btnStartQuiz.setOnClickListener {
            val selectedClass = spinnerClass.selectedItem?.toString() ?: "Class_9th"
            val selectedSubject = spinnerSubject.selectedItem?.toString() ?: "English"
            val selectedChapter = spinnerChapter.selectedItem?.toString() ?: ""

            val chapterParam = if (selectedChapter.isBlank() || selectedChapter == "All Chapters") "" else selectedChapter

            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("sheetName", selectedClass)
                putExtra("subject", selectedSubject)
                putExtra("chapter", chapterParam)
            }
            startActivity(intent)
        }
    }
}
