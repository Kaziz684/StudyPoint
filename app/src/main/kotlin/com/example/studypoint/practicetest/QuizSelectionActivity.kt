package com.example.studypoint.practicetest

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.R
import com.example.studypoint.TextbookBoard
import com.google.android.material.button.MaterialButton

class QuizSelectionActivity : AppCompatActivity() {

    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerSubject: Spinner
    private lateinit var spinnerChapter: Spinner
    private lateinit var spinnerTopic: Spinner
    private lateinit var tvTopicLabel: TextView
    private lateinit var btnStartQuiz: MaterialButton

    private val classes = listOf("Class_9th", "Class_10th", "Class_11th", "Class_12th")
    private val chapters = listOf("All Chapters", "Chapter 0") + (1..28).map { "Chapter $it" }
    private val questionCounts = listOf(10, 15, 20, 25, 30)
    private var selectedCount = 10
    private lateinit var countChips: List<TextView>

    // Topic-wise (Milestone 6 final bundle, v160): Classes 9-12, all
    // mapped subjects. "All Topics" (value "") keeps the chapter-level quiz.
    private fun selectedChapterNumber(): Int? =
        spinnerChapter.selectedItem?.toString()?.let {
            Regex("""(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull()
        }

    private fun selectedClassDigits(): String =
        Regex("""(\d+)""").find(spinnerClass.selectedItem?.toString() ?: "")
            ?.groupValues?.get(1) ?: "9"

    private fun topicLabelsFor(classDigits: String, subject: String, chapterNumber: Int?): List<String> =
        listOf("All Topics") +
            (TopicQuestions.topicsFor(classDigits, subject, chapterNumber).orEmpty()
                .map { "${it.number} ${it.title}" })

    private fun topicNumberOf(label: String?): String {
        if (label.isNullOrBlank() || label == "All Topics") return ""
        return label.substringBefore(" ").trim()
    }

    private fun isTopicChapter(): Boolean {
        val subj = spinnerSubject.selectedItem?.toString() ?: ""
        val chNum = selectedChapterNumber()
        return chNum != null &&
            TopicQuestions.topicsFor(selectedClassDigits(), subj, chNum) != null
    }

    private fun updateTopicVisibility() {
        val show = isTopicChapter()
        if (show) {
            val adapter = ArrayAdapter(this, R.layout.spinner_item_selected, topicLabelsFor(selectedClassDigits(), spinnerSubject.selectedItem?.toString() ?: "", selectedChapterNumber()))
            adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
            spinnerTopic.adapter = adapter
        }
        tvTopicLabel.visibility = if (show) View.VISIBLE else View.GONE
        spinnerTopic.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) spinnerTopic.setSelection(0)
    }

    private fun getSubjectsForClass(className: String): List<String> {
        val baseSubjects = listOf("English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science")
        return when {
            className.contains("12") -> {
                // 12th Class: Statistics is a Science-group subject (v155);
                // Pak Studies replaces Islamiyat; Tarjuma-tul-Quran included
                baseSubjects + listOf("Pak Studies", "Tarjuma-tul-Quran", "Statistics")
            }
            className.contains("10") -> {
                // 10th Class: Pak Studies replaces Islamiyat; Tarjuma-tul-Quran included
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
        // Backstop: content is Punjab PCTB only — other boards show
        // Coming Soon instead of Punjab content.
        if (!TextbookBoard.isPunjab(this)) {
            TextbookBoard.showComingSoon(this) { finish() }
            return
        }
        setContentView(R.layout.activity_quiz_selection)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        spinnerClass = findViewById(R.id.spinnerClass)
        spinnerSubject = findViewById(R.id.spinnerSubject)
        spinnerChapter = findViewById(R.id.spinnerChapter)
        spinnerTopic = findViewById(R.id.spinnerTopic)
        tvTopicLabel = findViewById(R.id.tvTopicLabel)
        btnStartQuiz = findViewById(R.id.btnStartQuiz)

        val topicAdapter = ArrayAdapter(this, R.layout.spinner_item_selected, topicLabelsFor("9", "Physics", 1))
        topicAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spinnerTopic.adapter = topicAdapter

        val classAdapter = ArrayAdapter(this, R.layout.spinner_item_selected, classes)
        classAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spinnerClass.adapter = classAdapter

        val initialSubjects = getSubjectsForClass(classes[0])
        val subjectAdapter = ArrayAdapter(this, R.layout.spinner_item_selected, initialSubjects)
        subjectAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spinnerSubject.adapter = subjectAdapter

        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCls = classes[position]
                val subjectsForSelectedClass = getSubjectsForClass(selectedCls)
                val newSubjectAdapter = ArrayAdapter(this@QuizSelectionActivity, R.layout.spinner_item_selected, subjectsForSelectedClass)
                newSubjectAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
                spinnerSubject.adapter = newSubjectAdapter
                updateTopicVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateTopicVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val chapterAdapter = ArrayAdapter(this, R.layout.spinner_item_selected, chapters)
        chapterAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spinnerChapter.adapter = chapterAdapter

        spinnerChapter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateTopicVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateTopicVisibility()

        // Number of Questions chips (10 / 15 / 20 / 25 / 30, default 10).
        countChips = listOf(
            findViewById(R.id.chipCount10),
            findViewById(R.id.chipCount15),
            findViewById(R.id.chipCount20),
            findViewById(R.id.chipCount25),
            findViewById(R.id.chipCount30)
        )
        countChips.forEachIndexed { i, chip ->
            chip.setOnClickListener {
                selectedCount = questionCounts[i]
                updateCountChips()
            }
        }
        updateCountChips()

        btnStartQuiz.setOnClickListener {
            val selectedClass = spinnerClass.selectedItem?.toString() ?: "Class_9th"
            val selectedSubject = spinnerSubject.selectedItem?.toString() ?: "English"
            val selectedChapter = spinnerChapter.selectedItem?.toString() ?: ""
            val selectedTopic = if (isTopicChapter()) {
                topicNumberOf(spinnerTopic.selectedItem?.toString())
            } else ""

            val chapterParam = if (selectedChapter.isBlank() || selectedChapter == "All Chapters") "" else selectedChapter

            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("sheetName", selectedClass)
                putExtra("subject", selectedSubject)
                putExtra("chapter", chapterParam)
                putExtra("topic", selectedTopic)
                putExtra("count", selectedCount)
            }
            startActivity(intent)
        }
    }

    private fun updateCountChips() {
        countChips.forEachIndexed { i, chip ->
            val selected = questionCounts[i] == selectedCount
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
            )
            chip.setTextColor(
                if (selected) Color.WHITE else Color.parseColor("#475569")
            )
        }
    }
}
