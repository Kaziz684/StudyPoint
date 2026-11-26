package com.example.studypoint.practicetest

/**
 * Topic-wise question map for the Paper Generator and MCQ Practice.
 *
 * PILOT (Milestone 5, v158): Physics, Class 9, Chapter 1
 * "Physical Quantities and Measurements" only. Other chapters and
 * classes keep the current chapter-level lists — topicsFor() returns
 * null for them, and callers fall back to the flat view.
 *
 * Source of truth: ~/workspace/topic_mockups/phy9_ch1_topic_map.json
 * (Milestone 4, verified 102/102 questions against the live sheet).
 * Topic structure is the CURRENT PCTB book (13 sections, 1.1-1.13):
 * prefixes live inside 1.3 SI Units (no separate section), measuring
 * instruments split by quantity (1.5 Length, 1.6 Mass, 1.7 Time,
 * 1.8 Volume), and there is NO Density section. Sub-topic lists are
 * SUBSETS of their topic lists — a question is never counted twice.
 * The two ambiguous Longs (2237, 6246) belong to NO topic and are
 * shown at chapter level via chapterLevelLongIds().
 */
data class SubtopicDef(
    val name: String,
    val mcqIds: Set<Int> = emptySet(),
    val shortIds: Set<Int> = emptySet(),
    val longIds: Set<Int> = emptySet()
)

data class TopicDef(
    val number: String,
    val title: String,
    val mcqIds: Set<Int> = emptySet(),
    val shortIds: Set<Int> = emptySet(),
    val longIds: Set<Int> = emptySet(),
    val subtopics: List<SubtopicDef> = emptyList()
)

object TopicQuestions {

    /** Physics-9 Chapter 1 topics, in book order (1.1-1.13). */
    val phy9Ch1Topics: List<TopicDef> = listOf(
        TopicDef(
            number = "1.1",
            title = "Physical and Non-Physical Quantities",
            mcqIds = setOf(152),
            shortIds = setOf(80, 3031, 3032, 3033),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "1.2",
            title = "Base and Derived Quantities",
            mcqIds = setOf(151),
            shortIds = setOf(81, 83, 3034, 3035, 3036, 3037, 3073, 3087),
            longIds = setOf(6247),
        ),
        TopicDef(
            number = "1.3",
            title = "International System of Units",
            mcqIds = setOf(150),
            shortIds = setOf(82, 84, 85, 86, 164, 3038, 3039, 3059, 3061),
            longIds = setOf(22, 23, 27, 2236, 2244),
        ),
        TopicDef(
            number = "1.4",
            title = "Scientific Notation",
            mcqIds = setOf(158),
            shortIds = setOf(3040),
            longIds = setOf(2238, 2239, 2240, 2241, 2243, 2245),
        ),
        TopicDef(
            number = "1.5",
            title = "Length Measuring Instruments",
            mcqIds = setOf(149, 157, 159),
            shortIds = setOf(87, 88, 3041, 3043, 3045, 3046, 3070, 3072, 3074, 3077, 3078, 3085, 3086),
            longIds = setOf(24, 25, 26, 81, 6248),
            subtopics = listOf(
                SubtopicDef("Meter Rule", emptySet(), setOf(88, 3074), setOf(24, 25, 26)),
                SubtopicDef("Measuring Tape", emptySet(), emptySet(), emptySet()),
                SubtopicDef("Vernier Callipers", setOf(149, 157, 159), setOf(87, 3043, 3070, 3077, 3078, 3086), setOf(81)),
                SubtopicDef("Screw Gauge", emptySet(), setOf(3045, 3046, 3072, 3085), emptySet()),
            )
        ),
        TopicDef(
            number = "1.6",
            title = "Mass Measuring Instruments",
            mcqIds = emptySet(),
            shortIds = setOf(3047, 3048, 3058, 3080),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "1.7",
            title = "Time Measuring Instruments",
            mcqIds = emptySet(),
            shortIds = setOf(3075, 3079),
            longIds = setOf(28),
        ),
        TopicDef(
            number = "1.8",
            title = "Volume Measuring Instruments",
            mcqIds = setOf(153, 154, 155),
            shortIds = setOf(3049, 3050, 3069, 3081, 3082),
            longIds = setOf(29),
        ),
        TopicDef(
            number = "1.9",
            title = "Errors in Measurements",
            mcqIds = emptySet(),
            shortIds = setOf(3042, 3044, 3051, 3052, 3053, 3060, 3062, 3063, 3064, 3065),
            longIds = setOf(6249),
        ),
        TopicDef(
            number = "1.10",
            title = "Uncertainty in a Measurement",
            mcqIds = emptySet(),
            shortIds = setOf(3054),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "1.11",
            title = "Significant Figures",
            mcqIds = setOf(156),
            shortIds = setOf(3055, 3071, 3076, 3083),
            longIds = setOf(2242),
        ),
        TopicDef(
            number = "1.12",
            title = "Precision and Accuracy",
            mcqIds = emptySet(),
            shortIds = setOf(3056, 3057, 3084),
            longIds = setOf(6250),
        ),
        TopicDef(
            number = "1.13",
            title = "Rounding Off the Digits",
            mcqIds = emptySet(),
            shortIds = setOf(3066, 3067, 3068),
            longIds = emptySet(),
        ),
    )

    /** Longs that belong to no topic (shown at chapter level). */
    val phy9Ch1ChapterLongIds: Set<Int> = setOf(2237, 6246)

    /**
     * Topics for a chapter, or null when the chapter has no topic map.
     * Pilot scope: Class 9 Physics Chapter 1 only.
     */
    fun topicsFor(classDigits: String, subject: String, chapterNumber: Int?): List<TopicDef>? =
        if (classDigits == "9" && subject.trim().equals("Physics", ignoreCase = true) && chapterNumber == 1)
            phy9Ch1Topics else null

    /** Longs shown at chapter level (no topic) for a chapter. */
    fun chapterLevelLongIds(classDigits: String, subject: String, chapterNumber: Int?): Set<Int> =
        if (classDigits == "9" && subject.trim().equals("Physics", ignoreCase = true) && chapterNumber == 1)
            phy9Ch1ChapterLongIds else emptySet()

    /** MCQ ids of one topic (for MCQ Practice), or empty when unknown. */
    fun mcqIdsForTopic(classDigits: String, subject: String, chapterNumber: Int?, topicNumber: String): Set<Int> =
        topicsFor(classDigits, subject, chapterNumber)
            ?.firstOrNull { it.number == topicNumber }?.mcqIds ?: emptySet()
}
