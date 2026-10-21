package com.example.studypoint.practicetest

/**
 * Section model for the Paper Generator's chapter selection.
 *
 * Urdu books are portioned (Ghazal / Nazm / Asbaq / Grammar) and each portion
 * restarts its own chapter numbering in the sheet, so a flat "Chapter 1,
 * Chapter 1, Chapter 1..." list confuses everyone. This helper classifies a
 * chapter bucket (by its chapter NAME) into its portion so the selection UI
 * can group buckets under portion headers, exactly like the printed book.
 * Physics / Chemistry chapters additionally split into Theory vs Numericals.
 */
object PaperSections {

    const val GHAZAL = "حصہ غزل"
    const val NAZM = "حصہ نظم"
    const val ASBAQ = "اسباق"
    const val GRAMMAR = "Grammar (قواعد)"
    const val CHAPTERS = "Textbook Chapters"
    const val WRITING = "Grammar & Composition"
    const val OBJECTIVE = "Objective"
    const val SUBJECTIVE = "Subjective"
    const val SURAH = "Surahs"
    const val ABWAB = "ابواب"
    const val NUMERICALS = "Numericals"
    const val THEORY = "Theory Questions"

    /** Urdu-9 + Urdu-10 ghazal titles (first-misra names, as filed in the sheet). */
    private val ghazalNames = setOf(
        "فقیرانہ آئے صدا کر چلے",
        "سن تو سہی جہاں میں ہے تیرا فسانہ کیا",
        "غم ہے یا خوشی ہے تو",
        "کاش طوفاں میں سفینے کو اتارا ہوتا"
    )

    /** Urdu-9 + Urdu-10 nazm titles. */
    private val nazmNames = setOf(
        "حمد",
        "نعت",
        "جاوید کے نام",
        "پیامِ لطیف",
        "سویرے جو کل آنکھ میری کھلی"
    )

    /** Grammar bucket names (Ehsas Urdu Grammar, Class 9). */
    private val grammarNames = setOf(
        "اسم و کلمہ", "اسم", "کلمہ", "فعل", "حرف", "مرکب ناقص",
        "مرکب مفید و جملہ و کلام", "جملے کی اقسام", "علم بیان", "علم بدیع",
        "فرہنگ"
    )

    /** Extracts the chapter name from a selection label like "Chapter 3: محنت کی برکت". */
    fun nameOf(label: String): String =
        label.substringAfter(": ", label).trim()

    /** Urdu portion for a chapter bucket name, or null when it is an ordinary sabaq. */
    fun urduPortion(name: String): String {
        val n = name.trim()
        return when {
            n in ghazalNames -> GHAZAL
            n in nazmNames -> NAZM
            n in grammarNames -> GRAMMAR
            n.contains("Grammar Portion") || n.startsWith("تفہیم") || n.startsWith("مضامین") -> GRAMMAR
            else -> ASBAQ
        }
    }

    /** Ordered section list for the given subject (empty = keep the flat list). */
    fun sectionsFor(subject: String): List<String> = when {
        subject.equals("Urdu", ignoreCase = true) -> listOf(GHAZAL, NAZM, ASBAQ, GRAMMAR)
        subject.equals("English", ignoreCase = true) -> listOf(CHAPTERS, WRITING)
        subject.contains("Tarjuma", ignoreCase = true) -> listOf(SURAH)
        subject.equals("Islamiyat", ignoreCase = true) -> listOf(ABWAB)
        else -> emptyList()
    }

    /**
     * Grammar sub-group for an English Grammar & Composition row, following the
     * 9th/10th-class curriculum split: Objective = verb forms, spellings,
     * synonyms/antonyms and parts of speech; everything else written (letters,
     * stories, essays, dialogue, comprehension, translation, voice, pair of
     * words) is Subjective. No row is ever invented for a topic with no
     * pool — such a topic simply has no checkbox until its material arrives.
     */
    fun englishGrammarGroup(name: String): String {
        val n = name.lowercase()
        val objective = listOf(
            "verb", "spell", "synonym", "antonym", "grammar", "parts of speech"
        )
        return if (objective.any { n.contains(it) }) OBJECTIVE else SUBJECTIVE
    }

    /** Section a chapter bucket belongs to (English only here; Urdu handled by [urduPortion]). */
    fun englishSection(name: String): String {
        val n = name.lowercase()
        val writing = listOf(
            "letter", "stor", "idiom", "grammar", "voice", "verb", "essay",
            "paragraph", "dialogue", "application", "tafheem", "translation",
            // Class-10 grammar MCQ buckets filed as "Grammar: ..." rows.
            "direct", "indirect", "speech", "pair", "synonym", "antonym", "spell"
        )
        return if (writing.any { n.contains(it) } ||
            name.contains("تفہیم") || name.contains("مضامین")
        ) WRITING else CHAPTERS
    }

    /** Short display prefix for a selection row, e.g. "غزل" / "نظم" / "سبق" / "Grammar". */
    fun urduRowPrefix(portion: String): String = when (portion) {
        GHAZAL -> "غزل"
        NAZM -> "نظم"
        ASBAQ -> "سبق"
        else -> "Grammar"
    }

    private val numVerbs = listOf(
        "calculate", "find", "determine", "compute", "how much", "how many",
        "evaluate", "solve"
    )
    private val numUnits = listOf(
        " m/s", " km/h", " m ", " cm", " mm", " km", " kg", " g ", " n ", " j ",
        " w ", " v ", " a ", " ohm", "°c", " k", " hz", " pa", " mol", " l ",
        "newton", "joule", "watt", "volt", "ampere", "meter", "metre"
    )

    /**
     * Conservative numerical detector for Physics / Chemistry written
     * questions: needs a calculation verb, or a number together with a
     * physical unit. Plain theory questions that merely mention a number
     * stay Theory.
     */
    fun isNumerical(q: WrittenQuestion): Boolean {
        val t = " " + q.question.lowercase() + " "
        val hasDigit = t.any { it.isDigit() }
        if (!hasDigit) return false
        if (numVerbs.any { t.contains(it) }) return true
        return numUnits.any { t.contains(it) }
    }

    /** Subjects whose chapters split into Theory / Numericals rows. */
    fun splitsNumericals(subject: String): Boolean =
        subject.equals("Physics", ignoreCase = true) ||
            subject.equals("Chemistry", ignoreCase = true)

    /** Subjects printed right-to-left (Urdu script) in generated papers. */
    fun isRtlSubject(subject: String): Boolean =
        subject.equals("Urdu", ignoreCase = true) ||
            subject.equals("Islamiyat", ignoreCase = true) ||
            subject.contains("Tarjuma", ignoreCase = true)
}
