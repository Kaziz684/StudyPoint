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
        StudySection("urdu_ghazal", "حصہ غزل", ""),
        StudySection("urdu_nazm", "حصہ نظم", ""),
        StudySection("urdu_asbaq", "اسباق", ""),
        StudySection("urdu_farhang", "فرہنگ", "")
    )

    fun getUrduContent(grade: String, sectionId: String): List<StudySection> {
        return when (sectionId) {
            "urdu_grammar" -> when (grade) {
                "10" -> listOf(
                    StudySection("ug1", "مضامین", "Essays (Mazameen)", "chapter"),
                    StudySection("ug2", "تفہیم عبارات", "Comprehension (Tafheem-e-Ebaraat)", "chapter")
                )
                "9" -> listOf(
                    StudySection("ug1", "اسم و کلمہ", "قواعد", "chapter"),
                    StudySection("ug2", "فعل", "قواعد", "chapter"),
                    StudySection("ug3", "حرف", "قواعد", "chapter"),
                    StudySection("ug4", "مرکب ناقص", "قواعد", "chapter"),
                    StudySection("ug5", "مرکب مفید و جملہ و کلام", "قواعد", "chapter"),
                    StudySection("ug6", "جملے کی اقسام", "قواعد", "chapter"),
                    StudySection("ug7", "علم بیان", "قواعد", "chapter"),
                    StudySection("ug8", "علم بدیع", "قواعد", "chapter")
                )
                else -> emptyList()
            }
            "urdu_ghazal" -> when (grade) {
                "9" -> listOf(
                    StudySection("g1", "فقیرانہ آئے صدا کر چلے", "غزل — میر تقی میر", "chapter"),
                    StudySection("g2", "سن تو سہی جہاں میں ہے تیرافسانہ کیا", "غزل — خواجہ حیدر علی آتش", "chapter"),
                    StudySection("g3", "غم ہے یا خوشی ہے تُو", "غزل — ناصر کاظمی", "chapter"),
                    StudySection("g4", "کاش طوفاں میں سفینے کو اتارا ہوتا", "غزل — پروین شاکر", "chapter")
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
                    StudySection("n1", "حمد", "نظم — مظفر وارثی", "chapter"),
                    StudySection("n2", "نعت", "نظم — مولانا ظفر علی خان", "chapter"),
                    StudySection("n3", "محنت کی برکت", "نظم — مولانا الطاف حسین حالی", "chapter"),
                    StudySection("n4", "جاوید کے نام", "نظم — علامہ محمد اقبال", "chapter"),
                    StudySection("n5", "پیامِ لطیف", "نظم — شاہ عبداللطیف بھٹائی (مترجم: شیخ ایاز)", "chapter"),
                    StudySection("n6", "کرکٹ اور مشاعرہ", "نظم — ڈاکٹر ڈلاور نگار", "chapter")
                )
                "10" -> listOf(
                    StudySection("n1", "Chapter 1: حمد", "نظم — امیر مینائی", "chapter"),
                    StudySection("n2", "Chapter 2: نعت", "نظم", "chapter")
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
                    StudySection("s1", "اخلاقِ حسنہ", "سبق — سید سلیمان ندوی", "chapter"),
                    StudySection("s2", "اپنی مدد آپ", "سبق — سر سید احمد خان", "chapter"),
                    StudySection("s3", "کلیئم اور مرزا ظاہر دار بیگ", "سبق — ڈپٹی نذیر احمد", "chapter"),
                    StudySection("s4", "نام دیو مالی", "سبق — مولوی عبدالحق", "chapter"),
                    StudySection("s5", "آرام و سکون", "سبق — امتیاز علی تاج", "chapter"),
                    StudySection("s6", "کتبہ", "سبق — غلام عباس", "chapter"),
                    StudySection("s7", "ابتدائی حساب", "سبق — ابنِ انشاء", "chapter"),
                    StudySection("s8", "لڑی میں پروئے ہوئے منتظر", "سبق — رضا علی عابدی", "chapter"),
                    StudySection("s9", "بھیڑیا", "سبق — فاروق سرور", "chapter")
                )
                "10" -> listOf(
                    StudySection("s1", "Chapter 1: حمد", "نظم — امیر مینائی", "chapter"),
                    StudySection("s2", "Chapter 2: نعت", "نظم", "chapter"),
                    StudySection("s3", "Chapter 3: اخلاق نبوی خاتم النبیین ﷺ", "سبق", "chapter"),
                    StudySection("s4", "Chapter 4: سرسید کا بچپن", "سبق", "chapter"),
                    StudySection("s5", "Chapter 5: محسن محلہ", "سبق", "chapter"),
                    StudySection("s6", "Chapter 6: کفارہ", "سبق", "chapter"),
                    StudySection("s7", "Chapter 7: سویرے جو کل آنکھ میری کھلی", "سبق — پیٹرن بخاری", "chapter"),
                    StudySection("s8", "Chapter 8: دوستی کا پھل", "سبق — شیخ عقیل", "chapter"),
                    StudySection("s9", "Chapter 9: میرا گاؤں", "سبق — سید غلام التقلین نقوی", "chapter"),
                    StudySection("s10", "Chapter 10: بابل کے کھنڈر", "سبق — جمیل الدین عالی", "chapter"),
                    StudySection("s11", "Chapter 11: اولڈ ایج ہوم", "سبق — مرزا ادیب", "chapter"),
                    StudySection("s12", "Chapter 12: کچھ ذریعہ تعلیم کے باب میں", "سبق — ڈاکٹر ابواللیث صدیقی", "chapter")
                )
                else -> (1..12).map {
                    StudySection("s$it", "Sabaq $it", "سبق $it", "chapter")
                }
            }
            "urdu_farhang" -> when (grade) {
                "9" -> listOf(
                    StudySection("f1", "فرہنگ", "لغت — معانی و مترادفات", "chapter")
                )
                else -> listOf(
                    StudySection("f1", "فرہنگ", "لغت", "chapter")
                )
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
                    StudySection("c3", "Chapter 3: Daffodils", "", "chapter"),
                    StudySection("c4", "Chapter 4: Hazrat Asma", "", "chapter"),
                    StudySection("c5", "Chapter 5: Women Empowerment through Entrepreneurship", "", "chapter"),
                    StudySection("c6", "Chapter 6: The Value of Time", "", "chapter"),
                    StudySection("c7", "Chapter 7: If", "", "chapter"),
                    StudySection("c8", "Chapter 8: The Impact of Globalisation on Culture and Economy", "", "chapter"),
                    StudySection("c9", "Chapter 9: Quality Education: A Key to Success", "", "chapter"),
                    StudySection("c10", "Chapter 10: The Silent Predator and the Majestic Prey – Snow Leopard and Markhor", "", "chapter"),
                    StudySection("c11", "Chapter 11: The Dear Departed", "", "chapter")
                )
                "10" -> (1..13).map { StudySection("c$it", "Chapter $it", "", "chapter") }
                else -> (1..14).map { StudySection("c$it", "Chapter $it", "", "chapter") }
            }
            "eng_grammar" -> when (grade) {
                "10" -> listOf(
                    StudySection("gr3", "Direct & Indirect Speech", "", "chapter"),
                    StudySection("gr4", "Pair of Words", "", "chapter"),
                    StudySection("gr10", "Correct Use of Verb", "", "chapter"),
                    StudySection("gr11", "Essays", "", "chapter"),
                    StudySection("gr7", "Paragraphs", "", "chapter"),
                    StudySection("gr9", "Translation (Urdu to English)", "", "chapter")
                )
                "9" -> listOf(
                    StudySection("gr1", "Tenses", "", "chapter"),
                    StudySection("gr2", "Active & Passive Voice", "", "chapter"),
                    StudySection("gr5", "Letters & Applications", "", "chapter"),
                    StudySection("gr6", "Stories", "", "chapter"),
                    StudySection("gr8", "Sentence Correction", "", "chapter"),
                    StudySection("gr10", "Applications", "", "chapter")
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
            s.contains("math") -> getMathChapters(grade)
            s.contains("pak") -> getPakStudiesChapters(grade)
            s.contains("islamiyat") || s.contains("islamiat") -> getIslamiyatChapters(grade)
            s.contains("tarjuma") || s.contains("quran") -> getTarjumaChapters(grade)
            else -> emptyList()
        }
    }

    // Pak Studies — PCTB new syllabus (Grade 10: 8 chapters; cleaned notes received 2026-10-16)
    private fun getPakStudiesChapters(grade: String): List<StudySection> = when (grade) {
        "10" -> listOf(
            StudySection("p1", "Chapter 1: پاکستان کی نظریاتی اساس (حصہ اول)", "", "chapter"),
            StudySection("p2", "Chapter 2: تحریک پاکستان (حصہ اول)", "", "chapter"),
            StudySection("p3", "Chapter 3: پاکستان کی تاریخ (1947ء تاحال) اور پاکستان میں آئینی ترقی (حصہ اول)", "", "chapter"),
            StudySection("p4", "Chapter 4: پاکستان کا جغرافیہ (حصہ اول)", "", "chapter"),
            StudySection("p5", "Chapter 5: پاکستان اور بین الاقوامی امور", "", "chapter"),
            StudySection("p6", "Chapter 6: پاکستان کے وسائل اور معاشی ترقی (حصہ اول)", "", "chapter"),
            StudySection("p7", "Chapter 7: آبادی، معاشرہ اور پاکستان میں ثقافتی تنوع (حصہ اول)", "", "chapter"),
            StudySection("p8", "Chapter 8: پاکستان میں خواتین کو با اختیار بنانا (حصہ اول)", "", "chapter")
        )
        else -> emptyList()
    }

    // Mathematics — PCTB new syllabus (Grade 9: 13 units + backmatter)
    private fun getMathChapters(grade: String): List<StudySection> = when (grade) {
        "9" -> listOf(
            StudySection("m1", "Unit 1: Real Numbers", "حقیقی اعداد", "chapter"),
            StudySection("m2", "Unit 2: Logarithms", "لاگھرتھم", "chapter"),
            StudySection("m3", "Unit 3: Sets and Functions", "سیٹ اور فنکشنز", "chapter"),
            StudySection("m4", "Unit 4: Factorization and Algebraic Manipulation", "تجزیہ اور الجبری عمل", "chapter"),
            StudySection("m5", "Unit 5: Linear Equations and Inequalities", "لکیری مساواتیں اور غیر مساواتیں", "chapter"),
            StudySection("m6", "Unit 6: Trigonometry", "تکونیات", "chapter"),
            StudySection("m7", "Unit 7: Coordinate Geometry", "کوآرڈینیٹ جیومیٹری", "chapter"),
            StudySection("m8", "Unit 8: Logic", "منطق", "chapter"),
            StudySection("m9", "Unit 9: Similar Figures", "ہم شکل اشکال", "chapter"),
            StudySection("m10", "Unit 10: Graphs of Functions", "فنکشنز کے گراف", "chapter"),
            StudySection("m11", "Unit 11: Loci and Construction", "لوکی اور ہندساتی تعمیرات", "chapter"),
            StudySection("m12", "Unit 12: Information Handling", "معلومات کا انتظام", "chapter"),
            StudySection("m13", "Unit 13: Probability", "احتمال", "chapter"),
            StudySection("mb1", "Answers", "جوابات", "chapter"),
            StudySection("mb2", "Glossary", "فرہنگ", "chapter"),
            StudySection("mb3", "Symbols and Notations", "علامات", "chapter"),
            StudySection("mb4", "Logarithmic Tables", "لاگھرتھم کے جدول", "chapter")
        )
        "10" -> listOf(
            StudySection("m1", "Unit 1: Complex Numbers", "مخلوط اعداد", "chapter"),
            StudySection("m2", "Unit 2: Quadratic Equations and Inequalities", "دو درجی مساواتیں اور غیر مساواتیں", "chapter"),
            StudySection("m3", "Unit 3: Matrices and Determinants", "میٹرکس اور ڈیٹرمینینٹس", "chapter"),
            StudySection("m4", "Unit 4: Functions and Graphs", "فنکشنز اور گراف", "chapter"),
            StudySection("m5", "Unit 5: Algebraic Fractions", "الجبری کسریں", "chapter"),
            StudySection("m6", "Unit 6: Vectors in Plane", "مستوی میں ویکٹرز", "chapter"),
            StudySection("m7", "Unit 7: Trigonometry", "تکونیات", "chapter"),
            StudySection("m8", "Unit 8: Chords and Arcs of a Circle", "دائرے کے وتر اور قوس", "chapter"),
            StudySection("m9", "Unit 9: Tangent and Angles of a Circle", "دائرے کے مماس اور زاویے", "chapter"),
            StudySection("m10", "Unit 10: Practical Geometry of Circles", "دائروں کی عملی جیومیٹری", "chapter"),
            StudySection("m11", "Unit 11: Information Handling", "معلومات کا انتظام", "chapter"),
            StudySection("m12", "Unit 12: Probability", "احتمال", "chapter"),
            StudySection("mb1", "Answers", "جوابات", "chapter"),
            StudySection("mb2", "Glossary", "فرہنگ", "chapter"),
            StudySection("mb3", "Symbols", "علامات", "chapter")
        )
        else -> (1..13).map { StudySection("m$it", "Unit $it", "", "chapter") }
    }

    private fun getPhysicsChapters(grade: String): List<StudySection> = when (grade) {
        "9" -> listOf(
            StudySection("p1", "Chapter 1: Physical Quantities and Measurements", "", "chapter"),
            StudySection("p2", "Chapter 2: Kinematics", "", "chapter"),
            StudySection("p3", "Chapter 3: Dynamics", "", "chapter"),
            StudySection("p4", "Chapter 4: Turning Effects of Force", "", "chapter"),
            StudySection("p5", "Chapter 5: Work, Energy and Power", "", "chapter"),
            StudySection("p6", "Chapter 6: Mechanical Properties of Matter", "", "chapter"),
            StudySection("p7", "Chapter 7: Thermal Properties of Matter", "", "chapter"),
            StudySection("p8", "Chapter 8: Magnetism", "", "chapter"),
            StudySection("p9", "Chapter 9: Nature of Science", "", "chapter"),
            StudySection("pb4", "Solved Numericals", "", "chapter"),
            StudySection("pb5", "Short Questions", "", "chapter")
        )
        "10" -> listOf(
            StudySection("p10", "Chapter 10: Thermal Physics", "حرارتی طبیعات", "chapter"),
            StudySection("p11", "Chapter 11: Transfer of Thermal Energy", "حرارتی توانائی کی منتقلی", "chapter"),
            StudySection("p12", "Chapter 12: Waves", "لہریں", "chapter"),
            StudySection("p13", "Chapter 13: Sound", "آواز", "chapter"),
            StudySection("p14", "Chapter 14: Light", "روشنی", "chapter"),
            StudySection("p15", "Chapter 15: Electrostatics", "ساکن بجلی", "chapter"),
            StudySection("p16", "Chapter 16: Electricity", "بجلی", "chapter"),
            StudySection("p17", "Chapter 17: Electromagnetism", "برقی مقناطیسیت", "chapter"),
            StudySection("p18", "Chapter 18: Electromagnetic Induction and Electromagnetic Waves", "برقی مقناطیسی امالہ اور لہریں", "chapter"),
            StudySection("p19", "Chapter 19: Electronics", "الیکٹرانکس", "chapter"),
            StudySection("p20", "Chapter 20: Atomic and Nuclear Physics", "ایٹمی اور نیوکلیائی طبیعات", "chapter"),
            StudySection("p21", "Chapter 21: Space and Environment", "خلا اور ماحول", "chapter")
        )
        "11" -> (1..11).map { StudySection("p$it", "Chapter $it", "", "chapter") }
        "12" -> listOf(
            StudySection("p13", "Chapter 13: Thermal Physics", "", "chapter"),
            StudySection("p14", "Chapter 14: Simple Harmonic Motion", "", "chapter"),
            StudySection("p15", "Chapter 15: Physical Optics", "", "chapter"),
            StudySection("p16", "Chapter 16: Electrostatics", "", "chapter"),
            StudySection("p17", "Chapter 17: Alternating Current", "", "chapter"),
            StudySection("p18", "Chapter 18: Quantum Physics", "", "chapter"),
            StudySection("p19", "Chapter 19: Nuclear and Particle Physics", "", "chapter"),
            StudySection("p20", "Chapter 20: Medical Physics", "", "chapter"),
            StudySection("p21", "Chapter 21: Space and Environment", "", "chapter")
        )
        else -> (1..10).map { StudySection("p$it", "Chapter $it", "", "chapter") }
    }

    private fun getChemistryChapters(grade: String): List<StudySection> = when (grade) {
        "9" -> listOf(
            StudySection("c1", "Chapter 1: States of Matter and Phase Changes", "", "chapter"),
            StudySection("c2", "Chapter 2: Atomic Structure", "", "chapter"),
            StudySection("c3", "Chapter 3: Chemical Bonding", "", "chapter"),
            StudySection("c4", "Chapter 4: Stoichiometry", "", "chapter"),
            StudySection("c5", "Chapter 5: Energetics", "", "chapter"),
            StudySection("c6", "Chapter 6: Equilibria", "", "chapter"),
            StudySection("c7", "Chapter 7: Acid Base Chemistry", "", "chapter"),
            StudySection("c8", "Chapter 8: Periodic Table and Periodicity", "", "chapter"),
            StudySection("c9", "Chapter 9: Group Properties and Elements", "", "chapter"),
            StudySection("c10", "Chapter 10: Environmental Chemistry", "", "chapter"),
            StudySection("c11", "Chapter 11: Hydrocarbons", "", "chapter"),
            StudySection("c12", "Chapter 12: Empirical Data Collection and Analysis", "", "chapter"),
            StudySection("c13", "Chapter 13: Laboratory and Practical Skills", "", "chapter")
        )
        "10" -> listOf(
            StudySection("c14", "Chapter 14: States of Matter and Phase Changes", "", "chapter"),
            StudySection("c15", "Chapter 15: Stoichiometry", "", "chapter"),
            StudySection("c16", "Chapter 16: Electrochemistry", "", "chapter"),
            StudySection("c17", "Chapter 17: Reaction Kinetics", "", "chapter"),
            StudySection("c18", "Chapter 18: Salts", "", "chapter"),
            StudySection("c19", "Chapter 19: Nitrogen and Sulphur", "", "chapter"),
            StudySection("c20", "Chapter 20: Water", "", "chapter"),
            StudySection("c21", "Chapter 21: Organic Chemistry", "", "chapter"),
            StudySection("c22", "Chapter 22: Hydrocarbons", "", "chapter"),
            StudySection("c23", "Chapter 23: Monohydroxy Alkanes or Alcohols", "", "chapter"),
            StudySection("c24", "Chapter 24: Carboxylic Acids", "", "chapter"),
            StudySection("c25", "Chapter 25", "", "chapter"),
            StudySection("c26", "Chapter 26: Polymers", "", "chapter")
        )
        "11" -> (1..11).map { StudySection("c$it", "Chapter $it", "", "chapter") }
        else -> (1..10).map { StudySection("c$it", "Chapter $it", "", "chapter") }
    }

    private fun getBiologyChapters(grade: String): List<StudySection> = when (grade) {
        "9" -> listOf(
            StudySection("b1", "Chapter 1: The Science of Biology", "", "chapter"),
            StudySection("b2", "Chapter 2: Biodiversity", "", "chapter"),
            StudySection("b3", "Chapter 3: The Cell", "", "chapter"),
            StudySection("b4", "Chapter 4: Cell Cycle", "", "chapter"),
            StudySection("b5", "Chapter 5: Tissues, Organs, and Organ Systems", "", "chapter"),
            StudySection("b6", "Chapter 6: Biomolecules", "", "chapter"),
            StudySection("b7", "Chapter 7: Enzymes", "", "chapter"),
            StudySection("b8", "Chapter 8: Bioenergetics", "", "chapter"),
            StudySection("b9", "Chapter 9: Plant Physiology", "", "chapter"),
            StudySection("b10", "Chapter 10: Reproduction in Plants", "", "chapter"),
            StudySection("b11", "Chapter 11: Biostatistics", "", "chapter")
        )
        "10" -> listOf(
            StudySection("b1", "Ch 1: Human Digestive System", "", "chapter"),
            StudySection("b2", "Ch 2: Human Respiratory System", "", "chapter"),
            StudySection("b3", "Ch 3: Human Blood Circulatory System", "", "chapter"),
            StudySection("b4", "Ch 4: Human Urinary System", "", "chapter"),
            StudySection("b5", "Ch 5: Coordination", "", "chapter"),
            StudySection("b6", "Ch 6: Reproduction", "", "chapter"),
            StudySection("b7", "Ch 7: Inheritance", "", "chapter"),
            StudySection("b8", "Ch 8: Biotechnology", "", "chapter"),
            StudySection("b9", "Ch 9: Diseases and Immunity", "", "chapter"),
            StudySection("b10", "Ch 10: Evolution", "", "chapter")
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
            StudySection("cs1", "Ch 1: Introduction to Systems", "", "chapter"),
            StudySection("cs2", "Ch 2: Number Systems", "", "chapter"),
            StudySection("cs3", "Ch 3: Digital Systems and Logic Design", "", "chapter"),
            StudySection("cs4", "Ch 4: System Troubleshooting", "", "chapter"),
            StudySection("cs5", "Ch 5: Software System", "", "chapter"),
            StudySection("cs6", "Ch 6: Introduction to Computer Networks", "", "chapter"),
            StudySection("cs7", "Ch 7: Computational Thinking", "", "chapter"),
            StudySection("cs8", "Ch 8: Web Development with HTML, CSS and Java-Script", "", "chapter"),
            StudySection("cs9", "Ch 9: Data Science and Data Gathering", "", "chapter"),
            StudySection("cs10", "Ch 10: Emerging Technologies in Computer Science", "", "chapter"),
            StudySection("cs11", "Ch 11: Ethical, Social and Legal Concerns in Computer Usage", "", "chapter"),
            StudySection("cs12", "Ch 12: Entrepreneurship in Digital Age", "", "chapter"),
            StudySection("cs13", "Ch 13: Important Shortcut Keys", "", "chapter")
        )
        "10" -> listOf(
            StudySection("cs1", "Ch 1: Operating Systems: Structure & Services", "", "chapter"),
            StudySection("cs2", "Ch 2: System Recovery & Advanced Maintenance", "", "chapter"),
            StudySection("cs3", "Ch 3: Introduction to Python Programming", "", "chapter"),
            StudySection("cs4", "Ch 4: Control Structures in Python", "", "chapter"),
            StudySection("cs5", "Ch 5: Introduction to Data Science", "", "chapter"),
            StudySection("cs6", "Ch 6: Introduction to Artificial Intelligence (AI) & Machine Learning (ML)", "", "chapter"),
            StudySection("cs7", "Ch 7: Application of AI", "", "chapter"),
            StudySection("cs8", "Ch 8: Digital Entrepreneurship", "", "chapter")
        )
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
               s.contains("biology") || s.contains("bio") || s.contains("computer") ||
               s.contains("math") || s.contains("pak") ||
               s.contains("islamiyat") || s.contains("islamiat") ||
               s.contains("tarjuma") || s.contains("quran")
    }

    // Islamiyat — AYAAN notes book (Grade 9: 6 abwaab, printed titles)
    private fun getIslamiyatChapters(grade: String): List<StudySection> = when (grade) {
        "9" -> listOf(
            StudySection("i1", "Bab 1: قرآن مجید و حدیث نبوی ﷺ", "", "chapter"),
            StudySection("i2", "Bab 2: ایمانیات و عبادات", "", "chapter"),
            StudySection("i3", "Bab 3: سیرت نبوی ﷺ", "", "chapter"),
            StudySection("i4", "Bab 4: اخلاق و آداب", "", "chapter"),
            StudySection("i5", "Bab 5: حسن معاملات و معاشرت", "", "chapter"),
            StudySection("i6", "Bab 6: ہدایت کے سرچشمے اور مشاہیر اسلام", "", "chapter")
        )
        else -> emptyList()
    }

    // Tarjuma-tul-Quran — PCTB new syllabus (Grade 10: 15 surahs, list from his book/ustad360 photo 2026-10-16)
    private fun getTarjumaChapters(grade: String): List<StudySection> = when (grade) {
        "10" -> listOf(
            StudySection("t1", "Ch 1: Surah Al Anam", "سورة الأنعام", "chapter"),
            StudySection("t2", "Ch 2: Surah A'araf", "سورة الأعراف", "chapter"),
            StudySection("t3", "Ch 3: Surah Yunus", "سورة يونس", "chapter"),
            StudySection("t4", "Ch 4: Surah Hud", "سورة هود", "chapter"),
            StudySection("t5", "Ch 5: Surah Ra'ad", "سورة الرعد", "chapter"),
            StudySection("t6", "Ch 6: Surah Ibrahim", "سورة إبراهيم", "chapter"),
            StudySection("t7", "Ch 7: Surah Hijr", "سورة الحجر", "chapter"),
            StudySection("t8", "Ch 8: Surah Nahl", "سورة النحل", "chapter"),
            StudySection("t9", "Ch 9: Surah Isra / Bani Israel", "سورة الإسراء", "chapter"),
            StudySection("t10", "Ch 10: Surah Kahf", "سورة الكهف", "chapter"),
            StudySection("t11", "Ch 11: Surah Mominoon", "سورة المؤمنون", "chapter"),
            StudySection("t12", "Ch 12: Surah Zumar", "سورة الزمر", "chapter"),
            StudySection("t13", "Ch 13: Surah Ghafir / Momin", "سورة غافر", "chapter"),
            StudySection("t14", "Ch 14: Surah Fussilat / Hamim Sajdah", "سورة فصلت", "chapter"),
            StudySection("t15", "Ch 15: Surah Shura", "سورة الشورى", "chapter")
        )
        "9" -> listOf(
            StudySection("t1", "Ch 1: Surah Maryam", "سورة مريم", "chapter"),
            StudySection("t2", "Ch 2: Surah Taha", "سورة طه", "chapter"),
            StudySection("t3", "Ch 3: Surah Al-Anbiya", "سورة الأنبياء", "chapter"),
            StudySection("t4", "Ch 4: Surah Al-Hajj", "سورة الحج", "chapter"),
            StudySection("t5", "Ch 5: Surah Al-Furqan", "سورة الفرقان", "chapter"),
            StudySection("t6", "Ch 6: Surah Ash-Shuara", "سورة الشعراء", "chapter"),
            StudySection("t7", "Ch 7: Surah An-Naml", "سورة النمل", "chapter"),
            StudySection("t8", "Ch 8: Surah Al-Qasas", "سورة القصص", "chapter"),
            StudySection("t9", "Ch 9: Surah Al-Ankabut", "سورة العنكبوت", "chapter"),
            StudySection("t10", "Ch 10: Surah Ar-Rum", "سورة الروم", "chapter"),
            StudySection("t11", "Ch 11: Surah Luqman", "سورة لقمان", "chapter"),
            StudySection("t12", "Ch 12: Surah As-Sajdah", "سورة السجدة", "chapter"),
            StudySection("t13", "Ch 13: Surah Saba", "سورة سبأ", "chapter"),
            StudySection("t14", "Ch 14: Surah Fatir", "سورة فاطر", "chapter"),
            StudySection("t15", "Ch 15: Surah Ya-Sin", "سورة يس", "chapter"),
            StudySection("t16", "Ch 16: Surah As-Saffat", "سورة الصافات", "chapter"),
            StudySection("t17", "Ch 17: Surah Sad", "سورة ص", "chapter"),
            StudySection("t18", "Ch 18: Surah Al-Ahqaf", "سورة الأحقاف", "chapter")
        )
        else -> (1..15).map { StudySection("t$it", "Chapter $it", "", "chapter") }
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
