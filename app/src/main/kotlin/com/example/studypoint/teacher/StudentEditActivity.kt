package com.example.studypoint.teacher

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.R

/** Add / edit / delete one student record. */
class StudentEditActivity : AppCompatActivity() {

    private var studentId: String = ""
    private var allStudents: List<TeacherStudent> = emptyList()

    private var campus = ""
    private var school = ""
    private var className = ""
    private var section = ""
    private var gender = ""

    private lateinit var pickCampus: TextView
    private lateinit var pickSchool: TextView
    private lateinit var pickClass: TextView
    private lateinit var pickSection: TextView
    private lateinit var pickGender: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_edit)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        studentId = intent.getStringExtra(EXTRA_STUDENT_ID) ?: ""
        // Prefill values (used by the Award List wizard's "Add New Student").
        campus = intent.getStringExtra(EXTRA_CAMPUS) ?: ""
        school = intent.getStringExtra(EXTRA_SCHOOL) ?: ""
        className = intent.getStringExtra(EXTRA_CLASS) ?: ""
        section = intent.getStringExtra(EXTRA_SECTION) ?: ""
        gender = intent.getStringExtra(EXTRA_GENDER) ?: ""

        title = if (studentId.isEmpty()) "Add Student" else "Edit Student"

        pickCampus = findViewById(R.id.pickCampus)
        pickSchool = findViewById(R.id.pickSchool)
        pickClass = findViewById(R.id.pickClass)
        pickSection = findViewById(R.id.pickSection)
        pickGender = findViewById(R.id.pickGender)

        pickCampus.setOnClickListener {
            val opts = allStudents.map { it.campus }.filter { it.isNotBlank() }.distinct().sorted()
            SearchablePicker.showSimple(this, "Campus", opts, allowCustom = true) {
                campus = it; refreshPickers()
            }
        }
        pickSchool.setOnClickListener {
            val opts = allStudents.map { it.school }.filter { it.isNotBlank() }.distinct().sorted()
            SearchablePicker.showSimple(this, "School / College", opts, allowCustom = true) {
                school = it; refreshPickers()
            }
        }
        pickClass.setOnClickListener {
            SearchablePicker.showSimple(this, "Class", CLASS_OPTIONS, allowCustom = true) {
                className = it; refreshPickers()
            }
        }
        pickSection.setOnClickListener {
            SearchablePicker.showSimple(this, "Section", SECTION_OPTIONS, allowCustom = true) {
                section = it; refreshPickers()
            }
        }
        pickGender.setOnClickListener {
            SearchablePicker.showSimple(this, "Gender", GENDER_OPTIONS, allowCustom = false) {
                gender = it; refreshPickers()
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }

        val btnDelete = findViewById<Button>(R.id.btnDelete)
        if (studentId.isNotEmpty()) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete Student")
                    .setMessage("Delete this student record permanently?")
                    .setPositiveButton("Delete") { _, _ ->
                        TeacherRepository.deleteStudent(studentId,
                            onOk = {
                                existing?.let { SheetSync.studentDelete(it) }
                                Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show()
                                finish()
                            },
                            onErr = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() })
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        refreshPickers()
        TeacherRepository.loadStudents(
            onOk = { list ->
                allStudents = list
                if (studentId.isNotEmpty()) {
                    list.firstOrNull { it.id == studentId }?.let { fillForm(it) }
                }
            },
            onErr = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        )
    }

    private fun fillForm(s: TeacherStudent) {
        findViewById<EditText>(R.id.editName).setText(s.name)
        findViewById<EditText>(R.id.editRoll).setText(s.rollNumber)
        findViewById<EditText>(R.id.editGuardianName).setText(s.guardianName)
        findViewById<EditText>(R.id.editGuardianPhone).setText(s.guardianPhone)
        campus = s.campus
        school = s.school
        className = s.className
        section = s.section
        gender = s.gender
        refreshPickers()
    }

    private fun refreshPickers() {
        pickCampus.text = if (campus.isEmpty()) "Select Campus ▾" else "$campus ▾"
        pickSchool.text = if (school.isEmpty()) "Select School / College ▾" else "$school ▾"
        pickClass.text = if (className.isEmpty()) "Select Class ▾" else "$className ▾"
        pickSection.text = if (section.isEmpty()) "Select Section ▾" else "$section ▾"
        pickGender.text = if (gender.isEmpty()) "Select Gender ▾" else "$gender ▾"
    }

    private fun save() {
        val name = findViewById<EditText>(R.id.editName).text.toString().trim()
        val roll = findViewById<EditText>(R.id.editRoll).text.toString().trim()
        if (name.isEmpty()) {
            toast("Please enter the student name"); return
        }
        if (roll.isEmpty()) {
            toast("Please enter the roll number"); return
        }
        if (className.isEmpty()) {
            toast("Please select the class"); return
        }
        val s = TeacherStudent(
            id = studentId,
            name = name,
            rollNumber = roll,
            campus = campus,
            school = school,
            className = className,
            section = section,
            gender = gender,
            guardianName = findViewById<EditText>(R.id.editGuardianName).text.toString().trim(),
            guardianPhone = findViewById<EditText>(R.id.editGuardianPhone).text.toString().trim()
        )
        TeacherRepository.findDuplicate(s, allStudents) { dup ->
            if (dup != null) {
                toast("Duplicate: Roll $roll already exists for ${dup.name} in this class/section")
                return@findDuplicate
            }
            TeacherRepository.saveStudent(s,
                onOk = {
                    SheetSync.student(s)
                    Toast.makeText(this, "Student saved", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                },
                onErr = { toast(it) })
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    companion object {
        const val EXTRA_STUDENT_ID = "student_id"
        const val EXTRA_CAMPUS = "prefill_campus"
        const val EXTRA_SCHOOL = "prefill_school"
        const val EXTRA_CLASS = "prefill_class"
        const val EXTRA_SECTION = "prefill_section"
        const val EXTRA_GENDER = "prefill_gender"

        val CLASS_OPTIONS = listOf(
            "Nursery", "KG", "1st", "2nd", "3rd", "4th", "5th", "6th",
            "7th", "8th", "9th", "10th", "11th", "12th"
        )
        val SECTION_OPTIONS = listOf("A", "B", "C", "D", "E", "F")
        val GENDER_OPTIONS = listOf("Male", "Female")
    }
}
