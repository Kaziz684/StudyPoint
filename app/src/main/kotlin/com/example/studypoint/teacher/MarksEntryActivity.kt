package com.example.studypoint.teacher

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.R

/**
 * Marks entry as a table: Sr. / Roll No. / Name / A-L chips / Obtained.
 * A = Absent, L = Leave (no marks needed). One shared Total for the list
 * (in the table header). The blue bar's Update button saves the marks.
 * Saved marks reload when the list is reopened.
 */
class MarksEntryActivity : AppCompatActivity() {

    private var award: AwardListData? = null
    private var studentsById: Map<String, TeacherStudent> = emptyMap()

    /** Working copy of the marks being edited: studentId -> (text, status). */
    private val draft = mutableMapOf<String, Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marks_entry)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        title = "Enter Marks"

        val awardId = intent.getStringExtra(EXTRA_AWARD_ID) ?: ""
        if (awardId.isEmpty()) {
            toast("Award list not found")
            finish()
            return
        }
        val progress = findViewById<ProgressBar>(R.id.progress)
        progress.visibility = View.VISIBLE

        TeacherRepository.loadStudents(
            onOk = { students ->
                studentsById = students.associateBy { it.id }
                TeacherRepository.loadAwardList(awardId,
                    onOk = { a ->
                        progress.visibility = View.GONE
                        if (a == null) {
                            toast("Award list not found")
                            finish()
                        } else {
                            award = a
                            initDraft()
                            bindHeader()
                            renderRows()
                        }
                    },
                    onErr = {
                        progress.visibility = View.GONE
                        toast(it)
                    })
            },
            onErr = {
                progress.visibility = View.GONE
                toast(it)
            }
        )

        findViewById<TextView>(R.id.btnUpdate).setOnClickListener { save() }
        findViewById<Button>(R.id.btnSaveMarks).setOnClickListener { save() }
        findViewById<Button>(R.id.btnExportPdf).setOnClickListener { export(asPdf = true) }
        findViewById<Button>(R.id.btnExportDocx).setOnClickListener { export(asPdf = false) }
        findViewById<TextView>(R.id.btnFilter).setOnClickListener {
            val f = findViewById<EditText>(R.id.editFilter)
            f.visibility = if (f.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (f.visibility == View.VISIBLE) f.requestFocus()
        }
        findViewById<EditText>(R.id.editFilter).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = renderRows()
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun initDraft() {
        val a = award ?: return
        draft.clear()
        for (sid in a.studentIds) {
            val m = a.marks[sid]
            draft[sid] = (m?.obtained?.let { TeacherUi.fmtMarks(it) } ?: "") to (m?.status ?: "")
        }
        val totals = a.marks.values.mapNotNull { it.total }.distinct()
        if (totals.size == 1) {
            findViewById<EditText>(R.id.editTotal).setText(TeacherUi.fmtMarks(totals[0]))
        }
    }

    private fun bindHeader() {
        val a = award ?: return
        val code = a.listCode.ifEmpty { a.id.take(6).uppercase() }
        findViewById<TextView>(R.id.txtBarTitle).text = "Exam Name: ${ExamTypes.label(a.examType)}"
        findViewById<TextView>(R.id.txtBarSub).text = "Exam Date: ${a.examDate}"
        findViewById<TextView>(R.id.txtInfoClass).text = "Class: ${a.className} (${a.section})"
        findViewById<TextView>(R.id.txtInfoId).text = "Award List id: $code"
        findViewById<TextView>(R.id.txtInfoSubject).text = "Subject(s): ${a.subject}"
    }

    private fun orderedStudents(): List<Pair<String, TeacherStudent>> {
        val a = award ?: return emptyList()
        return a.studentIds
            .mapNotNull { sid -> studentsById[sid]?.let { sid to it } }
            .sortedWith(compareBy({ it.second.rollNumber }, { it.second.name }))
    }

    private fun chip(text: String, selected: Boolean, colorHex: String): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = 13f
        t.gravity = Gravity.CENTER
        t.setTypeface(null, android.graphics.Typeface.BOLD)
        val size = TeacherUi.dp(this, 28)
        t.layoutParams = LinearLayout.LayoutParams(size, size)
        val bg = GradientDrawable()
        bg.cornerRadius = TeacherUi.dp(this, 5).toFloat()
        if (selected) {
            bg.setColor(Color.parseColor(colorHex))
            t.setTextColor(Color.WHITE)
        } else {
            bg.setColor(Color.WHITE)
            bg.setStroke(TeacherUi.dp(this, 1), Color.parseColor("#9CA3AF"))
            t.setTextColor(Color.parseColor("#374151"))
        }
        t.background = bg
        return t
    }

    private fun renderRows() {
        val container = findViewById<LinearLayout>(R.id.rowsContainer)
        container.removeAllViews()
        val q = findViewById<EditText>(R.id.editFilter).text.toString().trim()
        val ordered = orderedStudents().filter {
            q.isEmpty() || it.second.name.contains(q, ignoreCase = true) ||
                it.second.rollNumber.contains(q, ignoreCase = true)
        }
        if (ordered.isEmpty()) {
            val t = TeacherUi.text(this, "No students in this award list.", 14f, "#64748B")
            t.setPadding(TeacherUi.dp(this, 14), TeacherUi.dp(this, 16), TeacherUi.dp(this, 14), 0)
            container.addView(t)
            return
        }
        ordered.forEachIndexed { idx, (sid, s) ->
            val (marksText, status) = draft[sid] ?: ("" to "")
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setBackgroundColor(Color.parseColor(if (idx % 2 == 0) "#FFFFFF" else "#F3F6FA"))
            row.setPadding(TeacherUi.dp(this, 10), TeacherUi.dp(this, 7), TeacherUi.dp(this, 10), TeacherUi.dp(this, 7))

            val sr = TeacherUi.text(this, "${idx + 1}", 13.5f, "#1F2937", bold = true)
            sr.layoutParams = LinearLayout.LayoutParams(TeacherUi.dp(this, 30), LinearLayout.LayoutParams.WRAP_CONTENT)
            row.addView(sr)

            val roll = TeacherUi.text(this, s.rollNumber, 13.5f, "#1F2937", bold = true)
            roll.layoutParams = LinearLayout.LayoutParams(TeacherUi.dp(this, 56), LinearLayout.LayoutParams.WRAP_CONTENT)
            row.addView(roll)

            val name = TeacherUi.text(this, s.name, 14f, "#111827", bold = true)
            name.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            row.addView(name)

            // A / L chips
            val chips = LinearLayout(this)
            chips.orientation = LinearLayout.HORIZONTAL
            chips.layoutParams = LinearLayout.LayoutParams(TeacherUi.dp(this, 62), LinearLayout.LayoutParams.WRAP_CONTENT)
            val chipA = chip("A", status == "A", "#DC2626")
            val chipL = chip("L", status == "L", "#D97706")
            val lLp = LinearLayout.LayoutParams(TeacherUi.dp(this, 28), TeacherUi.dp(this, 28))
            lLp.marginStart = TeacherUi.dp(this, 6)
            chipL.layoutParams = lLp
            chips.addView(chipA)
            chips.addView(chipL)
            row.addView(chips)

            // Marks field
            val marks = EditText(this)
            marks.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            marks.gravity = Gravity.CENTER
            marks.textSize = 14f
            marks.setTextColor(Color.parseColor("#1F2937"))
            marks.setHintTextColor(Color.parseColor("#9CA3AF"))
            marks.hint = "—"
            marks.setBackgroundResource(R.drawable.bg_input)
            val mLp = LinearLayout.LayoutParams(TeacherUi.dp(this, 86), LinearLayout.LayoutParams.WRAP_CONTENT)
            marks.layoutParams = mLp
            if (status.isNotEmpty()) {
                marks.setText(status)
                marks.isEnabled = false
            } else {
                marks.setText(marksText)
                marks.isEnabled = true
            }
            row.addView(marks)

            chipA.setOnClickListener {
                val cur = draft[sid]?.second ?: ""
                val newStatus = if (cur == "A") "" else "A"
                draft[sid] = (draft[sid]?.first ?: "") to newStatus
                renderRows()
            }
            chipL.setOnClickListener {
                val cur = draft[sid]?.second ?: ""
                val newStatus = if (cur == "L") "" else "L"
                draft[sid] = (draft[sid]?.first ?: "") to newStatus
                renderRows()
            }
            marks.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s2: CharSequence?, a2: Int, b: Int, c: Int) {}
                override fun onTextChanged(s2: CharSequence?, a2: Int, b: Int, c: Int) {
                    val cur = draft[sid]
                    if (cur != null && cur.second.isEmpty()) {
                        draft[sid] = (s2?.toString() ?: "") to ""
                    }
                }
                override fun afterTextChanged(s2: Editable?) {}
            })

            container.addView(row)
            container.addView(TeacherUi.divider(this))
        }
    }

    /** Validates the draft and applies it to the award list. */
    private fun collectMarks(): Boolean {
        val a = award ?: return false
        val totalTxt = findViewById<EditText>(R.id.editTotal).text.toString().trim()
        val sharedTotal = totalTxt.toDoubleOrNull()
        val newMarks = mutableMapOf<String, MarkEntry>()
        for ((sid, pair) in draft) {
            val (text, status) = pair
            val name = studentsById[sid]?.name ?: sid
            val oldRemarks = a.marks[sid]?.remarks ?: ""
            if (status.isNotEmpty()) {
                newMarks[sid] = MarkEntry(
                    obtained = null, total = sharedTotal, remarks = oldRemarks, status = status
                )
                continue
            }
            if (text.isBlank()) continue
            val obtained = text.toDoubleOrNull()
            if (obtained == null) {
                toast("Invalid marks for $name"); return false
            }
            if (obtained < 0) {
                toast("Marks cannot be negative ($name)"); return false
            }
            if (sharedTotal == null || sharedTotal <= 0) {
                toast("Please enter the Total marks in the table header"); return false
            }
            if (obtained > sharedTotal) {
                toast("Obtained marks cannot exceed total marks ($name)"); return false
            }
            newMarks[sid] = MarkEntry(
                obtained = obtained, total = sharedTotal, remarks = oldRemarks, status = ""
            )
        }
        a.marks = newMarks
        if (a.status == AwardListData.STATUS_DRAFT) a.status = AwardListData.STATUS_ACTIVE
        return true
    }

    private fun persist(onDone: () -> Unit) {
        val a = award ?: return
        val btn = findViewById<Button>(R.id.btnSaveMarks)
        btn.isEnabled = false
        TeacherRepository.updateAwardList(a,
            onOk = {
                btn.isEnabled = true
                onDone()
            },
            onErr = {
                btn.isEnabled = true
                toast(it)
            })
    }

    private fun save() {
        if (!collectMarks()) return
        persist {
            Toast.makeText(this, "Marks saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /** Saves the current marks first, then generates and opens the file. */
    private fun export(asPdf: Boolean) {
        val a = award ?: return
        if (!collectMarks()) return
        toast(if (asPdf) "Preparing PDF..." else "Preparing Word file...")
        persist {
            Thread {
                try {
                    val file = if (asPdf) {
                        AwardListExporter.exportPdf(this, a, studentsById)
                    } else {
                        AwardListExporter.exportDocx(this, a, studentsById)
                    }
                    runOnUiThread { AwardListExporter.openFile(this, file) }
                } catch (e: Exception) {
                    runOnUiThread { toast("Export failed: ${e.message ?: "unknown error"}") }
                }
            }.start()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    companion object {
        const val EXTRA_AWARD_ID = "award_id"
    }
}
