package com.example.studypoint.teacher

import android.os.Bundle
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
 * Enter / edit marks for every student of one award list.
 * Validation: marks cannot be negative and cannot exceed total marks.
 * Previously saved marks are loaded back when the list is reopened.
 */
class MarksEntryActivity : AppCompatActivity() {

    private var award: AwardListData? = null
    private var studentsById: Map<String, TeacherStudent> = emptyMap()
    private val rowFields = mutableListOf<RowFields>()

    private data class RowFields(
        val studentId: String,
        val obtained: EditText,
        val total: EditText,
        val remarks: EditText
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marks_entry)
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
                            render()
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

        findViewById<Button>(R.id.btnExportPdf).setOnClickListener { export(asPdf = true) }
        findViewById<Button>(R.id.btnExportDocx).setOnClickListener { export(asPdf = false) }
        findViewById<Button>(R.id.btnApplyTotal).setOnClickListener {
            val v = findViewById<EditText>(R.id.editDefaultTotal).text.toString().trim()
            if (v.isEmpty()) return@setOnClickListener
            for (r in rowFields) r.total.setText(v)
        }
        findViewById<Button>(R.id.btnSaveMarks).setOnClickListener { save() }
    }

    private fun render() {
        val a = award ?: return
        findViewById<TextView>(R.id.txtHeader).text = a.title
        findViewById<TextView>(R.id.txtSub).text =
            "${ExamTypes.label(a.examType)} · ${a.subject} · ${a.className} (${a.section}) · ${a.examDate}"

        val container = findViewById<LinearLayout>(R.id.rowsContainer)
        container.removeAllViews()
        rowFields.clear()

        val ordered = a.studentIds
            .mapNotNull { sid -> studentsById[sid]?.let { sid to it } }
            .sortedWith(compareBy({ it.second.rollNumber }, { it.second.name }))

        if (ordered.isEmpty()) {
            container.addView(
                TeacherUi.text(this, "No students in this award list.", 14f, "#64748B")
            )
            return
        }
        // Prefill the shared total box when all saved totals agree.
        val totals = a.marks.values.mapNotNull { it.total }.distinct()
        if (totals.size == 1) {
            findViewById<EditText>(R.id.editDefaultTotal).setText(TeacherUi.fmtMarks(totals[0]))
        }

        for ((sid, s) in ordered) {
            val existing = a.marks[sid]
            val block = LinearLayout(this)
            block.orientation = LinearLayout.VERTICAL
            block.setPadding(0, TeacherUi.dp(this, 10), 0, TeacherUi.dp(this, 4))

            val nameRow = LinearLayout(this)
            nameRow.orientation = LinearLayout.HORIZONTAL
            nameRow.addView(TeacherUi.avatar(this, s.name))
            val nameCol = LinearLayout(this)
            nameCol.orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = TeacherUi.dp(this, 10)
            nameCol.layoutParams = lp
            nameCol.addView(TeacherUi.text(this, s.name, 15.5f, "#1F2937", bold = true))
            nameCol.addView(TeacherUi.text(this, "Roll No: ${s.rollNumber}", 12.5f, "#64748B"))
            nameRow.addView(nameCol)
            block.addView(nameRow)

            val marksRow = LinearLayout(this)
            marksRow.orientation = LinearLayout.HORIZONTAL
            marksRow.setPadding(0, TeacherUi.dp(this, 8), 0, 0)
            val obtained = markField("Obtained", TeacherUi.fmtMarks(existing?.obtained))
            val total = markField("Total", TeacherUi.fmtMarks(existing?.total))
            marksRow.addView(obtained)
            marksRow.addView(total)
            block.addView(marksRow)

            val remarks = EditText(this)
            remarks.hint = "Remarks (optional)"
            remarks.setText(existing?.remarks ?: "")
            remarks.setTextColor(android.graphics.Color.parseColor("#1F2937"))
            remarks.setHintTextColor(android.graphics.Color.parseColor("#94A3B8"))
            remarks.textSize = 13.5f
            remarks.setBackgroundResource(R.drawable.bg_input)
            val rlp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            rlp.topMargin = TeacherUi.dp(this, 6)
            remarks.layoutParams = rlp
            block.addView(remarks)

            container.addView(block)
            container.addView(TeacherUi.divider(this))
            rowFields.add(RowFields(sid, obtained, total, remarks))
        }
    }

    private fun markField(hint: String, value: String): EditText {
        val e = EditText(this)
        e.hint = hint
        e.setText(value)
        e.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        e.setTextColor(android.graphics.Color.parseColor("#1F2937"))
        e.setHintTextColor(android.graphics.Color.parseColor("#94A3B8"))
        e.textSize = 14f
        e.setBackgroundResource(R.drawable.bg_input)
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.marginEnd = TeacherUi.dp(this, 8)
        e.layoutParams = lp
        return e
    }

    /** Validates the on-screen marks and applies them to the award list. */
    private fun collectMarks(): Boolean {
        val a = award ?: return false
        val newMarks = a.marks.toMutableMap()
        for (r in rowFields) {
            val name = studentsById[r.studentId]?.name ?: r.studentId
            val obtainedTxt = r.obtained.text.toString().trim()
            val totalTxt = r.total.text.toString().trim()
            if (obtainedTxt.isEmpty()) {
                // Cleared field = remove the saved entry for this student.
                newMarks.remove(r.studentId)
                continue
            }
            val obtained = obtainedTxt.toDoubleOrNull()
            if (obtained == null) {
                toast("Invalid marks for $name"); return false
            }
            if (obtained < 0) {
                toast("Marks cannot be negative ($name)"); return false
            }
            val total = totalTxt.toDoubleOrNull()
            if (total == null || total <= 0) {
                toast("Please enter total marks for $name"); return false
            }
            if (obtained > total) {
                toast("Obtained marks cannot exceed total marks ($name)"); return false
            }
            newMarks[r.studentId] = MarkEntry(
                obtained = obtained,
                total = total,
                remarks = r.remarks.text.toString().trim()
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
