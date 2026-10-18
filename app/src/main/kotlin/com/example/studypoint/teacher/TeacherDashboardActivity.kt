package com.example.studypoint.teacher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.ProfileActivity
import com.example.studypoint.R
import com.example.studypoint.RoleSelectionActivity
import com.example.studypoint.practicetest.PaperGeneratorActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Teacher Dashboard — exactly four primary cards:
 * Paper Generator, Student Data, Award List, Award List Create.
 */
class TeacherDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)
        title = "Teacher Dashboard"

        val user = FirebaseAuth.getInstance().currentUser
        val name = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Teacher"
        findViewById<TextView>(R.id.txtWelcome).text = "Welcome, $name! 👋"

        findViewById<LinearLayout>(R.id.cardPaper).setOnClickListener {
            startActivity(Intent(this, PaperGeneratorActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.cardStudents).setOnClickListener {
            startActivity(Intent(this, StudentDataActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.cardAwardView).setOnClickListener {
            startActivity(Intent(this, AwardListsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.cardAwardCreate).setOnClickListener {
            startActivity(Intent(this, CreateAwardListActivity::class.java))
        }
        findViewById<Button>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<TextView>(R.id.btnSwitchRole).setOnClickListener {
            startActivity(
                Intent(this, RoleSelectionActivity::class.java)
                    .putExtra(RoleSelectionActivity.EXTRA_CHANGE_MODE, true)
            )
        }
    }
}
