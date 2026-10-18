package com.example.studypoint.teacher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Read-only RESULT view of one award list: header info, the full marks
 * table (roll order, obtained/total and percentage) and download buttons.
 * This is what the dashboard's "Award List — View Results" card promises.
 * Marks editing stays in MarksEntryActivity (button below).
 */
class AwardResultActivity : AppCompatActivity() {

    private var award: AwardListData? = null
    private var studentsById: Map<String, TeacherStudent> = emptyMap()

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Award List Result"
        buildUi()

        val awardId = intent.getStringExtra(EXTRA_AWARD_ID) ?: ""
        if (awardId.isEmpty()) {
            toast("Award list not found")
            finish()
            return
        }
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
    }

    private fun buildUi() {
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        // Blue top bar with back arrow + title
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setBackgroundColor(Color.parseColor("#3E8EBC"))
        bar.setPadding(TeacherUi.dp(this, 6), TeacherUi.dp(this, 4), TeacherUi.dp(this, 6), TeacherUi.dp(this, 8))
        val back = TextView(this)
        back.text = "←"
        back.textSize = 26f
        back.setTypeface(null, android.graphics.Typeface.BOLD)
        back.setTextColor(Color.WHITE)
        back.setPadding(TeacherUi.dp(this, 6), TeacherUi.dp(this, 6), TeacherUi.dp(this, 6), TeacherUi.dp(this, 6))
        back.setOnClickListener { finish() }
        bar.addView(back)
        val titleTv = TeacherUi.text(this, "Award List Result", 20f, "#FFFFFF", bold = true)
        val tLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        tLp.gravity = Gravity.CENTER_VERTICAL
        tLp.marginStart = TeacherUi.dp(this, 6)
        titleTv.layoutParams = tLp
        bar.addView(titleTv)
        root.addView(bar)

        progress = ProgressBar(this)
        val pLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        pLp.gravity = Gravity.CENTER_HORIZONTAL
        pLp.topMargin = TeacherUi.dp(this, 16)
        progress.layoutParams = pLp
        progress.visibility = View.GONE
        root.addView(progress)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(
            TeacherUi.dp(this, 14), TeacherUi.dp(this, 12),
            TeacherUi.dp(this, 14), TeacherUi.dp(this, 16)
        )
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun infoLine(label: String, value: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val lab = TeacherUi.text(this, label, 14.5f, "#111827", bold = true)
        lab.layoutParams = LinearLayout.LayoutParams(
            TeacherUi.dp(this, 104), LinearLayout.LayoutParams.WRAP_CONTENT
        )
        row.addView(lab)
        row.addView(TeacherUi.text(this, value, 14.5f, "#4B5563"))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = TeacherUi.dp(this, 3)
        row.layoutParams = lp
        content.addView(row)
    }

    private fun actionButton(label: String, bg: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.textSize = 13.5f
        b.setTextColor(Color.WHITE)
        b.setBackgroundColor(Color.parseColor(bg))
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.marginStart = TeacherUi.dp(this, 4)
        lp.marginEnd = TeacherUi.dp(this, 4)
        b.layoutParams = lp
        b.setOnClickListener { onClick() }
        return b
    }

    private fun render() {
        val a = award ?: return
        content.removeAllViews()

        val code = a.listCode.ifEmpty { a.id.take(6).uppercase() }
        infoLine("Award List ID:", code)
        infoLine("Class:", "${a.className} (${a.section})")
        if (a.campus.isNotBlank()) infoLine("Campus:", a.campus)
        infoLine("Subject(s):", a.subject)
        infoLine("Exam type:", ExamTypes.labelList(a.examType))
        infoLine("Exam date:", a.examDate)

        val rows = AwardListExporter.buildRows(a, studentsById)
        val entered = rows.count { it.obtained != null }
        val summary = TeacherUi.text(
            this,
            "Students: ${rows.size} · Marks entered: $entered/${rows.size}",
            13f, "#2563EB", bold = true
        )
        val sLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        sLp.topMargin = TeacherUi.dp(this, 10)
        sLp.bottomMargin = TeacherUi.dp(this, 10)
        summary.layoutParams = sLp
        content.addView(summary)

        // Action buttons
        val btnRow = LinearLayout(this)
        btnRow.orientation = LinearLayout.HORIZONTAL
        btnRow.addView(actionButton("Enter / Edit Marks", "#2563EB") {
            startActivity(
                Intent(this, MarksEntryActivity::class.java)
                    .putExtra(MarksEntryActivity.EXTRA_AWARD_ID, a.id)
            )
        })
        btnRow.addView(actionButton("PDF", "#059669") { export(asPdf = true) })
        btnRow.addView(actionButton("Word", "#7C3AED") { export(asPdf = false) })
        content.addView(btnRow)

        content.addView(TeacherUi.divider(this))

        if (rows.isEmpty()) {
            val t = TeacherUi.text(this, "No students in this award list.", 14f, "#64748B")
            t.setPadding(0, TeacherUi.dp(this, 16), 0, 0)
            content.addView(t)
            return
        }

        // Table header
        content.addView(tableRow("Sr.", "Roll", "Name", "Marks", "%", header = true))
        rows.forEachIndexed { idx, r ->
            val marksText = if (r.status.isNotEmpty()) {
                r.obtainedText()
            } else if (r.obtained != null) {
                "${TeacherUi.fmtMarks(r.obtained)}/${TeacherUi.fmtMarks(r.total)}"
            } else {
                "—"
            }
            content.addView(
                tableRow(
                    "${idx + 1}", r.roll, r.name, marksText,
                    if (r.status.isNotEmpty()) "—" else r.percent(),
                    header = false, alt = idx % 2 == 1
                )
            )
            content.addView(TeacherUi.divider(this))
        }
    }

    private fun tableRow(
        sr: String, roll: String, name: String, marks: String, pct: String,
        header: Boolean, alt: Boolean = false
    ): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setBackgroundColor(
            Color.parseColor(
                when {
                    header -> "#E8EEF5"
                    alt -> "#F3F6FA"
                    else -> "#FFFFFF"
                }
            )
        )
        row.setPadding(
            TeacherUi.dp(this, 8), TeacherUi.dp(this, 7),
            TeacherUi.dp(this, 8), TeacherUi.dp(this, 7)
        )
        val size = if (header) 13f else 13.5f
        fun cell(text: String, w: Int, weight: Float = 0f, bold: Boolean = false): TextView {
            val t = TeacherUi.text(this, text, size, "#1F2937", bold = bold || header)
            t.layoutParams = if (weight > 0f) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            } else {
                LinearLayout.LayoutParams(
                    TeacherUi.dp(this, w),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            return t
        }
        row.addView(cell(sr, 30))
        row.addView(cell(roll, 52))
        row.addView(cell(name, 0, weight = 1f, bold = !header))
        val m = cell(marks, 74)
        m.gravity = Gravity.CENTER
        row.addView(m)
        val p = cell(pct, 52)
        p.gravity = Gravity.CENTER
        row.addView(p)
        return row
    }

    /** Generates and opens the PDF / Word file of the SAVED marks. */
    private fun export(asPdf: Boolean) {
        val a = award ?: return
        toast(if (asPdf) "Preparing PDF..." else "Preparing Word file...")
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

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    companion object {
        const val EXTRA_AWARD_ID = "award_id"
    }
}
