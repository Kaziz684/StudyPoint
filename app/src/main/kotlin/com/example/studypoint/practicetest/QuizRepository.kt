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
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Fetches questions directly from the Google Sheet CSV stream:
     * https://docs.google.com/spreadsheets/d/1a1VGa7mYnFBe3fbFElx3jlFKFWBdYVGSW2AoHncm4uk/gviz/tq?tqx=out:csv&sheet={Class_Name}
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
        val csvUrl = "https://docs.google.com/spreadsheets/d/1a1VGa7mYnFBe3fbFElx3jlFKFWBdYVGSW2AoHncm4uk/gviz/tq?tqx=out:csv&sheet=$sheetParam"

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

        // 2. Load Local Backup from assets/mcqs_backup.json
        val localBackup = loadLocalBackup(appCtx, classNum)

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
