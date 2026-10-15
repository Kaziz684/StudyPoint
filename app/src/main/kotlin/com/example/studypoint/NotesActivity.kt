package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.practicetest.QuizActivity
import com.google.android.material.button.MaterialButton
import java.util.Locale

class NotesActivity : AppCompatActivity() {

    private fun isPlaceholderUrl(url: String): Boolean {
        return url.isBlank() ||
                !url.startsWith("http") ||
                url.contains("COMING_SOON") ||
                url.contains("REPLACE_WITH") ||
                url.contains("PLACEHOLDER") ||
                url.contains("_BOOK") ||
                url.contains("_TEXTBOOK") ||
                url.contains("_NOTES") ||
                url.contains("_NOTE") ||
                url.contains("_KEYBOOK") ||
                url.contains("_PP") ||
                url.contains("_MCQ")
    }

    // Google Drive URL converter: extracts clean file ID and converts to direct stream link
    private fun getDirectDriveLink(url: String): String {
        if (!url.contains("drive.google.com") && !url.contains("docs.google.com")) {
            return url
        }
        val fileId = when {
            url.contains("/file/d/") -> {
                url.substringAfter("/file/d/").substringBefore("/").substringBefore("?").substringBefore("&")
            }
            url.contains("id=") -> {
                url.substringAfter("id=").substringBefore("/").substringBefore("&").substringBefore("?")
            }
            else -> null
        }
        return if (!fileId.isNullOrBlank()) {
            "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
        } else {
            url
        }
    }

    private fun open(url: String, title: String, grade: String, subject: String): Boolean {
        Log.d("StudyPoint", "Opening PDF Raw URL: $url")
        if (isPlaceholderUrl(url)) {
            Toast.makeText(this, "PDF Coming Soon", Toast.LENGTH_LONG).show()
            return false
        }

        val formattedUrl = getDirectDriveLink(url)
        Log.d("StudyPoint", "Formatted Direct PDF URL: $formattedUrl")

        val intent = Intent(this, PdfViewActivity::class.java).apply {
            putExtra("pdfUrl", formattedUrl)
            putExtra("grade", grade)
            putExtra("subject", subject)
            putExtra("title", title)
        }
        startActivity(intent)
        return true
    }

    private fun normalizeSubject(rawSubject: String): String {
        val s = rawSubject.trim()
        return when {
            s.contains("Tarjuma", ignoreCase = true) || s.contains("Quran", ignoreCase = true) || s.contains("قرآن", ignoreCase = true) -> "Tarjuma-tul-Quran"
            s.contains("Islamiyat", ignoreCase = true) || s.contains("Islamiat", ignoreCase = true) || s.contains("اسلامیات", ignoreCase = true) -> "Islamiyat"
            s.contains("Pak Studies", ignoreCase = true) || s.contains("Pakistan Studies", ignoreCase = true) || s.contains("Pak Study", ignoreCase = true) || s.contains("Pak Science", ignoreCase = true) || s.contains("Pakistan Study", ignoreCase = true) || s.contains("مطالعہ پاکستان", ignoreCase = true) || s.contains("پاکستان اسٹڈیز", ignoreCase = true) || s.contains("پاکستان", ignoreCase = true) || s.contains("SST", ignoreCase = true) -> "Pak Studies"
            s.equals("Mathematics", ignoreCase = true) || s.equals("Maths", ignoreCase = true) || s.equals("Math", ignoreCase = true) || s.contains("ریاضی", ignoreCase = true) -> "Math"
            s.equals("Computer Science", ignoreCase = true) || s.equals("Computer", ignoreCase = true) || s.contains("کمپیوٹر", ignoreCase = true) -> "Computer"
            s.contains("Physics", ignoreCase = true) || s.contains("فزکس", ignoreCase = true) -> "Physics"
            s.contains("Chemistry", ignoreCase = true) || s.contains("کیمسٹری", ignoreCase = true) -> "Chemistry"
            s.contains("Biology", ignoreCase = true) || s.contains("بائیولوجی", ignoreCase = true) -> "Biology"
            s.contains("English", ignoreCase = true) || s.contains("انگریزی", ignoreCase = true) -> "English"
            s.contains("Urdu", ignoreCase = true) || s.contains("اردو", ignoreCase = true) -> "Urdu"
            else -> s
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val rawGrade = intent.getStringExtra("grade") ?: intent.getStringExtra("class") ?: "9"
        val gradeKey = rawGrade.filter { it.isDigit() }.ifBlank { "9" }
        val rawSubject = intent.getStringExtra("subject") ?: ""
        var material = intent.getStringExtra("material") ?: ""

        val normalizedSubject = normalizeSubject(rawSubject)

        // Grammar topic PDFs (Class 10 English): open the topic's own PDF directly
        val chapterTitle = intent.getStringExtra("chapterTitle") ?: ""
        val grammarTopicPdf = if (gradeKey == "10" && normalizedSubject == "English") {
            when (chapterTitle) {
                "Pair of Words" -> "https://drive.google.com/uc?export=download&id=17ssLAMsGLsA0SwVRC4hVbH2us5As48Fq"
                "Correct Use of Verb" -> "https://drive.google.com/uc?export=download&id=1dSiq_tL9Q45b4OuLi8uREWF-MV_xyLv_"
                "Direct & Indirect Speech" -> "https://drive.google.com/uc?export=download&id=18QNDosWey5xOFL1JPnvuKRSk0hgRtN3R"
                else -> null
            }
        } else null
        if (grammarTopicPdf != null) {
            open(grammarTopicPdf, chapterTitle, rawGrade, normalizedSubject)
            finish()
            return
        }

        // Comprehensive pdfLinks Map with all PCTB Subjects according to curriculum rules:
        // - All Classes (9th, 10th, 11th, 12th): Must include "Tarjuma-tul-Quran" as a dedicated subject section.
        // - 9th & 11th Classes: Must include "Islamiyat" compulsory (Pak Studies omitted).
        // - 10th & 12th Classes: REMOVE "Islamiyat" completely and REPLACE/ADD "Pak Studies" as a dedicated subject section.
        // - Materials inside each Subject Section: Textbook, Notes, Past Papers, Guess Papers, MCQs.
        val basePdfLinks = mapOf(
            "9" to mapOf(
                "English" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=9_ENGLISH_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=9_ENGLISH_BOOK",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=119eyVRNCHDIEEzKFVur18_bPed3nn0wY"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_ENG_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_ENG_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_ENG_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_ENG_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_ENG_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Biology" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1E82PoD4JjzKcH-08RfgrCcCDEkC3lTkz",
                    "Book" to "https://drive.google.com/uc?export=download&id=1E82PoD4JjzKcH-08RfgrCcCDEkC3lTkz",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=9_BIO_NOTE"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Math" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1eATq2CheWHx-_Pp4WCL4dhgy7U66m3ND",
                    "Book" to "https://drive.google.com/uc?export=download&id=1eATq2CheWHx-_Pp4WCL4dhgy7U66m3ND",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=1RtZZ5jE8w-CmLO2tVKbPczp-TO1Fc9Gk"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_MATH_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_MATH_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_MATH_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_MATH_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_MATH_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Physics" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1Z_WG6Y8wFuvqJu7qdy-xEQQm1KXX_e9r",
                    "Book" to "https://drive.google.com/uc?export=download&id=1Z_WG6Y8wFuvqJu7qdy-xEQQm1KXX_e9r",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_PHY_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_PHY_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_PHY_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_PHY_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_PHY_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Chemistry" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1X5QxTWBgKlwCubCY0OBcjFVlQdsu4kEw",
                    "Book" to "https://drive.google.com/uc?export=download&id=1X5QxTWBgKlwCubCY0OBcjFVlQdsu4kEw",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=15zybNRbmjPO7cIn0Te9gefo5GFPG1UeZ"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_CHEM_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_CHEM_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_CHEM_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_CHEM_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_CHEM_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Computer" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=15e8iWFCs2hZqTqQQbBE0gj5Om50B42Vx",
                    "Book" to "https://drive.google.com/uc?export=download&id=15e8iWFCs2hZqTqQQbBE0gj5Om50B42Vx",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=1suMI7yc4jD8OyXiLbOqqCQixpI_rt-o_"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_CS_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_CS_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_CS_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_CS_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_CS_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Urdu" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1ymJXZaSTWprln5k8Z4u0zZ6EwULbgDBk",
                    "Book" to "https://drive.google.com/uc?export=download&id=1ymJXZaSTWprln5k8Z4u0zZ6EwULbgDBk",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_URDU_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_URDU_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_URDU_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_URDU_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_URDU_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Islamiyat" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1pC6LTikKN0WEhHIMKoF5uM4iabk_snKF",
                    "Book" to "https://drive.google.com/uc?export=download&id=1pC6LTikKN0WEhHIMKoF5uM4iabk_snKF",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=13KfAtj5FM-6caBamFSBL0X0GZgK7RciR"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_ISL_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_ISL_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_ISL_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_ISL_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_ISL_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Tarjuma-tul-Quran" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1U4TI5vqLsGlh49iYWdatb28wUko2Djxv",
                    "Book" to "https://drive.google.com/uc?export=download&id=1U4TI5vqLsGlh49iYWdatb28wUko2Djxv",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_TQ_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_TQ_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_TQ_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_TQ_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_TQ_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                )
            ),
            "10" to mapOf(
                "English" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1rwPlvnVuSlaMwCvmq7WPEFqsLFg2xSgi",
                    "Book" to "https://drive.google.com/uc?export=download&id=1rwPlvnVuSlaMwCvmq7WPEFqsLFg2xSgi",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=1PmSzp3xmU88z597id2SQbEy0ftAbQ9K_"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_ENG_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_ENG_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_ENG_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_ENG_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_ENG_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Computer" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=15e8iWFCs2hZqTqQQbBE0gj5Om50B42Vx",
                    "Book" to "https://drive.google.com/uc?export=download&id=15e8iWFCs2hZqTqQQbBE0gj5Om50B42Vx",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=1suMI7yc4jD8OyXiLbOqqCQixpI_rt-o_"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_CS_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_CS_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_CS_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_CS_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_CS_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Biology" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1mPdYAMYe1iBGLC_2ZCJHVLhZoQxVxxGL",
                    "Book" to "https://drive.google.com/uc?export=download&id=1mPdYAMYe1iBGLC_2ZCJHVLhZoQxVxxGL",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Physics" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=137liQcICrTidgolg0HrVAvmNzadI7n4y",
                    "Book" to "https://drive.google.com/uc?export=download&id=137liQcICrTidgolg0HrVAvmNzadI7n4y",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_PHY_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_PHY_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_PHY_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_PHY_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_PHY_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Chemistry" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1hL9-HeRrPnOQwXGUJCkcldyqTy2y3Kd0",
                    "Book" to "https://drive.google.com/uc?export=download&id=1hL9-HeRrPnOQwXGUJCkcldyqTy2y3Kd0",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Urdu" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1YGc4bd5mdSXthR73Hw86pvoewqkvEzYk",
                    "Book" to "https://drive.google.com/uc?export=download&id=1YGc4bd5mdSXthR73Hw86pvoewqkvEzYk",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Math" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1TiZFVUnjdmGnZPc5Zsn7Ia-k6-P0gUFN",
                    "Book" to "https://drive.google.com/uc?export=download&id=1TiZFVUnjdmGnZPc5Zsn7Ia-k6-P0gUFN",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=1pNFnJtsV9_0E60qryeUuMq4FTF_WuYds"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_MATH_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_MATH_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_MATH_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_MATH_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_MATH_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                // Pak Studies replaces Islamiyat in Class 10 (Islamiyat completely removed)
                "Pak Studies" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=10_PS_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=10_PS_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Tarjuma-tul-Quran" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=10_TQ_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=10_TQ_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                )
            ),
            "11" to mapOf(
                "English" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=11_ENGLISH_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=11_ENGLISH_BOOK",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=11_ENGLISH_NOTES"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=11_ENG_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=11_ENG_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=11_ENG_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=11_ENG_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_ENG_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Urdu" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=11_URDU_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=11_URDU_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=11_URDU_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=11_URDU_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=11_URDU_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=11_URDU_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_URDU_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Physics" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=11_PHY_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=11_PHY_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=11_PHY_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=11_PHY_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=11_PHY_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=11_PHY_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_PHY_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Chemistry" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=11_CHEM_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=11_CHEM_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=11_CHEM_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=11_CHEM_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=11_CHEM_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=11_CHEM_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_CHEM_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Biology" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=11_BIO_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=11_BIO_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=11_BIO_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=11_BIO_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=11_BIO_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=11_BIO_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_BIO_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Math" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=11_MATH_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=11_MATH_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=11_MATH_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=11_MATH_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=11_MATH_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=11_MATH_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_MATH_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Computer" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=11_CS_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=11_CS_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=11_CS_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=11_CS_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=11_CS_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=11_CS_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_CS_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Islamiyat" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=11_ISL_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=11_ISL_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=11_ISL_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=11_ISL_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=11_ISL_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=11_ISL_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_ISL_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Tarjuma-tul-Quran" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=11_TQ_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=11_TQ_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=11_TQ_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=11_TQ_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=11_TQ_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=11_TQ_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_TQ_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                )
            ),
            "12" to mapOf(
                "English" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_ENGLISH_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_ENGLISH_BOOK",
                    "Notes" to listOf("https://drive.google.com/uc?export=download&id=12_ENGLISH_NOTES"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_ENG_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_ENG_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_ENG_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_ENG_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_ENG_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Urdu" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_URDU_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_URDU_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_URDU_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_URDU_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_URDU_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_URDU_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_URDU_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Physics" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_PHY_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_PHY_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Chemistry" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_CHEM_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_CHEM_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_CHEM_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_CHEM_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_CHEM_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_CHEM_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_CHEM_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Biology" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_BIO_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_BIO_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_BIO_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_BIO_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_BIO_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_BIO_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_BIO_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Math" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_MATH_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_MATH_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_MATH_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_MATH_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_MATH_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_MATH_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_MATH_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Computer" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_CS_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_CS_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_CS_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_CS_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_CS_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_CS_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_CS_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                // Pak Studies replaces Islamiyat in Class 12 (Islamiyat completely removed)
                "Pak Studies" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_PS_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_PS_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_PS_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_PS_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_PS_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_PS_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_PS_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                ),
                "Tarjuma-tul-Quran" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_TQ_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_TQ_BOOK",
                    "Notes" to listOf("COMING_SOON"),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_TQ_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_TQ_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_TQ_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_TQ_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_TQ_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
                )
            )
        )

        // Enrich with alias keys for each class
        val enrichedPdfLinks = basePdfLinks.mapValues { (_, subjectsMap) ->
            val enriched = subjectsMap.toMutableMap()
            subjectsMap["Math"]?.let { enriched["Mathematics"] = it }
            subjectsMap["Computer"]?.let { enriched["Computer Science"] = it }
            subjectsMap["Tarjuma-tul-Quran"]?.let { enriched["Tarjuma tul Quran"] = it }
            subjectsMap["Pak Studies"]?.let { enriched["Pakistan Studies"] = it }
            subjectsMap["Islamiyat"]?.let { enriched["Islamiat"] = it }
            enriched.toMap()
        }

        val pdfLinks = enrichedPdfLinks + enrichedPdfLinks.mapKeys { "${it.key}th" }
        val subjectMap = pdfLinks[rawGrade] ?: pdfLinks[gradeKey] ?: pdfLinks["${gradeKey}th"] ?: emptyMap()

        val capitalizedSubject = rawSubject.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        val defaultFallbackMap = mapOf(
            "Textbook" to "COMING_SOON",
            "Book" to "COMING_SOON",
            "Notes" to listOf("COMING_SOON"),
            "Past Papers" to mapOf(
                "2022" to "COMING_SOON",
                "2023" to "COMING_SOON",
                "2024" to "COMING_SOON",
                "2025" to "COMING_SOON",
                "2026" to "COMING_SOON"
            ),
            "MCQs" to "COMING_SOON",
                    "Guess Papers" to "COMING_SOON"
        )

        val resolvedSubjectData = subjectMap[rawSubject]
            ?: subjectMap[normalizedSubject]
            ?: subjectMap[capitalizedSubject]
            ?: subjectMap["Tarjuma-tul-Quran"]
            ?: subjectMap["Tarjuma tul Quran"]
            ?: subjectMap["Pak Studies"]
            ?: subjectMap["Pakistan Studies"]
            ?: subjectMap["Islamiyat"]
            ?: subjectMap["Islamiat"]
            ?: subjectMap["Math"]
            ?: subjectMap["Computer"]
            ?: defaultFallbackMap

        // If material is MCQs, launch QuizActivity directly
        if (material.equals("MCQs", ignoreCase = true) || material.equals("MCQ Practice", ignoreCase = true)) {
            val sheetName = "Class_${gradeKey}th"
            val quizIntent = Intent(this, QuizActivity::class.java).apply {
                putExtra("sheetName", sheetName)
                putExtra("subject", rawSubject.ifBlank { normalizedSubject })
                putExtra("chapter", "")
            }
            startActivity(quizIntent)
            finish()
            return
        }

        val ids = listOf(
            R.id.btnCh1, R.id.btnCh2, R.id.btnCh3,
            R.id.btnCh4, R.id.btnCh5, R.id.btnCh6,
            R.id.btnCh7, R.id.btnCh8, R.id.btnCh9
        )

        // Reset visibility of all buttons initially
        ids.forEach { findViewById<MaterialButton>(it).visibility = View.GONE }

        val displaySubject = if (rawSubject.isNotBlank()) rawSubject else normalizedSubject

        // If no material specified, show the 4 strictly required material buttons:
        // 1. Textbook, 2. Notes, 3. Past Papers, 4. MCQs
        if (material.isBlank()) {
            findViewById<TextView>(R.id.tvNotesTitle).text = "$displaySubject Study Material"

            val materialOptions = listOf("Textbook", "Notes", "Past Papers", "Guess Papers", "MCQs")
            materialOptions.forEachIndexed { index, mat ->
                findViewById<MaterialButton>(ids[index]).apply {
                    visibility = View.VISIBLE
                    text = when (mat) {
                        "Textbook" -> "📘 Textbook"
                        "Notes" -> "📄 Solved Notes"
                        "Past Papers" -> "📝 Past Papers (2022-2026)"
                        "MCQs" -> "🎯 MCQs Practice"
                        else -> mat
                    }
                    setOnClickListener {
                        if (mat == "MCQs") {
                            val sheetName = "Class_${gradeKey}th"
                            val quizIntent = Intent(this@NotesActivity, QuizActivity::class.java).apply {
                                putExtra("sheetName", sheetName)
                                putExtra("subject", displaySubject)
                                putExtra("chapter", "")
                            }
                            startActivity(quizIntent)
                        } else {
                            val intent = Intent(this@NotesActivity, NotesActivity::class.java).apply {
                                putExtra("grade", gradeKey)
                                putExtra("class", gradeKey)
                                putExtra("subject", displaySubject)
                                putExtra("material", mat)
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
            return
        }

        findViewById<TextView>(R.id.tvNotesTitle).text = when {
            material.equals("Notes", ignoreCase = true) || material.equals("Solved Notes", ignoreCase = true) -> "$displaySubject Solved Notes"
            material.equals("Book", ignoreCase = true) || material.equals("Textbook", ignoreCase = true) -> "$displaySubject Textbook"
            material.equals("Past Papers", ignoreCase = true) -> "$displaySubject Past Papers (2022-2026)"
            material.equals("Guess Papers", ignoreCase = true) -> "$displaySubject Guess Papers 2027"
            else -> displaySubject
        }

        if (material.equals("Past Papers", ignoreCase = true)) {
            @Suppress("UNCHECKED_CAST")
            val pastPapersMap = resolvedSubjectData["Past Papers"] as? Map<String, String> ?: emptyMap()
            val years = listOf("2022", "2023", "2024", "2025", "2026")

            ids.zip(years).forEach { (id, year) ->
                val btn = findViewById<MaterialButton>(id)
                val url = pastPapersMap[year] ?: ""
                btn.visibility = View.VISIBLE
                btn.text = "Past Paper $year"
                btn.setOnClickListener {
                    open(url, "$displaySubject Past Paper $year", gradeKey, displaySubject)
                }
            }
        } else {
            val linksAny = if (material.equals("Textbook", ignoreCase = true) || material.equals("Book", ignoreCase = true)) {
                resolvedSubjectData["Textbook"] ?: resolvedSubjectData["Book"]
            } else {
                resolvedSubjectData[material]
            }

            val links = when (linksAny) {
                is List<*> -> linksAny.filterIsInstance<String>()
                is String -> listOf(linksAny)
                else -> emptyList()
            }

            if (links.size > 1) {
                ids.zip(links).forEachIndexed { index, (id, url) ->
                    findViewById<MaterialButton>(id).apply {
                        visibility = View.VISIBLE
                        text = "$material Part ${index + 1}"
                        setOnClickListener { open(url, "$displaySubject $material Part ${index + 1}", gradeKey, displaySubject) }
                    }
                }
            } else {
                // Direct open for single Notes or Textbook
                val url = links.firstOrNull() ?: ""
                if (!isPlaceholderUrl(url)) {
                    open(url, "$displaySubject $material", gradeKey, displaySubject)
                    finish()
                } else {
                    Toast.makeText(this, "$material Coming Soon", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}