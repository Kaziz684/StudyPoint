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
                "Essays" -> "https://drive.google.com/uc?export=download&id=1PCyGJOIiW50u_cJzPZFforcSOgTX0saG"
                "Translation (Urdu to English)" -> "https://drive.google.com/uc?export=download&id=16A3hQteRW5xZm_fW9XUGkfOYkiTdSYAI"
                else -> null
            }
        } else null
        if (grammarTopicPdf != null) {
            open(grammarTopicPdf, chapterTitle, rawGrade, normalizedSubject)
            finish()
            return
        }

        // Grammar topic PDFs (Class 9 English): open the topic's own PDF directly
        val grammar9Pdf = if (gradeKey == "9" && normalizedSubject == "English") {
            when (chapterTitle) {
                "Letters & Applications" -> "https://drive.google.com/uc?export=download&id=1XWxUOyeiHQrulh_5087OiKOiZtAvRMf5"
                "Stories" -> "https://drive.google.com/uc?export=download&id=1Fg1v_nyXwSep3OW1z6AO_paIG6tH0S-l"
                "Idioms & Phrases" -> "https://drive.google.com/uc?export=download&id=1Bg7s7K-5lWUkUSzLxzgQ2wBT1oB4zr4k"
                else -> null
            }
        } else null
        if (grammar9Pdf != null) {
            open(grammar9Pdf, chapterTitle, rawGrade, normalizedSubject)
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

        // Grammar topic PDF (Class 9 Urdu): all 8 Qawaid topics open the same notes PDF
        val urduGrammar9Topics = setOf("اسم و کلمہ", "فعل", "حرف", "مرکب ناقص", "مرکب مفید و جملہ و کلام", "جملے کی اقسام", "علم بیان", "علم بدیع")
        if (gradeKey == "9" && normalizedSubject == "Urdu" && chapterTitle in urduGrammar9Topics) {
            open("https://drive.google.com/uc?export=download&id=1KfytQVv1cmYdnAgu96zHIvchXfpXvlvi", chapterTitle, rawGrade, normalizedSubject)
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
                    "Notes" to mapOf(
                        "Chapter 1: The Saviour of Mankind" to "https://drive.google.com/uc?export=download&id=14rhPtsCewz_ajUSn6Rm6lT1TiB3upVGX",
                        "Chapter 2: Patriotism" to "https://drive.google.com/uc?export=download&id=1Vob7jkJyFEpmxAhOPnn9vGxa2QzYqNmx",
                        "Chapter 3: Daffodils" to "https://drive.google.com/uc?export=download&id=1eif3X0RP3L3FWsXlDthGcOQ8xbpoeBAX",
                        "Chapter 4: Hazrat Asma" to "https://drive.google.com/uc?export=download&id=1_b7DEP3XCT0WK7h7Gv3PF49vfboJoEGa",
                        "Chapter 5: Women Empowerment through Entrepreneurship" to "https://drive.google.com/uc?export=download&id=1x0TT1bGmQ3Y62ardnDPioOrKbznJnEBg",
                        "Chapter 6: The Value of Time" to "https://drive.google.com/uc?export=download&id=1UXgt_vk6Sw-sUXnUf3yo6eXGHyZsC26J",
                        "Chapter 7: If" to "https://drive.google.com/uc?export=download&id=13oCX70BdM8nKUBCfvcgEziW-SicVEUUr",
                        "Chapter 8: The Impact of Globalisation on Culture and Economy" to "https://drive.google.com/uc?export=download&id=1-UHzxOBNCT5gabAYt0Qz7vTMqyfHM_YD",
                        "Chapter 9: Quality Education: A Key to Success" to "https://drive.google.com/uc?export=download&id=1d6U5eApF-s8j_utSp8hWBDOeSJz1A0Xr",
                        "Chapter 10: The Silent Predator and the Majestic Prey – Snow Leopard and Markhor" to "https://drive.google.com/uc?export=download&id=14kJmMDhMlJy4SLnajmUCivyZBlKXfAM9"
                    ),
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
                    "Notes" to mapOf(
                        "Chapter 1: Physical Quantities and Measurements" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Chapter 2: Kinematics" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Chapter 3: Dynamics" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Chapter 4: Turning Effects of Force" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Chapter 5: Work, Energy and Power" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Chapter 6: Mechanical Properties of Matter" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Chapter 7: Thermal Properties of Matter" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Chapter 8: Magnetism" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Chapter 9: Nature of Science" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Bibliography" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Glossary" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Index" to "https://drive.google.com/uc?export=download&id=1afh-_Q1aVnIODgyVPcPesSH1Ei0Gprk2",
                        "Solved Numericals" to "https://drive.google.com/uc?export=download&id=16AniinbfCtgJXimAfI9l3Pa1mNCl8V63",
                        "Short Questions" to "https://drive.google.com/uc?export=download&id=1wM6JH4YcUITTZz4sP8JTuyypEKp2nAKO"
                    ),
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
                    "Notes" to mapOf(
                        "Chapter 1: States of Matter and Phase Changes" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 2: Atomic Structure" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 3: Chemical Bonding" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 4: Stoichiometry" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 5: Energetics" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 6: Equilibria" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 7: Acid Base Chemistry" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 8: Periodic Table and Periodicity" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 9: Group Properties and Elements" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 10: Environmental Chemistry" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 12: Empirical Data Collection and Analysis" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8",
                        "Chapter 13: Laboratory and Practical Skills" to "https://drive.google.com/uc?export=download&id=1nMi64ikxSxvq-FihYZwQ4OfuMGJbjmf8"
                    ),
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
                    "Notes" to mapOf(
                        "Chapter 1: Computational Systems" to "https://drive.google.com/uc?export=download&id=1VmnC5N-rBBE4TnlYK8HHPNbx9gaMLczr",
                        "Chapter 2: System Design and Troubleshooting" to "https://drive.google.com/uc?export=download&id=1WoKg5hRxgGeNnz621ixIVyb0_MX9I7eC",
                        "Chapter 3: Computer Networks" to "https://drive.google.com/uc?export=download&id=1t0mp-iUg6b27lRxgZ-wpw8OG3BQazqp9",
                        "Chapter 4: Computational Thinking" to "https://drive.google.com/uc?export=download&id=1bR6zIrociAR6ys4aU4Wf5m3KIZOWqWNT",
                        "Chapter 5: Web Development" to "https://drive.google.com/uc?export=download&id=1O10Ag7Y8a9DdeMNbUfZ2WW7MySqUY4cD",
                        "Chapter 6: Data Science and Data Gathering" to "https://drive.google.com/uc?export=download&id=13yJAWo8dI-WSCGkga2Tg66oWNa0udRW1",
                        "Chapter 7: Emerging Technologies" to "https://drive.google.com/uc?export=download&id=1lXprez6nATa-hedZ2PrSIkLQaDgSR3sN",
                        "Chapter 8: Ethical and Legal Concerns" to "https://drive.google.com/uc?export=download&id=1I11amI089DeOkEUZyqg0kNSEylbc4wAq",
                        "Chapter 9: Entrepreneurship" to "https://drive.google.com/uc?export=download&id=18rV3_KwVmXbbjDKuQvAtCaMol-ugxlYC"
                    ),
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
                    "Notes" to mapOf(
                        "فقیرانہ آئے صدا کر چلے" to "https://drive.google.com/uc?export=download&id=1_18B3lfDvwyO4Nrd9KViIzGoUDgjWrfQ",
                        "سن تو سہی جہاں میں ہے تیرافسانہ کیا" to "https://drive.google.com/uc?export=download&id=1RY2sgb5r2A1NapUIFCJjl0uz9YiptXKK",
                        "غم ہے یا خوشی ہے تُو" to "https://drive.google.com/uc?export=download&id=1TV5AcwyI4pYt_d1f4hdaZC7FkWEjyE6R",
                        "کاش طوفاں میں سفینے کو اتارا ہوتا" to "https://drive.google.com/uc?export=download&id=1FhRFrHFAmOOpamG7b80VeBwaXCz7Ldqi",
                        "حمد" to "https://drive.google.com/uc?export=download&id=1Xa1d-W16jRWDit8TYiwXNd8axMYzZU2o",
                        "نعت" to "https://drive.google.com/uc?export=download&id=14OJb6syIBHc3r_sqFpTK-pxds0rJ1N7u",
                        "محنت کی برکت" to "https://drive.google.com/uc?export=download&id=1YO8c7pYpOoLqmHmv_AzhlYUyLEFsmiiS",
                        "جاوید کے نام" to "https://drive.google.com/uc?export=download&id=1jal2JQbN5yfKwoWv6uuqcnewJAZc8_MN",
                        "پیامِ لطیف" to "https://drive.google.com/uc?export=download&id=1bAA2hNKRxzkv6mswpMHrps0tdIyIeS2f",
                        "کرکٹ اور مشاعرہ" to "https://drive.google.com/uc?export=download&id=1p6nhyWbVKli7BBJC9OwrHelUaxvh8FKz",
                        "اخلاقِ حسنہ" to "https://drive.google.com/uc?export=download&id=1kWTL-RAxvmV2IVXl6ZUqAkf1kMK7SvgD",
                        "اپنی مدد آپ" to "https://drive.google.com/uc?export=download&id=14SE7JjZb4-V4L1eBABPldMAeVSHwuG5T",
                        "کلیئم اور مرزا ظاہر دار بیگ" to "https://drive.google.com/uc?export=download&id=1Ofc9TjKAWAQXNPMF9FPyZzX-o4q5PhW9",
                        "نام دیو مالی" to "https://drive.google.com/uc?export=download&id=1aGdH_1eer4wLXG24LFUWX2ZDGPp1zlFs",
                        "آرام و سکون" to "https://drive.google.com/uc?export=download&id=1-Dcby2qqf4dWfQygTHeoQ9ulQ7QifgpM",
                        "کتبہ" to "https://drive.google.com/uc?export=download&id=1pBVj6PPUYPiTg6TCNl1Qu7j2PY6Eia_4",
                        "ابتدائی حساب" to "https://drive.google.com/uc?export=download&id=1tKjlxjdaG_pSlLGm53SWXySlL0UFfhkg",
                        "لڑی میں پروئے ہوئے منتظر" to "https://drive.google.com/uc?export=download&id=1hDZje7iI4Chep7nuFYpRGN_aR5ijFZcy",
                        "بھیڑیا" to "https://drive.google.com/uc?export=download&id=1nT1CBjvbfLajWRv2RLS1PWBcVSbEQcLd"
                    ),
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
                    "Notes" to mapOf(
                        "Complete Notes (All Babs)" to "https://drive.google.com/uc?export=download&id=1w9BBMkAFllJwaJFYEpz8-gZX2Myxe1Tt",
                        "Bab 1: قرآن مجید و حدیث نبوی ﷺ" to "https://drive.google.com/uc?export=download&id=1vvCq6sLK97OVVpY4xkKc15OJdm0lp-Xe",
                        "Bab 2: ایمانیات و عبادات" to "https://drive.google.com/uc?export=download&id=1Y9TOovy2aKIv9YMapk4Ni-bGheVOTOIr",
                        "Bab 3: سیرت نبوی ﷺ" to "https://drive.google.com/uc?export=download&id=18EsCawH0JyHoDRLRl4sktB4uPJs5FAh3",
                        "Bab 4: اخلاق و آداب" to "https://drive.google.com/uc?export=download&id=1LePsrDuGnGAmNrSqOrqqKFnzfKR4Jnw9",
                        "Bab 5: حسن معاملات و معاشرت" to "https://drive.google.com/uc?export=download&id=1pd2u_UwovZWvFNg4AhTs0Xfi-nAwaP4E",
                        "Bab 6: ہدایت کے سرچشمے اور مشاہیر اسلام" to "https://drive.google.com/uc?export=download&id=1sCXLt6DiJNPfdGn-OCaSw92II8qY8Wfp"
                    ),
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
                    "Notes" to mapOf(
                        "Ch 1: Surah Maryam" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 2: Surah Taha" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 3: Surah Al-Anbiya" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 4: Surah Al-Hajj" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 5: Surah Al-Furqan" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 6: Surah Ash-Shuara" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 7: Surah An-Naml" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 8: Surah Al-Qasas" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 9: Surah Al-Ankabut" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 10: Surah Ar-Rum" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 11: Surah Luqman" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 12: Surah As-Sajdah" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 13: Surah Saba" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 14: Surah Fatir" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 15: Surah Ya-Sin" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 16: Surah As-Saffat" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 17: Surah Sad" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY",
                        "Ch 18: Surah Al-Ahqaf" to "https://drive.google.com/uc?export=download&id=15Zf9924pwq2ZhmndqOi21LY5xRXtW3MY"
                    ),
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
                    "Notes" to mapOf(
                        "Chapter 1: Operating Systems: Structure & Services" to "https://drive.google.com/uc?export=download&id=1I2K3E23CpTRYmEjmOKTQrDEzOpfaivPQ",
                        "Chapter 2: System Recovery & Advanced Maintenance" to "https://drive.google.com/uc?export=download&id=12faGLHryoe4Z_mcvb-jJ7MVNe0DLCrtQ",
                        "Chapter 3: Introduction to Python Programming" to "https://drive.google.com/uc?export=download&id=1eH_9gthG0gJq-weVu-Tlea792MNwqtKm",
                        "Chapter 4: Control Structures in Python" to "https://drive.google.com/uc?export=download&id=1Dt38b2f8Nv6PVKDHYDaQn4JvXd7R_y_E",
                        "Chapter 5: Introduction to Data Science" to "https://drive.google.com/uc?export=download&id=14bMnFujS3Pif_h63hG8Cf7FqPIDptqxI",
                        "Chapter 6: Introduction to Artificial Intelligence (AI) & Machine Learning (ML)" to "https://drive.google.com/uc?export=download&id=1HcGg59koH5JtqIkn3npEgUKFaRGmQtOv",
                        "Chapter 7: Application of AI" to "https://drive.google.com/uc?export=download&id=1zNqNoJlUlEcZeJXAtdHKuQDNCYqCzy2V",
                        "Chapter 8: Digital Entrepreneurship" to "https://drive.google.com/uc?export=download&id=18vnLKldICo7PYwxXFtmP4Oey3Zk5Jx0o"
                    ),
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
                    "Notes" to mapOf(
                        "Chapter 1: Complex Numbers" to "https://drive.google.com/uc?export=download&id=1XFsH54ifPZLpMr0EOs9UQO-ChMV1VMh-",
                        "Chapter 2: Quadratic Equations and Inequalities" to "https://drive.google.com/uc?export=download&id=14k5vGkvIVYviiUm34ioP0q3U4scYH4Nn",
                        "Chapter 3: Matrices and Determinants" to "https://drive.google.com/uc?export=download&id=1FkYccJwmWFIRly3_RkkaheB6NP9My7td",
                        "Chapter 4: Functions and Graphs" to "https://drive.google.com/uc?export=download&id=1G-o1HBtGcPwiqViRezPk24pSXNhn6x_o",
                        "Chapter 5: Algebraic Fractions" to "COMING_SOON",
                        "Chapter 6: Vectors in Plane" to "COMING_SOON",
                        "Chapter 7: Trigonometry" to "COMING_SOON",
                        "Chapter 8: Chords and Arcs of a Circle" to "COMING_SOON",
                        "Chapter 9: Tangent and Angles of a Circle" to "COMING_SOON",
                        "Chapter 10: Practical Geometry of Circles" to "COMING_SOON",
                        "Chapter 11: Information Handling" to "COMING_SOON",
                        "Chapter 12: Probability" to "COMING_SOON",
                        "Answers" to "https://drive.google.com/uc?export=download&id=1FRbrsJ3oYSvHRlaEIcIMw2XPPUnTKw6C",
                        "Glossary" to "https://drive.google.com/uc?export=download&id=1GJp_VgcRuA1CroNmvWUxIqzIycSfAlNp",
                        "Symbols" to "https://drive.google.com/uc?export=download&id=1zEg8kXuqfzYFu6Iz2I6_ufE2yAJh0vU1",
                        "Symbols and Notations" to "https://drive.google.com/uc?export=download&id=1zEg8kXuqfzYFu6Iz2I6_ufE2yAJh0vU1"
                    ),
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
            // Only a real "Chapter/Unit N" reference counts. A bare digit inside
            // an internal section id (e.g. "mb3" for Symbols) must NOT be read as
            // a chapter number — that bug opened Chapter 3 notes for Symbols,
            // Chapter 1 for Answers and Chapter 2 for Glossary.
            val chapterRef = Regex("""(?:Chapter|Ch\.?|Unit)\s*(\d+)""", RegexOption.IGNORE_CASE)
            val chapterNum = chapterRef.find(chapterExtra)?.groupValues?.get(1)?.toIntOrNull()
                ?: chapterRef.find(chapterTitle)?.groupValues?.get(1)?.toIntOrNull()
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