package com.example.studypoint.practicetest

import android.content.Context
import org.json.JSONObject

/**
 * Loads the Class 10 English board-pattern content pack from assets
 * (board_pattern_english10.json): lesson paragraphs for Q3, poem names and
 * paraphrase lines for Q4, essay / paragraph / ten-sentence topics for
 * Q5 and Q8, Urdu paragraphs for Q8, and the Pair of Words list for Q7.
 * All content is taken from the student's own notes / board pattern papers.
 */
object BoardPatternPack {

    data class ParaSet(val poem: String, val lines: String)
    data class Q3Para(val text: String, val urdu: String)
    data class Q8Para(val urdu: String, val english: String)
    data class PairWords(
        val first: String,
        val exampleFirst: String,
        val second: String,
        val exampleSecond: String
    )

    data class Pack(
        val summaryPoems: List<String>,
        val paraphraseSets: List<ParaSet>,
        val q3Paragraphs: List<Q3Para>,
        val q8Paragraphs: List<Q8Para>,
        val essayTopics: List<String>,
        val paragraphTopics: List<String>,
        val tenSentenceTopics: List<String>,
        val pairs: List<PairWords>
    )

    fun load(context: Context): Pack? = try {
        val raw = context.assets.open("board_pattern_english10.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(raw)
        fun strings(name: String): List<String> {
            val arr = root.optJSONArray(name) ?: return emptyList()
            return (0 until arr.length()).map { arr.optString(it) }
        }
        val paraSets = mutableListOf<ParaSet>()
        root.optJSONArray("paraphraseSets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                paraSets += ParaSet(o.optString("poem"), o.optString("lines"))
            }
        }
        val q3 = mutableListOf<Q3Para>()
        root.optJSONArray("q3Paragraphs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                q3 += Q3Para(o.optString("text"), o.optString("urdu"))
            }
        }
        val q8 = mutableListOf<Q8Para>()
        root.optJSONArray("q8Paragraphs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                q8 += Q8Para(o.optString("urdu"), o.optString("english"))
            }
        }
        val pairs = mutableListOf<PairWords>()
        root.optJSONArray("pairs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                pairs += PairWords(
                    o.optString("first"), o.optString("exampleFirst"),
                    o.optString("second"), o.optString("exampleSecond")
                )
            }
        }
        Pack(
            summaryPoems = strings("summaryPoems"),
            paraphraseSets = paraSets,
            q3Paragraphs = q3,
            q8Paragraphs = q8,
            essayTopics = strings("essayTopics"),
            paragraphTopics = strings("paragraphTopics"),
            tenSentenceTopics = strings("tenSentenceTopics"),
            pairs = pairs
        )
    } catch (e: Exception) {
        null
    }

    data class Eng11Pack(
        val letters: List<String>,
        val applications: List<String>,
        val stories: List<String>,
        val pairs: List<PairWords>,
        val referenceSets: List<ParaSet>,
        val punctuation: List<String>,
        val translationParas: List<Q3Para>,
        val essayTopics: List<String>
    )

    /** Class 11 English board-pattern pack (board_pattern_english11.json):
     *  letter / application topics for Q.5, story topics for Q.6, reference
     *  lines, punctuation passages and pairs for Q.7, and the English
     *  translation passage for Q.8 — all from the PECTAA model paper and
     *  the student's own grammar book contents. */
    fun loadEnglish11(context: Context): Eng11Pack? = try {
        val raw = context.assets.open("board_pattern_english11.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(raw)
        fun strings(name: String): List<String> {
            val arr = root.optJSONArray(name) ?: return emptyList()
            return (0 until arr.length()).map { arr.optString(it) }
        }
        val pairs = mutableListOf<PairWords>()
        root.optJSONArray("pairs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                pairs += PairWords(
                    o.optString("first"), o.optString("exampleFirst"),
                    o.optString("second"), o.optString("exampleSecond")
                )
            }
        }
        val refs = mutableListOf<ParaSet>()
        root.optJSONArray("referenceSets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                refs += ParaSet(o.optString("poem"), o.optString("lines"))
            }
        }
        val trans = mutableListOf<Q3Para>()
        root.optJSONArray("translationParas")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                trans += Q3Para(o.optString("text"), o.optString("urdu"))
            }
        }
        Eng11Pack(
            letters = strings("letters"),
            applications = strings("applications"),
            stories = strings("stories"),
            pairs = pairs,
            referenceSets = refs,
            punctuation = strings("punctuation"),
            translationParas = trans,
            essayTopics = strings("essayTopics")
        )
    } catch (e: Exception) {
        null
    }

    data class WordMeaning(val word: String, val meaning: String, val surah: String)

    /** Class 10 Tarjuma-tul-Quran board-pattern word meanings (board_pattern_tarjuma.json):
     *  56 words with meanings and their surah / chapter reference. */
    fun loadTarjuma(context: Context): List<WordMeaning> = try {
        val raw = context.assets.open("board_pattern_tarjuma.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(raw)
        val out = mutableListOf<WordMeaning>()
        root.optJSONArray("meanings")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out += WordMeaning(
                    o.optString("word"), o.optString("meaning"), o.optString("surah")
                )
            }
        }
        out
    } catch (e: Exception) {
        emptyList()
    }
}
