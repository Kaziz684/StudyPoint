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
                url.contains("_GP") ||
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
                "Translation (Urdu to English)" -> "https://drive.google.com/uc?export=download&id=16A3hQteRW5xZm_fW9XUGkfOYkiTdSYAI"
                else -> null
            }
        } else null
        if (grammarTopicPdf != null) {
            open(grammarTopicPdf, chapterTitle, rawGrade, normalizedSubject)
            finish()
            return
        }

        // Grammar topic PDFs (Class 10 Urdu): open the topic's own PDF directly
        val urduGrammarTopicPdf = if (gradeKey == "10" && normalizedSubject == "Urdu") {
            when (chapterTitle) {
                "مضامین" -> "https://drive.google.com/uc?export=download&id=19DdZGkJJkse7fkUxIS557vUmT7C0ImX-"
                "تفہیم عبارات" -> "https://drive.google.com/uc?export=download&id=1FvVtrRCmRWbeIpD7J8l0rc2En6v7gNqu"
                else -> null
            }
        } else null
        if (urduGrammarTopicPdf != null) {
            open(urduGrammarTopicPdf, chapterTitle, rawGrade, normalizedSubject)
            finish()
            return
        }

        // Chapter picked from the chapter list (SectionsActivity): open THAT chapter's
        // content directly below (once the pdf links are built) - never show the full
        // chapter list again inside a chapter.
        val chapterExtra = intent.getStringExtra("chapter") ?: ""
        val wantsChapterDirect = material.equals("Notes", ignoreCase = true) &&
            (chapterTitle.isNotBlank() || chapterExtra.isNotBlank())

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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=9_ENG_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_ENG_GP_2026"
                    )
                ),
                "Biology" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1E82PoD4JjzKcH-08RfgrCcCDEkC3lTkz",
                    "Book" to "https://drive.google.com/uc?export=download&id=1E82PoD4JjzKcH-08RfgrCcCDEkC3lTkz",
                    "Notes" to mapOf(
                        "Chapter 1: The Science of Biology" to "https://drive.google.com/uc?export=download&id=1SWtScvfaxJfeCUtEctlRBhdCo8uwKr8S",
                        "Chapter 2: Biodiversity" to "https://drive.google.com/uc?export=download&id=1miZBqZTb8HQ-b-K4rHqHG7QhnjrBGtoA",
                        "Chapter 3: The Cell" to "https://drive.google.com/uc?export=download&id=1A4ZIy3iC7zr2_Wl_1sOxeuu7IwvXwR-4",
                        "Chapter 4: Cell Cycle" to "https://drive.google.com/uc?export=download&id=1PD-yhDuqocbK0tTn3PiRDO9HzzkM-Dsq",
                        "Chapter 5: Tissues, Organs and Organ Systems" to "https://drive.google.com/uc?export=download&id=1T4FDdzrQwYvevnSedwZQ4Zi-OoO9yyiy",
                        "Chapter 6: Biomolecules" to "https://drive.google.com/uc?export=download&id=1fdnlbWwR7OXeY7a3oXpeRPLFtTfofkrF",
                        "Chapter 7: Enzymes" to "https://drive.google.com/uc?export=download&id=1OTmEUvZcHCccudlV0EeIG1Ah_9ZchWXb",
                        "Chapter 8: Bioenergetics" to "https://drive.google.com/uc?export=download&id=1d0keiAwcjOs2O7U_tq11ESnJfcpCoxWv",
                        "Chapter 9: Plant Physiology" to "https://drive.google.com/uc?export=download&id=1ZIOP8Kedi3MLEZyt803p8BHog1wdUwEd",
                        "Chapter 10: Reproduction in Plants" to "https://drive.google.com/uc?export=download&id=1pgyW3NqeN7KuusIO7ZYqccwoAQlLpZmg",
                        "Chapter 11: Biostatistics" to "https://drive.google.com/uc?export=download&id=1rEfUNmlBH45UAKTVIGNrkpaXbuW2DS_R"
                    ),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_BIO_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=9_BIO_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_BIO_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=9_MATH_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_MATH_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=9_PHY_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_PHY_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=9_CHEM_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_CHEM_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=9_CS_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_CS_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=9_URDU_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_URDU_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=9_ISL_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_ISL_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=9_TQ_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=9_TQ_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=10_ENG_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_ENG_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=10_CS_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_CS_GP_2026"
                    )
                ),
                "Biology" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1mPdYAMYe1iBGLC_2ZCJHVLhZoQxVxxGL",
                    "Book" to "https://drive.google.com/uc?export=download&id=1mPdYAMYe1iBGLC_2ZCJHVLhZoQxVxxGL",
                    "Notes" to mapOf(
                        "Chapter 1: Human Digestive System" to "https://drive.google.com/uc?export=download&id=1akb1GWw05hW9SMqi5vksbL0OVnDxjDPT",
                        "Chapter 2: Human Respiratory System" to "https://drive.google.com/uc?export=download&id=1Yt4cVxbQE65iAa_B8XltL3_xC4IDySiQ",
                        "Chapter 3: Human Blood Circulatory System" to "https://drive.google.com/uc?export=download&id=167MtIybLuyimrCG_1-8SQOHtRDJmrB5I",
                        "Chapter 4: Human Urinary System" to "https://drive.google.com/uc?export=download&id=11FRGcGs9tqCHmP77iny-E756PjLms45K",
                        "Chapter 5: Coordination" to "https://drive.google.com/uc?export=download&id=13rd_pgHlqQmMO4OfSUrw63pcAqYfgrce",
                        "Chapter 6: Reproduction" to "https://drive.google.com/uc?export=download&id=1slN3z4leLxbPJujT80pl0ojNdP-iC3Q4",
                        "Chapter 7: Inheritance" to "https://drive.google.com/uc?export=download&id=14YSKylhTBUhlJ8JaE5iwbTmNQQO3JSWr",
                        "Chapter 8: Biotechnology" to "https://drive.google.com/uc?export=download&id=1Y18NoI9-RHzbo8suJmfGT6n9b8Uca3Rx",
                        "Chapter 9: Diseases and Immunity" to "https://drive.google.com/uc?export=download&id=1Km3skwuXcUrUYe0Oee1ozhpwQdYDUW7Q",
                        "Chapter 10: Evolution" to "https://drive.google.com/uc?export=download&id=1F2hqH7ptmRQT08Xak7yLv2ZzJxXlKNcK"
                    ),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_BIO_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=10_BIO_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_BIO_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=10_PHY_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_PHY_GP_2026"
                    )
                ),
                "Chemistry" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1hL9-HeRrPnOQwXGUJCkcldyqTy2y3Kd0",
                    "Book" to "https://drive.google.com/uc?export=download&id=1hL9-HeRrPnOQwXGUJCkcldyqTy2y3Kd0",
                    "Notes" to mapOf(
                        "Chapter 14: States of Matter and Phase Changes" to "https://drive.google.com/uc?export=download&id=1GFFziQYeuCG47W82RuaJ2l2EJ2kxUxY1",
                        "Chapter 15: Stoichiometry" to "https://drive.google.com/uc?export=download&id=1SUhEFmeJQdyfDp5JrNNNn7yalEWkwLIj",
                        "Chapter 16: Electrochemistry" to "https://drive.google.com/uc?export=download&id=1aIJztg5Ef3GiK7yvrlycmh0XqeRUrdFk",
                        "Chapter 17: Reaction Kinetics" to "https://drive.google.com/uc?export=download&id=1rHnBQTQgQDh5ql0QXMRoxmYmQy0IVYPW",
                        "Chapter 18: Salts" to "https://drive.google.com/uc?export=download&id=1UmLnn8ifVXE3jK_ekaDJ68kZ2rXyAFEN",
                        "Chapter 19: Nitrogen and Sulphur" to "https://drive.google.com/uc?export=download&id=1ntgzEN04u6z_VvrUyIsFOkr0j17xxdrm",
                        "Chapter 20: Water" to "https://drive.google.com/uc?export=download&id=1vYKpwd0--j1YYbYU89_CuzL5GsrO5Dz1",
                        "Chapter 21: Organic Chemistry" to "https://drive.google.com/uc?export=download&id=1rgLjGOUzZ8r4P1JQp_cotnwD-qon5LwP",
                        "Chapter 22: Hydrocarbons" to "https://drive.google.com/uc?export=download&id=1Zaabyu3ExqQueY5gJ9G-q1rU8VSYHqDV",
                        "Chapter 23: Monohydroxy Alkanes or Alcohols" to "https://drive.google.com/uc?export=download&id=1FKJ0XE-ZKyXVuTMO3MATsu6vupaUmNp4",
                        "Chapter 24: Carboxylic Acids" to "https://drive.google.com/uc?export=download&id=1RjLvA5UJCVE5kcXL_ddh2gjvzClyF7VD",
                        "Chapter 25" to "COMING_SOON",
                        "Chapter 26: Polymers" to "https://drive.google.com/uc?export=download&id=1GUCWIAEMCi1yMrvsTIlfeZveOShTvOo8"
                    ),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_CHEM_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=10_CHEM_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_CHEM_GP_2026"
                    )
                ),
                "Urdu" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=1YGc4bd5mdSXthR73Hw86pvoewqkvEzYk",
                    "Book" to "https://drive.google.com/uc?export=download&id=1YGc4bd5mdSXthR73Hw86pvoewqkvEzYk",
                    "Notes" to mapOf(
                        "Chapter 1: حمد" to "https://drive.google.com/uc?export=download&id=1XzPkkfNGX4nFFXEu8nlpPhbqT6fDPdNR",
                        "Chapter 2: نعت" to "https://drive.google.com/uc?export=download&id=1ue3vB61wAA10pyQaA6uGig8i6-mawD1D",
                        "Chapter 3: اخلاق نبوی خاتم النبیین ﷺ" to "https://drive.google.com/uc?export=download&id=1qZ1NVIk6CfwqncxmolfXNL3wBDL-gmB8",
                        "Chapter 4: سرسید کا بچپن" to "https://drive.google.com/uc?export=download&id=1fX-XfDlIihF8fkbERkwOIE6tsWHurVxc",
                        "Chapter 5: محسن محلہ" to "https://drive.google.com/uc?export=download&id=1KQYr9KdvMlD2TFh-KivCiDQu3pJpW-jg",
                        "Chapter 6: کفارہ" to "https://drive.google.com/uc?export=download&id=1rmGA8Q-cpmGPi-y0heBKLAGybkpzIimq",
                        "Chapter 7: سویرے جو کل آنکھ میری کھلی" to "https://drive.google.com/uc?export=download&id=1R1V6sRMw0TRxNm75EpL_kWNNNtuVlDUq",
                        "Chapter 8: دوستی کا پھل" to "https://drive.google.com/uc?export=download&id=1KuI-w5ZkDFD6NdLc72t9MvDyMPCP4zeV",
                        "Chapter 9: میرا گاؤں" to "https://drive.google.com/uc?export=download&id=1xH12sejiNwx4O7og3Q3vnXNvC8xrVBMn",
                        "Chapter 10: بابل کے کھنڈر" to "https://drive.google.com/uc?export=download&id=1EZqqpimhtPTyApMvD51vN8VD_pqrjRWX",
                        "Chapter 11: اولڈ ایج ہوم" to "https://drive.google.com/uc?export=download&id=1E1NQurJMLk0Y6FlSHme4qqfqn-5ZadgX",
                        "Chapter 12: کچھ ذریعہ تعلیم کے باب میں" to "https://drive.google.com/uc?export=download&id=15vwBmYcna5RKMXFCcyeus431nylAWSqV",
                        "Grammar: Mazameen (Essays)" to "https://drive.google.com/uc?export=download&id=19DdZGkJJkse7fkUxIS557vUmT7C0ImX-",
                        "Grammar: Tafheem-e-Ebaraat (Comprehension)" to "https://drive.google.com/uc?export=download&id=1FvVtrRCmRWbeIpD7J8l0rc2En6v7gNqu"
                    ),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_URDU_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=10_URDU_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_URDU_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=10_MATH_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_MATH_GP_2026"
                    )
                ),
                // Pak Studies replaces Islamiyat in Class 10 (Islamiyat completely removed)
                "Pak Studies" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=10_PS_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=10_PS_BOOK",
                    "Notes" to mapOf(
                        "Chapter 1: پاکستان کی نظریاتی اساس (حصہ اول)" to "https://drive.google.com/uc?export=download&id=1fEPcK5DLoTY9EIHjZbqrWdt4m7EUG4hp",
                        "Chapter 2: تحریک پاکستان (حصہ اول)" to "https://drive.google.com/uc?export=download&id=1DE8Z8I-lCtfN3X8nqr7DReaatxqphn2l",
                        "Chapter 3: پاکستان کی تاریخ (1947ء تاحال) اور پاکستان میں آئینی ترقی (حصہ اول)" to "https://drive.google.com/uc?export=download&id=1oMo6TC-x-dC3lUynJESeoyyx68Q2ZqOo",
                        "Chapter 4: پاکستان کا جغرافیہ (حصہ اول)" to "https://drive.google.com/uc?export=download&id=1qOLkRmEvFklK77OwaGsYL4NzIOK4JAQi",
                        "Chapter 5: پاکستان اور بین الاقوامی امور" to "https://drive.google.com/uc?export=download&id=1pX6QklrIPE-pM2Yq10XrNMaIBrol9Bmc",
                        "Chapter 6: پاکستان کے وسائل اور معاشی ترقی (حصہ اول)" to "https://drive.google.com/uc?export=download&id=1sJUIbgIc95rED7Jww-FAGtBgExCcdMaT",
                        "Chapter 7: آبادی، معاشرہ اور پاکستان میں ثقافتی تنوع (حصہ اول)" to "https://drive.google.com/uc?export=download&id=1Ae7ZQFwYUyhFLRtQH9oYIs8Jrrr3jTpJ",
                        "Chapter 8: پاکستان میں خواتین کو با اختیار بنانا (حصہ اول)" to "https://drive.google.com/uc?export=download&id=1eFTP0ebAoWbRp3NkxaG23kK0GefVMGRl"
                    ),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_PS_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=10_PS_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_PS_GP_2026"
                    )
                ),
                "Tarjuma-tul-Quran" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=10_TQ_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=10_TQ_BOOK",
                    "Notes" to mapOf(
                        "Chapter 1: Surah Al Anam" to "https://drive.google.com/uc?export=download&id=1HaDYHKpxLLVCwU1nfWDMxCzZ13Bv0md9",
                        "Chapter 2: Surah A'araf" to "https://drive.google.com/uc?export=download&id=122oAbTsS7uBqjftGxGHJ24lQiE9Ij_yY",
                        "Chapter 3: Surah Yunus" to "https://drive.google.com/uc?export=download&id=1cjNVYxR12zd1ZnM9qkjpMIblsDJs9ITe",
                        "Chapter 4: Surah Hud" to "https://drive.google.com/uc?export=download&id=17AFcXriRBTGV98yLA1Hpt9RPSSbltTYS",
                        "Chapter 5: Surah Ra'ad" to "https://drive.google.com/uc?export=download&id=1sT3bAW1ipDO67tS6RBK9kYvNyxs9ZV4h",
                        "Chapter 6: Surah Ibrahim" to "https://drive.google.com/uc?export=download&id=1N_g0P7UtUpO_ET6vjP_Lctl-XW9Jn9-U",
                        "Chapter 7: Surah Hijr" to "https://drive.google.com/uc?export=download&id=1HO-EoKxXujN7dxc6KCxo67psCYqQQVTd",
                        "Chapter 8: Surah Nahl" to "https://drive.google.com/uc?export=download&id=14sMF7FVN3HywvvwmpA4E-yIc_3Xnzldd",
                        "Chapter 9: Surah Isra / Bani Israel" to "COMING_SOON",
                        "Chapter 10: Surah Kahf" to "https://drive.google.com/uc?export=download&id=1S3oPh8aXYprP4jGBU7CNijkhHZ-s9khw",
                        "Chapter 11: Surah Mominoon" to "https://drive.google.com/uc?export=download&id=1i3nVnuRuV5YbWxkstnUbDPp5rJnANk8-",
                        "Chapter 12: Surah Zumar" to "https://drive.google.com/uc?export=download&id=13vUYFbZddEZpfSjoFexUASQ35dG5g9Uz",
                        "Chapter 13: Surah Ghafir / Momin" to "https://drive.google.com/uc?export=download&id=1YR0naMH0org0KboPlhB_JMG894FORNx5",
                        "Chapter 14: Surah Fussilat / Hamim Sajdah" to "https://drive.google.com/uc?export=download&id=1Bl5NhD2SaFq7tSb8f8fAuRPhXDgApupC",
                        "Chapter 15: Surah Shura" to "https://drive.google.com/uc?export=download&id=1EkJdOnhTqQcaL5npxHDptg1fC0Ki43Vb"
                    ),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_TQ_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=10_TQ_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=10_TQ_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=11_ENG_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_ENG_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=11_URDU_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_URDU_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=11_PHY_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_PHY_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=11_CHEM_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_CHEM_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=11_BIO_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_BIO_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=11_MATH_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_MATH_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=11_CS_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_CS_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=11_ISL_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_ISL_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=11_TQ_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=11_TQ_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=12_ENG_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_ENG_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=12_URDU_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_URDU_GP_2026"
                    )
                ),
                "Physics" to mapOf(
                    "Textbook" to "https://drive.google.com/uc?export=download&id=12_PHY_BOOK",
                    "Book" to "https://drive.google.com/uc?export=download&id=12_PHY_BOOK",
                    "Notes" to mapOf(
                        "Chapter 13: Thermal Physics" to "https://drive.google.com/uc?export=download&id=1Oy9DnC_ItnZYnUt47AnhJkU63Dy4WNxR"
                    ),
                    "Past Papers" to mapOf(
                        "2022" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2022",
                        "2023" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2023",
                        "2024" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2024",
                        "2025" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2025",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_PHY_PP_2026"
                    ),
                    "MCQs" to "COMING_SOON",
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=12_PHY_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_PHY_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=12_CHEM_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_CHEM_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=12_BIO_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_BIO_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=12_MATH_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_MATH_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=12_CS_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_CS_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=12_PS_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_PS_GP_2026"
                    )
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
                    "Guess Papers" to mapOf(
                        "2027" to "https://drive.google.com/uc?export=download&id=12_TQ_GP_2027",
                        "2026" to "https://drive.google.com/uc?export=download&id=12_TQ_GP_2026"
                    )
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

        // Direct chapter open: the student already picked a chapter in the chapter
        // list, so open that chapter's own notes PDF right away (or honestly say
        // Coming Soon). Never fall through to the subject-level chapter list.
        if (wantsChapterDirect) {
            val chapterNum = Regex("""\d+""").find(chapterExtra)?.value?.toIntOrNull()
                ?: Regex("""(?:Chapter|Ch\.?|Unit)\s*(\d+)""", RegexOption.IGNORE_CASE)
                    .find(chapterTitle)?.groupValues?.get(1)?.toIntOrNull()
            val notesAny = resolvedSubjectData["Notes"]
            val directUrl: String? = when (notesAny) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val nMap = notesAny as Map<String, String>
                    nMap.entries.firstOrNull { e ->
                        val keyNum = Regex("""(?:Chapter|Ch\.?|Unit)\s*(\d+)""", RegexOption.IGNORE_CASE)
                            .find(e.key)?.groupValues?.get(1)?.toIntOrNull()
                        (chapterNum != null && keyNum == chapterNum) ||
                            e.key.equals(chapterTitle, ignoreCase = true)
                    }?.value
                }
                is List<*> -> notesAny.filterIsInstance<String>().firstOrNull()
                is String -> notesAny
                else -> null
            }
            if (!directUrl.isNullOrBlank() && !isPlaceholderUrl(directUrl)) {
                open(directUrl, chapterTitle.ifBlank { "${rawSubject.ifBlank { normalizedSubject }} Notes" }, gradeKey, normalizedSubject)
            } else {
                Toast.makeText(this, "Notes Coming Soon", Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }

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
            R.id.btnCh7, R.id.btnCh8, R.id.btnCh9,
            R.id.btnCh10, R.id.btnCh11
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
        } else if (material.equals("Notes", ignoreCase = true) &&
            resolvedSubjectData["Notes"] is Map<*, *>
        ) {
            @Suppress("UNCHECKED_CAST")
            val notesMap = resolvedSubjectData["Notes"] as Map<String, String>

            ids.zip(notesMap.entries.toList()).forEach { (id, entry) ->
                val btn = findViewById<MaterialButton>(id)
                btn.visibility = View.VISIBLE
                btn.text = entry.key
                btn.setOnClickListener {
                    open(entry.value, "$displaySubject ${entry.key}", gradeKey, displaySubject)
                }
            }
        } else if (material.equals("Guess Papers", ignoreCase = true) &&
            resolvedSubjectData["Guess Papers"] is Map<*, *>
        ) {
            @Suppress("UNCHECKED_CAST")
            val guessPapersMap = resolvedSubjectData["Guess Papers"] as Map<String, String>
            val years = guessPapersMap.keys.sortedDescending()

            ids.zip(years).forEach { (id, year) ->
                val btn = findViewById<MaterialButton>(id)
                val url = guessPapersMap[year] ?: ""
                btn.visibility = View.VISIBLE
                btn.text = "Guess Paper $year"
                btn.setOnClickListener {
                    open(url, "$displaySubject Guess Paper $year", gradeKey, displaySubject)
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