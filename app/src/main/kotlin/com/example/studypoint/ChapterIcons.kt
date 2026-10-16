package com.example.studypoint

/**
 * Picks a chapter icon (colored circle + white glyph, ic_ch_* vectors) for the
 * chapter/section lists. Topical match from the title first, then a per-subject
 * pool by chapter number, then the subject's own icon as a safe fallback.
 */
object ChapterIcons {

    private fun keyed(title: String): Int? {
        val t = title.lowercase()
        return when {
            // Biology systems & topics
            "digestive" in t -> R.drawable.ic_ch_stomach
            "respiratory" in t -> R.drawable.ic_ch_lungs
            "circulatory" in t || "blood" in t -> R.drawable.ic_ch_heart
            "urinary" in t || "excretory" in t -> R.drawable.ic_ch_kidney
            "coordination" in t || "nervous" in t -> R.drawable.ic_ch_brain
            "inheritance" in t || "genetics" in t -> R.drawable.ic_ch_dna
            "biotechnology" in t -> R.drawable.ic_ch_flask
            "diseases" in t || "immunity" in t -> R.drawable.ic_ch_shield_virus
            "evolution" in t || "ecology" in t -> R.drawable.ic_ch_evolution
            "biodiversity" in t -> R.drawable.ic_ch_leaf
            "biomolecules" in t -> R.drawable.ic_ch_molecule
            "bioenergetics" in t -> R.drawable.ic_ch_sun
            "biostatistics" in t -> R.drawable.ic_ch_chart
            "reproduction in plants" in t -> R.drawable.ic_ch_flower
            "reproduction" in t -> R.drawable.ic_ch_sprout
            "cell cycle" in t -> R.drawable.ic_ch_cycle
            "cell" in t -> R.drawable.ic_ch_cell
            "enzymes" in t -> R.drawable.ic_ch_flask
            "tissues" in t -> R.drawable.ic_ch_layers
            "plant physiology" in t -> R.drawable.ic_ch_flower
            "science of biology" in t -> R.drawable.ic_ch_microscope
            // Physics topics
            "measurements" in t || "quantities" in t -> R.drawable.ic_ch_ruler
            "kinematics" in t -> R.drawable.ic_ch_motion
            "dynamics" in t -> R.drawable.ic_ch_force
            "turning effects" in t -> R.drawable.ic_ch_balance
            "mechanical properties" in t -> R.drawable.ic_ch_spring
            "magnetism" in t && "electro" !in t -> R.drawable.ic_ch_magnet
            "nature of science" in t -> R.drawable.ic_ch_book
            "thermal" in t -> R.drawable.ic_ch_thermo
            "waves" in t && "sound" !in t -> R.drawable.ic_ch_wave
            "sound" in t -> R.drawable.ic_ch_sound
            "light" in t -> R.drawable.ic_ch_light
            "electrostatics" in t -> R.drawable.ic_ch_charge
            "electromagnetism" in t || "electromagnetic" in t -> R.drawable.ic_ch_magnet
            "electricity" in t -> R.drawable.ic_ch_circuit
            "electronics" in t -> R.drawable.ic_ch_chip
            "atomic" in t || "nuclear" in t -> R.drawable.ic_ch_atom
            "space" in t -> R.drawable.ic_ch_globe
            // Chemistry topics
            "states of matter" in t -> R.drawable.ic_ch_states
            "atomic structure" in t -> R.drawable.ic_ch_atom
            "bonding" in t -> R.drawable.ic_ch_bond
            "stoichiometry" in t -> R.drawable.ic_ch_scale
            "energetics" in t -> R.drawable.ic_ch_fire
            "equilibria" in t -> R.drawable.ic_ch_equilibrium
            "acid" in t -> R.drawable.ic_ch_testtube
            "periodic" in t -> R.drawable.ic_ch_periodic
            "hydrocarbons" in t -> R.drawable.ic_ch_hexagon
            "environmental chemistry" in t -> R.drawable.ic_ch_leaf_chem
            "laboratory" in t -> R.drawable.ic_ch_flask
            // Computer Science topics
            "computational systems" in t -> R.drawable.ic_ch_monitor
            "troubleshooting" in t -> R.drawable.ic_ch_wrench
            "networks" in t -> R.drawable.ic_ch_network
            "computational thinking" in t -> R.drawable.ic_ch_puzzle
            "web development" in t -> R.drawable.ic_ch_web
            "data science" in t || "data gathering" in t -> R.drawable.ic_ch_database
            "emerging technologies" in t -> R.drawable.ic_ch_ai
            "ethical" in t || "legal" in t -> R.drawable.ic_ch_gavel
            "entrepreneurship" in t -> R.drawable.ic_ch_rocket
            // Mathematics topics (specific first)
            "real numbers" in t || "complex numbers" in t -> R.drawable.ic_ch_numbers
            "logarithms" in t || "logarithmic tables" in t -> R.drawable.ic_ch_log
            "sets and functions" in t || "sets" in t -> R.drawable.ic_ch_sets
            "factorization" in t -> R.drawable.ic_ch_factor
            "linear equations" in t || "quadratic" in t -> R.drawable.ic_ch_line_eq
            "trigonometry" in t -> R.drawable.ic_ch_trig
            "coordinate geometry" in t -> R.drawable.ic_ch_coords
            "logic" in t -> R.drawable.ic_ch_logic
            "similar figures" in t -> R.drawable.ic_ch_similar
            "graphs of functions" in t || "functions and graphs" in t -> R.drawable.ic_ch_graph
            "loci" in t || "construction" in t -> R.drawable.ic_ch_compass
            "matrices" in t -> R.drawable.ic_ch_matrix
            "algebraic fractions" in t -> R.drawable.ic_ch_fraction
            "vectors" in t -> R.drawable.ic_ch_vector
            "chords" in t || "practical geometry of circles" in t -> R.drawable.ic_ch_circle
            "tangent" in t -> R.drawable.ic_ch_tangent
            "information handling" in t -> R.drawable.ic_ch_chart
            "probability" in t -> R.drawable.ic_ch_numbers
            "answers" in t -> R.drawable.ic_ch_check_answers
            "glossary" in t -> R.drawable.ic_ch_book_open
            "symbols" in t -> R.drawable.ic_ch_sigma
            "bibliography" in t || "index" in t -> R.drawable.ic_ch_book
            // English grammar topics
            "direct" in t && "indirect" in t -> R.drawable.ic_ch_quote
            "pair of words" in t -> R.drawable.ic_ch_pair
            "correct use of verb" in t -> R.drawable.ic_ch_verb
            "tenses" in t -> R.drawable.ic_ch_clock
            "active" in t && "passive" in t -> R.drawable.ic_ch_chat
            "letters" in t || "applications" in t -> R.drawable.ic_ch_pencil
            "stories" in t -> R.drawable.ic_ch_scroll
            "paragraphs" in t -> R.drawable.ic_ch_paragraph
            "sentence correction" in t -> R.drawable.ic_ch_check_answers
            "comprehension" in t -> R.drawable.ic_ch_book_open
            "essays" in t -> R.drawable.ic_ch_pencil
            "translation" in t -> R.drawable.ic_ch_translate
            "synonyms" in t || "antonyms" in t || "idioms" in t || "one word" in t -> R.drawable.ic_ch_book_open
            else -> null
        }
    }

    private val phyPool = listOf(
        R.drawable.ic_ch_ruler, R.drawable.ic_ch_motion, R.drawable.ic_ch_force,
        R.drawable.ic_ch_balance, R.drawable.ic_ch_bolt, R.drawable.ic_ch_spring,
        R.drawable.ic_ch_thermo, R.drawable.ic_ch_magnet, R.drawable.ic_ch_wave,
        R.drawable.ic_ch_sound, R.drawable.ic_ch_light, R.drawable.ic_ch_charge,
        R.drawable.ic_ch_circuit, R.drawable.ic_ch_atom, R.drawable.ic_ch_globe,
        R.drawable.ic_ch_chip, R.drawable.ic_ch_book
    )
    private val chemPool = listOf(
        R.drawable.ic_ch_states, R.drawable.ic_ch_atom, R.drawable.ic_ch_bond,
        R.drawable.ic_ch_scale, R.drawable.ic_ch_fire, R.drawable.ic_ch_equilibrium,
        R.drawable.ic_ch_testtube, R.drawable.ic_ch_periodic, R.drawable.ic_ch_molecule,
        R.drawable.ic_ch_hexagon, R.drawable.ic_ch_flask, R.drawable.ic_ch_leaf_chem
    )
    private val bioPool = listOf(
        R.drawable.ic_ch_microscope, R.drawable.ic_ch_leaf, R.drawable.ic_ch_cell,
        R.drawable.ic_ch_cycle, R.drawable.ic_ch_layers, R.drawable.ic_ch_molecule,
        R.drawable.ic_ch_flask, R.drawable.ic_ch_sun, R.drawable.ic_ch_flower,
        R.drawable.ic_ch_sprout, R.drawable.ic_ch_chart, R.drawable.ic_ch_dna,
        R.drawable.ic_ch_heart, R.drawable.ic_ch_brain, R.drawable.ic_ch_evolution
    )
    private val csPool = listOf(
        R.drawable.ic_ch_monitor, R.drawable.ic_ch_wrench, R.drawable.ic_ch_network,
        R.drawable.ic_ch_puzzle, R.drawable.ic_ch_web, R.drawable.ic_ch_database,
        R.drawable.ic_ch_ai, R.drawable.ic_ch_gavel, R.drawable.ic_ch_rocket
    )
    private val mathPool = listOf(
        R.drawable.ic_ch_numbers, R.drawable.ic_ch_log, R.drawable.ic_ch_sets,
        R.drawable.ic_ch_factor, R.drawable.ic_ch_line_eq, R.drawable.ic_ch_trig,
        R.drawable.ic_ch_coords, R.drawable.ic_ch_logic, R.drawable.ic_ch_similar,
        R.drawable.ic_ch_graph, R.drawable.ic_ch_compass, R.drawable.ic_ch_sqrt,
        R.drawable.ic_ch_matrix, R.drawable.ic_ch_fraction, R.drawable.ic_ch_vector,
        R.drawable.ic_ch_circle, R.drawable.ic_ch_tangent, R.drawable.ic_ch_sigma
    )
    private val genPool = listOf(
        R.drawable.ic_ch_book_open, R.drawable.ic_ch_pencil, R.drawable.ic_ch_scroll,
        R.drawable.ic_ch_quote, R.drawable.ic_ch_paragraph, R.drawable.ic_ch_translate
    )

    fun forChapter(subject: String, itemId: String, title: String): Int {
        keyed(title)?.let { return it }
        val num = Regex("""\d+""").find(itemId)?.value?.toIntOrNull() ?: 1
        val s = subject.lowercase()
        val pool = when {
            s.contains("physics") -> phyPool
            s.contains("chemistry") -> chemPool
            s.contains("biology") || s.contains("bio") -> bioPool
            s.contains("computer") -> csPool
            s.contains("math") -> mathPool
            else -> genPool
        }
        return pool[(num - 1).coerceAtLeast(0) % pool.size]
    }
}
