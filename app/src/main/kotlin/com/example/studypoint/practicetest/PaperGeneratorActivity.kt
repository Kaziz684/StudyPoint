package com.example.studypoint.practicetest

import android.app.Activity
import android.graphics.Color
import android.graphics.BitmapFactory
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
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
    private lateinit var radioFormatDocx: RadioButton
    private lateinit var etTitle: EditText
    private lateinit var etTime: EditText
    private lateinit var etSchoolName: EditText
    private lateinit var etSchoolAddress: EditText
    private lateinit var etSchoolPhone: EditText
    private lateinit var etPaperCode: EditText
    private lateinit var etPassingMarks: EditText
    private lateinit var ivSchoolLogo: ImageView
    private lateinit var btnPickLogo: Button
    private lateinit var btnRemoveLogo: Button
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
    private val sectionChecks = mutableListOf<CheckBox>()
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

    // --- Wizard navigation (4 steps) ---
    private lateinit var scrollPaper: ScrollView
    private lateinit var stepContainers: List<LinearLayout>
    private lateinit var stepSegments: List<View>
    private lateinit var tvStepTitle: TextView
    private lateinit var tvStepSub: TextView
    private lateinit var tvSummary: TextView
    private lateinit var btnBackStep: Button
    private lateinit var btnNextStep: Button
    private var currentStep = 1

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
                        if (ok) "Saved" else "Save failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Teacher-only module: students never reach the Paper Generator.
        if (com.example.studypoint.RoleManager.cachedRole(this) == com.example.studypoint.RoleManager.ROLE_STUDENT) {
            android.widget.Toast.makeText(this, "Paper Generator is available in the Teacher dashboard.", android.widget.Toast.LENGTH_LONG).show()
            finish()
            return
        }
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
        radioFormatDocx = findViewById(R.id.radioFormatDocxPaper)
        etTitle = findViewById(R.id.etPaperTitle)
        etTime = findViewById(R.id.etTimePaper)
        etSchoolName = findViewById(R.id.etSchoolName)
        etSchoolAddress = findViewById(R.id.etSchoolAddress)
        etSchoolPhone = findViewById(R.id.etSchoolPhone)
        etPaperCode = findViewById(R.id.etPaperCode)
        etPassingMarks = findViewById(R.id.etPassingMarks)
        ivSchoolLogo = findViewById(R.id.ivSchoolLogo)
        btnPickLogo = findViewById(R.id.btnPickLogo)
        btnRemoveLogo = findViewById(R.id.btnRemoveLogo)
        setupPaperHeader()
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

        scrollPaper = findViewById(R.id.scrollPaper)
        stepContainers = listOf(
            findViewById(R.id.step1Paper),
            findViewById(R.id.step2Paper),
            findViewById(R.id.step3Paper),
            findViewById(R.id.step4Paper)
        )
        stepSegments = listOf(
            findViewById(R.id.stepSeg1Paper),
            findViewById(R.id.stepSeg2Paper),
            findViewById(R.id.stepSeg3Paper),
            findViewById(R.id.stepSeg4Paper)
        )
        tvStepTitle = findViewById(R.id.tvStepTitlePaper)
        tvStepSub = findViewById(R.id.tvStepSubPaper)
        tvSummary = findViewById(R.id.tvSummaryPaper)
        btnBackStep = findViewById(R.id.btnBackStepPaper)
        btnNextStep = findViewById(R.id.btnNextStepPaper)
        btnBackStep.setOnClickListener { showStep(currentStep - 1) }
        btnNextStep.setOnClickListener {
            if (currentStep == 1 && currentType() != PaperType.BOARD) {
                if (loading) {
                    Toast.makeText(this, "Please wait — questions are loading", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (chapterNames.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No chapters are loaded for this subject yet. Check your internet connection or choose another subject.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
            }
            showStep(currentStep + 1)
        }
        showStep(1)

        findViewById<TextView>(R.id.btnBackPaper).setOnClickListener { finish() }

        val incomingGrade = (intent.getStringExtra("grade") ?: intent.getStringExtra("class") ?: "9")
            .filter { it.isDigit() }.ifBlank { "9" }
        spinnerClass.adapter = ArrayAdapter(this, R.layout.spinner_item_selected, classLabels).apply { setDropDownViewResource(R.layout.spinner_item_dropdown) }
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
            sectionChecks.forEach { it.isEnabled = !checked }
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

    // --- Wizard step navigation ---

    // ---------- Paper header (school name / logo, editable + remembered) ----------
    private fun headerPrefs() = getSharedPreferences("paper_header", MODE_PRIVATE)

    private fun logoFile() = File(filesDir, "paper_logo.png")

    private val logoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    logoFile().outputStream().use { output -> input.copyTo(output) }
                }
                headerPrefs().edit().putString("logo_path", logoFile().absolutePath).apply()
                refreshLogoPreview()
                Toast.makeText(this, "Logo saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not save logo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPaperHeader() {
        val prefs = headerPrefs()
        val savedName = prefs.getString("school_name", null)
        val savedAddress = prefs.getString("address", null)
        val savedPhone = prefs.getString("phone", null)
        etSchoolName.setText(
            if (savedName == null || savedName == "AL QURAN SYSTEM OF EDUCATION") "AL SAMAD ACADEMY BWP" else savedName
        )
        etSchoolAddress.setText(
            if (savedAddress == null || savedAddress == "Hussaini Chowk, Satellite Town, Bahawalpur") "" else savedAddress
        )
        etSchoolPhone.setText(
            if (savedPhone == null || savedPhone == "0300 5675271") "0301 23456789" else savedPhone
        )
        etPaperCode.setText(prefs.getString("paper_code", ""))
        etPassingMarks.setText(prefs.getString("passing_marks", ""))
        refreshLogoPreview()
        btnPickLogo.setOnClickListener { logoPicker.launch("image/*") }
        btnRemoveLogo.setOnClickListener {
            logoFile().delete()
            headerPrefs().edit().remove("logo_path").apply()
            refreshLogoPreview()
            Toast.makeText(this, "Logo removed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshLogoPreview() {
        val f = logoFile()
        if (f.exists()) {
            val bmp = BitmapFactory.decodeFile(f.absolutePath)
            if (bmp != null) {
                ivSchoolLogo.setImageBitmap(bmp)
                return
            }
        }
        try {
            ivSchoolLogo.setImageDrawable(packageManager.getApplicationIcon(packageName))
        } catch (e: Exception) { /* keep the placeholder background */ }
    }

    private fun currentHeader(): PaperPdfGenerator.SchoolHeader {
        val name = etSchoolName.text.toString().trim()
        val address = etSchoolAddress.text.toString().trim()
        val phone = etSchoolPhone.text.toString().trim()
        val code = etPaperCode.text.toString().trim()
        val passing = etPassingMarks.text.toString().trim()
        headerPrefs().edit()
            .putString("school_name", name)
            .putString("address", address)
            .putString("phone", phone)
            .putString("paper_code", code)
            .putString("passing_marks", passing)
            .apply()
        return PaperPdfGenerator.SchoolHeader(
            schoolName = name.ifBlank { "AL SAMAD ACADEMY BWP" },
            address = address,
            phone = phone,
            paperCode = code,
            passingMarks = passing,
            logoPath = if (logoFile().exists()) logoFile().absolutePath else ""
        )
    }

    private fun showStep(n: Int) {
        currentStep = n.coerceIn(1, 4)
        stepContainers.forEachIndexed { i, v ->
            v.visibility = if (i == currentStep - 1) View.VISIBLE else View.GONE
        }
        stepSegments.forEachIndexed { i, v ->
            v.setBackgroundColor(Color.parseColor(if (i < currentStep) "#2563EB" else "#E2E8F0"))
        }
        val titles = listOf(
            "Step 1 of 4 — Paper Type, Class, Subject & Chapters",
            "Step 2 of 4 — Select Questions & Counts",
            "Step 3 of 4 — Paper Options",
            "Step 4 of 4 — Preview & Generate"
        )
        val subs = listOf(
            "First pick the paper type, then choose the class, subject and chapters.",
            "Select questions yourself or set the numbers for automatic selection.",
            "Choose solved or unsolved, question order, title and time.",
            "Review your selected options before creating the test paper."
        )
        tvStepTitle.text = titles[currentStep - 1]
        tvStepSub.text = subs[currentStep - 1]
        btnBackStep.visibility = if (currentStep > 1) View.VISIBLE else View.GONE
        btnNextStep.visibility = if (currentStep < 4) View.VISIBLE else View.GONE
        btnGenerate.visibility = if (currentStep == 4) View.VISIBLE else View.GONE
        if (currentStep == 4) tvSummary.text = buildSummary()
        scrollPaper.scrollTo(0, 0)
    }

    private fun buildSummary(): String {
        val typeLabel = when (currentType()) {
            PaperType.MCQS -> "MCQs Test"
            PaperType.SHORT -> "Short Questions"
            PaperType.LONG -> "Long Questions"
            PaperType.COMBINED -> "Mixed Paper (MCQs + Short + Long)"
            PaperType.BOARD -> "Board Pattern Paper"
        }
        val classLabel = classLabels[spinnerClass.selectedItemPosition.coerceIn(0, classLabels.lastIndex)]
        val subject = spinnerSubject.selectedItem?.toString() ?: ""
        val sb = StringBuilder()
        sb.append("Paper Type:  $typeLabel\n")
        sb.append("Class:  $classLabel\n")
        sb.append("Subject:  $subject\n")
        sb.append("School:  ${etSchoolName.text.toString().trim().ifBlank { "AL SAMAD ACADEMY BWP" }}\n")
        if (currentType() == PaperType.BOARD) {
            val sheetNow = classSheets[spinnerClass.selectedItemPosition.coerceIn(0, classSheets.lastIndex)]
            if ((sheetNow == "Class_9th" || sheetNow == "Class_10th") && subject.equals("Computer Science", ignoreCase = true)) {
                sb.append("Total Marks:  50  •  Time:  2 Hours\n")
                sb.append("Structure:  Q.1: 10 MCQs • Q.2–Q.4: Short answers (any 4 of 6 each) • Q.5–Q.7: Long questions (any 2 of 3)\n")
            } else if (sheetNow == "Class_11th" && subject.equals("English", ignoreCase = true)) {
                sb.append("Total Marks:  100  •  Time:  3 Hours\n")
                sb.append("Structure:  Q.1: 20 MCQs • Q.2: Prose shorts (any 7 of 10) • Q.3: Poem shorts (any 5 of 8) • Q.4: Play shorts (any 3 of 5) • Q.5: Letter/Application • Q.6: Story • Q.7: Reference/Punctuation/Pairs • Q.8: Translation\n")
            } else if (sheetNow == "Class_12th" && subject.equals("Physics", ignoreCase = true)) {
                sb.append("Total Marks:  85  •  Time:  3 Hours\n")
                sb.append("Structure:  Q.1: 17 MCQs • Q.2: Shorts Ch 13-15 (any 8 of 12) • Q.3: Shorts Ch 16-18 (any 8 of 12) • Q.4: Shorts Ch 19-21 (any 6 of 9) • Q.5-Q.9: Detailed questions (any 3 of 5)\n")
            } else if (sheetNow == "Class_10th" && subject.equals("Chemistry", ignoreCase = true)) {
                sb.append("Total Marks:  60  •  Time:  2 Hours\n")
                sb.append("Structure:  Q.1: 12 MCQs (one per chapter, Ch 14-24 and 26) • Q.2: Shorts Ch 14-18 (any 5 of 8) • Q.3: Shorts Ch 19-22 (any 5 of 8) • Q.4: Shorts Ch 23-26 (any 5 of 8) • Q.5-Q.7: Detailed questions (any 2 of 3)\n")
            } else if (sheetNow == "Class_9th" && subject.equals("English", ignoreCase = true)) {
                sb.append("Total Marks:  75  •  Time:  2 Hours 30 Minutes\n")
                sb.append("Structure:  Q.1: 19 MCQs (Verbs 5, Spelling 4, Meanings 5, Grammar 5) • Q.2: Shorts (any 4 of 7) • Q.3: Translate into Urdu (any 2 of 3) • Q.4: Poem summary • Q.5: Sentences (any 5 of 8) • Q.6: Letter/Story/Dialogue (any 1 of 3) • Q.7: Comprehension • Q.8: Translate into English (any 5 of 8) • Q.9: Active/Passive (5)\n")
            } else if (((sheetNow == "Class_9th" || sheetNow == "Class_10th") &&
                    (subject.equals("Physics", ignoreCase = true) || subject.equals("Biology", ignoreCase = true))) ||
                (sheetNow == "Class_10th" && subject.equals("Pak Studies", ignoreCase = true)) ||
                (sheetNow == "Class_9th" && subject.equals("Chemistry", ignoreCase = true))
            ) {
                sb.append("Total Marks:  60  •  Time:  2 Hours\n")
                sb.append("Structure:  Q.1: 12 MCQs (spread across chapters) • Q.2–Q.4: Short answers (any 5 of 8 each) • Q.5–Q.7: Detailed questions (any 2 of 3)\n")
            } else {
                sb.append("Total Marks:  75  •  Time:  2 Hours 30 Minutes\n")
                sb.append("Structure:  Q.1: 19 MCQs • Q.2: Short answers (any 5 of 8) • Q.3–Q.8: Board pattern sections\n")
            }
        } else {
            val chapters = when {
                checkAllChapters.isChecked -> "All Chapters / Full Book"
                selectedChapterLabels().isEmpty() -> "None selected"
                else -> selectedChapterLabels().joinToString(", ")
            }
            sb.append("Chapters:  $chapters\n")
            if (isManualMode()) {
                val m = eligibleMcqs().count { it.id in selectedMcqIds }
                val s = eligibleShorts().count { it.id in selectedShortIds }
                val l = eligibleLongs().count { it.id in selectedLongIds }
                sb.append("Selection:  Manual — $m MCQs, $s Short, $l Long selected\n")
            } else {
                val custom = radioCustom.isChecked
                fun cnt(field: EditText, pool: Int): String {
                    if (!custom) return "$pool (all)"
                    val n = field.text.toString().toIntOrNull()
                    return if (n == null || n < 1) "$pool (all)" else "${minOf(n, pool)} of $pool"
                }
                val parts = mutableListOf<String>()
                val t = currentType()
                if (t == PaperType.MCQS || t == PaperType.COMBINED) parts += "${cnt(etCustomCount, eligibleMcqs().size)} MCQs"
                if (t == PaperType.SHORT || t == PaperType.COMBINED) parts += "${cnt(etCountShort, eligibleShorts().size)} Short"
                if (t == PaperType.LONG || t == PaperType.COMBINED) parts += "${cnt(etCountLong, eligibleLongs().size)} Long"
                sb.append("Questions:  ${parts.joinToString(", ")}\n")
                sb.append("Selection Style:  ${if (radioSelectRandom.isChecked) "Random (no repeats)" else "First questions in original order"}\n")
            }
        }
        sb.append("Paper Mode:  ${if (radioSolved.isChecked) "Solved (with answers)" else "Unsolved (no answers)"}\n")
        sb.append("Format:  ${if (radioFormatDocx.isChecked) "MS Word (.docx)" else "PDF"}\n")
        if (currentType() != PaperType.BOARD) {
            sb.append("Order:  ${if (radioRandom.isChecked) "Random (shuffle)" else "Original"}\n")
            sb.append("Title:  ${etTitle.text.toString().trim().ifBlank { "Question Paper" }}\n")
            sb.append("Time Allowed:  ${etTime.text.toString().trim().ifBlank { "30 Minutes" }}")
        }
        return sb.toString()
    }

    private fun currentType(): PaperType = when {
        radioTypeBoard.isChecked -> PaperType.BOARD
        radioTypeShort.isChecked -> PaperType.SHORT
        radioTypeLong.isChecked -> PaperType.LONG
        radioTypeCombined.isChecked -> PaperType.COMBINED
        else -> PaperType.MCQS
    }

    /** Board Pattern mode hides the chapter / selection-mode / order blocks —
     *  the full BISE structure is fixed. Supported: Class 10 English,
     *  Class 11 English, Class 12 Physics, Class 10 Chemistry and Class 9 Computer Science. */
    private fun applyBoardVisibility() {
        if (!::tvBoardInfo.isInitialized) return
        val board = currentType() == PaperType.BOARD
        tvBoardInfo.visibility = if (board) View.VISIBLE else View.GONE
        if (board) tvBoardInfo.text = boardInfoText()
        layoutChapterSection.visibility = if (board) View.GONE else View.VISIBLE
        layoutModeSection.visibility = if (board) View.GONE else View.VISIBLE
        layoutOrderSection.visibility = if (board) View.GONE else View.VISIBLE
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
        spinnerSubject.adapter = ArrayAdapter(this, R.layout.spinner_item_selected, subjects).apply { setDropDownViewResource(R.layout.spinner_item_dropdown) }
        val idx = subjects.indexOfFirst { it.equals(incomingSubject, ignoreCase = true) }
        if (idx >= 0) spinnerSubject.setSelection(idx)
    }

    /** English writing sections (essays, paragraphs, letters, applications,
     *  stories) have no rows in the sheet - they are built from the board
     *  pattern content packs and offered as selectable "Writing:" chapters
     *  of long questions in the custom / mixed paper modes. */
    private fun englishWritingQuestions(sheet: String): List<WrittenQuestion> {
        val out = mutableListOf<WrittenQuestion>()
        fun add(idBase: Int, classNum: String, chapter: String, questions: List<String>) {
            questions.forEachIndexed { i, q ->
                if (q.isNotBlank()) out += WrittenQuestion(
                    id = -(idBase + i), classNum = classNum, subject = "English",
                    chapterNumber = "", chapterName = chapter,
                    question = q, answer = "", marks = ""
                )
            }
        }
        try {
            if (sheet == "Class_10th") {
                val pack = BoardPatternPack.load(applicationContext)
                if (pack != null) {
                    add(1000, "10th", "Writing: Essays",
                        pack.essayTopics.map { "Write an essay of 150-200 words on the topic: \"" + it + "\"" })
                    add(2000, "10th", "Writing: Paragraphs",
                        pack.paragraphTopics.map { "Write a paragraph of 100-150 words on the topic: \"" + it + "\"" } +
                            pack.tenSentenceTopics.map { "Write ten sentences on the topic: \"" + it + "\"" })
                    add(7000, "10th", "Writing: Translation (English to Urdu)",
                        pack.q3Paragraphs.map { "Translate the following paragraph into Urdu:\n" + it.text })
                    add(8000, "10th", "Writing: Translation (Urdu to English)",
                        pack.q8Paragraphs.map { "Translate the following paragraph into English:\n" + it.urdu })
                    add(9000, "10th", "Writing: Poem Summary",
                        pack.summaryPoems.map { "Write the summary of the poem: \"" + it + "\"." })
                    add(10000, "10th", "Writing: Paraphrase",
                        pack.paraphraseSets.map { "Paraphrase the following lines into simple English:\n" + it.lines })
                    add(11000, "10th", "Writing: Pair of Words",
                        pack.pairs.map { "Use the following pair of words in your own meaningful sentences: \"" + it.first + "\" and \"" + it.second + "\"." })
                }
            } else if (sheet == "Class_11th") {
                val pack = BoardPatternPack.loadEnglish11(applicationContext)
                if (pack != null) {
                    add(3000, "11th", "Writing: Letters", pack.letters)
                    add(4000, "11th", "Writing: Applications", pack.applications)
                    add(5000, "11th", "Writing: Stories",
                        pack.stories.map { "Write a story on the topic: \"" + it + "\"" })
                    add(6000, "11th", "Writing: Essays",
                        pack.essayTopics.map { "Write an essay on the topic: \"" + it + "\"" })
                }
            }
        } catch (e: Exception) { /* content packs are best-effort */ }
        return out
    }

    /** Removes textbook source labels (e.g. "Exercise 5.1 Q1(iii):", "Review Exercise 5 Q2(iv):") from question texts. */
    private fun cleanPaperText(text: String): String =
        text.replace(
            Regex("""^\s*(Review\s+Exercise|Exercise)\s+[\d.]*\s*Q\s*\d+\s*(\([ivxlcdm]+\))?\s*[:.\-]?\s*""", RegexOption.IGNORE_CASE),
            ""
        ).replace(
            // Urdu grammar books prefix every question with "سوال نمبر N:" — a paper
            // must show only its own numbering, never the book's.
            Regex("""^\s*سوال\s*(نمبر)?\s*[0-9۰-۹]+\s*[:۔.\-]?\s*"""),
            ""
        ).replace(
            Regex("""^\s*Question\s+\d+\s*[:.\-]?\s*""", RegexOption.IGNORE_CASE),
            ""
        ).trim()

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
        val prevChecked = chapterChecks.filter { it.isChecked }.map { it.tag as String }.toSet()
        layoutChapters.removeAllViews()
        chapterChecks.clear()
        sectionChecks.clear()
        loadJob = scope.launch {
            if (type == PaperType.MCQS || type == PaperType.COMBINED || type == PaperType.BOARD) {
                val fetched = try {
                    quizRepository.fetchQuestions(sheet, applicationContext)
                } catch (e: Exception) {
                    emptyList()
                }
                mcqAll = fetched.filter { subjectsMatch(it.subject, subject) }
            }
            if ((type == PaperType.SHORT || type == PaperType.COMBINED || type == PaperType.BOARD) && shortRaw.isEmpty()) {
                shortRaw = try {
                    paperRepository.fetchWritten(PaperRepository.TAB_SHORTS)
                } catch (e: Exception) {
                    emptyList()
                }
                shortTabLoaded = true
            }
            if ((type == PaperType.LONG || type == PaperType.COMBINED || type == PaperType.BOARD) && longRaw.isEmpty()) {
                longRaw = try {
                    paperRepository.fetchWritten(PaperRepository.TAB_LONGS)
                } catch (e: Exception) {
                    emptyList()
                }
                longTabLoaded = true
            }
            shortAll = shortRaw.filter { writtenMatches(it, sheet, subject) }
            longAll = longRaw.filter { writtenMatches(it, sheet, subject) }
            if (type != PaperType.BOARD && subject.equals("English", ignoreCase = true)) {
                longAll = longAll + englishWritingQuestions(sheet)
            }
            // Strip textbook source labels from all pools so every paper (manual + board) shows clean questions.
            mcqAll = mcqAll.map { it.copy(questionText = cleanPaperText(it.questionText)) }
            shortAll = shortAll.map { it.copy(question = cleanPaperText(it.question)) }
            longAll = longAll.map { it.copy(question = cleanPaperText(it.question)) }

            val labels = sortedSetOf(
                compareBy<String>({ chapterNumber(it) ?: Int.MAX_VALUE }, { it.lowercase() })
            )
            if (type == PaperType.MCQS || type == PaperType.COMBINED || type == PaperType.BOARD) labels += mcqAll.map { it.chapter }
            if (type == PaperType.SHORT || type == PaperType.COMBINED || type == PaperType.BOARD) labels += shortAll.map { it.chapterLabel }
            if (type == PaperType.LONG || type == PaperType.COMBINED) labels += longAll.map { it.chapterLabel }
            chapterNames = labels.toList()

            // Selection rows mirror the printed book: Urdu buckets group under
            // portion headers (حصہ غزل / حصہ نظم / اسباق / Grammar), Islamiyat under
            // ابواب, English splits Chapters vs Grammar & Writing, and Physics /
            // Chemistry chapters split into Theory / Numericals rows — so the
            // per-portion numbering restarts never look like duplicate chapters.
            val splitNums = PaperSections.splitsNumericals(subject) &&
                type != PaperType.MCQS && type != PaperType.BOARD
            val rowTags = mutableListOf<String>()
            for (ch in chapterNames) {
                if (splitNums) {
                    val tCount = countForChapter("$ch|THEORY", type)
                    val nCount = countForChapter("$ch|NUM", type)
                    if (tCount > 0) rowTags += "$ch|THEORY"
                    if (nCount > 0) rowTags += "$ch|NUM"
                    if (tCount == 0 && nCount == 0) rowTags += ch
                } else rowTags += ch
            }
            val sections = PaperSections.sectionsFor(subject)
            fun sectionOf(tag: String): String? {
                if (sections.isEmpty()) return null
                val name = PaperSections.nameOf(tag.substringBefore("|"))
                return when {
                    subject.equals("Urdu", ignoreCase = true) -> PaperSections.urduPortion(name)
                    subject.equals("English", ignoreCase = true) -> PaperSections.englishSection(name)
                    else -> sections.first()
                }
            }
            fun displayFor(tag: String): String {
                val base = tag.substringBefore("|")
                val name = PaperSections.nameOf(base)
                val count = countForChapter(tag, type)
                val kind = when {
                    tag.endsWith("|NUM") -> "Numericals"
                    tag.endsWith("|THEORY") -> "Theory"
                    else -> null
                }
                val main = when {
                    subject.equals("Urdu", ignoreCase = true) ->
                        PaperSections.urduRowPrefix(PaperSections.urduPortion(name)) + ": " + name
                    subject.equals("Islamiyat", ignoreCase = true) ->
                        "باب " + (chapterNumber(base) ?: "") + ": " + name
                    else -> base
                }
                return main + (kind?.let { " \u2014 $it" } ?: "") + "  ($count questions)"
            }
            val orderedTags = if (sections.isEmpty()) rowTags
                else sections.flatMap { sec -> rowTags.filter { sectionOf(it) == sec } } +
                    rowTags.filter { sectionOf(it) !in sections }
            var currentSection: String? = null
            val sectionChildren = mutableMapOf<String, MutableList<CheckBox>>()
            for (tag in orderedTags) {
                val sec = sectionOf(tag)
                if (sections.isNotEmpty() && sec != null && sec != currentSection) {
                    currentSection = sec
                    val header = CheckBox(this@PaperGeneratorActivity).apply {
                        text = sec
                        textSize = 15.5f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(Color.parseColor("#1F2937"))
                        buttonTintList = selectColors()
                        isEnabled = !checkAllChapters.isChecked
                        setOnCheckedChangeListener { btn, checked ->
                            if (btn.isPressed) {
                                sectionChildren[sec]?.forEach { it.isChecked = checked }
                                renderQuestions()
                            }
                        }
                    }
                    sectionChecks += header
                    layoutChapters.addView(header)
                }
                val cb = CheckBox(this@PaperGeneratorActivity).apply {
                    text = displayFor(tag)
                    this.tag = tag
                    textSize = 14.5f
                    setTextColor(selectColors())
                    buttonTintList = selectColors()
                    isChecked = prevChecked.contains(tag)
                    isEnabled = !checkAllChapters.isChecked
                    if (sections.isNotEmpty()) setPadding(56, 0, 0, 0)
                    setOnCheckedChangeListener { _, _ -> renderQuestions() }
                }
                chapterChecks += cb
                if (sec != null) sectionChildren.getOrPut(sec) { mutableListOf() } += cb
                layoutChapters.addView(cb)
            }
            tvChapterStatus.text = statusText(type)
            tvBoardInfo.text = boardInfoText()
            progress.visibility = View.GONE
            loading = false
            updateCountFieldsEnabled()
            renderQuestions()
        }
    }

    /** Board Pattern info line for the CURRENTLY selected class + subject only. */
    private fun boardInfoText(): String {
        val sheet = classSheets[spinnerClass.selectedItemPosition.coerceIn(0, classSheets.lastIndex)]
        val subject = spinnerSubject.selectedItem?.toString() ?: ""
        return when {
            sheet == "Class_10th" && subject.equals("English", ignoreCase = true) ->
                "Board Pattern - Class 10 English (75 Marks, 2 Hours 30 Minutes)\n" +
                    "Q.1: 19 MCQs  -  Q.2: Short answers (any 5 of 8)\n" +
                    "Q.3: Paragraph translation / simple English  -  Q.4: Poem summary / paraphrase\n" +
                    "Q.5: Essay or Paragraph  -  Q.6: Direct to Indirect (any 5 of 8)\n" +
                    "Q.7: Pair of Words (any 5 of 8)  -  Q.8: Urdu paragraph translation / ten sentences"
            sheet == "Class_10th" && subject.equals("Chemistry", ignoreCase = true) ->
                "Board Pattern - Class 10 Chemistry (60 Marks, 2 Hours)\n" +
                    "Q.1: 12 MCQs (one per chapter, Ch 14-24 and 26)\n" +
                    "Q.2: Shorts Ch 14-18 (any 5 of 8)  -  Q.3: Shorts Ch 19-22 (any 5 of 8)\n" +
                    "Q.4: Shorts Ch 23-26 (any 5 of 8)  -  Q.5-Q.7: Detailed questions (any 2 of 3)"
            sheet == "Class_10th" && subject.contains("Tarjuma", ignoreCase = true) ->
                "Board Pattern - Class 10 Tarjuma-tul-Quran (50 Marks)\n" +
                    "Q.1: 10 MCQs  -  Q.2: Short answers (any 5 of 8)\n" +
                    "Q.3: Word meanings (any 5 of 8)  -  Q.4: Ayat translation (any 3 of 5)\n" +
                    "Q.5: Detailed question (any 1 of 2)"
            sheet == "Class_11th" && subject.equals("English", ignoreCase = true) ->
                "Board Pattern - Class 11 English (100 Marks, 3 Hours)\n" +
                    "Q.1: 20 MCQs  -  Q.2: Prose shorts (any 7 of 10)  -  Q.3: Poem shorts (any 5 of 8)\n" +
                    "Q.4: Play shorts (any 3 of 5)  -  Q.5: Letter or Application  -  Q.6: Story\n" +
                    "Q.7: Reference / Punctuation / Pairs  -  Q.8: Translation into Urdu"
            sheet == "Class_12th" && subject.equals("Physics", ignoreCase = true) ->
                "Board Pattern - Class 12 Physics (85 Marks, 3 Hours)\n" +
                    "Q.1: 17 MCQs  -  Q.2: Shorts Ch 13-15 (any 8 of 12)  -  Q.3: Shorts Ch 16-18 (any 8 of 12)\n" +
                    "Q.4: Shorts Ch 19-21 (any 6 of 9)  -  Q.5-Q.9: Detailed questions (any 3 of 5)"
            sheet == "Class_10th" && (subject.equals("Mathematics", ignoreCase = true) || subject.equals("Math", ignoreCase = true)) ->
                "Board Pattern - Class 10 Mathematics (75 Marks, 2 Hours 30 Minutes)\n" +
                    "Objective (20 Min, 15 Marks): Q.1: 15 MCQs (1 each from Chapters 1-8 and 10, 2 each from Chapters 9, 11, 12)\n" +
                    "Subjective (2:10 Hours, 60 Marks): Q.2: Shorts Ch 1-4 (any 6 of 9)  -  Q.3: Shorts Ch 5-8 (any 6 of 9)\n" +
                    "Q.4: Shorts Ch 9-12 (any 6 of 9)  -  Part-II: Q.5 (Ch 1+2), Q.6 (Ch 3+4), Q.7 (Ch 5+7) - any 2\n" +
                    "Part-III: Q.8 (Ch 6 + 8/11), Q.9 (Ch 10 + 9/12) - any 1"
            (sheet == "Class_9th" || sheet == "Class_10th") && subject.equals("Urdu", ignoreCase = true) ->
                "Board Pattern - Urdu (75 Marks, 2 Hours 10 Minutes) \u2014 bilkul board paper ki tarteeb\n" +
                    "Q.1: 15 MCQs  -  Q.2: Ashaar tashreeh, Nazm 3 + Ghazal 2 (10)\n" +
                    "Q.3: Ibarat tashreeh (10)  -  Q.4: Short answers (any 5 of 8)\n" +
                    "Q.5: Khulasa (any 1 of 2)  -  Q.6: Markazi Khayal (5)\n" +
                    "Q.7: Khat / Darkhwast (10)  -  Q.8: Kahani / Mukalma (5)  -  Q.9: Muhawaray (5)"
            sheet == "Class_9th" && subject.equals("Computer Science", ignoreCase = true) ->
                "Board Pattern - Class 9 Computer Science (50 Marks, 2 Hours)\n" +
                    "Q.1: 10 MCQs  -  Q.2-Q.4: Short answers (any 4 of 6 each)\n" +
                    "Q.5-Q.7: Long questions (any 2 of 3)"
            (sheet == "Class_9th" || sheet == "Class_10th") &&
                (subject.equals("Physics", ignoreCase = true) || subject.equals("Biology", ignoreCase = true)) ||
                (sheet == "Class_10th" && subject.equals("Pak Studies", ignoreCase = true)) ||
                (sheet == "Class_9th" && subject.equals("Chemistry", ignoreCase = true)) ->
                "Board Pattern - " + classLabels[spinnerClass.selectedItemPosition.coerceIn(0, classLabels.lastIndex)] + " " + subject + " (60 Marks, 2 Hours)\n" +
                    "Q.1: 12 MCQs (spread across chapters)\n" +
                    "Q.2-Q.4: Short answers (any 5 of 8 each)  -  Q.5-Q.7: Detailed questions (any 2 of 3)"
            sheet == "Class_10th" && subject.equals("Computer Science", ignoreCase = true) ->
                "Board Pattern - Class 10 Computer Science (50 Marks, 2 Hours)\n" +
                    "Q.1: 10 MCQs  -  Q.2-Q.4: Short answers (any 4 of 6 each)\n" +
                    "Q.5-Q.7: Long questions (any 2 of 3)"
            sheet == "Class_9th" && subject.equals("English", ignoreCase = true) ->
                "Board Pattern - Class 9 English (75 Marks, 2 Hours 30 Minutes)\n" +
                    "Q.1: 19 MCQs (Verbs 5, Spelling 4, Meanings 5, Grammar 5)  -  Q.2: Short answers (any 4 of 7)\n" +
                    "Q.3: Translate into Urdu (any 2 of 3)  -  Q.4: Poem summary  -  Q.5: Sentences (any 5 of 8)\n" +
                    "Q.6: Letter/Story/Dialogue (any 1 of 3)  -  Q.7: Comprehension\n" +
                    "Q.8: Translate into English (any 5 of 8)  -  Q.9: Active/Passive Voice (5)"
            else ->
                "Board Pattern is available for Class 9/10 Urdu, Class 9 English, Class 10 English, Class 10 Chemistry, " +
                    "Class 10 Tarjuma-tul-Quran, Class 10 Mathematics, Class 11 English, " +
                    "Class 12 Physics, Class 9/10 Computer Science, Class 9/10 Physics, Class 9/10 Biology, " +
                    "Class 10 Pak Studies and Class 9 Chemistry. Select one of these " +
                    "to generate a board pattern paper."
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
        var base = label
        var wantNum: Boolean? = null
        if (label.endsWith("|NUM")) { base = label.dropLast(4); wantNum = true }
        else if (label.endsWith("|THEORY")) { base = label.dropLast(7); wantNum = false }
        if (wantNum != null && PaperSections.isNumerical(q) != wantNum) return false
        if (q.chapterLabel == base) return true
        val n = chapterNumber(base) ?: return false
        if (chapterNumber(q.chapterLabel) != n) return false
        // Urdu portions restart numbering per portion (حمد and اسم و کلمہ are both
        // "Chapter 1"), so a shared number alone must never merge two buckets:
        // the chapter NAME has to agree as well.
        val a = PaperSections.nameOf(base).lowercase().filter { it.isLetterOrDigit() }
        val b = PaperSections.nameOf(q.chapterLabel).lowercase().filter { it.isLetterOrDigit() }
        return a == b
    }

    private fun countForChapter(label: String, type: PaperType): Int {
        var n = 0
        if ((type == PaperType.MCQS || type == PaperType.COMBINED) && !label.contains("|")) {
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
        val sel = selectedChapterLabels().map { it.substringBefore("|") }.toSet()
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

    /** Selection colours (user-requested): green when an item is selected,
     *  red when it is not - one glance shows what is picked. */
    private fun selectColors(): android.content.res.ColorStateList =
        android.content.res.ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#16A34A"), Color.parseColor("#DC2626"))
        )

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
            setTextColor(selectColors())
            buttonTintList = selectColors()
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
            setTextColor(Color.parseColor("#475569"))
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
            setTextColor(selectColors())
            buttonTintList = selectColors()
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
            setTextColor(Color.parseColor("#475569"))
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
            else -> selected.joinToString(", ") { it.substringBefore("|") }
        }
        val data = PaperPdfGenerator.PaperData(
            testTitle = title,
            classLabel = classLabels[spinnerClass.selectedItemPosition.coerceIn(0, classLabels.lastIndex)],
            subject = spinnerSubject.selectedItem?.toString() ?: "",
            header = currentHeader(),
            chaptersLabel = chaptersLabel,
            timeAllowed = time,
            mcqs = mcqs,
            shorts = shorts,
            longs = longs,
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val safeSubject = data.subject.replace(Regex("[^A-Za-z0-9]+"), "_")
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "Paper_${safeSubject}_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generate(applicationContext, data, out)
                    else PaperPdfGenerator.generate(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                var msg = "Paper ready: $total questions (${mcqs.size} MCQs, ${shorts.size} Short, ${longs.size} Long) \u2022 ${file.name}"
                if (shortfallNote.isNotBlank()) msg += " $shortfallNote"
                tvStatus.text = msg
                Toast.makeText(this@PaperGeneratorActivity, "Paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 10 English): fixed BISE structure —
     *  Q.1 19 MCQs, Q.2 shorts (any 5 of 8), Q.3-Q.8 from the content pack. */
    private fun generateBoardPattern() {
        val sheet = classSheets[spinnerClass.selectedItemPosition.coerceIn(0, classSheets.lastIndex)]
        val subject = spinnerSubject.selectedItem?.toString() ?: ""
        if ((sheet == "Class_9th" || sheet == "Class_10th") && subject.equals("Computer Science", ignoreCase = true)) {
            generateBoardPatternCs()
            return
        }
        if ((sheet == "Class_9th" || sheet == "Class_10th") && subject.equals("Urdu", ignoreCase = true)) {
            generateBoardPatternUrdu()
            return
        }
        if (sheet == "Class_11th" && subject.equals("English", ignoreCase = true)) {
            generateBoardPatternEng11()
            return
        }
        if (sheet == "Class_12th" && subject.equals("Physics", ignoreCase = true)) {
            generateBoardPatternPhy12()
            return
        }
        if (sheet == "Class_10th" && subject.equals("Chemistry", ignoreCase = true)) {
            generateBoardPatternChem10()
            return
        }
        if (sheet == "Class_10th" && subject.contains("Tarjuma", ignoreCase = true)) {
            generateBoardPatternTq()
            return
        }
        if (sheet == "Class_10th" && (subject.equals("Mathematics", ignoreCase = true) || subject.equals("Math", ignoreCase = true))) {
            generateBoardPatternMath10()
            return
        }
        val isSixty = ((sheet == "Class_9th" || sheet == "Class_10th") &&
            (subject.equals("Physics", ignoreCase = true) || subject.equals("Biology", ignoreCase = true))) ||
            (sheet == "Class_10th" && subject.equals("Pak Studies", ignoreCase = true)) ||
            (sheet == "Class_9th" && subject.equals("Chemistry", ignoreCase = true))
        if (isSixty) {
            generateBoardPattern60()
            return
        }
        if (sheet == "Class_9th" && subject.equals("English", ignoreCase = true)) {
            generateBoardPatternEng9()
            return
        }
        if (sheet != "Class_10th" || !subject.equals("English", ignoreCase = true)) {
            Toast.makeText(
                this,
                "Board Pattern is available for Class 9/10 Urdu, Class 9 English, Class 10 English, Class 10 Chemistry, Class 10 Tarjuma-tul-Quran, Class 10 Mathematics, Class 11 English, Class 12 Physics, Class 9/10 Computer Science, Class 9/10 Physics, Class 9/10 Biology, Class 10 Pak Studies and Class 9 Chemistry only",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (mcqAll.size < 19) {
            Toast.makeText(
                this,
                "Not enough MCQs loaded yet (${mcqAll.size} found, 19 needed). Check your internet connection.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val diMcqs = mcqAll.filter {
            it.chapter.contains("Direct", ignoreCase = true) &&
                it.chapter.contains("Indirect", ignoreCase = true)
        }
        if (diMcqs.size < 8) {
            Toast.makeText(
                this,
                "Direct and Indirect Speech questions are not loaded yet (needed for Q.6).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (shortAll.size < 8) {
            Toast.makeText(
                this,
                "Not enough Short Questions loaded yet (${shortAll.size} found, 8 needed for Q.2).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val pack = BoardPatternPack.load(applicationContext)
        if (pack == null || pack.q3Paragraphs.isEmpty() || pack.q8Paragraphs.isEmpty() ||
            pack.pairs.size < 8 || pack.essayTopics.size < 3 || pack.paragraphTopics.size < 3 ||
            pack.summaryPoems.isEmpty() || pack.paraphraseSets.isEmpty() || pack.tenSentenceTopics.isEmpty()
        ) {
            Toast.makeText(this, "Board pattern content is missing in this build", Toast.LENGTH_LONG).show()
            return
        }
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)
        val narration = pick(diMcqs, 8).map { q ->
            val sentence = q.questionText
                .replace("Grammar: Choose the correct indirect speech of: ", "")
                .replace("Choose the correct indirect speech of: ", "")
                .trim()
            PaperPdfGenerator.NarrItem(
                sentence,
                q.options.getOrNull(q.correctAnswerIndex) ?: ""
            )
        }
        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val data = PaperPdfGenerator.BoardPaperData(
            testTitle = title,
            classLabel = "Class 10th",
            subject = "English",
            header = currentHeader(),
            mcqs = pick(mcqAll, 19),
            shorts = pick(shortAll, 8),
            q3 = pack.q3Paragraphs.random(rnd),
            poemSummary = pack.summaryPoems.random(rnd),
            paraphrase = pack.paraphraseSets.random(rnd),
            essayTopics = pick(pack.essayTopics, 3),
            paragraphTopics = pick(pack.paragraphTopics, 3),
            narration = narration,
            pairs = pick(pack.pairs, 8),
            q8 = pack.q8Paragraphs.random(rnd),
            tenSentenceTopic = pack.tenSentenceTopics.random(rnd),
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_English10_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPattern(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPattern(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Q.1: 19 MCQs, Q.2-Q.8 complete) \u2022 ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 11 English, PECTAA model paper): 100 marks —
     *  Q.1 20 MCQs; Q.2 prose shorts (any 7 of 10); Q.3 poem shorts (any 5 of 8);
     *  Q.4 play shorts (any 3 of 5); Q.5 letter OR application; Q.6 story;
     *  Q.7 reference / punctuation / pairs; Q.8 translation into Urdu. */
    private fun generateBoardPatternEng11() {
        fun isPlay(q: WrittenQuestion): Boolean {
            val s = (q.chapterName + " " + q.chapterLabel).lowercase()
            return s.contains("play") || s.contains("end of the beginning")
        }
        fun isPoem(q: WrittenQuestion): Boolean {
            val s = (q.chapterName + " " + q.chapterLabel).lowercase()
            return s.contains("poem") || s.contains("poetry") ||
                s.contains("echoing green") || s.contains("winter sundays") ||
                s.contains("ruba") || s.contains("bird came down")
        }
        val prosePool = shortAll.filter { !isPlay(it) && !isPoem(it) }
        val poemPool = shortAll.filter { isPoem(it) && !isPlay(it) }
        val playPool = shortAll.filter { isPlay(it) }
        if (mcqAll.size < 20) {
            Toast.makeText(
                this,
                "Not enough MCQs loaded yet (${mcqAll.size} found, 20 needed). Check your internet connection.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (prosePool.size < 10) {
            Toast.makeText(
                this,
                "Not enough prose Short Questions loaded yet (${prosePool.size} found, 10 needed for Q.2).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (poemPool.size < 8) {
            Toast.makeText(
                this,
                "Not enough poem Short Questions loaded yet (${poemPool.size} found, 8 needed for Q.3).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (playPool.size < 5) {
            Toast.makeText(
                this,
                "Not enough play Short Questions loaded yet (${playPool.size} found, 5 needed for Q.4).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val pack = BoardPatternPack.loadEnglish11(applicationContext)
        if (pack == null || pack.letters.isEmpty() || pack.applications.isEmpty() ||
            pack.stories.size < 2 || pack.pairs.size < 8 || pack.referenceSets.isEmpty() ||
            pack.punctuation.isEmpty() || pack.translationParas.isEmpty() || pack.essayTopics.isEmpty()
        ) {
            Toast.makeText(this, "Board pattern content is missing in this build", Toast.LENGTH_LONG).show()
            return
        }
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)
        val stories = pick(pack.stories, 2)
        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val data = PaperPdfGenerator.Eng11BoardPaperData(
            testTitle = title,
            classLabel = "Class 11th",
            subject = "English",
            header = currentHeader(),
            mcqs = pick(mcqAll, 20),
            proseShorts = pick(prosePool, 10),
            poemShorts = pick(poemPool, 8),
            playShorts = pick(playPool, 5),
            letterTopic = pack.letters.random(rnd),
            applicationTopic = pack.applications.random(rnd),
            storyA = stories[0],
            storyB = stories[1],
            refLines = pack.referenceSets.random(rnd).lines,
            punctuation = pack.punctuation.random(rnd),
            pairs = pick(pack.pairs, 8),
            translation = pack.translationParas.random(rnd),
            essayTopic = pack.essayTopics.random(rnd),
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_English11_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPatternEng11(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPatternEng11(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Q.1: 20 MCQs, Q.2-Q.8 complete) • ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 12 Physics, official pairing scheme): 85 marks —
     *  Q.1 17 MCQs (2 each from Chapters 13-20, 1 from Chapter 21); Q.2 shorts
     *  from Chapters 13-15 (any 8 of 12); Q.3 from Chapters 16-18 (any 8 of 12);
     *  Q.4 from Chapters 19-21 (any 6 of 9); Q.5-Q.9 detailed (any 3 of 5). */
    private fun generateBoardPatternPhy12() {
        fun mcqChapter(q: Question): Int =
            Regex("^Chapter\\s+(\\d+)").find(q.chapter.trim())?.groupValues?.get(1)?.toIntOrNull() ?: -1
        fun wqChapter(q: WrittenQuestion): Int =
            q.chapterNumber.filter { it.isDigit() }.toIntOrNull() ?: -1
        if (mcqAll.size < 17) {
            Toast.makeText(
                this,
                "Not enough MCQs loaded yet (${mcqAll.size} found, 17 needed). Check your internet connection.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)

        // Q.1 chapter distribution per the pairing scheme
        val mcqByCh = mcqAll.groupBy { mcqChapter(it) }
        val pickedMcqs = mutableListOf<Question>()
        for (ch in 13..20) pickedMcqs += pick(mcqByCh[ch] ?: emptyList(), 2)
        pickedMcqs += pick(mcqByCh[21] ?: emptyList(), 1)
        if (pickedMcqs.size < 17) {
            pickedMcqs += pick(mcqAll - pickedMcqs.toSet(), 17 - pickedMcqs.size)
        }
        val mcqs = pickedMcqs.take(17).shuffled(rnd)

        fun shortsQuota(chapters: List<Int>, perChapter: Int, total: Int): List<WrittenQuestion> {
            val out = mutableListOf<WrittenQuestion>()
            for (ch in chapters) {
                out += pick(shortAll.filter { wqChapter(it) == ch }, perChapter)
            }
            if (out.size < total) {
                val rest = shortAll.filter { wqChapter(it) in chapters } - out.toSet()
                out += pick(rest, total - out.size)
            }
            return out.take(total)
        }
        val q2 = shortsQuota(listOf(13, 14, 15), 4, 12)
        if (q2.size < 12) {
            Toast.makeText(
                this,
                "Not enough Short Questions from Chapters 13-15 loaded yet (${q2.size} found, 12 needed for Q.2).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val q3 = shortsQuota(listOf(16, 17, 18), 4, 12)
        if (q3.size < 12) {
            Toast.makeText(
                this,
                "Not enough Short Questions from Chapters 16-18 loaded yet (${q3.size} found, 12 needed for Q.3).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val q4 = shortsQuota(listOf(19, 20, 21), 3, 9)
        if (q4.size < 9) {
            Toast.makeText(
                this,
                "Not enough Short Questions from Chapters 19-21 loaded yet (${q4.size} found, 9 needed for Q.4).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val pairGroups = listOf(listOf(13, 14), listOf(15, 16), listOf(17), listOf(18, 19), listOf(20, 21))
        val detailed = mutableListOf<WrittenQuestion>()
        for (g in pairGroups) {
            val pool = longAll.filter { wqChapter(it) in g }
            if (pool.isNotEmpty()) detailed += pick(pool, 1)
        }
        if (detailed.size < 5) {
            detailed += pick(longAll - detailed.toSet(), 5 - detailed.size)
        }
        if (detailed.size < 5) {
            Toast.makeText(
                this,
                "Not enough Long Questions loaded yet (${longAll.size} found, 5 needed for Q.5-Q.9).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val data = PaperPdfGenerator.Phy12BoardPaperData(
            testTitle = title,
            classLabel = "Class 12th",
            subject = "Physics",
            header = currentHeader(),
            mcqs = mcqs,
            q2Shorts = q2,
            q3Shorts = q3,
            q4Shorts = q4,
            detailed = detailed.take(5),
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_Physics12_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPatternPhy12(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPatternPhy12(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Q.1: 17 MCQs, Q.2-Q.9 complete) • ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 10 Chemistry, official pairing scheme):
     *  60 marks — Q.1 12 MCQs (one per chapter), Q.2-Q.4 shorts (any 5 of 8),
     *  Q.5-Q.7 detailed (any 2 of 3). */
    private fun generateBoardPatternChem10() {
        fun mcqChapter(q: Question): Int =
            Regex("^Chapter\\s+(\\d+)").find(q.chapter.trim())?.groupValues?.get(1)?.toIntOrNull() ?: -1
        fun wqChapter(q: WrittenQuestion): Int =
            q.chapterNumber.filter { it.isDigit() }.toIntOrNull() ?: -1
        if (mcqAll.size < 12) {
            Toast.makeText(
                this,
                "Not enough MCQs loaded yet (${mcqAll.size} found, 12 needed). Check your internet connection.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)

        // Q.1: one MCQ from each chapter of the pairing scheme (Ch 25 has none)
        val mcqChapters = listOf(14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 26)
        val mcqByCh = mcqAll.groupBy { mcqChapter(it) }
        val pickedMcqs = mutableListOf<Question>()
        for (ch in mcqChapters) pickedMcqs += pick(mcqByCh[ch] ?: emptyList(), 1)
        if (pickedMcqs.size < 12) {
            pickedMcqs += pick(mcqAll - pickedMcqs.toSet(), 12 - pickedMcqs.size)
        }
        val mcqs = pickedMcqs.take(12).shuffled(rnd)

        fun shortsByCounts(counts: Map<Int, Int>, total: Int): List<WrittenQuestion> {
            val out = mutableListOf<WrittenQuestion>()
            for ((ch, n) in counts) {
                out += pick(shortAll.filter { wqChapter(it) == ch }, n)
            }
            if (out.size < total) {
                val rest = shortAll.filter { wqChapter(it) in counts.keys } - out.toSet()
                out += pick(rest, total - out.size)
            }
            return out.take(total)
        }
        val q2 = shortsByCounts(mapOf(14 to 2, 15 to 1, 16 to 1, 17 to 2, 18 to 2), 8)
        if (q2.size < 8) {
            Toast.makeText(
                this,
                "Not enough Short Questions from Chapters 14-18 loaded yet (${q2.size} found, 8 needed for Q.2).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val q3 = shortsByCounts(mapOf(19 to 2, 20 to 2, 21 to 2, 22 to 2), 8)
        if (q3.size < 8) {
            Toast.makeText(
                this,
                "Not enough Short Questions from Chapters 19-22 loaded yet (${q3.size} found, 8 needed for Q.3).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val q4 = shortsByCounts(mapOf(23 to 2, 24 to 2, 25 to 2, 26 to 2), 8)
        if (q4.size < 8) {
            Toast.makeText(
                this,
                "Not enough Short Questions from Chapters 23-26 loaded yet (${q4.size} found, 8 needed for Q.4).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val pairGroups = listOf(listOf(14, 15), listOf(20, 22), listOf(23, 25))
        val detailed = mutableListOf<WrittenQuestion>()
        for (g in pairGroups) {
            val pool = longAll.filter { wqChapter(it) in g }
            if (pool.isNotEmpty()) detailed += pick(pool, 1)
        }
        if (detailed.size < 3) {
            detailed += pick(longAll - detailed.toSet(), 3 - detailed.size)
        }
        if (detailed.size < 3) {
            Toast.makeText(
                this,
                "Not enough Long Questions loaded yet (${longAll.size} found, 3 needed for Q.5-Q.7).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val data = PaperPdfGenerator.Chem10BoardPaperData(
            testTitle = title,
            classLabel = "Class 10th",
            subject = "Chemistry",
            header = currentHeader(),
            mcqs = mcqs,
            q2Shorts = q2,
            q3Shorts = q3,
            q4Shorts = q4,
            detailed = detailed.take(3),
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_Chemistry10_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPatternChem10(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPatternChem10(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Q.1: 12 MCQs, Q.2-Q.7 complete) • ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 10 Mathematics, official PCTB pairing scheme):
     *  75 marks, 2:30 hours - Objective (20 min): Q.1 15 MCQs (1 each from
     *  Chapters 1-8 and 10; 2 each from Chapters 9, 11, 12). Subjective (2:10):
     *  Part-I Q.2 shorts Chapters 1-4, Q.3 Chapters 5-8, Q.4 Chapters 9-12
     *  (any 6 of 9 each); Part-II any 2 of Q.5 (Ch 1+2), Q.6 (Ch 3+4),
     *  Q.7 (Ch 5+7) with (a)/(b) parts of 4 marks; Part-III any 1 of
     *  Q.8 (Ch 6 + Ch 8 or 11), Q.9 (Ch 10 + Ch 9 or 12). */
    private fun generateBoardPatternMath10() {
        fun mcqChapter(q: Question): Int =
            Regex("^Chapter\\s+(\\d+)").find(q.chapter.trim())?.groupValues?.get(1)?.toIntOrNull() ?: -1
        fun wqChapter(q: WrittenQuestion): Int =
            q.chapterNumber.filter { it.isDigit() }.toIntOrNull() ?: -1
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)

        // Q.1 - chapter-exact MCQ spread from the pairing scheme
        if (mcqAll.size < 15) {
            Toast.makeText(
                this,
                "Not enough MCQs loaded yet (${mcqAll.size} found, 15 needed). Check your internet connection.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val spread = linkedMapOf(1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 1, 8 to 1, 9 to 2, 10 to 1, 11 to 2, 12 to 2)
        val mcqByCh = mcqAll.groupBy { mcqChapter(it) }
        val pickedMcqs = mutableListOf<Question>()
        for ((ch, n) in spread) pickedMcqs += pick(mcqByCh[ch] ?: emptyList(), n)
        if (pickedMcqs.size < 15) {
            pickedMcqs += pick(mcqAll - pickedMcqs.toSet(), 15 - pickedMcqs.size)
        }
        val mcqs = pickedMcqs.take(15).shuffled(rnd)

        // Q.2-Q.4 - nine shorts per chapter group (any 6 attempted by students)
        fun groupShorts(chapters: List<Int>, preferred: Map<Int, Int>, label: String): List<WrittenQuestion>? {
            val out = mutableListOf<WrittenQuestion>()
            for ((ch, n) in preferred) {
                out += pick(shortAll.filter { wqChapter(it) == ch }, n)
            }
            if (out.size < 9) {
                val rest = shortAll.filter { wqChapter(it) in chapters } - out.toSet()
                out += pick(rest, 9 - out.size)
            }
            if (out.size < 9) {
                Toast.makeText(
                    this,
                    "Not enough Short Questions from Chapters ${chapters.first()}-${chapters.last()} loaded yet (${out.size} found, 9 needed for $label).",
                    Toast.LENGTH_LONG
                ).show()
                return null
            }
            return out.take(9)
        }
        val q2 = groupShorts(listOf(1, 2, 3, 4), mapOf(1 to 3, 2 to 2, 3 to 2, 4 to 2), "Q.2") ?: return
        val q3 = groupShorts(listOf(5, 6, 7, 8), mapOf(5 to 3, 6 to 2, 7 to 2, 8 to 2), "Q.3") ?: return
        val q4 = groupShorts(listOf(9, 10, 11, 12), mapOf(9 to 3, 10 to 2, 11 to 2, 12 to 2), "Q.4") ?: return

        // Part-II / Part-III long questions with (a) + (b) parts
        fun longFrom(ch: Int): WrittenQuestion? = pick(longAll.filter { wqChapter(it) == ch }, 1).firstOrNull()
        fun longPair(aCh: Int, bCh: Int, label: String): Pair<WrittenQuestion, WrittenQuestion>? {
            val a = longFrom(aCh)
            val b = longFrom(bCh)
            if (a == null || b == null) {
                val missing = if (a == null) aCh else bCh
                Toast.makeText(
                    this,
                    "Long Questions for Chapter $missing are not available yet (needed for $label).",
                    Toast.LENGTH_LONG
                ).show()
                return null
            }
            return a to b
        }
        fun longPairAlt(aCh: Int, bFirst: Int, bAlt: Int, label: String): Pair<WrittenQuestion, WrittenQuestion>? {
            val a = longFrom(aCh)
            val b = longFrom(bFirst) ?: longFrom(bAlt)
            if (a == null || b == null) {
                Toast.makeText(
                    this,
                    "Long Questions for Chapter ${if (a == null) aCh else bFirst} are not available yet (needed for $label).",
                    Toast.LENGTH_LONG
                ).show()
                return null
            }
            return a to b
        }
        val p5 = longPair(1, 2, "Q.5") ?: return
        val p6 = longPair(3, 4, "Q.6") ?: return
        val p7 = longPair(5, 7, "Q.7") ?: return
        val p8 = longPairAlt(6, 8, 11, "Q.8") ?: return
        val p9 = longPairAlt(10, 9, 12, "Q.9") ?: return

        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val data = PaperPdfGenerator.Math10BoardPaperData(
            testTitle = title,
            classLabel = "Class 10th",
            subject = "Mathematics",
            header = currentHeader(),
            mcqs = mcqs,
            q2Shorts = q2,
            q3Shorts = q3,
            q4Shorts = q4,
            partII = listOf(p5, p6, p7),
            partIII = listOf(p8, p9),
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_Math10_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPatternMath10(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPatternMath10(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Q.1: 15 MCQs, Q.2-Q.9 complete) \u2022 ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 9 Computer Science): fixed BISE structure —
     *  Q.1 10 MCQs, Q.2-Q.4 shorts (any 4 of 6 each), Q.5-Q.7 longs (any 2 of 3). */
    private fun generateBoardPatternCs() {
        if (mcqAll.size < 10) {
            Toast.makeText(
                this,
                "Not enough MCQs loaded yet (${mcqAll.size} found, 10 needed). Check your internet connection.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (shortAll.size < 18) {
            Toast.makeText(
                this,
                "Not enough Short Questions loaded yet (${shortAll.size} found, 18 needed for Q.2-Q.4).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (longAll.size < 3) {
            Toast.makeText(
                this,
                "Not enough Long Questions loaded yet (${longAll.size} found, 3 needed for Q.5-Q.7).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)
        val shortPool = pick(shortAll, 18)
        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val csClass = classSheets[spinnerClass.selectedItemPosition.coerceIn(0, classSheets.lastIndex)]
        val data = PaperPdfGenerator.CsBoardPaperData(
            testTitle = title,
            classLabel = if (csClass == "Class_10th") "Class 10th" else "Class 9th",
            subject = "Computer Science",
            header = currentHeader(),
            mcqs = pick(mcqAll, 10),
            shortSections = listOf(shortPool.take(6), shortPool.drop(6).take(6), shortPool.drop(12).take(6)),
            longs = pick(longAll, 3),
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_CS" + (if (csClass == "Class_10th") "10" else "9") + "_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPatternCs(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPatternCs(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Q.1: 10 MCQs, Q.2-Q.4 Shorts, Q.5-Q.7 Longs) • ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (60 marks — Physics/Biology Class 9 & 10, Pak Studies
     *  Class 10, Chemistry Class 9): Q.1 12 MCQs spread round-robin across the
     *  subject's chapters; Q.2-Q.4 shorts (any 5 of 8 each) drawn from three
     *  contiguous chapter groups (shortfalls filled from the whole pool);
     *  Q.5-Q.7 one long each from the three groups (any 2 of 3). */
    private fun generateBoardPattern60() {
        val subject = spinnerSubject.selectedItem?.toString() ?: ""
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)

        fun mcqChapter(q: Question): Int =
            Regex("""Chapter\s*(\d+)""").find(q.chapter)?.groupValues?.get(1)?.toIntOrNull() ?: -1
        fun wqChapter(q: WrittenQuestion): Int =
            q.chapterNumber.filter { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE

        if (mcqAll.size < 12) {
            Toast.makeText(
                this,
                "Not enough MCQs loaded yet (${mcqAll.size} found, 12 needed). Note: Class 9 Chemistry has no MCQs in the source book, so its board pattern paper cannot be generated yet.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (shortAll.size < 24) {
            Toast.makeText(
                this,
                "Not enough Short Questions loaded yet (${shortAll.size} found, 24 needed for Q.2-Q.4).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (longAll.size < 3) {
            Toast.makeText(
                this,
                "Not enough Long Questions loaded yet (${longAll.size} found, 3 needed for Q.5-Q.7).",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Q.1 — 12 MCQs, round-robin across chapters so every chapter is covered.
        val perChapter = mcqAll.filter { mcqChapter(it) > 0 }
            .groupBy { mcqChapter(it) }.toSortedMap()
            .mapValues { (_, v) -> v.shuffled(rnd).toMutableList() }
        val mcqs = mutableListOf<Question>()
        while (mcqs.size < 12 && perChapter.values.any { it.isNotEmpty() }) {
            for (list in perChapter.values) {
                if (mcqs.size < 12 && list.isNotEmpty()) mcqs += list.removeAt(0)
            }
        }
        if (mcqs.size < 12) {
            Toast.makeText(
                this,
                "Could not spread 12 MCQs across chapters (only ${mcqs.size} chapter-tagged MCQs found).",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Q.2-Q.4 — chapters (sorted) split into three contiguous groups.
        val chapters = shortAll.map { wqChapter(it) }.filter { it != Int.MAX_VALUE }.distinct().sorted()
        val groupSize = ((chapters.size + 2) / 3).coerceAtLeast(1)
        val groups = chapters.chunked(groupSize).toMutableList()
        while (groups.size < 3) groups += emptyList<Int>()
        fun pickShorts(chaps: List<Int>, n: Int): List<WrittenQuestion> {
            val inGroup = shortAll.filter { wqChapter(it) in chaps }.shuffled(rnd)
            if (inGroup.size >= n) return inGroup.take(n)
            val rest = shortAll.filter { it !in inGroup }.shuffled(rnd)
            return (inGroup + rest).take(n)
        }
        val shortSections = groups.take(3).map { pickShorts(it, 8) }
        fun pickLong(chaps: List<Int>): WrittenQuestion? {
            val inGroup = longAll.filter { wqChapter(it) in chaps }.shuffled(rnd)
            return (if (inGroup.isNotEmpty()) inGroup else longAll.shuffled(rnd)).firstOrNull()
        }
        val longs = groups.take(3).mapNotNull { pickLong(it) }

        val classLabel = classLabels[spinnerClass.selectedItemPosition.coerceIn(0, classLabels.lastIndex)]
        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val data = PaperPdfGenerator.SixtyBoardPaperData(
            testTitle = title,
            classLabel = classLabel,
            subject = subject,
            header = currentHeader(),
            mcqs = mcqs,
            shortSections = shortSections,
            longs = longs,
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_60_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPattern60(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPattern60(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Q.1: 12 MCQs, Q.2-Q.4 Shorts, Q.5-Q.7 Longs) • ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 9 English, Pairing Scheme 2027): 75 marks —
     *  Q.1 19 MCQs (verbs 5, spelling 4, meanings 5, grammar 5); Q.2 shorts
     *  (attempt 3 of 5 + 1 of 2); Q.3 translate into Urdu (any 2 of 3);
     *  Q.4 poem summary; Q.5 sentences (any 5 of 8); Q.6 letter/story/dialogue
     *  (any 1 of 3); Q.7 comprehension (passage + 5 questions from its
     *  chapter); Q.8 translate into English (any 5 of 8); Q.9 voice (5). */
    private fun generateBoardPatternEng9() {
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)

        val pack = BoardPatternPack.loadEnglish9(applicationContext)
        if (pack == null || pack.translations.size < 3 || pack.summaries.isEmpty() ||
            pack.words.size < 8 || pack.prompts.size < 3 || pack.urduToEnglish.size < 8 ||
            pack.passages.isEmpty()
        ) {
            Toast.makeText(this, "Board pattern content is missing in this build", Toast.LENGTH_LONG).show()
            return
        }

        // Q.1 pools — classify the live Class_9th English MCQs by question text,
        // topping up from the content pack if a live pool runs short.
        fun poolOf(t: String): String {
            val l = t.lowercase()
            return when {
                l.contains("correct spelling") -> "spelling"
                l.contains("means") || l.contains("synonym") || l.contains("antonym") || l.contains("meaning of") -> "meanings"
                l.contains("passive") || l.contains(" voice") -> "voice"
                l.contains("verb") || l.contains("form of") || l.contains("correct form") -> "verbs"
                else -> "grammar"
            }
        }
        val live = mcqAll.groupBy { poolOf(it.questionText) }
        fun pool(key: String, packPool: List<BoardPatternPack.Eng9Mcq>, quota: Int): List<Question> {
            val out = (live[key] ?: emptyList()).shuffled(rnd).toMutableList()
            if (out.size < quota) {
                val used = out.map { it.id }.toSet()
                val extra = packPool.filter { it.id !in used }.map {
                    Question(it.id, "9", "English", "Grammar", it.question, it.options, it.correct, it.answer)
                }
                out += extra.shuffled(rnd).take(quota - out.size)
            }
            return out.shuffled(rnd).take(quota)
        }
        // NOTE: verbs and voice use live-sheet items ONLY — no verb-form or
        // voice MCQs exist in the English-9 sources yet, and topping these two
        // pools up from grammar would print mislabeled questions. The guards
        // below will honestly report the missing pools until such MCQs arrive.
        val verbs = (live["verbs"] ?: emptyList()).shuffled(rnd).take(5)
        val spelling = pool("spelling", pack.spelling, 4)
        val meanings = pool("meanings", pack.meanings, 5)
        val grammar = pool("grammar", pack.grammar, 5)
        val voice = (live["voice"] ?: emptyList()).shuffled(rnd).take(5)
        val poolChecks = listOf(
            Triple("Form of Verbs", verbs, 5), Triple("Correct Spelling", spelling, 4),
            Triple("Correct Meanings", meanings, 5), Triple("Grammar", grammar, 5),
            Triple("Active/Passive Voice", voice, 5)
        )
        for ((name, list, need) in poolChecks) {
            if (list.size < need) {
                val hint = if (list.isEmpty())
                    "Questions for this section are not available in the question bank yet."
                else "Check your internet connection."
                Toast.makeText(
                    this,
                    "Not enough '$name' MCQs loaded yet (${list.size} found, $need needed). $hint",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        if (shortAll.size < 7) {
            Toast.makeText(
                this,
                "Not enough Short Questions loaded yet (${shortAll.size} found, 7 needed for Q.2).",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Q.7 — a passage whose chapter has at least 5 short questions.
        fun chapNum(wq: WrittenQuestion): Int? = wq.chapterNumber.filter { it.isDigit() }.toIntOrNull()
        val viable = pack.passages.filter { p -> shortAll.count { chapNum(it) == p.chapter } >= 5 }
        if (viable.isEmpty()) {
            Toast.makeText(
                this,
                "Not enough short questions in any passage chapter for the Q.7 comprehension section.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val passage = viable.random(rnd)
        val q7Questions = pick(shortAll.filter { chapNum(it) == passage.chapter }, 5)

        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val data = PaperPdfGenerator.Eng9BoardPaperData(
            testTitle = title,
            classLabel = "Class 9th",
            subject = "English",
            header = currentHeader(),
            verbs = verbs,
            spelling = spelling,
            meanings = meanings,
            grammar = grammar,
            voice = voice,
            q2Shorts = pick(shortAll, 7),
            q3Translations = pick(pack.translations, 3),
            q4Summary = pack.summaries.random(rnd),
            q5Words = pick(pack.words, 8),
            q6Prompts = pick(pack.prompts, 3),
            q7Passage = passage,
            q7Questions = q7Questions,
            q8Options = pick(pack.urduToEnglish, 8),
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_English9_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPatternEng9(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPatternEng9(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Q.1: 19 MCQs, Q.2-Q.9 complete) • ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 10 Tarjuma-tul-Quran): 50 marks —
     *  Q.1 10 MCQs; Q.2 shorts (any 5 of 8); Q.3 word meanings (any 5 of 8);
     *  Q.4 ayat translation (any 3 of 5); Q.5 detailed (any 1 of 2). */
    private fun generateBoardPatternTq() {
        val tqPrefix = "درج ذیل آیت کا بامحاورہ ترجمہ کریں:"
        val ayatPool = longAll.filter { it.question.startsWith(tqPrefix) }
            .map { it.copy(question = it.question.removePrefix(tqPrefix).trim()) }
        val qaLongPool = longAll.filterNot { it.question.startsWith(tqPrefix) }
        if (mcqAll.size < 10) {
            Toast.makeText(
                this,
                "Not enough MCQs loaded yet (${mcqAll.size} found, 10 needed). Check your internet connection.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (shortAll.size < 8) {
            Toast.makeText(
                this,
                "Not enough Short Questions loaded yet (${shortAll.size} found, 8 needed for Q.2).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (ayatPool.size < 5) {
            Toast.makeText(
                this,
                "Not enough Ayat Translation questions loaded yet (${ayatPool.size} found, 5 needed for Q.4).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (qaLongPool.size < 2) {
            Toast.makeText(
                this,
                "Not enough Long Questions loaded yet (${qaLongPool.size} found, 2 needed for Q.5).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val meanings = BoardPatternPack.loadTarjuma(applicationContext)
        if (meanings.size < 8) {
            Toast.makeText(
                this,
                "Not enough Word Meanings loaded yet (${meanings.size} found, 8 needed for Q.3).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)
        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val data = PaperPdfGenerator.TqBoardPaperData(
            testTitle = title,
            classLabel = "Class 10th",
            subject = spinnerSubject.selectedItem?.toString() ?: "",
            header = currentHeader(),
            mcqs = pick(mcqAll, 10),
            shorts = pick(shortAll, 8),
            meanings = pick(meanings, 8),
            ayat = pick(ayatPool, 5),
            longs = pick(qaLongPool, 2),
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_TQ10_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPatternTq(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPatternTq(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Tarjuma: 10 MCQs, Q.2 Shorts, Q.3 Meanings, Q.4 Ayat Translation, Q.5 Detailed) • ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Board Pattern paper (Class 9/10 Urdu) — the real BISE structure from the
     *  board paper the user photographed: Q.1 15 MCQs; Q.2 ashaar tashreeh
     *  (Nazm 3 + Ghazal 2, 10=2x5); Q.3 ibarat tashreeh (10=5+5); Q.4 short
     *  answers (any 5 of 8, 10=2x5); Q.5 khulasa (any 1 of 2, 5); Q.6 markazi
     *  khayal (5=1+4); Q.7 khat / darkhwast (10) — or mazmoon where the book
     *  files essays instead; Q.8 kahani / mukalma (5); Q.9 muhawaray (5).
     *  Every pool comes from the sheet; nothing is invented. */
    private fun generateBoardPatternUrdu() {
        val sheet = classSheets[spinnerClass.selectedItemPosition.coerceIn(0, classSheets.lastIndex)]
        val rnd = kotlin.random.Random.Default
        fun <T> pick(list: List<T>, n: Int): List<T> = list.shuffled(rnd).take(n)
        fun fail(msg: String): Boolean {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            return true
        }
        fun portion(wq: WrittenQuestion) = PaperSections.urduPortion(wq.chapterName)

        if (mcqAll.size < 15) { fail("Not enough MCQs loaded yet (${mcqAll.size} found, 15 needed). Check your internet connection."); return }
        val nazmAny = longAll.filter { portion(it) == PaperSections.NAZM }
        val nazmT = nazmAny.filter { it.question.contains("تشریح") }
        val ghazalAny = longAll.filter { portion(it) == PaperSections.GHAZAL }
        val ghazalT = ghazalAny.filter { it.question.contains("تشریح") }
        val q2a = pick(if (nazmT.size >= 3) nazmT else nazmAny, 3)
        val q2bPool = if (ghazalT.isNotEmpty()) ghazalT else ghazalAny
        val q2b = pick(if (q2bPool.isNotEmpty()) q2bPool else nazmAny.filter { it !in q2a }, 2)
        if (q2a.size < 3 || q2b.size < 2) { fail("Not enough Tashreeh questions loaded yet for Q.2."); return }
        val tafheem = longAll.filter { it.chapterNumber == "21" }
        val asbaqAny = longAll.filter { portion(it) == PaperSections.ASBAQ }
        val asbaqT = asbaqAny.filter { it.question.contains("تشریح") }
        val q3 = pick(if (tafheem.size >= 2) tafheem else if (asbaqT.size >= 2) asbaqT else asbaqAny, 2)
        if (q3.size < 2) { fail("Not enough Ibarat Tashreeh questions loaded yet for Q.3."); return }
        val asbaqShorts = shortAll.filter { portion(it) == PaperSections.ASBAQ }
        val q4 = pick(if (asbaqShorts.size >= 8) asbaqShorts else shortAll, 8)
        if (q4.size < 8) { fail("Not enough Short Questions loaded yet (${q4.size} found, 8 needed for Q.4)."); return }
        val khulasa = asbaqAny.filter { it.question.contains("خلاصہ") }
        val q5 = pick(if (khulasa.isNotEmpty()) khulasa else asbaqAny, 2)
        if (q5.isEmpty()) { fail("No Khulasa question loaded yet for Q.5."); return }
        val markazi = (longAll + shortAll).filter { it.question.contains("مرکزی خیال") }.distinctBy { it.question }
        var q6 = pick(markazi, 2)
        var q6Fallback = false
        if (q6.isEmpty()) {
            q6 = pick(nazmAny, 1)
            q6Fallback = true
        }
        if (q6.isEmpty()) { fail("No Markazi Khayal question loaded yet for Q.6."); return }
        val khatPool = (longAll + shortAll).filter {
            it.question.contains("خط") || it.question.contains("درخواست")
        }.distinctBy { it.question }
        val mazPool = longAll.filter { it.chapterNumber == "23" }
        val q7Kind: String
        val q7: List<WrittenQuestion>
        if (khatPool.size >= 2) { q7 = pick(khatPool, 2); q7Kind = "KHAT" }
        else if (mazPool.size >= 3) { q7 = pick(mazPool, 3); q7Kind = "MAZMOON" }
        else { fail("Not enough Khat / Darkhwast questions loaded yet for Q.7."); return }
        val kahaniPool = (longAll + shortAll).filter {
            it.question.contains("کہانی") || it.question.contains("مکالم")
        }.distinctBy { it.question }
        val q8 = pick(kahaniPool, 2)
        if (q8.isEmpty()) { fail("No Kahani / Mukalma question loaded yet for Q.8."); return }
        val idiomPool = shortAll.filter { it.question.contains("محاور") || it.question.contains("ضرب المثل") }
        val q9 = pick(idiomPool, 7)
        if (q9.size < 5) { fail("Not enough Muhawaray questions loaded yet (${q9.size} found, 5 needed for Q.9)."); return }

        val classLabel = classLabels[spinnerClass.selectedItemPosition.coerceIn(0, classLabels.lastIndex)]
        val typedTitle = etTitle.text.toString().trim()
        val title = if (typedTitle.isBlank() || typedTitle == "Question Paper") {
            "Board Pattern Paper"
        } else typedTitle
        val data = PaperPdfGenerator.UrduBoardPaperData(
            testTitle = title,
            classLabel = classLabel,
            subject = spinnerSubject.selectedItem?.toString() ?: "",
            header = currentHeader(),
            mcqs = pick(mcqAll, 15),
            q2a = q2a, q2aLabel = portion(q2a.first()),
            q2b = q2b, q2bLabel = portion(q2b.first()),
            q3 = q3, q4 = q4, q5 = q5,
            q6 = q6, q6Fallback = q6Fallback,
            q7 = q7, q7Kind = q7Kind,
            q8 = q8, q9 = q9, q9Attempt = if (q9.size >= 7) 5 else q9.size,
            includeAnswers = radioSolved.isChecked
        )
        btnGenerate.isEnabled = false
        tvStatus.text = "Generating paper..."
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val docx = isDocx()
                    val out = File(papersDir(), "BoardPattern_Urdu_$ts." + if (docx) "docx" else "pdf")
                    if (docx) PaperDocxGenerator.generateBoardPatternUrdu(applicationContext, data, out)
                    else PaperPdfGenerator.generateBoardPatternUrdu(applicationContext, data, out)
                }
            } catch (e: Exception) {
                null
            }
            btnGenerate.isEnabled = true
            if (file != null && file.exists()) {
                lastFile = file
                layoutActions.visibility = View.VISIBLE
                tvStatus.text = "Paper ready: Board Pattern Paper (Urdu: 15 MCQs, Q.2 Tashreeh, Q.3 Ibarat, Q.4 Shorts, Q.5 Khulasa, Q.6 Markazi Khayal, Q.7 Khat/Darkhwast, Q.8 Kahani/Mukalma, Q.9 Muhawaray) \u2022 ${file.name}"
                Toast.makeText(this@PaperGeneratorActivity, "Board Pattern paper generated", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "Paper generation failed. Please try again."
            }
        }
    }

    /** Generated papers live in the app-specific external folder (not the
     *  internal cache): external viewers such as MS Word open them reliably
     *  through the FileProvider external-files path. */
    private fun papersDir(): File {
        val base = getExternalFilesDir(null) ?: cacheDir
        return File(base, "papers").apply { mkdirs() }
    }

    private fun pdfUri(file: File) =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun isDocx(): Boolean = radioFormatDocx.isChecked

    private fun mimeFor(file: File): String =
        if (file.extension.equals("docx", ignoreCase = true)) {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        } else {
            "application/pdf"
        }

    private fun previewPdf() {
        val file = lastFile ?: return
        try {
            val uri = pdfUri(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeFor(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri(null, uri)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No viewer app found for this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdf() {
        val file = lastFile ?: return
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeFor(file)
                putExtra(Intent.EXTRA_STREAM, pdfUri(file))
                putExtra(Intent.EXTRA_SUBJECT, "StudyPoint Question Paper")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Paper"))
        } catch (e: Exception) {
            Toast.makeText(this, "Sharing failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePdf() {
        val file = lastFile ?: return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeFor(file)
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
