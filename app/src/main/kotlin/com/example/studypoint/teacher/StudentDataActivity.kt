package com.example.studypoint.teacher

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

/** Student Data: searchable, filterable list of the teacher's students. */
class StudentDataActivity : AppCompatActivity() {

    private var allStudents: List<TeacherStudent> = emptyList()
    private var filterCampus = "All"
    private var filterClass = "All"
    private var filterSection = "All"

    private lateinit var listContainer: LinearLayout
    private lateinit var txtEmpty: TextView
    private lateinit var progress: ProgressBar
    private lateinit var editSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_data)
        title = "Student Data"

        listContainer = findViewById(R.id.listContainer)
        txtEmpty = findViewById(R.id.txtEmpty)
        progress = findViewById(R.id.progress)
        editSearch = findViewById(R.id.editSearch)

        findViewById<Button>(R.id.btnAddStudent).setOnClickListener {
            startActivity(Intent(this, StudentEditActivity::class.java))
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = render()
            override fun afterTextChanged(s: Editable?) {}
        })

        val fCampus = findViewById<TextView>(R.id.filterCampus)
        val fClass = findViewById<TextView>(R.id.filterClass)
        val fSection = findViewById<TextView>(R.id.filterSection)

        fCampus.setOnClickListener {
            val opts = listOf("All") + allStudents.map { it.campus }.filter { it.isNotBlank() }.distinct().sorted()
            SearchablePicker.showSimple(this, "Campus", opts, false) {
                filterCampus = it
                fCampus.text = "Campus: $it ▾"
                render()
            }
        }
        fClass.setOnClickListener {
            val opts = listOf("All") + allStudents.map { it.className }.filter { it.isNotBlank() }.distinct().sorted()
            SearchablePicker.showSimple(this, "Class", opts, false) {
                filterClass = it
                fClass.text = "Class: $it ▾"
                render()
            }
        }
        fSection.setOnClickListener {
            val opts = listOf("All") + allStudents.map { it.section }.filter { it.isNotBlank() }.distinct().sorted()
            SearchablePicker.showSimple(this, "Section", opts, false) {
                filterSection = it
                fSection.text = "Section: $it ▾"
                render()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        progress.visibility = View.VISIBLE
        TeacherRepository.loadStudents(
            onOk = {
                progress.visibility = View.GONE
                allStudents = it
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
        val shown = allStudents.filter { s ->
            (filterCampus == "All" || s.campus == filterCampus) &&
                (filterClass == "All" || s.className == filterClass) &&
                (filterSection == "All" || s.section == filterSection) &&
                (q.isEmpty() || s.name.contains(q, ignoreCase = true) ||
                    s.rollNumber.contains(q, ignoreCase = true))
        }
        listContainer.removeAllViews()
        if (shown.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            txtEmpty.text = if (allStudents.isEmpty()) {
                "No students yet.\nTap + Add Student to add your first student."
            } else {
                "No students match your search or filters."
            }
            return
        }
        txtEmpty.visibility = View.GONE
        for (s in shown) {
            listContainer.addView(studentRow(s))
            listContainer.addView(TeacherUi.divider(this))
        }
    }

    private fun studentRow(s: TeacherStudent): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(
            TeacherUi.dp(this, 6), TeacherUi.dp(this, 10),
            TeacherUi.dp(this, 6), TeacherUi.dp(this, 10)
        )
        row.addView(TeacherUi.avatar(this, s.name))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.marginStart = TeacherUi.dp(this, 12)
        col.layoutParams = lp
        col.addView(TeacherUi.text(this, s.name, 16f, "#1F2937", bold = true))
        val sub = listOf(
            s.className.ifBlank { "—" },
            "Sec ${s.section.ifBlank { "—" }}",
            "Roll ${s.rollNumber.ifBlank { "—" }}"
        ).joinToString(" · ")
        col.addView(TeacherUi.text(this, sub, 13f, "#64748B"))
        val sub2 = listOf(s.campus, s.school).filter { it.isNotBlank() }.joinToString(" · ")
        if (sub2.isNotEmpty()) col.addView(TeacherUi.text(this, sub2, 12f, "#94A3B8"))
        row.addView(col)

        val chevron = TeacherUi.text(this, "›", 24f, "#94A3B8")
        row.addView(chevron)

        row.setOnClickListener {
            startActivity(
                Intent(this, StudentEditActivity::class.java)
                    .putExtra(StudentEditActivity.EXTRA_STUDENT_ID, s.id)
            )
        }
        return row
    }
}
