package com.example.studypoint.practicetest

/**
 * Topic-wise question map for the Paper Generator and MCQ Practice.
 *
 * Milestone 6 Phase 1 (v159): Physics, Class 9, Chapters 1-9
 * (Kinematics, Dynamics, Turning Effects of Force, Work/Energy/Power,
 * Mechanical Properties, Thermal Properties, Magnetism, Nature of Science
 * + Ch1 Physical Quantities and Measurements). Other chapters, subjects
 * and classes keep the current chapter-level lists — topicsFor() returns
 * null for them, and callers fall back to the flat view.
 *
 * Source of truth: ~/workspace/topic_mockups/phy9_ch<N>_topic_map.json
 * (Milestone 4 pilot Ch1 + Milestone 6 Phase 1 Ch2-Ch9, verified against
 * the live sheet: 88 topics, 908 questions). Topic structure is the
 * CURRENT PCTB book (Ch1: 13 sections 1.1-1.13; Ch4 list textbook-
 * verified pp. 80-98). Sub-topic lists are SUBSETS of their topic
 * lists — a question is never counted twice. The 16 ambiguous items
 * (9 Longs + 7 Shorts) belong to NO topic and are shown at chapter
 * level via chapterLevelLongIds() / chapterLevelShortIds().
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

    /** Physics-9 Chapter 1 topics (Physical Quantities and Measurements), in book order. */
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

    /** Shorts that belong to no topic (shown at chapter level). */
    val phy9Ch1ChapterShortIds: Set<Int> = emptySet()

    /** Physics-9 Chapter 2 topics (Kinematics), in book order. */
    val phy9Ch2Topics: List<TopicDef> = listOf(
        TopicDef(
            number = "2.1",
            title = "Scalars and Vectors",
            mcqIds = emptySet(),
            shortIds = setOf(89, 90, 91, 94, 3091, 3092, 3093, 3094, 3095, 3096, 3097, 3122, 3123, 3124, 3125, 3147, 3148, 3149, 3152),
            longIds = setOf(2246, 6260),
        ),
        TopicDef(
            number = "2.2",
            title = "Rest and Motion",
            mcqIds = setOf(161),
            shortIds = setOf(3098, 3099, 3126, 3127),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "2.3",
            title = "Types of Motion",
            mcqIds = emptySet(),
            shortIds = setOf(3100, 3101, 3102, 3103, 3104, 3105, 3128, 3129, 3130),
            longIds = setOf(6262),
            subtopics = listOf(
                SubtopicDef("Translatory Motion", emptySet(), setOf(3100, 3101, 3102, 3103, 3128, 3129), emptySet()),
                SubtopicDef("Rotatory Motion", emptySet(), setOf(3104, 3130), emptySet()),
                SubtopicDef("Vibratory Motion", emptySet(), setOf(3105), emptySet()),
            )
        ),
        TopicDef(
            number = "2.4",
            title = "Distance and Displacement",
            mcqIds = setOf(160),
            shortIds = setOf(3106, 3107),
            longIds = setOf(6259, 6263),
        ),
        TopicDef(
            number = "2.5",
            title = "Speed and Velocity",
            mcqIds = emptySet(),
            shortIds = setOf(95, 3108, 3109, 3110, 3111, 3112, 3131, 3132, 3133, 3134, 3153),
            longIds = setOf(31, 2248, 2254),
        ),
        TopicDef(
            number = "2.6",
            title = "Acceleration",
            mcqIds = emptySet(),
            shortIds = setOf(96, 3113, 3114, 3115, 3116, 3135, 3136, 3137, 3154),
            longIds = setOf(32, 2249),
        ),
        TopicDef(
            number = "2.7",
            title = "Graphical Analysis of Motion",
            mcqIds = emptySet(),
            shortIds = setOf(92, 3117, 3138, 3139, 3150),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "2.8",
            title = "Gradient of a Distance-Time Graph",
            mcqIds = setOf(165, 168),
            shortIds = setOf(3118, 3141),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "2.9",
            title = "Speed-Time Graph",
            mcqIds = setOf(169),
            shortIds = setOf(3140),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "2.10",
            title = "Gradient of a Speed-Time Graph",
            mcqIds = setOf(167),
            shortIds = setOf(3142),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "2.11",
            title = "Area Under Speed-Time Graph",
            mcqIds = setOf(166),
            shortIds = setOf(3143),
            longIds = setOf(6265),
        ),
        TopicDef(
            number = "2.12",
            title = "Solving Problems for Motion Under Gravity",
            mcqIds = setOf(163, 164),
            shortIds = emptySet(),
            longIds = setOf(30, 2250, 2251, 2252),
        ),
        TopicDef(
            number = "2.13",
            title = "Free Fall Acceleration",
            mcqIds = setOf(162),
            shortIds = setOf(93, 3119, 3144, 3145, 3151),
            longIds = setOf(2247, 2253, 2255, 6266),
        ),
    )

    /** Longs that belong to no topic (shown at chapter level). */
    val phy9Ch2ChapterLongIds: Set<Int> = setOf(6261, 6264)

    /** Shorts that belong to no topic (shown at chapter level). */
    val phy9Ch2ChapterShortIds: Set<Int> = setOf(3088, 3089, 3090, 3120, 3121, 3146)

    /** Physics-9 Chapter 3 topics (Dynamics), in book order. */
    val phy9Ch3Topics: List<TopicDef> = listOf(
        TopicDef(
            number = "3.1",
            title = "Concept of Force",
            mcqIds = setOf(172),
            shortIds = setOf(97, 98, 3155, 3156, 3158, 3159, 3160, 3161, 3162, 3163, 3164, 3186, 3187, 3188, 3189, 3190, 3216),
            longIds = setOf(6291),
        ),
        TopicDef(
            number = "3.2",
            title = "Fundamental Forces",
            mcqIds = emptySet(),
            shortIds = setOf(3165, 3166, 3167, 3168, 3169, 3191, 3192),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "3.3",
            title = "Forces in a Free-Body Diagram",
            mcqIds = emptySet(),
            shortIds = setOf(3170, 3193),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "3.4",
            title = "Newton's Laws of Motion",
            mcqIds = setOf(170, 171),
            shortIds = setOf(99, 101, 102, 103, 106, 3171, 3172, 3173, 3174, 3175, 3194, 3195, 3196, 3197, 3198, 3199, 3200, 3201, 3217, 3218, 3219, 3220, 3222),
            longIds = setOf(33, 35, 37, 2256, 2258, 2262, 6292),
            subtopics = listOf(
                SubtopicDef("Newton's First Law of Motion", emptySet(), setOf(99, 101, 102, 3171, 3172, 3194, 3195, 3196, 3197, 3217, 3218, 3219), emptySet()),
                SubtopicDef("Newton's Second Law of Motion", setOf(171), setOf(103, 3173, 3174, 3198, 3199, 3220), setOf(2256, 2258, 2262)),
                SubtopicDef("Newton's Third Law of Motion", setOf(170), setOf(106, 3175, 3200, 3201, 3222), setOf(33, 35, 37)),
            )
        ),
        TopicDef(
            number = "3.5",
            title = "Limitations of Newton's Laws of Motion",
            mcqIds = emptySet(),
            shortIds = setOf(3202),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "3.6",
            title = "Mass and Weight",
            mcqIds = setOf(2327, 2328, 2330),
            shortIds = setOf(3176, 3177, 3203, 3204, 3206, 3207),
            longIds = setOf(2257),
        ),
        TopicDef(
            number = "3.7",
            title = "Mechanical and Electronic Balances",
            mcqIds = setOf(2329),
            shortIds = setOf(3205),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "3.8",
            title = "Friction",
            mcqIds = setOf(178, 2331),
            shortIds = setOf(104, 105, 3157, 3178, 3179, 3180, 3181, 3182, 3208, 3209, 3210, 3211, 3221, 9363, 9365),
            longIds = setOf(38, 6281, 6295),
            subtopics = listOf(
                SubtopicDef("Terminal Velocity", emptySet(), setOf(105, 3181, 9363), emptySet()),
            )
        ),
        TopicDef(
            number = "3.9",
            title = "Momentum and Impulse",
            mcqIds = setOf(173, 176, 177),
            shortIds = setOf(100, 3183, 3184, 3212, 3213, 3214, 9366),
            longIds = setOf(34, 36, 2263, 6293),
        ),
        TopicDef(
            number = "3.10",
            title = "Principle of Conservation of Momentum",
            mcqIds = setOf(174, 175),
            shortIds = setOf(3185, 3215, 9364),
            longIds = setOf(2259, 2260, 2261, 2264, 6294),
        ),
    )

    /** Longs that belong to no topic (shown at chapter level). */
    val phy9Ch3ChapterLongIds: Set<Int> = setOf(4301)

    /** Shorts that belong to no topic (shown at chapter level). */
    val phy9Ch3ChapterShortIds: Set<Int> = emptySet()

    /** Physics-9 Chapter 4 topics (Turning Effects of Force), in book order. */
    val phy9Ch4Topics: List<TopicDef> = listOf(
        TopicDef(
            number = "4.1",
            title = "Like and Unlike Parallel Forces",
            mcqIds = emptySet(),
            shortIds = setOf(107, 3233, 3234),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "4.2",
            title = "Addition of Forces",
            mcqIds = setOf(179),
            shortIds = setOf(3235),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "4.3",
            title = "Turning Effect of a Force",
            mcqIds = setOf(181),
            shortIds = setOf(109, 110, 111, 3236, 3237, 3238, 3239, 3240, 3241, 3242, 3243, 3264, 3265),
            longIds = setOf(2266, 2270),
        ),
        TopicDef(
            number = "4.4",
            title = "Resolution of Vectors",
            mcqIds = setOf(180),
            shortIds = setOf(108, 3244, 3245, 3263),
            longIds = setOf(2265),
        ),
        TopicDef(
            number = "4.5",
            title = "Determination of a Force from its Perpendicular Components",
            mcqIds = emptySet(),
            shortIds = emptySet(),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "4.6",
            title = "Principle of Moments",
            mcqIds = setOf(183, 184),
            shortIds = setOf(3246),
            longIds = setOf(2267, 2268, 2269, 2273, 6282),
        ),
        TopicDef(
            number = "4.7",
            title = "Centre of Gravity and Centre of Mass",
            mcqIds = setOf(186),
            shortIds = setOf(113, 3223, 3247, 3248, 3249, 3268),
            longIds = setOf(6283),
        ),
        TopicDef(
            number = "4.8",
            title = "Equilibrium",
            mcqIds = emptySet(),
            shortIds = setOf(112, 3224, 3225, 3226, 3250, 3251, 3252, 3253, 3267),
            longIds = setOf(42),
            subtopics = listOf(
                SubtopicDef("Dynamic Equilibrium", emptySet(), setOf(112, 3226, 3253, 3267), emptySet()),
                SubtopicDef("Static Equilibrium", emptySet(), setOf(3225, 3252), emptySet()),
            )
        ),
        TopicDef(
            number = "4.9",
            title = "Conditions of Equilibrium",
            mcqIds = setOf(182),
            shortIds = setOf(3227, 3228, 3254, 3255),
            longIds = setOf(2271, 2272, 2274, 6284),
        ),
        TopicDef(
            number = "4.10",
            title = "States of Equilibrium",
            mcqIds = setOf(185, 187),
            shortIds = setOf(3229, 3230, 3231, 3258, 3259, 3266),
            longIds = emptySet(),
            subtopics = listOf(
                SubtopicDef("Neutral Equilibrium", emptySet(), setOf(3231, 3259), emptySet()),
                SubtopicDef("Stable Equilibrium", setOf(185, 187), setOf(3229, 3258), emptySet()),
                SubtopicDef("Unstable Equilibrium", emptySet(), setOf(3230), emptySet()),
            )
        ),
        TopicDef(
            number = "4.11",
            title = "Improvement of Stability",
            mcqIds = emptySet(),
            shortIds = setOf(114, 3256, 3269),
            longIds = setOf(41, 43, 6285),
        ),
        TopicDef(
            number = "4.12",
            title = "Application of Stability in Real Life",
            mcqIds = emptySet(),
            shortIds = setOf(3257),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "4.13",
            title = "Centripetal Force",
            mcqIds = setOf(188),
            shortIds = setOf(115, 3232, 3260, 3261, 3262, 3270),
            longIds = setOf(39),
        ),
    )

    /** Longs that belong to no topic (shown at chapter level). */
    val phy9Ch4ChapterLongIds: Set<Int> = setOf(40)

    /** Shorts that belong to no topic (shown at chapter level). */
    val phy9Ch4ChapterShortIds: Set<Int> = emptySet()

    /** Physics-9 Chapter 5 topics (Work, Energy and Power), in book order. */
    val phy9Ch5Topics: List<TopicDef> = listOf(
        TopicDef(
            number = "5.1",
            title = "Work",
            mcqIds = setOf(189, 190),
            shortIds = setOf(116, 120, 3271, 3272, 3297, 3298, 3299, 3300, 3301, 3302, 3324, 3325, 3327),
            longIds = setOf(84, 2275, 2276, 2282, 2283, 2287),
        ),
        TopicDef(
            number = "5.2",
            title = "Energy",
            mcqIds = setOf(193, 194, 195, 196),
            shortIds = setOf(117, 119, 121, 122, 3273, 3274, 3275, 3276, 3277, 3278, 3279, 3280, 3281, 3303, 3304, 3305, 3306, 3307, 3326, 3328, 3329),
            longIds = setOf(44, 45, 46, 82, 2278, 2279, 6276),
            subtopics = listOf(
                SubtopicDef("Kinetic Energy", setOf(193, 194, 196), setOf(117, 122, 3274, 3329), setOf(44, 45, 82, 2278, 6276)),
                SubtopicDef("Potential Energy", setOf(195), setOf(119, 121, 3275, 3277, 3278, 3279, 3280, 3281, 3306, 3307, 3326, 3328), setOf(46, 2279)),
            )
        ),
        TopicDef(
            number = "5.3",
            title = "Conservation of Energy",
            mcqIds = emptySet(),
            shortIds = setOf(3282, 3308),
            longIds = setOf(47, 83, 2280, 2281, 6277),
        ),
        TopicDef(
            number = "5.4",
            title = "Sources of Energy",
            mcqIds = emptySet(),
            shortIds = setOf(3283, 3284, 3285, 3286, 3287, 3288, 3309, 3310, 3311, 3312, 3313),
            longIds = setOf(6280),
            subtopics = listOf(
                SubtopicDef("Fossil Fuel Energy", emptySet(), setOf(3283, 3309), emptySet()),
                SubtopicDef("Hydroelectric Generation", emptySet(), setOf(3284, 3310), emptySet()),
                SubtopicDef("Solar Energy", emptySet(), setOf(3285, 3311), emptySet()),
                SubtopicDef("Nuclear Energy", emptySet(), emptySet(), emptySet()),
                SubtopicDef("Geothermal Energy", emptySet(), setOf(3286, 3312), emptySet()),
                SubtopicDef("Wind Energy", emptySet(), setOf(3287), emptySet()),
                SubtopicDef("Energy from Tides", emptySet(), emptySet(), emptySet()),
                SubtopicDef("Energy from Waves in the Sea", emptySet(), emptySet(), emptySet()),
                SubtopicDef("Biofuel Energy", emptySet(), setOf(3288, 3313), emptySet()),
            )
        ),
        TopicDef(
            number = "5.5",
            title = "Renewable and Non-Renewable Sources",
            mcqIds = setOf(197),
            shortIds = setOf(125, 3289, 3290, 3314, 3315, 3332),
            longIds = setOf(48, 6278),
        ),
        TopicDef(
            number = "5.6",
            title = "The Advantages and Disadvantages of Methods of Energy Production",
            mcqIds = emptySet(),
            shortIds = emptySet(),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "5.7",
            title = "Power",
            mcqIds = setOf(191, 192),
            shortIds = setOf(118, 124, 3291, 3292, 3293, 3316, 3317, 3318, 3319, 3323, 3331),
            longIds = setOf(2277, 2285, 2286),
        ),
        TopicDef(
            number = "5.8",
            title = "Efficiency",
            mcqIds = emptySet(),
            shortIds = setOf(123, 3294, 3295, 3296, 3320, 3321, 3322, 3330),
            longIds = setOf(2284, 6279),
            subtopics = listOf(
                SubtopicDef("Perpetual Energy Machines", emptySet(), setOf(3296), emptySet()),
            )
        ),
    )

    /** Longs that belong to no topic (shown at chapter level). */
    val phy9Ch5ChapterLongIds: Set<Int> = emptySet()

    /** Shorts that belong to no topic (shown at chapter level). */
    val phy9Ch5ChapterShortIds: Set<Int> = emptySet()

    /** Physics-9 Chapter 6 topics (Mechanical Properties of Matter), in book order. */
    val phy9Ch6Topics: List<TopicDef> = listOf(
        TopicDef(
            number = "6.1",
            title = "Deformation of Solids",
            mcqIds = emptySet(),
            shortIds = setOf(130, 3333, 3334, 3335, 3336, 3337, 3338, 3339, 3354, 3355, 3358, 3383),
            longIds = setOf(49, 50),
        ),
        TopicDef(
            number = "6.2",
            title = "Hooke's Law",
            mcqIds = setOf(198, 199, 204),
            shortIds = setOf(131, 3340, 3341, 3353, 3356, 3357, 3359, 3360, 3384),
            longIds = setOf(2288, 6286),
            subtopics = listOf(
                SubtopicDef("Applications of Hooke's Law", emptySet(), setOf(3359), emptySet()),
            )
        ),
        TopicDef(
            number = "6.3",
            title = "Density",
            mcqIds = emptySet(),
            shortIds = setOf(3342, 3361, 3362),
            longIds = setOf(56, 57, 2289, 2290, 2291),
            subtopics = listOf(
                SubtopicDef("Density Measurement", emptySet(), setOf(3362), setOf(2290)),
            )
        ),
        TopicDef(
            number = "6.4",
            title = "Pressure",
            mcqIds = emptySet(),
            shortIds = setOf(126, 127, 128, 132, 3343, 3344, 3363, 3364, 3365, 3379, 3380, 3381),
            longIds = setOf(2292),
            subtopics = listOf(
                SubtopicDef("Daily Life Examples", emptySet(), setOf(126, 127, 128, 3363, 3379, 3380, 3381), emptySet()),
            )
        ),
        TopicDef(
            number = "6.5",
            title = "Pressure in Liquids",
            mcqIds = setOf(200, 201),
            shortIds = setOf(133, 3366, 3367, 3378, 3385),
            longIds = setOf(51, 2295, 6289),
        ),
        TopicDef(
            number = "6.6",
            title = "Atmospheric Pressure",
            mcqIds = setOf(205),
            shortIds = setOf(3345, 3346, 3347, 3368, 3369, 3370, 3371),
            longIds = setOf(54, 2297, 6290),
            subtopics = listOf(
                SubtopicDef("Evidence of Atmospheric Pressure", emptySet(), emptySet(), emptySet()),
                SubtopicDef("Variation of Atmospheric Pressure with Height", emptySet(), setOf(3371), setOf(2297)),
            )
        ),
        TopicDef(
            number = "6.7",
            title = "Measurement of Atmospheric Pressure",
            mcqIds = emptySet(),
            shortIds = setOf(134, 3348, 3372, 3373, 3386),
            longIds = setOf(53, 55, 2293),
            subtopics = listOf(
                SubtopicDef("Changes in Atmospheric Pressure as Weather Indicator", emptySet(), setOf(3373), emptySet()),
            )
        ),
        TopicDef(
            number = "6.8",
            title = "Measurement of Pressure by Manometer",
            mcqIds = emptySet(),
            shortIds = setOf(3349, 3374),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "6.9",
            title = "Pascal's Law",
            mcqIds = setOf(202, 203),
            shortIds = setOf(129, 135, 3350, 3351, 3352, 3375, 3376, 3377, 3382, 3387),
            longIds = setOf(52, 2294, 2296, 2298, 2299, 6288),
            subtopics = listOf(
                SubtopicDef("Hydraulic Press", emptySet(), setOf(3351, 3377), setOf(2296, 2298)),
                SubtopicDef("Hydraulic Brakes", emptySet(), setOf(135, 3387), setOf(2294, 2299)),
            )
        ),
    )

    /** Longs that belong to no topic (shown at chapter level). */
    val phy9Ch6ChapterLongIds: Set<Int> = setOf(6287)

    /** Shorts that belong to no topic (shown at chapter level). */
    val phy9Ch6ChapterShortIds: Set<Int> = emptySet()

    /** Physics-9 Chapter 7 topics (Thermal Properties of Matter), in book order. */
    val phy9Ch7Topics: List<TopicDef> = listOf(
        TopicDef(
            number = "7.1",
            title = "Kinetic Molecular Theory of Matter",
            mcqIds = setOf(5725, 5726),
            shortIds = setOf(136, 137, 138, 139, 3388, 3389, 3390, 3391, 3392, 3393, 3394, 3411, 3412, 3413, 3414, 3415, 3435, 3439, 3440, 3441, 3442),
            longIds = setOf(58, 67, 68, 70, 6296),
            subtopics = listOf(
                SubtopicDef("Plasma", emptySet(), setOf(3393, 3394, 3414, 3415), setOf(58, 70)),
            )
        ),
        TopicDef(
            number = "7.2",
            title = "Temperature and Heat",
            mcqIds = setOf(5727, 5728),
            shortIds = setOf(140, 141, 146, 147, 148, 166, 3395, 3396, 3397, 3416, 3417, 3418, 3419, 3443, 3446, 3447, 3448, 3449),
            longIds = setOf(62, 65, 69),
            subtopics = listOf(
                SubtopicDef("Temperature and Internal Energy", emptySet(), setOf(166, 3397, 3418, 3419, 3447), setOf(62, 69)),
            )
        ),
        TopicDef(
            number = "7.3",
            title = "Thermometers",
            mcqIds = setOf(5729, 5730, 5731, 5732, 5733, 5735),
            shortIds = setOf(142, 165, 3398, 3399, 3400, 3401, 3402, 3403, 3404, 3405, 3406, 3407, 3420, 3421, 3422, 3423, 3424, 3425, 3427, 3436, 3437, 3438, 3444),
            longIds = setOf(59, 60, 63, 64, 66, 2300, 2301, 2302, 2303, 2304, 2305, 6298),
            subtopics = listOf(
                SubtopicDef("Basic Thermometric Properties", setOf(5731, 5732, 5733), setOf(142, 3421, 3436), setOf(59)),
                SubtopicDef("Conversion of Temperature from One Scale to Another", setOf(5730), emptySet(), setOf(66, 2300, 2301, 2302, 2303)),
                SubtopicDef("Liquid-in-Glass Thermometer", emptySet(), setOf(3399), emptySet()),
                SubtopicDef("Temperature Scales", setOf(5729), setOf(165, 3400, 3401, 3402, 3403, 3404, 3405, 3406, 3422, 3423, 3424, 3425, 3437, 3438, 3444), setOf(63, 2304, 2305, 6298)),
                SubtopicDef("Thermocouple Thermometer", setOf(5735), setOf(3407, 3427), emptySet()),
            )
        ),
        TopicDef(
            number = "7.4",
            title = "Sensitivity, Range and Linearity of Thermometers",
            mcqIds = setOf(5734),
            shortIds = setOf(143, 144, 145, 3408, 3409, 3410, 3426, 3428, 3429, 3430, 3432, 3433, 3434, 3445),
            longIds = setOf(61, 6299),
            subtopics = listOf(
                SubtopicDef("Linearity", emptySet(), setOf(144, 3410, 3430), emptySet()),
                SubtopicDef("Range", emptySet(), setOf(3409, 3426, 3429, 3433, 3434), emptySet()),
                SubtopicDef("Sensitivity", setOf(5734), setOf(143, 3408, 3428, 3432), setOf(61)),
            )
        ),
        TopicDef(
            number = "7.5",
            title = "Structure of a Liquid-in-Glass Thermometer",
            mcqIds = emptySet(),
            shortIds = setOf(3431),
            longIds = setOf(6300),
        ),
    )

    /** Longs that belong to no topic (shown at chapter level). */
    val phy9Ch7ChapterLongIds: Set<Int> = setOf(6297)

    /** Shorts that belong to no topic (shown at chapter level). */
    val phy9Ch7ChapterShortIds: Set<Int> = emptySet()

    /** Physics-9 Chapter 8 topics (Magnetism), in book order. */
    val phy9Ch8Topics: List<TopicDef> = listOf(
        TopicDef(
            number = "8.1",
            title = "Magnetic Materials",
            mcqIds = setOf(206),
            shortIds = setOf(3450, 3451, 3452, 3453, 3475, 3476, 3477),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "8.2",
            title = "Properties of Magnets",
            mcqIds = emptySet(),
            shortIds = setOf(3454, 3455, 3456, 3478, 3479, 3480, 3481),
            longIds = setOf(71, 6301, 6302),
            subtopics = listOf(
                SubtopicDef("Attraction and Repulsion of Magnetic Poles", emptySet(), setOf(3479), emptySet()),
                SubtopicDef("Identification of a Magnet", emptySet(), setOf(3481), setOf(6301, 6302)),
                SubtopicDef("Is Isolated Magnetic Pole Possible?", emptySet(), setOf(3480), emptySet()),
                SubtopicDef("Magnetic Poles", emptySet(), setOf(3454, 3455, 3456, 3478), setOf(71)),
            )
        ),
        TopicDef(
            number = "8.3",
            title = "Induced Magnetism",
            mcqIds = emptySet(),
            shortIds = setOf(3457, 3458, 3482, 3483),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "8.4",
            title = "Temporary and Permanent Magnets",
            mcqIds = setOf(208),
            shortIds = setOf(149, 3459, 3460, 3484, 3485),
            longIds = emptySet(),
        ),
        TopicDef(
            number = "8.5",
            title = "Magnetic Fields",
            mcqIds = setOf(207),
            shortIds = setOf(150, 151, 3461, 3462, 3463, 3486, 3487, 3488, 3489),
            longIds = emptySet(),
            subtopics = listOf(
                SubtopicDef("Magnetic Lines of Force", setOf(207), setOf(151, 3462, 3487), emptySet()),
                SubtopicDef("Strength of the Magnetic Field", emptySet(), setOf(3463, 3488, 3489), emptySet()),
            )
        ),
        TopicDef(
            number = "8.6",
            title = "Uses of Permanent Magnets",
            mcqIds = setOf(209),
            shortIds = setOf(3490),
            longIds = setOf(73),
            subtopics = listOf(
                SubtopicDef("A.C Generator", emptySet(), emptySet(), setOf(73)),
                SubtopicDef("Moving Coil Loudspeaker", setOf(209), emptySet(), emptySet()),
            )
        ),
        TopicDef(
            number = "8.7",
            title = "Electromagnets",
            mcqIds = emptySet(),
            shortIds = setOf(3464, 3465, 3466, 3467, 3491, 3492, 3493, 3494, 3495),
            longIds = emptySet(),
            subtopics = listOf(
                SubtopicDef("Circuit Breaker", emptySet(), setOf(3466, 3494), emptySet()),
                SubtopicDef("Electromagnetic Cranes", emptySet(), emptySet(), emptySet()),
                SubtopicDef("Magnetic Relay", emptySet(), setOf(3465, 3493), emptySet()),
                SubtopicDef("Telephone Receiver", emptySet(), emptySet(), emptySet()),
            )
        ),
        TopicDef(
            number = "8.8",
            title = "Domain Theory of Magnetism",
            mcqIds = emptySet(),
            shortIds = setOf(153, 154, 155, 3468, 3469, 3470, 3471, 3472, 3496, 3497, 3498, 3499),
            longIds = setOf(6303, 6304),
            subtopics = listOf(
                SubtopicDef("Alignment of Domains", emptySet(), emptySet(), emptySet()),
                SubtopicDef("Diamagnetic Materials", emptySet(), setOf(3470, 3498), emptySet()),
                SubtopicDef("Ferromagnetic Materials", emptySet(), setOf(3471, 3499), setOf(6304)),
                SubtopicDef("Paramagnetic Materials", emptySet(), setOf(3469, 3497), emptySet()),
            )
        ),
        TopicDef(
            number = "8.9",
            title = "Magnetisation and Demagnetisation",
            mcqIds = setOf(210),
            shortIds = setOf(3500, 3501),
            longIds = setOf(72),
            subtopics = listOf(
                SubtopicDef("Demagnetisation of Magnets", emptySet(), setOf(3501), emptySet()),
                SubtopicDef("Making a Magnet using Solenoid", emptySet(), emptySet(), setOf(72)),
                SubtopicDef("Stroking", setOf(210), setOf(3500), emptySet()),
            )
        ),
        TopicDef(
            number = "8.10",
            title = "Applications of Magnets in Recording Technology",
            mcqIds = emptySet(),
            shortIds = setOf(3502, 3503),
            longIds = emptySet(),
            subtopics = listOf(
                SubtopicDef("Hard Disk Recording", emptySet(), setOf(3503), emptySet()),
                SubtopicDef("Magnetic Tape Recording", emptySet(), emptySet(), emptySet()),
            )
        ),
        TopicDef(
            number = "8.11",
            title = "Soft Iron as Magnetic Shield",
            mcqIds = setOf(211),
            shortIds = setOf(3473, 3474, 3504),
            longIds = emptySet(),
        ),
    )

    /** Longs that belong to no topic (shown at chapter level). */
    val phy9Ch8ChapterLongIds: Set<Int> = emptySet()

    /** Shorts that belong to no topic (shown at chapter level). */
    val phy9Ch8ChapterShortIds: Set<Int> = setOf(152)

    /** Physics-9 Chapter 9 topics (Nature of Science), in book order. */
    val phy9Ch9Topics: List<TopicDef> = listOf(
        TopicDef(
            number = "9.1",
            title = "Scope of Physics",
            mcqIds = setOf(5736),
            shortIds = setOf(156, 3505, 3506, 3507, 3508, 3509, 3510, 3511, 3540, 3541, 3542, 3543, 3544, 3568),
            longIds = setOf(6308),
        ),
        TopicDef(
            number = "9.2",
            title = "Branches of Physics",
            mcqIds = emptySet(),
            shortIds = setOf(157, 3512, 3513, 3514, 3515, 3516, 3517, 3518, 3519, 3520, 3521, 3522, 3545, 3546, 3547, 3548, 3569),
            longIds = setOf(6305),
            subtopics = listOf(
                SubtopicDef("Acoustics", emptySet(), setOf(3513, 3545), emptySet()),
                SubtopicDef("Astronomy", emptySet(), setOf(3520), emptySet()),
                SubtopicDef("Cosmology", emptySet(), setOf(3521, 3548), emptySet()),
                SubtopicDef("Electromagnetism", emptySet(), setOf(3515), emptySet()),
                SubtopicDef("Heat and Thermodynamics", emptySet(), setOf(3512), emptySet()),
                SubtopicDef("Mechanics", emptySet(), emptySet(), emptySet()),
                SubtopicDef("Nuclear Physics", emptySet(), setOf(3518), emptySet()),
                SubtopicDef("Optics", emptySet(), setOf(3514, 3546), emptySet()),
                SubtopicDef("Particle Physics", emptySet(), setOf(3519, 3547), emptySet()),
                SubtopicDef("Quantum Mechanics", emptySet(), setOf(3516), emptySet()),
                SubtopicDef("Relativistic Mechanics", emptySet(), setOf(3517), emptySet()),
                SubtopicDef("Solid State Physics", emptySet(), setOf(3522), emptySet()),
            )
        ),
        TopicDef(
            number = "9.3",
            title = "Interdisciplinary Nature of Physics",
            mcqIds = emptySet(),
            shortIds = setOf(158, 3523, 3524, 3525, 3526, 3527, 3528, 3529, 3549, 3550, 3566, 3570),
            longIds = emptySet(),
            subtopics = listOf(
                SubtopicDef("Astrophysics", emptySet(), setOf(3526), emptySet()),
                SubtopicDef("Biophysics", emptySet(), setOf(3524, 3566), emptySet()),
                SubtopicDef("Climate Physics", emptySet(), setOf(3528), emptySet()),
                SubtopicDef("Computational Physics", emptySet(), setOf(3529), emptySet()),
                SubtopicDef("Geophysics", emptySet(), setOf(3527, 3550), emptySet()),
                SubtopicDef("Medical Physics", emptySet(), setOf(3525, 3549), emptySet()),
            )
        ),
        TopicDef(
            number = "9.4",
            title = "Interdisciplinary Research",
            mcqIds = emptySet(),
            shortIds = setOf(3551),
            longIds = setOf(80),
        ),
        TopicDef(
            number = "9.5",
            title = "Scientific Method",
            mcqIds = setOf(5741, 5742, 5743, 5744, 5745, 5746),
            shortIds = setOf(159, 160, 161, 163, 3530, 3531, 3532, 3533, 3534, 3535, 3536, 3537, 3553, 3554, 3555, 3556, 3557, 3558, 3567, 3571, 3572, 3574),
            longIds = setOf(74, 75, 77, 78, 79, 85, 86, 87, 6306),
            subtopics = listOf(
                SubtopicDef("Experiment", emptySet(), setOf(3533, 3555), emptySet()),
                SubtopicDef("Falsifiability", emptySet(), setOf(163, 3536, 3574), setOf(77)),
                SubtopicDef("Hypothesis", setOf(5744), setOf(160, 3532, 3554), emptySet()),
                SubtopicDef("Law", setOf(5741), setOf(3537, 3557, 3558), setOf(75, 78)),
                SubtopicDef("Observation", setOf(5746), setOf(3531, 3553), emptySet()),
                SubtopicDef("Prediction", setOf(5742), setOf(3535), emptySet()),
                SubtopicDef("Theory", setOf(5745), setOf(3534), setOf(74, 85)),
            )
        ),
        TopicDef(
            number = "9.6",
            title = "Scientific Base of Technologies and Engineering",
            mcqIds = setOf(5737, 5738, 5739, 5740),
            shortIds = setOf(162, 3538, 3539, 3552, 3559, 3560, 3561, 3562, 3563, 3564, 3565, 3573),
            longIds = setOf(76),
        ),
    )

    /** Longs that belong to no topic (shown at chapter level). */
    val phy9Ch9ChapterLongIds: Set<Int> = setOf(6307)

    /** Shorts that belong to no topic (shown at chapter level). */
    val phy9Ch9ChapterShortIds: Set<Int> = emptySet()

    /**
     * Topics for a chapter, or null when the chapter has no topic map.
     * Milestone 6 Phase 1 scope: Class 9 Physics Chapters 1-9.
     */
    fun topicsFor(classDigits: String, subject: String, chapterNumber: Int?): List<TopicDef>? {
        if (classDigits != "9" || !subject.trim().equals("Physics", ignoreCase = true)) return null
        return when (chapterNumber) {
            1 -> phy9Ch1Topics
            2 -> phy9Ch2Topics
            3 -> phy9Ch3Topics
            4 -> phy9Ch4Topics
            5 -> phy9Ch5Topics
            6 -> phy9Ch6Topics
            7 -> phy9Ch7Topics
            8 -> phy9Ch8Topics
            9 -> phy9Ch9Topics
            else -> null
        }
    }

    /** Longs shown at chapter level (no topic) for a chapter. */
    fun chapterLevelLongIds(classDigits: String, subject: String, chapterNumber: Int?): Set<Int> {
        if (classDigits != "9" || !subject.trim().equals("Physics", ignoreCase = true)) return emptySet()
        return when (chapterNumber) {
            1 -> phy9Ch1ChapterLongIds
            2 -> phy9Ch2ChapterLongIds
            3 -> phy9Ch3ChapterLongIds
            4 -> phy9Ch4ChapterLongIds
            5 -> phy9Ch5ChapterLongIds
            6 -> phy9Ch6ChapterLongIds
            7 -> phy9Ch7ChapterLongIds
            8 -> phy9Ch8ChapterLongIds
            9 -> phy9Ch9ChapterLongIds
            else -> emptySet()
        }
    }

    /** Shorts shown at chapter level (no topic) for a chapter. */
    fun chapterLevelShortIds(classDigits: String, subject: String, chapterNumber: Int?): Set<Int> {
        if (classDigits != "9" || !subject.trim().equals("Physics", ignoreCase = true)) return emptySet()
        return when (chapterNumber) {
            1 -> phy9Ch1ChapterShortIds
            2 -> phy9Ch2ChapterShortIds
            3 -> phy9Ch3ChapterShortIds
            4 -> phy9Ch4ChapterShortIds
            5 -> phy9Ch5ChapterShortIds
            6 -> phy9Ch6ChapterShortIds
            7 -> phy9Ch7ChapterShortIds
            8 -> phy9Ch8ChapterShortIds
            9 -> phy9Ch9ChapterShortIds
            else -> emptySet()
        }
    }

    /** MCQ ids of one topic (for MCQ Practice), or empty when unknown. */
    fun mcqIdsForTopic(classDigits: String, subject: String, chapterNumber: Int?, topicNumber: String): Set<Int> =
        topicsFor(classDigits, subject, chapterNumber)
            ?.firstOrNull { it.number == topicNumber }?.mcqIds ?: emptySet()
}
