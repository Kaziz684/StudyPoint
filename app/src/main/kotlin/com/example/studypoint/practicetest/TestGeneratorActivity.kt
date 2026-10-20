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
 * "Generate MCQs Test" — builds a printable PDF test paper from the real
 * MCQs stored in the Google Sheet (same source as the practice quiz),
 * filtered by class, subject and selected chapters.
 */
class TestGeneratorActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadJob: Job? = null
    private lateinit var repository: QuizRepository

    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerSubject: Spinner
    private lateinit var progressGen: ProgressBar
    private lateinit var tvChapterStatus: TextView
    private lateinit var checkAllChapters: CheckBox
    private lateinit var layoutChapters: LinearLayout
    private lateinit var radioCustomMcqs: RadioButton
    private lateinit var etCustomCount: EditText
    private lateinit var radioWithAnswers: RadioButton
    private lateinit var radioRandomOrder: RadioButton
    private lateinit var etTestTitle: EditText
    private lateinit var etTimeAllowed: EditText
    private lateinit var btnGenerate: Button
    private lateinit var tvGenStatus: TextView
    private lateinit var layoutActions: LinearLayout

    private val classLabels = listOf("Class 9th", "Class 10th", "Class 11th", "Class 12th")
    private val classSheets = listOf("Class_9th", "Class_10th", "Class_11th", "Class_12th")

    private var allQuestions: List<Question> = emptyList()
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
                        this@TestGeneratorActivity,
                        if (ok) "PDF saved" else "Save failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_generator)
        repository = QuizRepository(applicationContext)

        spinnerClass = findViewById(R.id.spinnerClassGen)
        spinnerSubject = findViewById(R.id.spinnerSubjectGen)
        progressGen = findViewById(R.id.progressGen)
        tvChapterStatus = findViewById(R.id.tvChapterStatus)
        checkAllChapters = findViewById(R.id.checkAllChapters)
        layoutChapters = findViewById(R.id.layoutChapters)
        radioCustomMcqs = findViewById(R.id.radioCustomMcqs)
        etCustomCount = findViewById(R.id.etCustomCount)
        radioWithAnswers = findViewById(R.id.radioWithAnswers)
        radioRandomOrder = findViewById(R.id.radioRandomOrder)
        etTestTitle = findViewById(R.id.etTestTitle)
        etTimeAllowed = findViewById(R.id.etTimeAllowed)
        btnGenerate = findViewById(R.id.btnGenerate)
        tvGenStatus = findViewById(R.id.tvGenStatus)
        layoutActions = findViewById(R.id.layoutActions)

        findViewById<TextView>(R.id.btnBackGen).setOnClickListener { finish() }

        // Class spinner
        val incomingGrade = (intent.getStringExtra("grade") ?: intent.getStringExtra("class") ?: "9")
            .filter { it.isDigit() }.ifBlank { "9" }
        val classAdapter = ArrayAdapter(this, R.layout.spinner_item_selected, classLabels).apply { setDropDownViewResource(R.layout.spinner_item_dropdown) }
        spinnerClass.adapter = classAdapter
        val classIdx = classSheets.indexOf("Class_${incomingGrade}th").coerceAtLeast(0)
        spinnerClass.setSelection(classIdx)

        // Subject spinner for the incoming class, preselecting the incoming subject
        rebuildSubjects(incomingSubject = intent.getStringExtra("subject") ?: "")

        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                rebuildSubjects(incomingSubject = "")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadQuestions()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        checkAllChapters.setOnCheckedChangeListener { _, checked ->
            chapterChecks.forEach { it.isEnabled = !checked }
        }
        findViewById<RadioButton>(R.id.radioAllMcqs).setOnCheckedChangeListener { _, checked ->
            if (checked) etCustomCount.isEnabled = false
        }
        radioCustomMcqs.setOnCheckedChangeListener { _, checked ->
            etCustomCount.isEnabled = checked
            if (checked) etCustomCount.requestFocus()
        }

        btnGenerate.setOnClickListener { generate() }
        findViewById<Button>(R.id.btnPreview).setOnClickListener { previewPdf() }
        findViewById<Button>(R.id.btnSharePdf).setOnClickListener { sharePdf() }
        findViewById<Button>(R.id.btnSavePdf).setOnClickListener { savePdf() }
        findViewById<Button>(R.id.btnRegenerate).setOnClickListener { generate() }

        loadQuestions()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
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
        val adapter = ArrayAdapter(this, R.layout.spinner_item_selected, subjects).apply { setDropDownViewResource(R.layout.spinner_item_dropdown) }
        spinnerSubject.adapter = adapter
        val idx = subjects.indexOfFirst { it.equals(incomingSubject, ignoreCase = true) }
        if (idx >= 0) spinnerSubject.setSelection(idx)
    }

    private fun loadQuestions() {
        val sheetPos = spinnerClass.selectedItemPosition.coerceIn(0, classSheets.lastIndex)
        val sheet = classSheets[sheetPos]
        val subject = spinnerSubject.selectedItem?.toString() ?: return
        loadJob?.cancel()
        loading = true
        progressGen.visibility = View.VISIBLE
        tvChapterStatus.text = "Loading MCQs..."
        layoutChapters.removeAllViews()
        chapterChecks.clear()
        loadJob = scope.launch {
            val fetched = try {
                repository.fetchQuestions(sheet, applicationContext)
            } catch (e: Exception) {
                emptyList()
            }
            allQuestions = fetched.filter { subjectsMatch(it.subject, subject) }
            chapterNames = allQuestions.map { it.chapter }.distinct()
                .sortedWith(compareBy({ chapterNumber(it) ?: Int.MAX_VALUE }, { it.lowercase() }))
            for (ch in chapterNames) {
                val count = allQuestions.count { it.chapter == ch }
                val cb = CheckBox(this@TestGeneratorActivity).apply {
                    text = "$ch  ($count MCQs)"
                    tag = ch
                    textSize = 14.5f
                    isEnabled = !checkAllChapters.isChecked
                }
                chapterChecks += cb
                layoutChapters.addView(cb)
            }
            tvChapterStatus.text = if (allQuestions.isEmpty()) {
                "No MCQs found for this subject yet. Check your internet connection, or try another subject."
            } else {
                "${allQuestions.size} MCQs available in ${chapterNames.size} chapter(s)."
            }
            progressGen.visibility = View.GONE
            loading = false
        }
    }

    private fun generate() {
        if (loading) {
            Toast.makeText(this, "Please wait — MCQs are loading", Toast.LENGTH_SHORT).show()
            return
        }
        if (allQuestions.isEmpty()) {
            Toast.makeText(this, "No MCQs available for this subject", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedChapters: List<String> = if (checkAllChapters.isChecked) {
            chapterNames
        } else {
            chapterChecks.filter { it.isChecked }.map { it.tag as String }
        }
        if (selectedChapters.isEmpty()) {
            Toast.makeText(this, "Select at least one chapter", Toast.LENGTH_SHORT).show()
            return
        }
        var pool = allQuestions.filter { it.chapter in selectedChapters }.sortedBy { it.id }
        if (radioRandomOrder.isChecked) pool = pool.shuffled()
        if (radioCustomMcqs.isChecked) {
            val n = etCustomCount.text.toString().toIntOrNull()
            if (n == null || n < 1) {
                Toast.makeText(this, "Enter a valid number of MCQs", Toast.LENGTH_SHORT).show()
                return
            }
            pool = pool.take(n.coerceAtMost(pool.size))
        }
        if (pool.isEmpty()) {
            Toast.makeText(this, "No MCQs in the selected chapters", Toast.LENGTH_SHORT).show()
            return
        }
        val title = etTestTitle.text.toString().trim().ifBlank { "MCQs Test Paper" }
        val time = etTimeAllowed.text.toString().trim().ifBlank { "${pool.size} Minutes" }
        val chaptersLabel = when {
            selectedChapters.size == chapterNames.size -> "Full Book (All Chapters)"
            selectedChapters.size == 1 -> selectedChapters[0]
            else -> selectedChapters.joinToString(", ")
        }
        val data = TestPdfGenerator.TestPaperData(
            testTitle = title,
            classLabel = classLabels[spinnerClass.selectedItemPosition.coerceIn(0, classLabels.lastIndex)],
            subject = spinnerSubject.selectedItem?.toString() ?: "",
            chaptersLabel = chaptersLabel,
            timeAllowed = time,
            questions = pool,
            withAnswers = radioWithAnswers.isChecked
        )
        btnGenerate.isEnabled = false
        tvGenStatus.text = "Generating PDF..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val safeSubject = data.subject.replace(Regex("[^A-Za-z0-9]+"), "_")
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val out = File(File(cacheDir, "tests"), "Test_${safeSubject}_$ts.pdf")
                    TestPdfGenerator.generate(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvGenStatus.text = "PDF ready: ${pool.size} MCQs • ${file.name}"
                Toast.makeText(this@TestGeneratorActivity, "Test PDF generated", Toast.LENGTH_SHORT).show()
            } else {
                tvGenStatus.text = "PDF generation failed. Please try again."
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
                putExtra(Intent.EXTRA_SUBJECT, "StudyPoint MCQs Test Paper")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Test PDF"))
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

    // --- Matching helpers (same logic as the practice quiz) ---

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
