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
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Teacher Dashboard — exactly four primary cards:
 * Paper Generator, Student Data, Award List, Award List Create.
 */
class TeacherDashboardActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var quoteIndex = 0

    private val quotes = listOf(
        "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ" to
            "Allahumma Salli 'Ala Muhammadin Wa 'Ala Aali Muhammad",
        "رَبِّ زِدْنِي عِلْمًا" to
            "My Lord, increase me in knowledge. (Surah Taha 20:114)",
        "الْعِلْمُ خَيْرٌ مِنَ الْمَالِ" to
            "Knowledge is better than wealth. (Hazrat Ali RA)",
        "طَلَبُ الْعِلْمِ فَرِيضَةٌ عَلَى كُلِّ مُسْلِمٍ" to
            "Seeking knowledge is an obligation upon every Muslim."
    )

    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 20_000)
        }
    }

    private val quoteTick = object : Runnable {
        override fun run() {
            quoteIndex = (quoteIndex + 1) % quotes.size
            showQuote()
            handler.postDelayed(this, 5_000)
        }
    }


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
        findViewById<LinearLayout>(R.id.cardBoards).setOnClickListener {
            startActivity(Intent(this, com.example.studypoint.BoardsActivity::class.java))
        }
        findViewById<Button>(R.id.btnWhatsApp).setOnClickListener {
            com.example.studypoint.SocialMediaManager.openWhatsApp(this)
        }
        findViewById<Button>(R.id.btnYouTube).setOnClickListener {
            com.example.studypoint.SocialMediaManager.openYouTubeChannel(this)
        }
        findViewById<Button>(R.id.btnFacebook).setOnClickListener {
            com.example.studypoint.SocialMediaManager.openFacebook(this)
        }
        findViewById<LinearLayout>(R.id.cardQuote).setOnClickListener {
            handler.removeCallbacks(quoteTick)
            quoteIndex = (quoteIndex + 1) % quotes.size
            showQuote()
            handler.postDelayed(quoteTick, 5_000)
        }
        showQuote()
        handler.post(clockTick)
        handler.postDelayed(quoteTick, 5_000)

        findViewById<TextView>(R.id.btnSwitchRole).setOnClickListener {
            startActivity(
                Intent(this, RoleSelectionActivity::class.java)
                    .putExtra(RoleSelectionActivity.EXTRA_CHANGE_MODE, true)
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        handler.removeCallbacks(quoteTick)
        super.onDestroy()
    }

    private fun showQuote() {
        val (arabic, meaning) = quotes[quoteIndex]
        findViewById<TextView>(R.id.txtQuoteArabic).text = arabic
        findViewById<TextView>(R.id.txtQuoteMeaning).text = meaning
    }

    private fun updateClock() {
        val now = Date()
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
        val date = SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(now)
        var hijri = ""
        try {
            // Pakistan (Ruet-e-Hilal) runs one day behind the Umalqura calendar.
            val hd = java.time.chrono.HijrahDate.now()
                .minus(1, java.time.temporal.ChronoUnit.DAYS)
            val months = listOf(
                "", "Muharram", "Safar", "Rabi-ul-Awwal", "Rabi-us-Sani",
                "Jumada-ul-Awwal", "Jumada-us-Sani", "Rajab", "Sha'ban",
                "Ramadan", "Shawwal", "Zil-Qa'dah", "Zil-Hajjah"
            )
            val m = hd.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            val dd = hd.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            val y = hd.get(java.time.temporal.ChronoField.YEAR)
            hijri = "$dd ${months[m]} $y AH"
        } catch (t: Throwable) {
            hijri = ""
        }
        findViewById<TextView>(R.id.txtTime).text = "🕐 $time"
        findViewById<TextView>(R.id.txtDateHijri).text =
            if (hijri.isNotEmpty()) "$date  ·  $hijri" else date
    }
}
