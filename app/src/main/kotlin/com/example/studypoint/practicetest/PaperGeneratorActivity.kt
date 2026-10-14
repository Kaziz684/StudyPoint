package com.example.studypoint.practicetest

import android.app.Activity
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

    private enum class PaperType { MCQS, SHORT, LONG, COMBINED }

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
        radioCustom = findViewById(R.id.radioCustomPaper)
        etCustomCount = findViewById(R.id.etCustomCountPaper)
        radioSolved = findViewById(R.id.radioSolvedPaper)
        radioRandom = findViewById(R.id.radioRandomPaper)
        etTitle = findViewById(R.id.etPaperTitle)
        etTime = findViewById(R.id.etTimePaper)
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
                loadData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        findViewById<RadioGroup>(R.id.radioTypeGroupPaper).setOnCheckedChangeListener { _, _ -> loadData() }

        checkAllChapters.setOnCheckedChangeListener { _, checked ->
            chapterChecks.forEach { it.isEnabled = !checked }
        }
        findViewById<RadioButton>(R.id.radioAllPaper).setOnCheckedChangeListener { _, checked ->
            if (checked) etCustomCount.isEnabled = false
        }
        radioCustom.setOnCheckedChangeListener { _, checked ->
            etCustomCount.isEnabled = checked
            if (checked) etCustomCount.requestFocus()
        }

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
        radioTypeShort.isChecked -> PaperType.SHORT
        radioTypeLong.isChecked -> PaperType.LONG
        radioTypeCombined.isChecked -> PaperType.COMBINED
        else -> PaperType.MCQS
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
            if (type == PaperType.MCQS || type == PaperType.COMBINED) {
                val fetched = try {
                    quizRepository.fetchQuestions(sheet, applicationContext)
                } catch (e: Exception) {
                    emptyList()
                }
                mcqAll = fetched.filter { subjectsMatch(it.subject, subject) }
            }
            if ((type == PaperType.SHORT || type == PaperType.COMBINED) && !shortTabLoaded) {
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
            if (type == PaperType.MCQS || type == PaperType.COMBINED) labels += mcqAll.map { it.chapter }
            if (type == PaperType.SHORT || type == PaperType.COMBINED) labels += shortAll.map { it.chapterLabel }
            if (type == PaperType.LONG || type == PaperType.COMBINED) labels += longAll.map { it.chapterLabel }
            chapterNames = labels.toList()

            for (ch in chapterNames) {
                val count = countForChapter(ch, type)
                val cb = CheckBox(this@PaperGeneratorActivity).apply {
                    text = "$ch  ($count questions)"
                    tag = ch
                    textSize = 14.5f
                    isEnabled = !checkAllChapters.isChecked
                }
                chapterChecks += cb
                layoutChapters.addView(cb)
            }
            tvChapterStatus.text = statusText(type)
            progress.visibility = View.GONE
            loading = false
        }
    }

    private fun statusText(type: PaperType): String {
        val parts = mutableListOf<String>()
        if (type == PaperType.MCQS || type == PaperType.COMBINED) parts += "${mcqAll.size} MCQs"
        if (type == PaperType.SHORT || type == PaperType.COMBINED) parts += "${shortAll.size} Short"
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

    private fun generate() {
        if (loading) {
            Toast.makeText(this, "Please wait — questions are loading", Toast.LENGTH_SHORT).show()
            return
        }
        val type = currentType()
        val selected: List<String> = if (checkAllChapters.isChecked) {
            chapterNames
        } else {
            chapterChecks.filter { it.isChecked }.map { it.tag as String }
        }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one chapter", Toast.LENGTH_SHORT).show()
            return
        }
        var mcqs: List<Question> = emptyList()
        var shorts: List<WrittenQuestion> = emptyList()
        var longs: List<WrittenQuestion> = emptyList()
        if (type == PaperType.MCQS || type == PaperType.COMBINED) {
            mcqs = mcqAll.filter { it.chapter in selected }.sortedBy { it.id }
        }
        if (type == PaperType.SHORT || type == PaperType.COMBINED) {
            shorts = shortAll.filter { wq -> selected.any { writtenInChapter(wq, it) } }.sortedBy { it.id }
        }
        if (type == PaperType.LONG || type == PaperType.COMBINED) {
            longs = longAll.filter { wq -> selected.any { writtenInChapter(wq, it) } }.sortedBy { it.id }
        }
        if (radioRandom.isChecked) {
            mcqs = mcqs.shuffled(); shorts = shorts.shuffled(); longs = longs.shuffled()
        }
        if (radioCustom.isChecked) {
            val n = etCustomCount.text.toString().toIntOrNull()
            if (n == null || n < 1) {
                Toast.makeText(this, "Enter a valid number of questions", Toast.LENGTH_SHORT).show()
                return
            }
            mcqs = mcqs.take(n); shorts = shorts.take(n); longs = longs.take(n)
        }
        val total = mcqs.size + shorts.size + longs.size
        if (total == 0) {
            Toast.makeText(this, "No questions in the selected chapters", Toast.LENGTH_SHORT).show()
            return
        }
        val defaultTitle = when (type) {
            PaperType.MCQS -> "MCQs Test Paper"
            PaperType.SHORT -> "Short Questions Paper"
            PaperType.LONG -> "Long Questions Paper"
            PaperType.COMBINED -> "Question Paper"
        }
        val title = etTitle.text.toString().trim().ifBlank { defaultTitle }
        val time = etTime.text.toString().trim().ifBlank { "$total Minutes" }
        val chaptersLabel = when {
            selected.size == chapterNames.size -> "Full Book (All Chapters)"
            selected.size == 1 -> selected[0]
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
                tvStatus.text = "PDF ready: $total questions (${mcqs.size} MCQs, ${shorts.size} Short, ${longs.size} Long) • ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Paper PDF generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "PDF generation failed. Please try again."
            }
        }
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
