package com.example.studypoint.practicetest

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.studypoint.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Paper Generator — one screen for MCQs, Short Questions, Long Questions and
 * Combined papers. MCQs come from the class tabs (same source as the quiz);
 * Short/Long questions come from the "Shorts"/"Longs" tabs of the same
 * spreadsheet. Generates a printable PDF (solved or unsolved) with preview,
 * share and save — reusing the established generator patterns of this app.
 */
class PaperGeneratorActivity : AppCompatActivity() {

    private enum class PaperType { MCQS, SHORT, LONG, COMBINED, BOARD }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadJob: Job? = null
    private lateinit var quizRepository: QuizRepository
    private lateinit var paperRepository: PaperRepository

    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerSubject: Spinner
    private lateinit var progress: ProgressBar
    private lateinit var tvChapterStatus: TextView
    private lateinit var checkAllChapters: CheckBox
    private lateinit var layoutChapters: LinearLayout
    private lateinit var radioTypeMcqs: RadioButton
    private lateinit var radioTypeShort: RadioButton
    private lateinit var radioTypeLong: RadioButton
    private lateinit var radioTypeCombined: RadioButton
    private lateinit var radioTypeBoard: RadioButton
    private lateinit var tvBoardInfo: TextView
    private lateinit var layoutChapterSection: LinearLayout
    private lateinit var layoutModeSection: LinearLayout
    private lateinit var layoutOrderSection: LinearLayout
    private lateinit var radioCustom: RadioButton
    private lateinit var etCustomCount: EditText
    private lateinit var radioSolved: RadioButton
    private lateinit var radioRandom: RadioButton
    private lateinit var etTitle: EditText
    private lateinit var etTime: EditText
    private lateinit var btnGenerate: Button
    private lateinit var tvStatus: TextView
    private lateinit var layoutActions: LinearLayout

    private val classLabels = listOf("Class 9th", "Class 10th", "Class 11th", "Class 12th")
    private val classSheets = listOf("Class_9th", "Class_10th", "Class_11th", "Class_12th")

    private var mcqAll: List<Question> = emptyList()
    private var shortRaw: List<WrittenQuestion> = emptyList()
    private var longRaw: List<WrittenQuestion> = emptyList()
    private var shortTabLoaded = false
    private var longTabLoaded = false
    private var shortAll: List<WrittenQuestion> = emptyList()
    private var longAll: List<WrittenQuestion> = emptyList()
    private var chapterNames: List<String> = emptyList()
    private val chapterChecks = mutableListOf<CheckBox>()
    private var lastFile: File? = null
    private var loading = false

    private lateinit var radioModeManual: RadioButton
    private lateinit var layoutAuto: LinearLayout
    private lateinit var layoutManual: LinearLayout
    private lateinit var etCountShort: EditText
    private lateinit var etCountLong: EditText
    private lateinit var radioSelectRandom: RadioButton
    private lateinit var etSearch: EditText
    private lateinit var layoutQuestions: LinearLayout
    private val selectedMcqIds = mutableSetOf<Int>()
    private val selectedShortIds = mutableSetOf<Int>()
    private val selectedLongIds = mutableSetOf<Int>()
    private var tvMcqCounter: TextView? = null
    private var tvShortCounter: TextView? = null
    private var tvLongCounter: TextView? = null

    private val saveLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            val src = lastFile
            if (uri != null && src != null) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            contentResolver.openOutputStream(uri)?.use { out ->
                                src.inputStream().use { it.copyTo(out) }
                            }
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    Toast.makeText(
                        this@PaperGeneratorActivity,
                        if (ok) "PDF saved" else "Save failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paper_generator)
        quizRepository = QuizRepository(applicationContext)
        paperRepository = PaperRepository()

        spinnerClass = findViewById(R.id.spinnerClassPaper)
        spinnerSubject = findViewById(R.id.spinnerSubjectPaper)
        progress = findViewById(R.id.progressPaper)
        tvChapterStatus = findViewById(R.id.tvChapterStatusPaper)
        checkAllChapters = findViewById(R.id.checkAllChaptersPaper)
        layoutChapters = findViewById(R.id.layoutChaptersPaper)
        radioTypeMcqs = findViewById(R.id.radioTypeMcqs)
        radioTypeShort = findViewById(R.id.radioTypeShort)
        radioTypeLong = findViewById(R.id.radioTypeLong)
        radioTypeCombined = findViewById(R.id.radioTypeCombined)
        radioTypeBoard = findViewById(R.id.radioTypeBoard)
        tvBoardInfo = findViewById(R.id.tvBoardInfoPaper)
        layoutChapterSection = findViewById(R.id.layoutChapterSectionPaper)
        layoutModeSection = findViewById(R.id.layoutModeSectionPaper)
        layoutOrderSection = findViewById(R.id.layoutOrderSectionPaper)
        radioCustom = findViewById(R.id.radioCustomPaper)
        etCustomCount = findViewById(R.id.etCustomCountPaper)
        radioSolved = findViewById(R.id.radioSolvedPaper)
        radioRandom = findViewById(R.id.radioRandomPaper)
        etTitle = findViewById(R.id.etPaperTitle)
        etTime = findViewById(R.id.etTimePaper)
        radioModeManual = findViewById(R.id.radioModeManualPaper)
        layoutAuto = findViewById(R.id.layoutAutoPaper)
        layoutManual = findViewById(R.id.layoutManualPaper)
        etCountShort = findViewById(R.id.etCountShortPaper)
        etCountLong = findViewById(R.id.etCountLongPaper)
        radioSelectRandom = findViewById(R.id.radioSelectRandomPaper)
        etSearch = findViewById(R.id.etSearchPaper)
        layoutQuestions = findViewById(R.id.layoutQuestionsPaper)
        btnGenerate = findViewById(R.id.btnGeneratePaper)
        tvStatus = findViewById(R.id.tvPaperStatus)
        layoutActions = findViewById(R.id.layoutActionsPaper)

        findViewById<TextView>(R.id.btnBackPaper).setOnClickListener { finish() }

        val incomingGrade = (intent.getStringExtra("grade") ?: intent.getStringExtra("class") ?: "9")
            .filter { it.isDigit() }.ifBlank { "9" }
        spinnerClass.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, classLabels)
        spinnerClass.setSelection(classSheets.indexOf("Class_${incomingGrade}th").coerceAtLeast(0))
        rebuildSubjects(intent.getStringExtra("subject") ?: "")

        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                shortAll = emptyList(); longAll = emptyList()
                rebuildSubjects("")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                clearQuestionSelections()
                loadData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        findViewById<RadioGroup>(R.id.radioTypeGroupPaper).setOnCheckedChangeListener { _, _ ->
            applyBoardVisibility()
            loadData()
        }

        checkAllChapters.setOnCheckedChangeListener { _, checked ->
            chapterChecks.forEach { it.isEnabled = !checked }
            renderQuestions()
        }
        findViewById<RadioButton>(R.id.radioAllPaper).setOnCheckedChangeListener { _, _ ->
            updateCountFieldsEnabled()
        }
        radioCustom.setOnCheckedChangeListener { _, checked ->
            updateCountFieldsEnabled()
            if (checked) etCustomCount.requestFocus()
        }
        findViewById<RadioGroup>(R.id.radioModeGroupPaper).setOnCheckedChangeListener { _, _ -> applyMode() }
        findViewById<RadioGroup>(R.id.radioAnswerGroupPaper).setOnCheckedChangeListener { _, _ -> renderQuestions() }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderQuestions() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        findViewById<Button>(R.id.btnClearSearchPaper).setOnClickListener { etSearch.setText("") }

        btnGenerate.setOnClickListener { generate() }
        findViewById<Button>(R.id.btnPreviewPaper).setOnClickListener { previewPdf() }
        findViewById<Button>(R.id.btnSharePaper).setOnClickListener { sharePdf() }
        findViewById<Button>(R.id.btnSavePaper).setOnClickListener { savePdf() }
        findViewById<Button>(R.id.btnRegeneratePaper).setOnClickListener { generate() }

        loadData()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun currentType(): PaperType = when {
        radioTypeBoard.isChecked -> PaperType.BOARD
        radioTypeShort.isChecked -> PaperType.SHORT
        radioTypeLong.isChecked -> PaperType.LONG
        radioTypeCombined.isChecked -> PaperType.COMBINED
        else -> PaperType.MCQS
    }

    /** Board Pattern mode hides the chapter / selection-mode / order blocks —
     *  the full BISE structure is fixed. Currently Class 10 English only. */
    private fun applyBoardVisibility() {
        if (!::tvBoardInfo.isInitialized) return
        val board = currentType() == PaperType.BOARD
        tvBoardInfo.visibility = if (board) View.VISIBLE else View.GONE
        layoutChapterSection.visibility = if (board) View.GONE else View.VISIBLE
        layoutModeSection.visibility = if (board) View.GONE else View.VISIBLE
        layoutOrderSection.visibility = if (board) View.GONE else View.VISIBLE
        if (board && spinnerClass.selectedItemPosition != classSheets.indexOf("Class_10th")) {
            spinnerClass.setSelection(classSheets.indexOf("Class_10th"))
        }
    }

    private fun subjectsFor(sheet: String): List<String> = when (sheet) {
        "Class_9th", "Class_11th" -> listOf(
            "English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology",
            "Computer Science", "Islamiyat", "Tarjuma-tul-Quran"
        )

        else -> listOf(
            "English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology",
            "Computer Science", "Pak Studies", "Tarjuma-tul-Quran"
        )
    }

    private fun rebuildSubjects(incomingSubject: String) {
        val sheet = classSheets[spinnerClass.selectedItemPosition.coerceIn(0, classSheets.lastIndex)]
        val subjects = subjectsFor(sheet)
        spinnerSubject.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subjects)
        val idx = subjects.indexOfFirst { it.equals(incomingSubject, ignoreCase = true) }
        if (idx >= 0) spinnerSubject.setSelection(idx)
    }

    private fun writtenMatches(q: WrittenQuestion, sheet: String, subject: String): Boolean {
        val classOk = q.classNum.filter { it.isDigit() } == sheet.filter { it.isDigit() }
        return classOk && subjectsMatch(q.subject, subject)
    }

    private fun loadData() {
        val sheet = classSheets[spinnerClass.selectedItemPosition.coerceIn(0, classSheets.lastIndex)]
        val subject = spinnerSubject.selectedItem?.toString() ?: return
        val type = currentType()
        loadJob?.cancel()
        loading = true
        progress.visibility = View.VISIBLE
        tvChapterStatus.text = "Loading questions..."
        layoutChapters.removeAllViews()
        chapterChecks.clear()
        loadJob = scope.launch {
            if (type == PaperType.MCQS || type == PaperType.COMBINED || type == PaperType.BOARD) {
                val fetched = try {
                    quizRepository.fetchQuestions(sheet, applicationContext)
                } catch (e: Exception) {
                    emptyList()
                }
                mcqAll = fetched.filter { subjectsMatch(it.subject, subject) }
            }
            if ((type == PaperType.SHORT || type == PaperType.COMBINED || type == PaperType.BOARD) && !shortTabLoaded) {
                shortRaw = try {
                    paperRepository.fetchWritten(PaperRepository.TAB_SHORTS)
                } catch (e: Exception) {
                    emptyList()
                }
                shortTabLoaded = true
            }
            if ((type == PaperType.LONG || type == PaperType.COMBINED) && !longTabLoaded) {
                longRaw = try {
                    paperRepository.fetchWritten(PaperRepository.TAB_LONGS)
                } catch (e: Exception) {
                    emptyList()
                }
                longTabLoaded = true
            }
            shortAll = shortRaw.filter { writtenMatches(it, sheet, subject) }
            longAll = longRaw.filter { writtenMatches(it, sheet, subject) }

            val labels = sortedSetOf(
                compareBy<String>({ chapterNumber(it) ?: Int.MAX_VALUE }, { it.lowercase() })
            )
            if (type == PaperType.MCQS || type == PaperType.COMBINED || type == PaperType.BOARD) labels += mcqAll.map { it.chapter }
            if (type == PaperType.SHORT || type == PaperType.COMBINED || type == PaperType.BOARD) labels += shortAll.map { it.chapterLabel }
            if (type == PaperType.LONG || type == PaperType.COMBINED) labels += longAll.map { it.chapterLabel }
            chapterNames = labels.toList()

            for (ch in chapterNames) {
                val count = countForChapter(ch, type)
                val cb = CheckBox(this@PaperGeneratorActivity).apply {
                    text = "$ch  ($count questions)"
                    tag = ch
                    textSize = 14.5f
                    isEnabled = !checkAllChapters.isChecked
                    setOnCheckedChangeListener { _, _ -> renderQuestions() }
                }
                chapterChecks += cb
                layoutChapters.addView(cb)
            }
            tvChapterStatus.text = statusText(type)
            progress.visibility = View.GONE
            loading = false
            updateCountFieldsEnabled()
            renderQuestions()
        }
    }

    private fun statusText(type: PaperType): String {
        val parts = mutableListOf<String>()
        if (type == PaperType.MCQS || type == PaperType.COMBINED || type == PaperType.BOARD) parts += "${mcqAll.size} MCQs"
        if (type == PaperType.SHORT || type == PaperType.COMBINED || type == PaperType.BOARD) parts += "${shortAll.size} Short"
        if (type == PaperType.LONG || type == PaperType.COMBINED) parts += "${longAll.size} Long"
        val total = parts.joinToString(" • ")
        return when {
            chapterNames.isEmpty() && (type == PaperType.SHORT) ->
                "No Short Questions available yet for this class/subject. Add data in the 'Shorts' tab of the Google Sheet."
            chapterNames.isEmpty() && (type == PaperType.LONG) ->
                "No Long Questions available yet for this class/subject. Add data in the 'Longs' tab of the Google Sheet."
            chapterNames.isEmpty() ->
                "No questions found for this subject yet. Check your internet connection, or try another subject."
            else -> "$total available in ${chapterNames.size} chapter(s)."
        }
    }

    private fun writtenInChapter(q: WrittenQuestion, label: String): Boolean {
        if (q.chapterLabel == label) return true
        val n = chapterNumber(label) ?: return false
        return chapterNumber(q.chapterLabel) == n
    }

    private fun countForChapter(label: String, type: PaperType): Int {
        var n = 0
        if (type == PaperType.MCQS || type == PaperType.COMBINED) {
            n += mcqAll.count { it.chapter == label }
        }
        if (type == PaperType.SHORT || type == PaperType.COMBINED) {
            n += shortAll.count { writtenInChapter(it, label) }
        }
        if (type == PaperType.LONG || type == PaperType.COMBINED) {
            n += longAll.count { writtenInChapter(it, label) }
        }
        return n
    }


    // --- Selection mode + manual question browser ---

    private fun isManualMode(): Boolean = radioModeManual.isChecked

    private fun applyMode() {
        val manual = isManualMode()
        layoutManual.visibility = if (manual) View.VISIBLE else View.GONE
        layoutAuto.visibility = if (manual) View.GONE else View.VISIBLE
        if (manual) renderQuestions()
    }

    private fun clearQuestionSelections() {
        selectedMcqIds.clear()
        selectedShortIds.clear()
        selectedLongIds.clear()
        if (::etSearch.isInitialized) etSearch.setText("")
    }

    private fun updateCountFieldsEnabled() {
        if (!::etCountShort.isInitialized) return
        val custom = radioCustom.isChecked
        val type = currentType()
        etCustomCount.isEnabled = custom && (type == PaperType.MCQS || type == PaperType.COMBINED)
        etCountShort.isEnabled = custom && (type == PaperType.SHORT || type == PaperType.COMBINED)
        etCountLong.isEnabled = custom && (type == PaperType.LONG || type == PaperType.COMBINED)
    }

    private fun selectedChapterLabels(): List<String> =
        if (checkAllChapters.isChecked) chapterNames
        else chapterChecks.filter { it.isChecked }.map { it.tag as String }

    private fun eligibleMcqs(): List<Question> {
        val sel = selectedChapterLabels()
        return mcqAll.filter { it.chapter in sel }.sortedBy { it.id }
    }

    private fun eligibleShorts(): List<WrittenQuestion> {
        val sel = selectedChapterLabels()
        return shortAll.filter { wq -> sel.any { writtenInChapter(wq, it) } }.sortedBy { it.id }
    }

    private fun eligibleLongs(): List<WrittenQuestion> {
        val sel = selectedChapterLabels()
        return longAll.filter { wq -> sel.any { writtenInChapter(wq, it) } }.sortedBy { it.id }
    }

    private fun divider(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        setBackgroundColor(Color.parseColor("#E2E8F0"))
    }

    private fun noteView(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 12.5f
        setTextColor(Color.parseColor("#64748B"))
        setPadding(2, 8, 2, 8)
    }

    private fun counterView(): TextView = TextView(this).apply {
        textSize = 12f
        setTextColor(Color.parseColor("#475569"))
        setPadding(2, 2, 2, 2)
    }

    private fun sectionHeader(title: String, visibleIds: List<Int>, selection: MutableSet<Int>): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 2)
        }
        val tv = TextView(this).apply {
            text = title
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1F2937"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnAll = Button(this).apply {
            text = "Select All"
            textSize = 11f
            minHeight = 0
            setPadding(10, 4, 10, 4)
            setOnClickListener {
                selection.addAll(visibleIds)
                renderQuestions()
            }
        }
        val btnNone = Button(this).apply {
            text = "Deselect All"
            textSize = 11f
            minHeight = 0
            setPadding(10, 4, 10, 4)
            setOnClickListener {
                selection.removeAll(visibleIds.toSet())
                renderQuestions()
            }
        }
        row.addView(tv)
        row.addView(btnAll)
        row.addView(btnNone)
        return row
    }

    private fun mcqRow(q: Question): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(2, 6, 2, 6)
        }
        val cb = CheckBox(this).apply {
            text = q.questionText
            textSize = 13.5f
            isChecked = q.id in selectedMcqIds
            setOnCheckedChangeListener { _, checked ->
                if (checked) selectedMcqIds += q.id else selectedMcqIds -= q.id
                updateCounters()
            }
        }
        container.addView(cb)
        container.addView(TextView(this).apply {
            text = q.options.mapIndexed { i, o -> "${'A' + i}) $o" }.joinToString("\n")
            textSize = 12f
            setTextColor(Color.parseColor("#475569"))
            setPadding(38, 0, 0, 0)
        })
        container.addView(TextView(this).apply {
            text = q.chapter
            textSize = 10.5f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(38, 2, 0, 0)
        })
        if (radioSolved.isChecked) {
            container.addView(TextView(this).apply {
                val letter = if (q.correctAnswerIndex in q.options.indices) ('A' + q.correctAnswerIndex).toString() else "?"
                val ansText = q.options.getOrNull(q.correctAnswerIndex) ?: ""
                var line = "\u2713 Correct answer: $letter) $ansText"
                if (q.explanation.isNotBlank()) line += "\n" + q.explanation
                text = line
                textSize = 12f
                setTextColor(Color.parseColor("#047857"))
                setPadding(38, 4, 0, 0)
            })
        }
        container.addView(divider())
        return container
    }

    private fun writtenRow(q: WrittenQuestion, selection: MutableSet<Int>): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(2, 6, 2, 6)
        }
        val cb = CheckBox(this).apply {
            text = q.question
            textSize = 13.5f
            isChecked = q.id in selection
            setOnCheckedChangeListener { _, checked ->
                if (checked) selection += q.id else selection -= q.id
                updateCounters()
            }
        }
        container.addView(cb)
        container.addView(TextView(this).apply {
            text = q.chapterLabel + if (q.marks.isNotBlank()) " \u2022 ${q.marks} marks" else ""
            textSize = 10.5f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(38, 2, 0, 0)
        })
        if (radioSolved.isChecked) {
            val answerView = TextView(this).apply {
                text = if (q.answer.isNotBlank()) "Answer: ${q.answer}"
                    else "(Answer not available in the source data)"
                textSize = 12.5f
                setTextColor(Color.parseColor("#065F46"))
                setPadding(38, 4, 0, 0)
                visibility = View.GONE
            }
            val toggle = TextView(this).apply {
                text = "Show Answer \u25be"
                textSize = 12f
                setTextColor(Color.parseColor("#2563EB"))
                setPadding(38, 4, 0, 0)
                setOnClickListener {
                    val show = answerView.visibility == View.GONE
                    answerView.visibility = if (show) View.VISIBLE else View.GONE
                    text = if (show) "Hide Answer \u25b4" else "Show Answer \u25be"
                }
            }
            container.addView(toggle)
            container.addView(answerView)
        }
        container.addView(divider())
        return container
    }

    private fun updateCounters() {
        val type = currentType()
        if (type == PaperType.MCQS || type == PaperType.COMBINED) {
            val e = eligibleMcqs()
            tvMcqCounter?.text = "Selected: ${e.count { it.id in selectedMcqIds }} of ${e.size} MCQs"
        }
        if (type == PaperType.SHORT || type == PaperType.COMBINED) {
            val e = eligibleShorts()
            tvShortCounter?.text = "Selected: ${e.count { it.id in selectedShortIds }} of ${e.size} Short Questions"
        }
        if (type == PaperType.LONG || type == PaperType.COMBINED) {
            val e = eligibleLongs()
            tvLongCounter?.text = "Selected: ${e.count { it.id in selectedLongIds }} of ${e.size} Long Questions"
        }
    }

    private fun renderQuestions() {
        if (!::layoutQuestions.isInitialized || !::radioModeManual.isInitialized) return
        if (!isManualMode()) return
        layoutQuestions.removeAllViews()
        tvMcqCounter = null
        tvShortCounter = null
        tvLongCounter = null
        val type = currentType()
        val query = etSearch.text.toString().trim().lowercase()
        if (type == PaperType.MCQS || type == PaperType.COMBINED) {
            val eligible = eligibleMcqs()
            val visible = eligible.filter {
                query.isBlank() || it.questionText.lowercase().contains(query) ||
                    it.options.any { o -> o.lowercase().contains(query) }
            }
            layoutQuestions.addView(sectionHeader("MCQs", visible.map { it.id }, selectedMcqIds))
            tvMcqCounter = counterView()
            layoutQuestions.addView(tvMcqCounter)
            when {
                eligible.isEmpty() -> layoutQuestions.addView(noteView("No MCQs available for the selected chapters."))
                visible.isEmpty() -> layoutQuestions.addView(noteView("No MCQs match your search."))
                else -> visible.forEach { layoutQuestions.addView(mcqRow(it)) }
            }
        }
        if (type == PaperType.SHORT || type == PaperType.COMBINED) {
            val eligible = eligibleShorts()
            val visible = eligible.filter {
                query.isBlank() || it.question.lowercase().contains(query)
            }
            layoutQuestions.addView(sectionHeader("Short Questions", visible.map { it.id }, selectedShortIds))
            tvShortCounter = counterView()
            layoutQuestions.addView(tvShortCounter)
            when {
                eligible.isEmpty() && shortAll.isEmpty() ->
                    layoutQuestions.addView(noteView("No Short Questions available yet for this class/subject. Add data in the 'Shorts' tab of the Google Sheet."))
                eligible.isEmpty() -> layoutQuestions.addView(noteView("No Short Questions available for the selected chapters."))
                visible.isEmpty() -> layoutQuestions.addView(noteView("No Short Questions match your search."))
                else -> visible.forEach { layoutQuestions.addView(writtenRow(it, selectedShortIds)) }
            }
        }
        if (type == PaperType.LONG || type == PaperType.COMBINED) {
            val eligible = eligibleLongs()
            val visible = eligible.filter {
                query.isBlank() || it.question.lowercase().contains(query)
            }
            layoutQuestions.addView(sectionHeader("Long Questions", visible.map { it.id }, selectedLongIds))
            tvLongCounter = counterView()
            layoutQuestions.addView(tvLongCounter)
            when {
                eligible.isEmpty() && longAll.isEmpty() ->
                    layoutQuestions.addView(noteView("No Long Questions available yet for this class/subject. Add data in the 'Longs' tab of the Google Sheet."))
                eligible.isEmpty() -> layoutQuestions.addView(noteView("No Long Questions available for the selected chapters."))
                visible.isEmpty() -> layoutQuestions.addView(noteView("No Long Questions match your search."))
                else -> visible.forEach { layoutQuestions.addView(writtenRow(it, selectedLongIds)) }
            }
        }
        updateCounters()
    }

    /** Automatic-mode picking: first-N or random selection (no repeats).
     *  A blank/invalid count field means "all available". Returns the picked
     *  list plus an optional shortfall note. */
    private fun <T> applyCount(pool: List<T>, field: EditText, label: String): Pair<List<T>, String?> {
        val n = field.text.toString().toIntOrNull()
        if (n == null || n < 1) return pool to null
        val picked = if (radioSelectRandom.isChecked) pool.shuffled().take(n) else pool.take(n)
        val note = if (n > pool.size) "Only ${pool.size} $label available - all included." else null
        return picked to note
    }

    private fun generate() {
        if (loading) {
            Toast.makeText(this, "Please wait \u2014 questions are loading", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentType() == PaperType.BOARD) {
            generateBoardPattern()
            return
        }
        val type = currentType()
        if (chapterNames.isEmpty()) {
            Toast.makeText(
                this,
                "No chapters are loaded for this subject yet. Check your internet connection or choose another subject.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val selected = selectedChapterLabels()
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one chapter", Toast.LENGTH_SHORT).show()
            return
        }
        var mcqs: List<Question> = emptyList()
        var shorts: List<WrittenQuestion> = emptyList()
        var longs: List<WrittenQuestion> = emptyList()
        var shortfallNote = ""
        if (isManualMode()) {
            if (type == PaperType.MCQS || type == PaperType.COMBINED) {
                mcqs = eligibleMcqs().filter { it.id in selectedMcqIds }
            }
            if (type == PaperType.SHORT || type == PaperType.COMBINED) {
                shorts = eligibleShorts().filter { it.id in selectedShortIds }
            }
            if (type == PaperType.LONG || type == PaperType.COMBINED) {
                longs = eligibleLongs().filter { it.id in selectedLongIds }
            }
            if (mcqs.size + shorts.size + longs.size == 0) {
                Toast.makeText(
                    this,
                    "Select at least one question to include in the paper",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        } else {
            if (type == PaperType.MCQS || type == PaperType.COMBINED) mcqs = eligibleMcqs()
            if (type == PaperType.SHORT || type == PaperType.COMBINED) shorts = eligibleShorts()
            if (type == PaperType.LONG || type == PaperType.COMBINED) longs = eligibleLongs()
            if (radioCustom.isChecked) {
                val notes = mutableListOf<String>()
                if (type == PaperType.MCQS || type == PaperType.COMBINED) {
                    val r = applyCount(mcqs, etCustomCount, "MCQs"); mcqs = r.first; r.second?.let { notes += it }
                }
                if (type == PaperType.SHORT || type == PaperType.COMBINED) {
                    val r = applyCount(shorts, etCountShort, "Short Questions"); shorts = r.first; r.second?.let { notes += it }
                }
                if (type == PaperType.LONG || type == PaperType.COMBINED) {
                    val r = applyCount(longs, etCountLong, "Long Questions"); longs = r.first; r.second?.let { notes += it }
                }
                shortfallNote = notes.joinToString(" ")
            }
            if (mcqs.size + shorts.size + longs.size == 0) {
                Toast.makeText(this, "No questions in the selected chapters", Toast.LENGTH_SHORT).show()
                return
            }
        }
        if (radioRandom.isChecked) {
            mcqs = mcqs.shuffled(); shorts = shorts.shuffled(); longs = longs.shuffled()
        }
        val total = mcqs.size + shorts.size + longs.size
        val defaultTitle = when (type) {
            PaperType.MCQS -> "MCQs Test Paper"
            PaperType.SHORT -> "Short Questions Paper"
            PaperType.LONG -> "Long Questions Paper"
            PaperType.COMBINED -> "Question Paper"
            PaperType.BOARD -> "Board Pattern Paper"
        }
        val title = etTitle.text.toString().trim().ifBlank { defaultTitle }
        val time = etTime.text.toString().trim().ifBlank { "$total Minutes" }
        val contributing = (mcqs.map { it.chapter } + shorts.map { it.chapterLabel } + longs.map { it.chapterLabel }).distinct()
        val chaptersLabel = when {
            contributing.size == chapterNames.size && contributing.isNotEmpty() -> "Full Book (All Chapters)"
            contributing.size == 1 -> contributing[0]
            contributing.isNotEmpty() -> contributing.joinToString(", ")
            else -> selected.joinToString(", ")
        }
        val data = PaperPdfGenerator.PaperData(
            testTitle = title,
            classLabel = classLabels[spinnerClass.selectedItemPosition.coerceIn(0, classLabels.lastIndex)],
            subject = spinnerSubject.selectedItem?.toString() ?: "",
            chaptersLabel = chaptersLabel,
            timeAllowed = time,
            mcqs = mcqs,
            shorts = shorts,
            longs = longs,
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating PDF..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val safeSubject = data.subject.replace(Regex("[^A-Za-z0-9]+"), "_")
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val out = File(File(cacheDir, "papers"), "Paper_${safeSubject}_$ts.pdf")
                    PaperPdfGenerator.generate(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                var msg = "PDF ready: $total questions (${mcqs.size} MCQs, ${shorts.size} Short, ${longs.size} Long) \u2022 ${file.name}"
                if (shortfallNote.isNotBlank()) msg += " $shortfallNote"
                tvStatus.text = msg
                Toast.makeText(this@PaperGeneratorActivity, "Paper PDF generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "PDF generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 10 English): fixed BISE structure. */
    private fun generateBoardPattern() {
        Toast.makeText(this, "Board Pattern is currently available for Class 10 English only", Toast.LENGTH_LONG).show()
    }

    private fun pdfUri(file: File) =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun previewPdf() {
        val file = lastFile ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri(file), "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No PDF viewer found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdf() {
        val file = lastFile ?: return
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri(file))
                putExtra(Intent.EXTRA_SUBJECT, "StudyPoint Question Paper")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Paper PDF"))
        } catch (e: Exception) {
            Toast.makeText(this, "Sharing failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePdf() {
        val file = lastFile ?: return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, file.name)
        }
        saveLauncher.launch(intent)
    }

    // --- Matching helpers (same logic as the practice quiz / test generator) ---

    private fun subjectsMatch(questionSubject: String, selectedSubject: String): Boolean {
        val q = questionSubject.lowercase().trim()
        val s = selectedSubject.lowercase().trim()
        if (q == s) return true
        val aliases = mapOf(
            "mathematics" to listOf("math", "mathematics"),
            "math" to listOf("math", "mathematics"),
            "computer science" to listOf("computer", "computer science", "cs"),
            "computer" to listOf("computer", "computer science", "cs"),
            "tarjuma-tul-quran" to listOf("tarjuma", "quran", "tarjuma-tul-quran"),
            "tarjumatulquran" to listOf("tarjuma", "quran", "tarjuma-tul-quran"),
            "pak studies" to listOf("pak studies", "pakistan studies", "pak study"),
            "pakistan studies" to listOf("pak studies", "pakistan studies")
        )
        val terms = aliases[s] ?: listOf(s)
        fun words(text: String): List<String> =
            text.split(Regex("[^a-z]+")).filter { it.isNotBlank() }
        val qWords = words(q)
        return terms.any { term ->
            q == term || qWords.contains(term) || (q.length >= 3 && words(term).contains(q))
        }
    }

    private fun chapterNumber(text: String): Int? =
        Regex("""chapter\s*(\d+)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""\d+""").find(text)?.value?.toIntOrNull()
}
