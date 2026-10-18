package com.example.studypoint.teacher

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
 * Award List screen: saved award lists as green label/value cards
 * (Award List ID, Class, Campus, Subject, Exam type, dates),
 * with search and a round "create" button.
 */
class AwardListsActivity : AppCompatActivity() {

    private var allLists: List<AwardListData> = emptyList()

    private lateinit var listContainer: LinearLayout
    private lateinit var txtEmpty: TextView
    private lateinit var progress: ProgressBar
    private lateinit var editSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_award_lists)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        title = "Award List"

        listContainer = findViewById(R.id.listContainer)
        txtEmpty = findViewById(R.id.txtEmpty)
        progress = findViewById(R.id.progress)
        editSearch = findViewById(R.id.editSearch)

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = render()
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<Button>(R.id.fabCreate).setOnClickListener {
            startActivity(Intent(this, CreateAwardListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        progress.visibility = View.VISIBLE
        TeacherRepository.loadAwardLists(
            onOk = {
                progress.visibility = View.GONE
                allLists = it
                render()
            },
            onErr = {
                progress.visibility = View.GONE
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                render()
            }
        )
    }

    private fun displayCode(a: AwardListData): String =
        a.listCode.ifEmpty { a.id.take(6).uppercase() }

    private fun render() {
        val q = editSearch.text.toString().trim()
        val shown = allLists.filter { a ->
            q.isEmpty() ||
                a.title.contains(q, ignoreCase = true) ||
                displayCode(a).contains(q, ignoreCase = true) ||
                a.id.contains(q, ignoreCase = true) ||
                a.className.contains(q, ignoreCase = true) ||
                a.subject.contains(q, ignoreCase = true) ||
                a.examType.contains(q, ignoreCase = true) ||
                ExamTypes.label(a.examType).contains(q, ignoreCase = true)
        }
        listContainer.removeAllViews()
        if (shown.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            txtEmpty.text = if (allLists.isEmpty()) {
                "No award lists yet.\nTap the round create button to make your first award list."
            } else {
                "No award lists match your search."
            }
            return
        }
        txtEmpty.visibility = View.GONE
        for (a in shown) {
            listContainer.addView(awardCard(a))
        }
    }

    private fun labelRow(label: String, value: String): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = TeacherUi.dp(this, 3)
        row.layoutParams = lp
        val lab = TeacherUi.text(this, label, 14.5f, "#111827", bold = true)
        val labLp = LinearLayout.LayoutParams(TeacherUi.dp(this, 104), LinearLayout.LayoutParams.WRAP_CONTENT)
        lab.layoutParams = labLp
        row.addView(lab)
        row.addView(TeacherUi.text(this, value, 14.5f, "#4B5563"))
        return row
    }

    private fun awardCard(a: AwardListData): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundResource(R.drawable.bg_award_card)
        card.elevation = TeacherUi.dp(this, 3).toFloat()
        card.setPadding(
            TeacherUi.dp(this, 16), TeacherUi.dp(this, 14),
            TeacherUi.dp(this, 16), TeacherUi.dp(this, 14)
        )
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = TeacherUi.dp(this, 14)
        card.layoutParams = lp

        card.addView(labelRow("Award List ID:", displayCode(a)))
        card.addView(labelRow("Class:", "${a.className} (${a.section})"))
        card.addView(labelRow("Campus:", a.campus))
        if (a.school.isNotBlank()) card.addView(labelRow("School:", a.school))
        card.addView(labelRow("Subject(s):", a.subject))
        card.addView(labelRow("Exam type:", ExamTypes.label(a.examType)))
        card.addView(labelRow("Exam date:", a.examDate))
        if (a.returnDate.isNotBlank()) card.addView(labelRow("Return Date:", a.returnDate))

        val entered = a.studentIds.count { a.marks.containsKey(it) }
        val statusText = if (a.status == AwardListData.STATUS_DRAFT) {
            "DRAFT · Students: ${a.studentIds.size}"
        } else {
            "Students: ${a.studentIds.size} · Marks entered: $entered/${a.studentIds.size}"
        }
        val statusView = TeacherUi.text(this, statusText, 12.5f, "#2563EB", bold = true)
        val slp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        slp.topMargin = TeacherUi.dp(this, 8)
        statusView.layoutParams = slp
        card.addView(statusView)

        card.setOnClickListener {
            startActivity(
                Intent(this, MarksEntryActivity::class.java)
                    .putExtra(MarksEntryActivity.EXTRA_AWARD_ID, a.id)
            )
        }
        card.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Award List")
                .setMessage("Delete award list ${displayCode(a)} and its marks permanently?")
                .setPositiveButton("Delete") { _, _ ->
                    TeacherRepository.deleteAwardList(a.id,
                        onOk = {
                            Toast.makeText(this, "Award list deleted", Toast.LENGTH_SHORT).show()
                            allLists = allLists.filter { it.id != a.id }
                            render()
                        },
                        onErr = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() })
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
        return card
    }
}
