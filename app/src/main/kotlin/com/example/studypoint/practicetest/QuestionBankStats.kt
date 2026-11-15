package com.example.studypoint.practicetest

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Live question-bank counts, parsed from the same public gviz CSV feeds the
 * app already reads for quizzes and papers. No local-backup merge: these are
 * the live sheet rows, counted — never the ID maxima (IDs have gaps).
 *
 * Class totals (verified 2026-11-15, live gviz parse):
 * Class 9 = 7,459 · Class 10 = 9,059 · Class 11 = 11,697 · Class 12 = 2,146
 * Total = 30,361 (MCQs 13,618 · Shorts 11,379 · Longs 5,364).
 *
 * Two sheet quirks are normalised here, exactly like the generator does:
 * - class_num "9" (544 rows) means "9th".
 * - the sheet writes "Math" where the app lists "Mathematics".
 */
object QuestionBankStats {

    data class Counts(val mcqs: Int = 0, val shorts: Int = 0, val longs: Int = 0) {
        val total: Int get() = mcqs + shorts + longs
    }

    data class BankSnapshot(
        val perClass: Map<String, Counts> = emptyMap(),
        val perClassSubject: Map<String, Map<String, Counts>> = emptyMap(),
        val total: Counts = Counts(),
        val fetchedAt: Long = 0L
    )

    /** 4 classes x 9 subjects (see subjectsFor() in PaperGeneratorActivity). */
    const val SUBJECT_COMBOS = 36

    val SUBJECTS_9_11: List<String> = listOf(
        "English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology",
        "Computer Science", "Islamiyat", "Tarjuma-tul-Quran"
    )
    val SUBJECTS_10_12: List<String> = listOf(
        "English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology",
        "Computer Science", "Pak Studies", "Tarjuma-tul-Quran"
    )

    fun subjectsForClassDigits(digits: String): List<String> =
        if (digits == "10" || digits == "12") SUBJECTS_10_12 else SUBJECTS_9_11

    fun formatCount(n: Int): String = NumberFormat.getInstance(Locale.US).format(n)

    private const val SHEET_ID = "1a1VGa7mYnFBe3fbFElx3jlFKFWBdYVGSW2AoHncm4uk"
    private const val PREFS = "question_bank_stats"
    private const val KEY_SNAPSHOT = "snapshot_json"
    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    @Volatile
    private var cached: BankSnapshot? = null

    /** Canonical app subject name for a sheet subject string, or null if unknown. */
    fun canonicalSubject(raw: String): String? {
        val s = raw.lowercase().trim()
        if (s.isEmpty()) return null
        CANONICAL[s]?.let { return it }
        val words = s.split(Regex("[^a-z]+")).filter { it.isNotBlank() }
        for ((key, value) in CANONICAL) {
            if (words.contains(key)) return value
        }
        return null
    }

    private val CANONICAL: Map<String, String> = mapOf(
        "english" to "English",
        "urdu" to "Urdu",
        "mathematics" to "Mathematics",
        "math" to "Mathematics",
        "physics" to "Physics",
        "chemistry" to "Chemistry",
        "biology" to "Biology",
        "bio" to "Biology",
        "computer science" to "Computer Science",
        "computer" to "Computer Science",
        "cs" to "Computer Science",
        "islamiyat" to "Islamiyat",
        "islamiat" to "Islamiyat",
        "tarjuma-tul-quran" to "Tarjuma-tul-Quran",
        "tarjuma" to "Tarjuma-tul-Quran",
        "quran" to "Tarjuma-tul-Quran",
        "tarjumatulquran" to "Tarjuma-tul-Quran",
        "pak studies" to "Pak Studies",
        "pakistan studies" to "Pak Studies",
        "pak study" to "Pak Studies"
    )

    /** Class digits ("9".."12") for a class_num cell ("9th", "9", "10th", ...). */
    fun classDigits(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        return if (digits in listOf("9", "10", "11", "12")) digits else null
    }

    /**
     * Loads live counts. Returns the in-memory snapshot when fresh, else the
     * last saved snapshot when the network fails, else null. Pass
     * [forceRefresh] to skip the in-memory cache.
     */
    suspend fun load(context: Context, forceRefresh: Boolean = false): BankSnapshot? {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val mem = cached
            if (mem != null && now - mem.fetchedAt < CACHE_TTL_MS) return mem
        }
        val fresh = try {
            fetchLive()
        } catch (e: Exception) {
            null
        }
        if (fresh != null) {
            cached = fresh
            saveSnapshot(context, fresh)
            return fresh
        }
        val saved = loadSaved(context)
        if (saved != null) {
            cached = saved
            return saved
        }
        return cached
    }

    /** Last snapshot saved on this device (instant paint), or null. */
    fun loadCached(context: Context): BankSnapshot? {
        cached?.let { return it }
        val saved = loadSaved(context)
        if (saved != null) cached = saved
        return saved
    }

    private fun loadSaved(context: Context): BankSnapshot? {
        return try {
            val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SNAPSHOT, null) ?: return null
            val type = object : TypeToken<BankSnapshot>() {}.type
            gson.fromJson<BankSnapshot>(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveSnapshot(context: Context, snapshot: BankSnapshot) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_SNAPSHOT, gson.toJson(snapshot))
                .apply()
        } catch (e: Exception) {
            // Cache is best-effort only.
        }
    }

    /** Fetches all six tabs concurrently and counts actual data rows. */
    private suspend fun fetchLive(): BankSnapshot = coroutineScope {
        val mcqTabs = listOf("9" to "Class_9th", "10" to "Class_10th", "11" to "Class_11th", "12" to "Class_12th")
        val mcqJobs = mcqTabs.map { (digits, tab) ->
            async(Dispatchers.IO) { digits to fetchCsv(tab) }
        }
        val shortsJob = async(Dispatchers.IO) { fetchCsv("Shorts") }
        val longsJob = async(Dispatchers.IO) { fetchCsv("Longs") }

        val perClass = mutableMapOf<String, Counts>()
        val perSubject = mutableMapOf<String, MutableMap<String, Counts>>()
        fun add(digits: String, subject: String?, mcq: Int, short: Int, long: Int) {
            val cur = perClass[digits] ?: Counts()
            perClass[digits] = Counts(cur.mcqs + mcq, cur.shorts + short, cur.longs + long)
            if (subject != null) {
                val bySubject = perSubject.getOrPut(digits) { mutableMapOf() }
                val sc = bySubject[subject] ?: Counts()
                bySubject[subject] = Counts(sc.mcqs + mcq, sc.shorts + short, sc.longs + long)
            }
        }
        for ((digits, csv) in mcqJobs.map { it.await() }) {
            // MCQ tabs: positional columns — 1=class_num, 2=subject, 4=questionText.
            for (row in parseCsvRows(csv)) {
                if (row.size < 5) continue
                if (row[0].equals("id", ignoreCase = true)) continue
                if (row[4].isBlank()) continue
                add(digits, canonicalSubject(row[2]), mcq = 1, short = 0, long = 0)
            }
        }
        for ((csv, isShort) in listOf(shortsJob.await() to true, longsJob.await() to false)) {
            // Shorts/Longs: columns by header name.
            val rows = parseCsvRows(csv)
            if (rows.isEmpty()) continue
            val header = rows.first().map { it.trim().lowercase() }
            val iClass = header.indexOf("class_num")
            val iSubject = header.indexOf("subject")
            val iQuestion = header.indexOf("question")
            if (iQuestion < 0) continue
            for (row in rows.drop(1)) {
                val q = if (iQuestion in row.indices) row[iQuestion].trim() else ""
                if (q.isBlank()) continue
                val digits = (if (iClass in row.indices) classDigits(row[iClass]) else null)
                    ?: continue
                val subject = if (iSubject in row.indices) canonicalSubject(row[iSubject]) else null
                if (isShort) add(digits, subject, mcq = 0, short = 1, long = 0)
                else add(digits, subject, mcq = 0, short = 0, long = 1)
            }
        }
        var total = Counts()
        for (c in perClass.values) total = Counts(total.mcqs + c.mcqs, total.shorts + c.shorts, total.longs + c.longs)
        BankSnapshot(
            perClass = perClass,
            perClassSubject = perSubject,
            total = total,
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun fetchCsv(sheet: String): String {
        val url = "https://docs.google.com/spreadsheets/d/$SHEET_ID/gviz/tq?tqx=out:csv&sheet=$sheet"
        val request = Request.Builder().url(url)
            .header("User-Agent", "StudyPoint-Android-App")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("gviz HTTP ${response.code} for $sheet")
            return response.body?.string() ?: throw IllegalStateException("empty gviz body for $sheet")
        }
    }

    /** Small RFC-4180 CSV tokenizer (quoted cells, commas and newlines inside quotes). */
    private fun parseCsvRows(csvText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = csvText.length
        while (i < len) {
            val c = csvText[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && csvText[i + 1] == '"') {
                        currentField.append('"'); i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < len && csvText[i + 1] == '\n') i++
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                        if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                        if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
        }
        return rows
    }
}
