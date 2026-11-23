package com.example.studypoint.practicetest

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class QuizRepository(private val context: Context? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        // In-memory cache, shared by every activity (each one builds its own
        // QuizRepository, so an instance cache would never survive a screen
        // change). 10 minutes, same as QuestionBankStats: reopening a screen
        // is instant, and sheet edits still show up within minutes — a fresh
        // app start always downloads once, so nothing goes stale for long.
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
        private val cache = mutableMapOf<String, Pair<List<Question>, Long>>()
        private val cacheLock = Any()

        private fun cached(sheetParam: String): List<Question>? {
            synchronized(cacheLock) {
                val (list, at) = cache[sheetParam] ?: return null
                if (System.currentTimeMillis() - at > CACHE_TTL_MS) {
                    cache.remove(sheetParam)
                    return null
                }
                return list
            }
        }

        private fun store(sheetParam: String, list: List<Question>) {
            synchronized(cacheLock) {
                cache[sheetParam] = list to System.currentTimeMillis()
            }
        }

        /** True when the live rows already cover every subject of the class. */
        private fun coversAllSubjects(remote: List<Question>, classNum: String): Boolean {
            val want = if (classNum == "10th" || classNum == "12th") {
                setOf("english", "urdu", "mathematics", "physics", "chemistry", "biology", "computer science", "pak studies", "tarjuma-tul-quran")
            } else {
                setOf("english", "urdu", "mathematics", "physics", "chemistry", "biology", "computer science", "islamiyat", "tarjuma-tul-quran")
            }
            val have = remote.map { normalizeSubject(it.subject) }.toSet()
            return want.all { it in have }
        }

        private fun normalizeSubject(raw: String): String {
            val s = raw.lowercase().trim()
            return when {
                s == "math" -> "mathematics"
                s == "pakistan studies" || s == "pak study" -> "pak studies"
                s == "islamiat" -> "islamiyat"
                s == "tarjuma" || s == "quran" || s == "tarjumatulquran" -> "tarjuma-tul-quran"
                s == "computer" || s == "cs" -> "computer science"
                s == "bio" -> "biology"
                else -> s
            }
        }
    }

    /**
     * Fetches questions directly from the Google Sheet CSV stream:
     * https://docs.google.com/spreadsheets/d/1a1VGa7mYnFBe3fbFElx3jlFKFWBdYVGSW2AoHncm4uk/gviz/tq?tqx=out:csv&sheet={Class_Name}&headers=1
     *
     * NOTE (v156): headers=1 is REQUIRED, not cosmetic. Without it the
     * Class_11th feed collapses whole columns into single mega-cells
     * (Nov 2026: one row came back with a 132,106-char chapter field and a
     * 30,689-char subject field), which rendered as one giant chapter card
     * in every Class 11 subject and swallowed ~350 Physics + ~23 Math MCQs.
     * With headers=1 the same feed parses cleanly (6,632 questions).
     *
     * If network is slow, offline, or if the sheet is missing rows for a specific subject,
     * seamlessly combines or falls back to local assets/mcqs_backup.json.
     */
    suspend fun fetchQuestions(sheetName: String, context: Context? = null): List<Question> = withContext(Dispatchers.IO) {
        val appCtx = context ?: this@QuizRepository.context
        val classNum = when {
            sheetName.contains("9") -> "9th"
            sheetName.contains("10") -> "10th"
            sheetName.contains("11") -> "11th"
            sheetName.contains("12") -> "12th"
            else -> sheetName.replace("Class_", "").trim()
        }
        val sheetParam = "Class_${classNum}"
        // Fast path: this class tab was already downloaded minutes ago —
        // every subject/class change used to re-download the whole tab.
        cached(sheetParam)?.let { return@withContext it }
        val csvUrl = "https://docs.google.com/spreadsheets/d/1a1VGa7mYnFBe3fbFElx3jlFKFWBdYVGSW2AoHncm4uk/gviz/tq?tqx=out:csv&sheet=$sheetParam&headers=1"

        var remoteQuestions: List<Question> = emptyList()

        // 1. Attempt live Google Sheet CSV stream fetch
        try {
            val request = Request.Builder()
                .url(csvUrl)
                .header("User-Agent", "StudyPoint-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val csvBody = response.body?.string()
                if (!csvBody.isNullOrBlank()) {
                    remoteQuestions = parseCsvToQuestions(csvBody, classNum)
                    Log.d("QuizRepository", "Successfully parsed ${remoteQuestions.size} questions from live Google Sheet CSV for $sheetParam")
                }
            } else {
                Log.w("QuizRepository", "Google Sheet CSV returned status code ${response.code}")
            }
        } catch (e: Exception) {
            Log.w("QuizRepository", "Google Sheet fetch failed or timed out (${e.message}), utilizing local backup.")
        }

        // 2. Load Local Backup from assets/mcqs_backup.json — ONLY when the
        // live sheet is empty or is missing a subject. It used to be parsed
        // from assets on EVERY fetch even when the live sheet was complete.
        val localBackup = if (remoteQuestions.isEmpty() || !coversAllSubjects(remoteQuestions, classNum)) {
            loadLocalBackup(appCtx, classNum)
        } else {
            emptyList()
        }

        // 3. Fallback / Merge strategy:
        // If remote questions are available, ensure all subjects have questions by filling any missing subjects from local backup
        val combined = if (remoteQuestions.isNotEmpty()) {
            val remoteSubjects = remoteQuestions.map { it.subject.trim().lowercase() }.toSet()
            val missingFromRemote = localBackup.filter { localQ ->
                !remoteSubjects.contains(localQ.subject.trim().lowercase())
            }
            remoteQuestions + missingFromRemote
        } else {
            localBackup
        }

        if (combined.isNotEmpty()) store(sheetParam, combined)
        Log.d("QuizRepository", "Total questions ready for session: ${combined.size} ($classNum)")
        combined
    }

    private fun loadLocalBackup(context: Context?, classNum: String): List<Question> {
        if (context == null) return emptyList()
        return try {
            context.assets.open("mcqs_backup.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<Question>>() {}.type
                    val allLocal: List<Question> = gson.fromJson(reader, type)
                    allLocal.filter { it.class_num.equals(classNum, ignoreCase = true) }
                }
            }
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error reading mcqs_backup.json from assets: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Parses standard Google Sheet CSV rows into structured Question objects.
     * Supports both comma-separated options column and separate 4-option columns.
     */
    private fun parseCsvToQuestions(csvText: String, defaultClass: String): List<Question> {
        val rows = parseCsvRows(csvText)
        val questions = mutableListOf<Question>()

        for (row in rows) {
            if (row.size < 6) continue

            val idStr = row.getOrNull(0) ?: ""
            // Filter header row
            if (idStr.equals("id", ignoreCase = true) || row.getOrNull(1).equals("class_num", ignoreCase = true)) {
                continue
            }

            val classNum = row.getOrNull(1)?.ifBlank { defaultClass } ?: defaultClass
            val subject = row.getOrNull(2) ?: ""
            val chapter = row.getOrNull(3) ?: ""
            val questionText = row.getOrNull(4) ?: ""

            // Guard against a malformed gviz mega-row (see the headers=1 note
            // above): a real chapter/subject name is short, so drop any row
            // whose chapter or subject is absurdly long instead of letting a
            // collapsed feed render as one giant chapter card.
            if (chapter.length > 200 || subject.length > 100) continue

            if (questionText.isBlank()) continue

            val options: List<String>
            val ansIndex: Int
            val explanation: String

            if (row.size >= 11 && row.getOrNull(8)?.isNotBlank() == true && row.getOrNull(9)?.toIntOrNull() != null) {
                // Separate Option columns (A, B, C, D)
                options = listOf(
                    row.getOrNull(5) ?: "",
                    row.getOrNull(6) ?: "",
                    row.getOrNull(7) ?: "",
                    row.getOrNull(8) ?: ""
                )
                ansIndex = row.getOrNull(9)?.toIntOrNull() ?: 0
                explanation = row.getOrNull(10) ?: ""
            } else {
                // Comma-separated options in single column
                val rawOptions = row.getOrNull(5) ?: ""
                options = rawOptions.split(",").map { it.trim() }.filter { it.isNotBlank() }
                ansIndex = row.getOrNull(6)?.toIntOrNull() ?: 0
                explanation = row.getOrNull(7) ?: ""
            }

            val parsedId = idStr.toIntOrNull() ?: (questions.size + 1)

            questions.add(
                Question(
                    id = parsedId,
                    class_num = classNum,
                    subject = subject,
                    chapter = chapter,
                    questionText = questionText,
                    options = if (options.size >= 2) options else listOf("Option A", "Option B", "Option C", "Option D"),
                    correctAnswerIndex = if (ansIndex in options.indices) ansIndex else 0,
                    explanation = explanation
                )
            )
        }
        return questions
    }

    /**
     * RFC 4180 compliant CSV tokenizer handling multiline fields and escaped quotes.
     */
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
                        currentField.append('"')
                        i++
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
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }
        return rows
    }
}
