package com.example.studypoint.teacher

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.R

/** Award List screen: all saved award lists with search and campus filter. */
class AwardListsActivity : AppCompatActivity() {

    private var allLists: List<AwardListData> = emptyList()
    private var filterCampus = "All"

    private lateinit var listContainer: LinearLayout
    private lateinit var txtEmpty: TextView
    private lateinit var progress: ProgressBar
    private lateinit var editSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_award_lists)
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

        val fCampus = findViewById<TextView>(R.id.filterCampus)
        fCampus.setOnClickListener {
            val opts = listOf("All") + allLists.map { it.campus }.filter { it.isNotBlank() }.distinct().sorted()
            SearchablePicker.showSimple(this, "Campus", opts, false) {
                filterCampus = it
                fCampus.text = "Campus: $it ▾"
                render()
            }
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

    private fun render() {
        val q = editSearch.text.toString().trim()
        val shown = allLists.filter { a ->
            (filterCampus == "All" || a.campus == filterCampus) &&
                (q.isEmpty() ||
                    a.title.contains(q, ignoreCase = true) ||
                    a.id.contains(q, ignoreCase = true) ||
                    a.className.contains(q, ignoreCase = true) ||
                    a.subject.contains(q, ignoreCase = true) ||
                    a.examType.contains(q, ignoreCase = true) ||
                    ExamTypes.label(a.examType).contains(q, ignoreCase = true))
        }
        listContainer.removeAllViews()
        if (shown.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            txtEmpty.text = if (allLists.isEmpty()) {
                "No award lists yet.\nCreate one from the Teacher Dashboard → Award List Create."
            } else {
                "No award lists match your search or filter."
            }
            return
        }
        txtEmpty.visibility = View.GONE
        for (a in shown) {
            listContainer.addView(awardCard(a))
        }
    }

    private fun awardCard(a: AwardListData): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundResource(R.drawable.bg_role_card)
        card.setPadding(
            TeacherUi.dp(this, 14), TeacherUi.dp(this, 12),
            TeacherUi.dp(this, 14), TeacherUi.dp(this, 12)
        )
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = TeacherUi.dp(this, 12)
        card.layoutParams = lp

        card.addView(TeacherUi.text(this, a.title, 16f, "#1F2937", bold = true))
        card.addView(TeacherUi.text(this, "ID: ${a.id}", 11f, "#94A3B8"))
        card.addView(TeacherUi.text(this, "${a.campus} · ${a.school}", 13f, "#64748B"))
        card.addView(
            TeacherUi.text(
                this,
                "${a.className} (${a.section}) · ${a.subject} · ${a.session}",
                13f, "#64748B"
            )
        )
        card.addView(
            TeacherUi.text(
                this,
                "${ExamTypes.label(a.examType)} · Exam: ${a.examDate}" +
                    if (a.returnDate.isNotEmpty()) " · Return: ${a.returnDate}" else "",
                13f, "#64748B"
            )
        )
        val entered = a.studentIds.count { a.marks.containsKey(it) }
        val statusText = if (a.status == AwardListData.STATUS_DRAFT) {
            "Status: DRAFT · Students: ${a.studentIds.size}"
        } else {
            "Students: ${a.studentIds.size} · Marks entered: $entered/${a.studentIds.size}"
        }
        val statusView = TeacherUi.text(this, statusText, 13f, "#2563EB", bold = true)
        val slp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        slp.topMargin = TeacherUi.dp(this, 6)
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
                .setMessage("Delete \"${a.title}\" and its marks permanently?")
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
