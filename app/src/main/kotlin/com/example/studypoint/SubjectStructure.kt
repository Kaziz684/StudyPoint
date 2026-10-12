package com.example.studypoint

/**
 * Subject structure data: sections and chapters per class and subject.
 * Based on PCTB new syllabus 2025-26.
 */

// A section or chapter item
data class StudySection(
    val id: String,           // unique id e.g. "urdu_ghazal"
    val title: String,        // display title
    val titleUrdu: String = "", // Urdu title if applicable
    val type: String = "section" // "section" or "chapter"
)

object SubjectStructure {

    // ==================== URDU ====================
    // 3 sections: Hissa-e-Ghazal, Hissa-e-Nazm, Asbaq
    fun getUrduSections(grade: String): List<StudySection> = listOf(
        StudySection("urdu_ghazal", "Hissa-e-Ghazal", "حصہ غزل"),
        StudySection("urdu_nazm", "Hissa-e-Nazm", "حصہ نظم"),
        StudySection("urdu_asbaq", "Asbaq", "اسباق")
    )

    fun getUrduContent(grade: String, sectionId: String): List<StudySection> {
        return when (sectionId) {
            "urdu_ghazal" -> when (grade) {
                "9" -> listOf(
                    StudySection("g1", "Hasti Apni Habab Ki Si Hai", "ہستی اپنی حباب کی سی ہے", "chapter"),
                    StudySection("g2", "Rukh-o-Zulf Par Jaan Khoya Hai", "رخ و زلف پر جان کھویا ہے", "chapter"),
                    StudySection("g3", "Dil-e-Nadan Tujhe Hua Kya Hai", "دل ناداں تجھے ہوا کیا ہے", "chapter"),
                    StudySection("g4", "Lagta Nahin Hai Dil Mera", "لگتا نہیں ہے دل میرا", "chapter")
                )
                else -> listOf(
                    StudySection("g1", "Ghazal 1", "غزل ۱", "chapter"),
                    StudySection("g2", "Ghazal 2", "غزل ۲", "chapter"),
                    StudySection("g3", "Ghazal 3", "غزل ۳", "chapter"),
                    StudySection("g4", "Ghazal 4", "غزل ۴", "chapter")
                )
            }
            "urdu_nazm" -> when (grade) {
                "9" -> listOf(
                    StudySection("n1", "Hamd", "حمد", "chapter"),
                    StudySection("n2", "Naat", "نعت", "chapter"),
                    StudySection("n3", "Barsaat Ki Baharain", "برسات کی بہاریں", "chapter"),
                    StudySection("n4", "Pewasta Reh Shajar Se", "پیوستہ رہ شجر سے", "chapter")
                )
                else -> listOf(
                    StudySection("n1", "Nazm 1", "نظم ۱", "chapter"),
                    StudySection("n2", "Nazm 2", "نظم ۲", "chapter"),
                    StudySection("n3", "Nazm 3", "نظم ۳", "chapter"),
                    StudySection("n4", "Nazm 4", "نظم ۴", "chapter")
                )
            }
            "urdu_asbaq" -> when (grade) {
                "9" -> listOf(
                    StudySection("s1", "Hijrat-e-Nabwi (SAW)", "ہجرت نبوی ﷺ", "chapter"),
                    StudySection("s2", "Mirza Ghalib Ke Aadaat-o-Khasail", "مرزا غالب کے عادات و خصائل", "chapter"),
                    StudySection("s3", "Kahili", "کاہلی", "chapter"),
                    StudySection("s4", "Shairon Ke Lateefay", "شاعروں کے لطیفے", "chapter"),
                    StudySection("s5", "Nasoo Aur Saleem Ki Guftugu", "نسو اور سلیم کی گفتگو", "chapter"),
                    StudySection("s6", "Panchaiyat", "پنچایت", "chapter"),
                    StudySection("s7", "Araam-o-Sukoon", "آرام و سکون", "chapter"),
                    StudySection("s8", "Lahu Aur Qaleen", "لہو اور قالین", "chapter"),
                    StudySection("s9", "Imtehaan", "امتحان", "chapter"),
                    StudySection("s10", "Mulki Parinday", "ملکی پرندے", "chapter"),
                    StudySection("s11", "Qadr-e-Ayaz", "قدر ایاز", "chapter"),
                    StudySection("s12", "Hausla Na Haaro", "حوصلہ نہ ہارو", "chapter")
                )
                else -> (1..12).map {
                    StudySection("s$it", "Sabaq $it", "سبق $it", "chapter")
                }
            }
            else -> emptyList()
        }
    }

    // ==================== ENGLISH ====================
    // 3 sections: Chapter Notes, Grammar, Vocabulary
    fun getEnglishSections(grade: String): List<StudySection> = listOf(
        StudySection("eng_chapters", "Chapter Notes", "اسباق کے نوٹس"),
        StudySection("eng_grammar", "Grammar", "گرامر"),
        StudySection("eng_vocab", "Vocabulary", "ذخیرہ الفاظ")
    )

    fun getEnglishContent(grade: String, sectionId: String): List<StudySection> {
        return when (sectionId) {
            "eng_chapters" -> when (grade) {
                "9" -> listOf(
                    StudySection("c1", "Chapter 1: The Saviour of Mankind", "", "chapter"),
                    StudySection("c2", "Chapter 2: Patriotism", "", "chapter"),
                    StudySection("c3", "Chapter 3: Media and Its Impact", "", "chapter"),
                    StudySection("c4", "Chapter 4: Hazrat Asma (R.A)", "", "chapter"),
                    StudySection("c5", "Chapter 5: Daffodils", "", "chapter"),
                    StudySection("c6", "Chapter 6: The Quaid's Vision", "", "chapter"),
                    StudySection("c7", "Chapter 7: Sultan Ahmad Mosque", "", "chapter"),
                    StudySection("c8", "Chapter 8: Stopping by Woods", "", "chapter"),
                    StudySection("c9", "Chapter 9: All is Not Lost", "", "chapter"),
                    StudySection("c10", "Chapter 10: Drug Addiction", "", "chapter"),
                    StudySection("c11", "Chapter 11: Noise in the Environment", "", "chapter"),
                    StudySection("c12", "Chapter 12: Three Days to See", "", "chapter")
                )
                "10" -> (1..13).map { StudySection("c$it", "Chapter $it", "", "chapter") }
                else -> (1..14).map { StudySection("c$it", "Chapter $it", "", "chapter") }
            }
            "eng_grammar" -> when (grade) {
                "9", "10" -> listOf(
                    StudySection("gr1", "Tenses", "", "chapter"),
                    StudySection("gr2", "Active & Passive Voice", "", "chapter"),
                    StudySection("gr3", "Direct & Indirect Speech", "", "chapter"),
                    StudySection("gr4", "Pair of Words", "", "chapter"),
                    StudySection("gr5", "Letters & Applications", "", "chapter"),
                    StudySection("gr6", "Stories", "", "chapter"),
                    StudySection("gr7", "Paragraphs", "", "chapter"),
                    StudySection("gr8", "Sentence Correction", "", "chapter"),
                    StudySection("gr9", "Translation (Urdu to English)", "", "chapter")
                )
                else -> listOf(
                    StudySection("gr1", "Tenses", "", "chapter"),
                    StudySection("gr2", "Active & Passive Voice", "", "chapter"),
                    StudySection("gr3", "Direct & Indirect Speech", "", "chapter"),
                    StudySection("gr4", "Pair of Words", "", "chapter"),
                    StudySection("gr5", "Essays", "", "chapter"),
                    StudySection("gr6", "Letters & Applications", "", "chapter"),
                    StudySection("gr7", "Comprehension", "", "chapter"),
                    StudySection("gr8", "Translation", "", "chapter")
                )
            }
            "eng_vocab" -> listOf(
                StudySection("v1", "Synonyms", "", "chapter"),
                StudySection("v2", "Antonyms", "", "chapter"),
                StudySection("v3", "Idioms & Phrases", "", "chapter"),
                StudySection("v4", "One Word Substitution", "", "chapter")
            )
            else -> emptyList()
        }
    }

    // ==================== SCIENCES: Chapter-wise Notes ====================
    fun getScienceChapters(grade: String, subject: String): List<StudySection> {
        val s = subject.lowercase()
        return when {
            s.contains("physics") -> getPhysicsChapters(grade)
            s.contains("chemistry") -> getChemistryChapters(grade)
            s.contains("biology") || s.contains("bio") -> getBiologyChapters(grade)
            s.contains("computer") -> getComputerChapters(grade)
            else -> emptyList()
        }
    }

    private fun getPhysicsChapters(grade: String): List<StudySection> = when (grade) {
        "9" -> listOf(
            StudySection("p1", "Ch 1: Physical Quantities & Measurements", "", "chapter"),
            StudySection("p2", "Ch 2: Kinematics", "", "chapter"),
            StudySection("p3", "Ch 3: Dynamics", "", "chapter"),
            StudySection("p4", "Ch 4: Turning Effects of Forces", "", "chapter"),
            StudySection("p5", "Ch 5: Work, Energy and Power", "", "chapter"),
            StudySection("p6", "Ch 6: Mechanical Properties of Matter", "", "chapter"),
            StudySection("p7", "Ch 7: Thermal Properties of Matter", "", "chapter"),
            StudySection("p8", "Ch 8: Magnetism", "", "chapter"),
            StudySection("p9", "Ch 9: Nature of Science", "", "chapter")
        )
        "10" -> listOf(
            StudySection("p1", "Ch 1: Simple Harmonic Motion & Waves", "", "chapter"),
            StudySection("p2", "Ch 2: Sound", "", "chapter"),
            StudySection("p3", "Ch 3: Geometrical Optics", "", "chapter"),
            StudySection("p4", "Ch 4: Electrostatics", "", "chapter"),
            StudySection("p5", "Ch 5: Current Electricity", "", "chapter"),
            StudySection("p6", "Ch 6: Electromagnetism", "", "chapter"),
            StudySection("p7", "Ch 7: Basic Electronics", "", "chapter"),
            StudySection("p8", "Ch 8: Information Technology", "", "chapter"),
            StudySection("p9", "Ch 9: Atomic & Nuclear Physics", "", "chapter")
        )
        "11" -> (1..11).map { StudySection("p$it", "Chapter $it", "", "chapter") }
        else -> (1..10).map { StudySection("p$it", "Chapter $it", "", "chapter") }
    }

    private fun getChemistryChapters(grade: String): List<StudySection> = when (grade) {
        "9" -> listOf(
            StudySection("c1", "Ch 1: States of Matter & Phase Changes", "", "chapter"),
            StudySection("c2", "Ch 2: Atomic Structure", "", "chapter"),
            StudySection("c3", "Ch 3: Chemical Bonding", "", "chapter"),
            StudySection("c4", "Ch 4: Stoichiometry", "", "chapter"),
            StudySection("c5", "Ch 5: Energetics", "", "chapter"),
            StudySection("c6", "Ch 6: Equilibria", "", "chapter"),
            StudySection("c7", "Ch 7: Acid-Base Chemistry", "", "chapter"),
            StudySection("c8", "Ch 8: Periodic Table & Periodicity", "", "chapter"),
            StudySection("c9", "Ch 9: Group Properties & Elements", "", "chapter"),
            StudySection("c10", "Ch 10: Environmental Chemistry", "", "chapter"),
            StudySection("c11", "Ch 11: Hydrocarbons", "", "chapter"),
            StudySection("c12", "Ch 12: Empirical Data Collection", "", "chapter"),
            StudySection("c13", "Ch 13: Laboratory Skills", "", "chapter")
        )
        "10" -> (1..9).map { StudySection("c$it", "Chapter $it", "", "chapter") }
        "11" -> (1..11).map { StudySection("c$it", "Chapter $it", "", "chapter") }
        else -> (1..10).map { StudySection("c$it", "Chapter $it", "", "chapter") }
    }

    private fun getBiologyChapters(grade: String): List<StudySection> = when (grade) {
        "9" -> listOf(
            StudySection("b1", "Ch 1: Introduction to Biology", "", "chapter"),
            StudySection("b2", "Ch 2: Solving a Biological Problem", "", "chapter"),
            StudySection("b3", "Ch 3: Biodiversity", "", "chapter"),
            StudySection("b4", "Ch 4: Cells & Tissues", "", "chapter"),
            StudySection("b5", "Ch 5: Cell Cycle", "", "chapter"),
            StudySection("b6", "Ch 6: Enzymes", "", "chapter"),
            StudySection("b7", "Ch 7: Bioenergetics", "", "chapter"),
            StudySection("b8", "Ch 8: Nutrition", "", "chapter"),
            StudySection("b9", "Ch 9: Transport", "", "chapter")
        )
        "10" -> listOf(
            StudySection("b1", "Ch 1: Gaseous Exchange", "", "chapter"),
            StudySection("b2", "Ch 2: Homeostasis", "", "chapter"),
            StudySection("b3", "Ch 3: Coordination & Control", "", "chapter"),
            StudySection("b4", "Ch 4: Support & Movement", "", "chapter"),
            StudySection("b5", "Ch 5: Reproduction", "", "chapter"),
            StudySection("b6", "Ch 6: Inheritance", "", "chapter"),
            StudySection("b7", "Ch 7: Man & His Environment", "", "chapter"),
            StudySection("b8", "Ch 8: Biotechnology", "", "chapter"),
            StudySection("b9", "Ch 9: Pharmacology", "", "chapter")
        )
        "11" -> listOf(
            StudySection("b1", "Ch 1: Introduction to Biology", "", "chapter"),
            StudySection("b2", "Ch 2: The Cell", "", "chapter"),
            StudySection("b3", "Ch 3: Biological Molecules", "", "chapter"),
            StudySection("b4", "Ch 4: Enzymes", "", "chapter"),
            StudySection("b5", "Ch 5: Bioenergetics", "", "chapter"),
            StudySection("b6", "Ch 6: Nutrition", "", "chapter"),
            StudySection("b7", "Ch 7: Transport", "", "chapter"),
            StudySection("b8", "Ch 8: Respiration", "", "chapter"),
            StudySection("b9", "Ch 9: Excretion", "", "chapter"),
            StudySection("b10", "Ch 10: Coordination & Control", "", "chapter"),
            StudySection("b11", "Ch 11: Reproduction", "", "chapter"),
            StudySection("b12", "Ch 12: Genetics", "", "chapter"),
            StudySection("b13", "Ch 13: Evolution", "", "chapter"),
            StudySection("b14", "Ch 14: Ecology", "", "chapter")
        )
        else -> (1..12).map { StudySection("b$it", "Chapter $it", "", "chapter") }
    }

    private fun getComputerChapters(grade: String): List<StudySection> = when (grade) {
        "9" -> listOf(
            StudySection("cs1", "Ch 1: Computational Systems", "", "chapter"),
            StudySection("cs2", "Ch 2: System Design and Troubleshooting", "", "chapter"),
            StudySection("cs3", "Ch 3: Computer Networks", "", "chapter"),
            StudySection("cs4", "Ch 4: Computational Thinking", "", "chapter"),
            StudySection("cs5", "Ch 5: Web Development", "", "chapter"),
            StudySection("cs6", "Ch 6: Data Science and Data Gathering", "", "chapter"),
            StudySection("cs7", "Ch 7: Emerging Technologies", "", "chapter"),
            StudySection("cs8", "Ch 8: Ethical and Legal Concerns", "", "chapter"),
            StudySection("cs9", "Ch 9: Entrepreneurship", "", "chapter")
        )
        "10" -> (1..6).map { StudySection("cs$it", "Chapter $it", "", "chapter") }
        "11" -> (1..8).map { StudySection("cs$it", "Chapter $it", "", "chapter") }
        else -> (1..8).map { StudySection("cs$it", "Chapter $it", "", "chapter") }
    }

    // ==================== Helpers ====================
    fun hasSections(subject: String): Boolean {
        val s = subject.lowercase()
        return s.contains("urdu") || s.contains("english")
    }

    fun hasChapterNotes(subject: String): Boolean {
        val s = subject.lowercase()
        return s.contains("physics") || s.contains("chemistry") ||
               s.contains("biology") || s.contains("bio") || s.contains("computer")
    }

    fun getSections(grade: String, subject: String): List<StudySection> {
        val s = subject.lowercase()
        return when {
            s.contains("urdu") -> getUrduSections(grade)
            s.contains("english") -> getEnglishSections(grade)
            else -> emptyList()
        }
    }

    fun getSectionContent(grade: String, subject: String, sectionId: String): List<StudySection> {
        val s = subject.lowercase()
        return when {
            s.contains("urdu") -> getUrduContent(grade, sectionId)
            s.contains("english") -> getEnglishContent(grade, sectionId)
            else -> emptyList()
        }
    }
}
