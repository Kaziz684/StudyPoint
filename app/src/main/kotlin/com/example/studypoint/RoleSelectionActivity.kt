package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.teacher.TeacherDashboardActivity

/**
 * "I am a..." role selection. Shown after login when the authenticated
 * user has not chosen a role yet, and from Profile in change mode.
 */
class RoleSelectionActivity : AppCompatActivity() {

    private var selectedRole: String? = null
    private var changeMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)
        changeMode = intent.getBooleanExtra(EXTRA_CHANGE_MODE, false)

        val cardStudent = findViewById<LinearLayout>(R.id.cardStudent)
        val cardTeacher = findViewById<LinearLayout>(R.id.cardTeacher)
        val checkStudent = findViewById<TextView>(R.id.checkStudent)
        val checkTeacher = findViewById<TextView>(R.id.checkTeacher)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        fun refresh() {
            cardStudent.setBackgroundResource(
                if (selectedRole == RoleManager.ROLE_STUDENT) R.drawable.bg_role_card_selected else R.drawable.bg_role_card
            )
            cardTeacher.setBackgroundResource(
                if (selectedRole == RoleManager.ROLE_TEACHER) R.drawable.bg_role_card_selected else R.drawable.bg_role_card
            )
            checkStudent.visibility =
                if (selectedRole == RoleManager.ROLE_STUDENT) View.VISIBLE else View.INVISIBLE
            checkTeacher.visibility =
                if (selectedRole == RoleManager.ROLE_TEACHER) View.VISIBLE else View.INVISIBLE
            btnContinue.isEnabled = selectedRole != null
            btnContinue.alpha = if (selectedRole != null) 1f else 0.45f
        }

        // In change mode, pre-select the current role.
        if (changeMode) {
            selectedRole = RoleManager.cachedRole(this)
        }
        refresh()

        cardStudent.setOnClickListener {
            selectedRole = RoleManager.ROLE_STUDENT
            refresh()
        }
        cardTeacher.setOnClickListener {
            selectedRole = RoleManager.ROLE_TEACHER
            refresh()
        }

        btnContinue.setOnClickListener {
            val role = selectedRole ?: return@setOnClickListener
            btnContinue.isEnabled = false
            btnContinue.text = "Saving..."
            RoleManager.saveRole(this, role) { savedOnline ->
                if (!savedOnline) {
                    Toast.makeText(
                        this,
                        "Role saved on this device. It will sync when you are online.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                routeToDashboard(role)
            }
        }
    }

    private fun routeToDashboard(role: String) {
        val target = if (role == RoleManager.ROLE_TEACHER) {
            TeacherDashboardActivity::class.java
        } else {
            HomeActivity::class.java
        }
        startActivity(
            Intent(this, target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_CHANGE_MODE = "change_mode"
    }
}
