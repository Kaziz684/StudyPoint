package com.example.studypoint.practicetest

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches Short and Long (written) questions from the "Shorts" / "Longs" tabs
 * of the same StudyPoint spreadsheet the MCQs use, through the same public
 * Google Sheets CSV (gviz) feed as [QuizRepository]. No credentials involved.
 *
 * Expected columns (by header name, order-independent):
 * id, class_num, subject, chapter_number, chapter_name, question, answer, marks
 * Missing optional columns and blank rows are tolerated.
 */
class PaperRepository {

    companion object {
        const val SPREADSHEET_ID = "1a1VGa7mYnFBe3fbFElx3jlFKFWBdYVGSW2AoHncm4uk"
        const val TAB_SHORTS = "Shorts"
        const val TAB_LONGS = "Longs"
    }

    private val client = OkHttpClient()

    suspend fun fetchWritten(tab: String, context: Context? = null): List<WrittenQuestion> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://docs.google.com/spreadsheets/d/$SPREADSHEET_ID/gviz/tq?tqx=out:csv&sheet=$tab"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val csv = response.body?.string() ?: return@withContext emptyList()
                parseWrittenCsv(csv)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    private fun parseWrittenCsv(csv: String): List<WrittenQuestion> {
        val rows = parseCsv(csv)
        if (rows.isEmpty()) return emptyList()

        // Map columns by header name so column order in the sheet does not matter.
        val header = rows.first().map { it.trim().lowercase() }
        fun col(name: String): Int = header.indexOf(name)
        val iId = col("id")
        val iClass = col("class_num")
        val iSubject = col("subject")
        val iChNum = col("chapter_number")
        val iChName = col("chapter_name")
        val iQuestion = col("question")
        val iAnswer = col("answer")
        val iMarks = col("marks")
        if (iQuestion < 0) return emptyList()

        fun cell(row: List<String>, idx: Int): String =
            if (idx in row.indices) row[idx].trim() else ""

        val result = mutableListOf<WrittenQuestion>()
        for (row in rows.drop(1)) {
            val question = cell(row, iQuestion)
            if (question.isBlank()) continue // skip blank rows safely
            result += WrittenQuestion(
                id = cell(row, iId).toIntOrNull() ?: 0,
                classNum = cell(row, iClass),
                subject = cell(row, iSubject),
                chapterNumber = cell(row, iChNum),
                chapterName = cell(row, iChName),
                question = question,
                answer = cell(row, iAnswer),
                marks = cell(row, iMarks)
            )
        }
        return result
    }

    /** RFC-4180 style CSV parser (handles quoted cells, commas and newlines inside quotes). */
    private fun parseCsv(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        var currentCell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < csv.length && csv[i + 1] == '"') {
                            currentCell.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        currentCell.append(c)
                    }
                }

                c == '"' -> inQuotes = true
                c == ',' -> {
                    currentRow.add(currentCell.toString())
                    currentCell = StringBuilder()
                }

                c == '\n' -> {
                    currentRow.add(currentCell.toString())
                    currentCell = StringBuilder()
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                }

                c == '\r' -> { /* ignore */
                }

                else -> currentCell.append(c)
            }
            i++
        }
        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentCell.toString())
            rows.add(currentRow)
        }
        return rows
    }
}
