package com.example.studypoint.practicetest

/**
 * ALP (Accelerated Learning Programme / Smart Syllabus 2027) question ids,
 * plus the Exercise-question detector used by the three-mode selector
 * (ALP / Exercise / Mixed) in the Paper Generator.
 *
 * The ids are the questions from the ALP test papers the teacher sends
 * (Rising Star, 18 Nov 2026): Mathematics 10th (MCQs + Shorts) and
 * Pak Studies 10th (MCQs). They are scoped by class + subject so an id
 * can never match the wrong class — sheet ids overlap across classes
 * (e.g. id 96 exists in Class 9th and in Class 10th). MCQ ids live in
 * alpIds, Shorts-tab ids in alpShortIds.
 *
 * Add one entry per subject as its ALP paper arrives. The Paper Generator
 * colours ALP questions green and the rest red in manual mode, and the mode
 * selector shows only ALP questions (ALP), only textbook exercise questions
 * (Exercise), or everything (Mixed) so any of the three papers can be built.
 *
 * Exercise questions are recognised by the leading textbook label in the
 * raw sheet text ("Exercise 1.1 Q1(i):", "Review Exercise 12 Q1(iv):") —
 * the same shape the paper generator strips at render time. That is why
 * the detector works for Shorts/Longs (labels kept in the sheet) but the
 * MCQ tab carries no labels (stripped at append), so MCQ exercise
 * questions are tagged by id in exerciseMcqIds once matched against the
 * textbook exercises.
 *
 * NOTE (21 Nov 2026): the PECTAA Grade-12/11 ALP tables are transcribed in
 * ~/workspace/alp12/alp_data.json, but they are NOT applied here yet — the
 * sheet's Class-12 chapters do not match the new book's units (English-12
 * rows are filed under "Unit 1-5, 12-15" + "Mr. Chips", Pak Studies-12
 * under "2nd Half Book" with no chapter number), so a chapter-exclusion
 * rule would mislabel questions. Apply them only after the Class-12
 * chapters are mapped to the book's units by content.
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
            4164, 4165, 4166, 4167, 4168, 4169, 4170, 4171, 4172, 4173,
            // Ch1 Q9 (held, key D = جدوجہد کا), appended 18 Nov 2026 as id 4174
            4174,
            // Ch2 paper 18 Nov 2026: 10 MCQs (already in the sheet)
            2505, 2506, 2507, 2508, 2509, 2510, 2511, 2512, 2513, 2514,
            // Ch3 paper 18 Nov 2026: 10 MCQs (already in the sheet)
            2790, 2791, 2792, 2793, 2794, 2795, 2796, 2797, 2798, 2799,
            // Ch4 paper 18 Nov 2026: 9 MCQs (already in the sheet)
            2610, 2611, 2612, 2613, 2614, 2615, 2616, 2617, 2618,
            // Ch5 paper 18 Nov 2026: 8 MCQs (already in the sheet)
            2410, 2411, 2412, 2413, 2414, 2415, 2416, 2417,
            // Ch6 paper 18 Nov 2026: 8 MCQs (already in the sheet)
            2385, 2386, 2387, 2388, 2389, 2390, 2391, 2392,
            // Ch7 paper 18 Nov 2026: 10 MCQs (already in the sheet)
            2685, 2686, 2687, 2688, 2689, 2690, 2691, 2692, 2693, 2694,
            // Ch8 paper 18 Nov 2026: 8 MCQs (already in the sheet)
            2455, 2456, 2457, 2458, 2459, 2460, 2461, 2462
        ),
        // Class 10th, Tarjuma-tul-Quran (sheet subject "Tarjuma-tul-Quran")
        ("10" to "tarjuma-tul-quran") to setOf(
            // ALP paper 18 Nov 2026 (20 MCQs, no Shorts/Longs): 11 already in sheet
            4087, 2919, 2952, 2957, 2955, 2977, 2949, 2943, 2916, 2922, 2925,
            // Same paper: 6 new MCQs (keys authored; paper prints none)
            4175, 4176, 4177, 4178, 4179, 4180,
            // Surah Kahf paper 18 Nov 2026 (5 MCQs, all already in sheet)
            2950, 2951, 2952, 2953, 2954,
            // Surah Al-An'am paper 18 Nov 2026 (5 MCQs, all already in sheet)
            2930, 2931, 2932, 2933, 2934
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
            // Ch1: Section-II Q1 (i)-(xiii), all already in the sheet
            1793, 1794, 1795, 1796, 1797, 1798, 1799, 1800, 1801, 1802,
            1803, 1804, 1805,
            // Ch2: Section-II Q2 (i)-(xv), (xvi)-(xix) are longs (ids 445-448)
            1768, 1769, 1770, 1771, 1772, 1773, 1775, 1776, 1777, 1778,
            1780, 1781, 1782,
            // Ch2: Section-II Q2 (vii) negative effects of British rule, new (id 11441)
            11441,
            // Ch3: Section-II Q1 (i)-(xvi); (ii) repeats (i) in the paper, both id 1874
            1874, 1875, 1876, 1877, 1878, 1879, 1880, 1881, 1882, 1883,
            1884, 1885, 1886, 1887, 1888,
            // Ch4: Section-II Q1 (i)-(xix); (iii) repeats (i) in the paper, both id 1816
            1816, 1817, 1818, 1819, 1820, 1821, 1822, 1823, 1824, 1825,
            1826, 1827, 1828, 1829, 1830, 1832, 1833,
            // Ch4: Section-II Q1 (xvii) global warming effects, new (id 11442)
            11442,
            // Ch5: Section-II Q1 (i)-(xv), all already in the sheet
            1723, 1724, 1725, 1726, 1727, 1728, 1729, 1730, 1731, 1732,
            1733, 1734, 1735, 1736, 1737,
            // Ch6: Section-II Q2 (i)-(vii), (ix), (x), all already in the sheet
            1703, 1704, 1705, 1706, 1707, 1708, 1709, 1711, 1712,
            // Ch6: Section-II Q2 (viii) health problems = sheet Ch7 short id 1870
            1870,
            // Ch7: Section-II Q2 (i)-(xvii), all already in the sheet
            1844, 1845, 1846, 1847, 1848, 1850, 1851, 1852, 1853, 1854,
            1855, 1857, 1859, 1860, 1861, 1862, 1863,
            // Ch8: Section-II Q1 (i)-(ix), (xi), (xiii); (ii) repeats (i), both id 1748
            1748, 1749, 1750, 1751, 1752, 1753, 1754, 1755, 1756, 1759,
            // Ch8: Section-II Q1 (x) Shamshad Akhtar, new (id 11443)
            11443
        ),
        // Class 10th, Tarjuma-tul-Quran: Section-II paper, 18 Nov 2026
        ("10" to "tarjuma-tul-quran") to setOf(
            // Ch1 Surah Al Anam: (iii)-(vi) already in sheet; (vii)-(ix) new
            1922, 1923, 1924, 1925, 11444, 11445, 11446,
            // Ch2 Surah A'araf: (x)-(xiv) already in sheet (1919+9380 are a duplicate pair)
            1915, 1916, 1917, 1918, 1919, 9380, 11447, 11448,
            // Ch3 Surah Yunus: (xix)-(xxii) already in sheet; (xviii), (23), (24) new
            1902, 1903, 1904, 1905, 11449, 11450, 11451,
            // Ch4 Surah Hud: (25)-(29) already in sheet (1908+9384, 1909+9385 duplicate pairs)
            1906, 1907, 1908, 9384, 1909, 9385, 1910,
            // Ch4 Surah Hud: (30)-(35) new
            11452, 11453, 11454, 11455, 11456, 11457,
            // Section-II paper 3, 18 Nov 2026 (Ch4 Hud to Ch8 Nahl)
            // Ch4: (ii) Hud end instruction, new (11458); (i)/(iii) Lut = 11457 (already above)
            11458,
            // Ch5 Surah Raad: (iv)-(viii) already in sheet; (ix)-(xi) new
            1926, 1927, 1928, 1929, 1930, 11459, 11460, 11461,
            // Ch6 Surah Ibrahim: (xii)-(xvi) already in sheet; (xvii)-(xxiii) new
            1933, 1934, 1935, 1936, 1937, 11462, 11463, 11464, 11465, 11466, 11467, 11468,
            // Ch7 Surah Hijr: (xxiv)-(xxviii) already in sheet; (xxix)-(xxxi), (xxxiii) new
            1959, 1960, 1961, 1962, 1963, 11469, 11470, 11471, 11472,
            // Ch8 Surah Nahl: (xxxiv)-(xxxv) already in sheet
            1938, 1939,
            // Surah Kahf paper 18 Nov 2026: (i)-(iv), (vi) already in sheet; (v), (vii)-(ix), (xi) new
            1944, 1945, 1946, 1947, 1948, 11473, 11474, 11475, 11476, 11477
        )
    )

    /** class digits -> normalised sheet subject -> ALP LONG question ids
     *  (Longs tab; same key scheme, ids are Longs-tab ids). */
    private val alpLongIds: Map<Pair<String, String>, Set<Int>> = mapOf(
        // Class 10th, Pak Studies: Ch1 Nazriya-e-Pakistan longs (full paper, 18 Nov 2026)
        ("10" to "pak studies") to setOf(
            // Ch1: Section-III Q2 (a)-(e), all already in the sheet
            449, 450, 451, 452, 453,
            // Ch2: Section-II Q2 (xvi)-(xix), all already in the sheet
            445, 446, 447, 448,
            // Ch3: Section-III Q2 (a)-(i), all already in the sheet
            468, 469, 470, 471, 472, 473, 474, 475, 476,
            // Ch4: Section-III Q2 (a)-(i), all already in the sheet
            454, 455, 456, 457, 458, 459, 460, 461, 462,
            // Ch5: Section-III Q2 (a)-(d), all already in the sheet
            435, 436, 437, 438,
            // Ch6: Section-II Q2 (xi)-(xiv), all already in the sheet
            431, 432, 433, 434,
            // Ch7: Section-II Q2 (xviii)-(xx), (21), all already in the sheet
            463, 465, 466, 467,
            // Ch8: Section-III Q2 (a)-(e), all already in the sheet
            440, 441, 442, 443, 444
        ),
        // Class 10th, Tarjuma-tul-Quran: Section-II paper, 18 Nov 2026
        ("10" to "tarjuma-tul-quran") to setOf(
            // (i)/(ii) Al-An'am central topic: in the sheet as a Long (id 1061)
            1061,
            // (xvi) A'araf central topic: in the sheet as a Long (id 780)
            780,
            // Surah Kahf paper 18 Nov 2026: (32) intro + topics (1532, 1533);
            // (25)-(31) verse translations (1535-1544, 1657, 1658)
            1532, 1533, 1535, 1536, 1537, 1538, 1539, 1540, 1541, 1542, 1543, 1544,
            1657, 1658,
            // Surah Al-An'am paper 18 Nov 2026: (23) intro + topics (1061 already above, 1062);
            // (xx)-(22) verses 151-153 translations (1245-1252, 1275)
            1062, 1245, 1246, 1247, 1248, 1249, 1250, 1251, 1252, 1275
        )
    )

    /**
     * Exercise-question label in the raw sheet text. Same shape as the
     * render-time strip regex in PaperGeneratorActivity.cleanPaperText:
     * "Exercise 12.1 Q1(iv):", "Review Exercise 12 Q1(iv):",
     * "Exercise Review 1 Q2(i):", multi-part "Q2(a)(i):", and bare
     * "Exercise:". A question that merely mentions the word "exercise"
     * (e.g. "effect of exercise on breathing") does NOT match.
     * Verified live 21 Nov 2026: matches all 1,182 exercise-labelled
     * Shorts rows (9th Math 653, 10th Math 528, 10th Computer Science 1)
     * and all 454 Longs rows (9th Math 268, 10th Math 186).
     */
    private val exerciseLabelRegex =
        Regex("""^\s*(?:Review\s+)?Exercise(?:\s+Review)?(?:\s+Exercise)?\s*[\d.]*\s*(?:\([^)]*\)\s*)?Q\s*R?\s*\d*\s*(?:\((?:[ivxlcdm]+|[a-z])\)\s*)*[:.\-]?\s*""", RegexOption.IGNORE_CASE)
    private val bareExerciseRegex =
        Regex("""^\s*Exercise\s*:\s*""", RegexOption.IGNORE_CASE)

    /** True when this written question is a textbook exercise question
     *  (its raw sheet text starts with an "Exercise ..." label). */
    fun isExerciseShort(q: WrittenQuestion): Boolean =
        exerciseLabelRegex.containsMatchIn(q.question) ||
            bareExerciseRegex.containsMatchIn(q.question)

    /** True when this long question is a textbook exercise question. */
    fun isExerciseLong(q: WrittenQuestion): Boolean = isExerciseShort(q)

    /**
     * MCQ exercise ids, scoped by class + subject (same key scheme as
     * alpIds). The MCQ tab strips the exercise label at append time, so
     * MCQs carry no label to match — exercise MCQs are tagged by id once
     * matched against the textbook exercises. EMPTY for now; populated as
     * textbooks are matched. Until then the Exercise option shows no MCQs
     * (the "No Exercise MCQs" note) — it never guesses.
     */
    private val exerciseMcqIds: Map<Pair<String, String>, Set<Int>> = mapOf()

    /** True when exercise MCQs are tagged for this class + subject. */
    fun hasExerciseMcq(classDigits: String, subject: String): Boolean =
        exerciseMcqIds.containsKey(classDigits to normaliseSubject(subject))

    /** True when this MCQ is a tagged textbook exercise question. */
    fun isExerciseMcq(q: Question): Boolean =
        exerciseMcqIds[digitsOf(q.class_num) to normaliseSubject(q.subject)]?.contains(q.id) == true

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
