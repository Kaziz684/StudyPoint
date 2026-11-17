package com.example.studypoint.teacher

import android.content.Intent
import android.graphics.Color
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.R
import com.example.studypoint.practicetest.PaperGeneratorActivity

/**
 * Shared teacher bottom navigation: Generate, Papers, Home, Student Data,
 * Awards. Used by the teacher dashboard and My Papers. The current tab is
 * highlighted and does nothing when tapped; other tabs open their screen.
 * (No Packages / Institutes — the app is free and has neither.)
 */
object TeacherBottomNav {

    enum class Tab { GENERATE, PAPERS, HOME, STUDENTS, AWARDS }

    private val ACTIVE = Color.parseColor("#2563EB")
    private val IDLE = Color.parseColor("#64748B")

    fun setup(activity: AppCompatActivity, current: Tab) {
        fun bind(navId: Int, iconId: Int, labelId: Int, tab: Tab, open: () -> Unit) {
            val item = activity.findViewById<LinearLayout>(navId) ?: return
            val icon = activity.findViewById<ImageView>(iconId)
            val label = activity.findViewById<TextView>(labelId)
            val color = if (tab == current) ACTIVE else IDLE
            icon?.setColorFilter(color)
            label?.setTextColor(color)
            if (tab == current) {
                label?.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            item.setOnClickListener {
                if (tab != current) open()
            }
        }
        bind(R.id.navGenerate, R.id.navGenerateIcon, R.id.navGenerateLabel, Tab.GENERATE) {
            activity.startActivity(Intent(activity, PaperGeneratorActivity::class.java))
        }
        bind(R.id.navPapers, R.id.navPapersIcon, R.id.navPapersLabel, Tab.PAPERS) {
            activity.startActivity(Intent(activity, MyPapersActivity::class.java))
        }
        bind(R.id.navHome, R.id.navHomeIcon, R.id.navHomeLabel, Tab.HOME) {
            activity.startActivity(
                Intent(activity, TeacherDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
        bind(R.id.navStudents, R.id.navStudentsIcon, R.id.navStudentsLabel, Tab.STUDENTS) {
            activity.startActivity(Intent(activity, StudentDataActivity::class.java))
        }
        bind(R.id.navAwards, R.id.navAwardsIcon, R.id.navAwardsLabel, Tab.AWARDS) {
            activity.startActivity(Intent(activity, AwardListsActivity::class.java))
        }
    }
}
