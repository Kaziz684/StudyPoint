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


    data class Eng9Mcq(
        val id: Int,
        val question: String,
        val options: List<String>,
        val correct: Int,
        val answer: String
    )
    data class Eng9Trans(val chapter: Int, val english: String, val urdu: String)
    data class Eng9Summary(val poem: String, val summary: String)
    data class Eng9Prompt(val kind: String, val prompt: String, val text: String)
    data class Eng9Word(val word: String, val sentence: String)

    data class Eng9Pack(
        val verbs: List<Eng9Mcq>,
        val spelling: List<Eng9Mcq>,
        val meanings: List<Eng9Mcq>,
        val grammar: List<Eng9Mcq>,
        val voice: List<Eng9Mcq>,
        val translations: List<Eng9Trans>,
        val summaries: List<Eng9Summary>,
        val words: List<Eng9Word>,
        val prompts: List<Eng9Prompt>,
        val passages: List<Eng9Trans>,
        val urduToEnglish: List<Eng9Trans>
    )

    /** Class 9 English board-pattern pack (board_pattern_english9.json):
     *  Q.1 MCQ pools (verbs / spelling / meanings / grammar / voice) harvested
     *  from the live Class_9th sheet, Q.3 + Q.7 translation passages and
     *  Q.8 Urdu-to-English passages from the freeilm chapter notes'
     *  translation pairs, Q.4 poem summaries, Q.5 words with model sentences,
     *  Q.6 letter / story / dialogue prompts. All content is transcribed
     *  from the student's own notes — never invented. */
    fun loadEnglish9(context: Context): Eng9Pack? = try {
        val raw = context.assets.open("board_pattern_english9.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(raw)
        fun mcqs(name: String): List<Eng9Mcq> {
            val out = mutableListOf<Eng9Mcq>()
            root.optJSONArray(name)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val opts = mutableListOf<String>()
                    o.optJSONArray("options")?.let { oa ->
                        for (j in 0 until oa.length()) opts += oa.optString(j)
                    }
                    if (opts.size == 4) {
                        out += Eng9Mcq(
                            o.optInt("id"), o.optString("question"), opts,
                            o.optInt("correct"), o.optString("answer")
                        )
                    }
                }
            }
            return out
        }
        fun trans(name: String): List<Eng9Trans> {
            val out = mutableListOf<Eng9Trans>()
            root.optJSONArray(name)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out += Eng9Trans(
                        o.optInt("chapter"), o.optString("english"), o.optString("urdu")
                    )
                }
            }
            return out
        }
        val summaries = mutableListOf<Eng9Summary>()
        root.optJSONArray("q4Summaries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                summaries += Eng9Summary(o.optString("poem"), o.optString("summary"))
            }
        }
        val words = mutableListOf<Eng9Word>()
        root.optJSONArray("q5Sentences")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("word").isNotBlank()) {
                    words += Eng9Word(o.optString("word"), o.optString("sentence"))
                }
            }
        }
        val prompts = mutableListOf<Eng9Prompt>()
        root.optJSONArray("q6Prompts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("prompt").isNotBlank()) {
                    prompts += Eng9Prompt(
                        o.optString("kind"), o.optString("prompt"), o.optString("text")
                    )
                }
            }
        }
        val translations = trans("q3Translations")
        val passages = trans("q7Passages").ifEmpty { translations }
        Eng9Pack(
            verbs = mcqs("q1Verbs"),
            spelling = mcqs("q1Spelling"),
            meanings = mcqs("q1Meanings"),
            grammar = mcqs("q1Grammar"),
            voice = mcqs("q9Voice"),
            translations = translations,
            summaries = summaries,
            words = words,
            prompts = prompts,
            passages = passages,
            urduToEnglish = trans("q8UrduToEnglish")
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
