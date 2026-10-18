package com.example.studypoint.teacher

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Award List Create — 3-step guided workflow:
 * Step 1: Campus / School / Class / Section / Gender
 * Step 2: Select students (only those matching Step 1)
 * Step 3: Title / Session / Subject / Exam Type / Dates, then Create or Save Draft
 */
class CreateAwardListActivity : AppCompatActivity() {

    private var allStudents: List<TeacherStudent> = emptyList()

    // Step 1 values
    private var campus = ""
    private var school = ""
    private var className = ""
    private var section = ""
    private var gender = "All"

    // Step 2 state
    private val selectedIds = mutableSetOf<String>()

    // Step 3 values
    private var session = "2026-27"
    private var subject = ""
    private var examType = ""
    private var examDate = ""
    private var returnDate = ""
    private var titleTouched = false

    private lateinit var step1: ScrollView
    private lateinit var step2: LinearLayout
    private lateinit var step3: ScrollView
    private lateinit var txtStepSub: TextView
    private lateinit var checkContainer: LinearLayout
    private lateinit var txtSelectedCount: TextView
    private lateinit var txtNoStudents: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_award_list)
        title = "Award List Create"

        step1 = findViewById(R.id.step1)
        step2 = findViewById(R.id.step2)
        step3 = findViewById(R.id.step3)
        txtStepSub = findViewById(R.id.txtStepSub)
        checkContainer = findViewById(R.id.checkContainer)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)
        txtNoStudents = findViewById(R.id.txtNoStudents)

        bindStep1()
        bindStep2()
        bindStep3()
        showStep(1)
    }

    override fun onResume() {
        super.onResume()
        TeacherRepository.loadStudents(
            onOk = {
                allStudents = it
                if (step2.visibility == View.VISIBLE) renderStudentChecks()
            },
            onErr = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        )
    }

    // ---------------- Step 1 ----------------

    private fun bindStep1() {
        val pickCampus = findViewById<TextView>(R.id.pickCampus)
        val pickSchool = findViewById<TextView>(R.id.pickSchool)
        val pickClass = findViewById<TextView>(R.id.pickClass)
        val pickSection = findViewById<TextView>(R.id.pickSection)
        val pickGender = findViewById<TextView>(R.id.pickGender)

        fun refresh() {
            pickCampus.text = if (campus.isEmpty()) "Select Campus ▾" else "$campus ▾"
            pickSchool.text = if (school.isEmpty()) "Select School / College ▾" else "$school ▾"
            pickClass.text = if (className.isEmpty()) "Select Class ▾" else "$className ▾"
            pickSection.text = if (section.isEmpty()) "Select Section ▾" else "$section ▾"
            pickGender.text = "$gender ▾"
        }
        pickCampus.setOnClickListener {
            val opts = allStudents.map { it.campus }.filter { it.isNotBlank() }.distinct().sorted()
            SearchablePicker.showSimple(this, "Campus", opts, allowCustom = true) { campus = it; refresh() }
        }
        pickSchool.setOnClickListener {
            val opts = allStudents.map { it.school }.filter { it.isNotBlank() }.distinct().sorted()
            SearchablePicker.showSimple(this, "School / College", opts, allowCustom = true) { school = it; refresh() }
        }
        pickClass.setOnClickListener {
            SearchablePicker.showSimple(this, "Class", StudentEditActivity.CLASS_OPTIONS, allowCustom = true) {
                className = it; refresh()
            }
        }
        pickSection.setOnClickListener {
            SearchablePicker.showSimple(this, "Section", StudentEditActivity.SECTION_OPTIONS, allowCustom = true) {
                section = it; refresh()
            }
        }
        pickGender.setOnClickListener {
            SearchablePicker.showSimple(this, "Gender", listOf("All", "Male", "Female"), allowCustom = false) {
                gender = it; refresh()
            }
        }
        refresh()

        findViewById<Button>(R.id.btnStep1Next).setOnClickListener {
            when {
                campus.isEmpty() -> toast("Please select the Campus")
                school.isEmpty() -> toast("Please select the School / College")
                className.isEmpty() -> toast("Please select the Class")
                section.isEmpty() -> toast("Please select the Section")
                else -> {
                    renderStudentChecks()
                    showStep(2)
                }
            }
        }
    }

    // ---------------- Step 2 ----------------

    private fun matchedStudents(): List<TeacherStudent> = allStudents.filter { s ->
        s.campus.equals(campus, ignoreCase = true) &&
            s.school.equals(school, ignoreCase = true) &&
            s.className.equals(className, ignoreCase = true) &&
            s.section.equals(section, ignoreCase = true) &&
            (gender == "All" || s.gender.equals(gender, ignoreCase = true))
    }

    private fun bindStep2() {
        findViewById<EditText>(R.id.editSearchStudent).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = renderStudentChecks()
            override fun afterTextChanged(s: Editable?) {}
        })
        findViewById<Button>(R.id.btnSelectAll).setOnClickListener {
            selectedIds.addAll(matchedStudents().map { it.id })
            renderStudentChecks()
        }
        findViewById<Button>(R.id.btnClearAll).setOnClickListener {
            selectedIds.clear()
            renderStudentChecks()
        }
        findViewById<Button>(R.id.btnAddNewStudent).setOnClickListener {
            val i = Intent(this, StudentEditActivity::class.java)
                .putExtra(StudentEditActivity.EXTRA_CAMPUS, campus)
                .putExtra(StudentEditActivity.EXTRA_SCHOOL, school)
                .putExtra(StudentEditActivity.EXTRA_CLASS, className)
                .putExtra(StudentEditActivity.EXTRA_SECTION, section)
            if (gender != "All") i.putExtra(StudentEditActivity.EXTRA_GENDER, gender)
            startActivity(i)
        }
        findViewById<Button>(R.id.btnStep2Back).setOnClickListener { showStep(1) }
        findViewById<Button>(R.id.btnStep2Next).setOnClickListener {
            if (selectedIds.isEmpty()) {
                toast("Please select at least one student")
                return@setOnClickListener
            }
            prepareStep3()
            showStep(3)
        }
    }

    private fun renderStudentChecks() {
        val q = findViewById<EditText>(R.id.editSearchStudent).text.toString().trim()
        val matched = matchedStudents()
        val shown = matched.filter {
            q.isEmpty() || it.name.contains(q, ignoreCase = true) ||
                it.rollNumber.contains(q, ignoreCase = true)
        }
        checkContainer.removeAllViews()
        if (matched.isEmpty()) {
            txtNoStudents.visibility = View.VISIBLE
            txtNoStudents.text =
                "No students found for $campus · $school · $className ($section).\nTap + Add Student to add them first."
        } else if (shown.isEmpty()) {
            txtNoStudents.visibility = View.VISIBLE
            txtNoStudents.text = "No students match your search."
        } else {
            txtNoStudents.visibility = View.GONE
        }
        for (s in shown) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(TeacherUi.dp(this, 4), TeacherUi.dp(this, 8), TeacherUi.dp(this, 4), TeacherUi.dp(this, 8))
            val cb = CheckBox(this)
            cb.isChecked = selectedIds.contains(s.id)
            cb.setOnCheckedChangeListener { _, checked ->
                if (checked) selectedIds.add(s.id) else selectedIds.remove(s.id)
                updateCount()
            }
            row.addView(cb)
            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = TeacherUi.dp(this, 6)
            col.layoutParams = lp
            col.addView(TeacherUi.text(this, s.name, 15f, "#1F2937", bold = true))
            col.addView(TeacherUi.text(this, "Roll No: ${s.rollNumber}", 12.5f, "#64748B"))
            row.addView(col)
            row.setOnClickListener { cb.isChecked = !cb.isChecked }
            checkContainer.addView(row)
            checkContainer.addView(TeacherUi.divider(this))
        }
        updateCount()
    }

    private fun updateCount() {
        txtSelectedCount.text = "${selectedIds.size} selected"
    }

    // ---------------- Step 3 ----------------

    private fun prepareStep3() {
        val editTitle = findViewById<EditText>(R.id.editTitle)
        if (!titleTouched && editTitle.text.toString().isBlank()) {
            editTitle.setText("$className ($section) Award List")
        }
        findViewById<TextView>(R.id.txtStudentsSummary).text =
            "Selected students: ${selectedIds.size} · $campus · $school · $className ($section)"
    }

    private fun bindStep3() {
        val editTitle = findViewById<EditText>(R.id.editTitle)
        editTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { titleTouched = true }
            override fun afterTextChanged(s: Editable?) {}
        })
        val pickSession = findViewById<TextView>(R.id.pickSession)
        val pickSubject = findViewById<TextView>(R.id.pickSubject)
        val pickExamType = findViewById<TextView>(R.id.pickExamType)
        val pickExamDate = findViewById<TextView>(R.id.pickExamDate)
        val pickReturnDate = findViewById<TextView>(R.id.pickReturnDate)

        pickSession.setOnClickListener {
            SearchablePicker.showSimple(
                this, "Academic Session",
                listOf("2024-25", "2025-26", "2026-27", "2027-28"), allowCustom = true
            ) { session = it; pickSession.text = "$it ▾" }
        }
        pickSubject.setOnClickListener {
            SearchablePicker.showSimple(this, "Subject", SUBJECT_OPTIONS, allowCustom = true) {
                subject = it; pickSubject.text = "$it ▾"
            }
        }
        pickExamType.setOnClickListener {
            SearchablePicker.show(
                this, "Exam Type",
                ExamTypes.ALL.map { it to ExamTypes.label(it) }, allowCustom = false
            ) {
                examType = it
                pickExamType.text = "${ExamTypes.label(it)} ▾"
            }
        }
        pickExamDate.setOnClickListener {
            pickDate(examDate) { examDate = it; pickExamDate.text = "$it ▾" }
        }
        pickReturnDate.setOnClickListener {
            pickDate(returnDate) { returnDate = it; pickReturnDate.text = "$it ▾" }
        }

        findViewById<Button>(R.id.btnStep3Back).setOnClickListener { showStep(2) }
        findViewById<Button>(R.id.btnSaveDraft).setOnClickListener { submit(AwardListData.STATUS_DRAFT) }
        findViewById<Button>(R.id.btnCreate).setOnClickListener { submit(AwardListData.STATUS_ACTIVE) }
    }

    private fun pickDate(current: String, onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        if (current.isNotEmpty()) {
            try { fmt.parse(current)?.let { cal.time = it } } catch (ignored: Exception) {}
        }
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d)
                onPicked(fmt.format(cal.time))
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submit(status: String) {
        val title = findViewById<EditText>(R.id.editTitle).text.toString().trim()
        when {
            title.isEmpty() -> { toast("Please enter the award list title"); return }
            subject.isEmpty() -> { toast("Please select the Subject"); return }
            examType.isEmpty() -> { toast("Please select the Exam Type"); return }
            examDate.isEmpty() -> { toast("Please select the Exam Date"); return }
        }
        val a = AwardListData(
            title = title,
            campus = campus,
            school = school,
            className = className,
            section = section,
            gender = gender,
            session = session,
            subject = subject,
            examType = examType,
            examDate = examDate,
            returnDate = returnDate,
            status = status,
            studentIds = selectedIds.toList()
        )
        val btnCreate = findViewById<Button>(R.id.btnCreate)
        btnCreate.isEnabled = false
        TeacherRepository.createAwardList(a,
            onOk = { newId ->
                if (status == AwardListData.STATUS_DRAFT) {
                    Toast.makeText(this, "Draft saved", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Award list created. Now enter marks.", Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(this, MarksEntryActivity::class.java)
                            .putExtra(MarksEntryActivity.EXTRA_AWARD_ID, newId)
                    )
                    finish()
                }
            },
            onErr = {
                btnCreate.isEnabled = true
                toast(it)
            })
    }

    private fun showStep(n: Int) {
        step1.visibility = if (n == 1) View.VISIBLE else View.GONE
        step2.visibility = if (n == 2) View.VISIBLE else View.GONE
        step3.visibility = if (n == 3) View.VISIBLE else View.GONE
        txtStepSub.text = when (n) {
            1 -> "Step 1 of 3 — School and Class Information"
            2 -> "Step 2 of 3 — Select Students"
            else -> "Step 3 of 3 — Award List Details"
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    companion object {
        val SUBJECT_OPTIONS = listOf(
            "English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology",
            "Computer Science", "Pakistan Studies", "Tarjuma-tul-Quran",
            "Islamiyat", "Arabic", "General Science", "General Math", "Social Studies"
        )
    }
}
