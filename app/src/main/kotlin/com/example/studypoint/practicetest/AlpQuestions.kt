package com.example.studypoint.practicetest

/**
 * ALP (Accelerated Learning Programme / Smart Syllabus 2027) question ids.
 *
 * The ids are the questions from the ALP test papers the teacher sends
 * (Rising Star, 18 Nov 2026): Mathematics 10th (MCQs + Shorts) and
 * Pak Studies 10th (MCQs). They are scoped by class + subject so an id
 * can never match the wrong class — sheet ids overlap across classes
 * (e.g. id 96 exists in Class 9th and in Class 10th). MCQ ids live in
 * alpIds, Shorts-tab ids in alpShortIds.
 *
 * Add one entry per subject as its ALP paper arrives. The Paper Generator
 * colours ALP questions green and the rest red in manual mode, and the ALP
 * toggle shows only ALP questions so an ALP paper can be built.
 */
object AlpQuestions {

    /** class digits -> normalised sheet subject -> ALP question ids. */
    private val alpIds: Map<Pair<String, String>, Set<Int>> = mapOf(
        // Class 10th, Math (sheet subject "Math")
        ("10" to "math") to setOf(
            // Paper 1: Ch1 Complex Numbers (Q1-10)
            96, 97, 98, 99, 100, 101, 102, 103, 104, 105,
            // Paper 1: Ch2 Quadratic Equations (Q11-20)
            106, 107, 108, 109, 110, 111, 112, 113, 114, 115,
            // Paper 2: Ch4 Functions and Graphs (Q1-10)
            126, 127, 128, 129, 130, 131, 132, 133, 134, 135,
            // Paper 2: Ch5 Algebraic Fractions (Q11-12)
            136, 137,
            // Duplicate rows of the same questions in the sheet
            2985, 2986, 2987, 2988, 2989, 2990, 2991, 2992,
            3003, 3004, 3005, 3006, 3007, 3008, 3009, 3010,
            // Paper 4 (18 Nov 2026): Ch1 Complex Numbers, 20 new MCQs
            4144, 4145, 4146, 4147, 4148, 4149, 4150, 4151, 4152, 4153,
            4154, 4155, 4156, 4157, 4158, 4159, 4160, 4161, 4162, 4163
        ),
        // Class 10th, Pak Studies (sheet subject "Pak Studies")
        ("10" to "pak studies") to setOf(
            // ALP paper 18 Nov 2026: Ch1 Nazriya-e-Pakistan (Q1-8, Q20 already in sheet)
            2570, 2571, 2572, 2573, 2574, 2575, 2576, 2577, 2582,
            // Same paper: Q10-19, 10 new MCQs (Q9 held, key unconfirmed)
            4164, 4165, 4166, 4167, 4168, 4169, 4170, 4171, 4172, 4173
        )
    )

    /** class digits -> normalised sheet subject -> ALP SHORT question ids
     *  (Shorts tab; same key scheme as alpIds, ids are Shorts-tab ids). */
    private val alpShortIds: Map<Pair<String, String>, Set<Int>> = mapOf(
        // Class 10th, Math: Ch1 Complex Numbers shorts (Section-II paper, 18 Nov 2026)
        ("10" to "math") to setOf(
            // Exercise 1.1 Q1-3 + Exercise 1.2 Q1-3 (already in the sheet)
            1990, 1991, 1992, 1993, 1994, 1995, 1996, 1997, 1998, 1999,
            2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009,
            2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019,
            // Same paper: 5 new shorts (i7, i8, i17, i-25, x/y Q22)
            11436, 11437, 11438, 11439, 11440
        ),
        // Class 10th, Pak Studies: Ch1 Nazriya-e-Pakistan shorts (full paper, 18 Nov 2026)
        ("10" to "pak studies") to setOf(
            // Section-II Q1 (i)-(xiii), all already in the sheet
            1793, 1794, 1795, 1796, 1797, 1798, 1799, 1800, 1801, 1802,
            1803, 1804, 1805
        )
    )

    /** class digits -> normalised sheet subject -> ALP LONG question ids
     *  (Longs tab; same key scheme, ids are Longs-tab ids). */
    private val alpLongIds: Map<Pair<String, String>, Set<Int>> = mapOf(
        // Class 10th, Pak Studies: Ch1 Nazriya-e-Pakistan longs (full paper, 18 Nov 2026)
        ("10" to "pak studies") to setOf(
            // Section-III Q2 (a)-(e), all already in the sheet
            449, 450, 451, 452, 453
        )
    )

    /** Normalise a subject name to the sheet's spelling ("Mathematics" -> "math"). */
    private fun normaliseSubject(subject: String): String {
        return when (subject.lowercase().trim()) {
            "mathematics", "math" -> "math"
            "computer science", "computer", "cs" -> "computer science"
            "tarjuma-tul-quran", "tarjuma", "quran" -> "tarjuma-tul-quran"
            "pak studies", "pakistan studies", "pak study" -> "pak studies"
            else -> subject.lowercase().trim()
        }
    }

    /** Class digits from a sheet class_num ("10th" -> "10"). */
    private fun digitsOf(classNum: String): String = classNum.filter { it.isDigit() }

    /** True when ALP questions are defined for this class + subject. */
    fun hasAlp(classDigits: String, subject: String): Boolean =
        alpIds.containsKey(classDigits to normaliseSubject(subject))

    /** True when ALP questions are defined for this question's class + subject. */
    fun hasAlp(q: Question): Boolean =
        alpIds.containsKey(digitsOf(q.class_num) to normaliseSubject(q.subject))

    /** True when this MCQ is an ALP question. */
    fun isAlp(q: Question): Boolean =
        alpIds[digitsOf(q.class_num) to normaliseSubject(q.subject)]?.contains(q.id) == true

    /** True when ALP short questions are defined for this class + subject. */
    fun hasAlpShort(classDigits: String, subject: String): Boolean =
        alpShortIds.containsKey(classDigits to normaliseSubject(subject))

    /** True when ALP short questions are defined for this question's class + subject. */
    fun hasAlpShort(q: WrittenQuestion): Boolean =
        alpShortIds.containsKey(digitsOf(q.classNum) to normaliseSubject(q.subject))

    /** True when this short question is an ALP question. */
    fun isAlpShort(q: WrittenQuestion): Boolean =
        alpShortIds[digitsOf(q.classNum) to normaliseSubject(q.subject)]?.contains(q.id) == true

    /** True when ALP long questions are defined for this class + subject. */
    fun hasAlpLong(classDigits: String, subject: String): Boolean =
        alpLongIds.containsKey(classDigits to normaliseSubject(subject))

    /** True when ALP long questions are defined for this question's class + subject. */
    fun hasAlpLong(q: WrittenQuestion): Boolean =
        alpLongIds.containsKey(digitsOf(q.classNum) to normaliseSubject(q.subject))

    /** True when this long question is an ALP question. */
    fun isAlpLong(q: WrittenQuestion): Boolean =
        alpLongIds[digitsOf(q.classNum) to normaliseSubject(q.subject)]?.contains(q.id) == true
}
