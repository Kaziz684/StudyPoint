package com.example.studypoint.practicetest

/**
 * A short/long (written) question from the "Shorts" or "Longs" tab of the
 * StudyPoint spreadsheet. Columns: id, class_num, subject, chapter_number,
 * chapter_name, question, answer, marks.
 */
data class WrittenQuestion(
    val id: Int,
    val classNum: String,
    val subject: String,
    val chapterNumber: String,
    val chapterName: String,
    val question: String,
    val answer: String,
    val marks: String
) {
    /** Display label used for chapter selection/filtering, e.g. "Chapter 12: Waves". */
    val chapterLabel: String
        get() = when {
            chapterName.isNotBlank() && chapterNumber.isNotBlank() -> "Chapter $chapterNumber: $chapterName"
            chapterName.isNotBlank() -> chapterName
            chapterNumber.isNotBlank() -> "Chapter $chapterNumber"
            else -> "General"
        }

    /** Numeric marks if the sheet provides them, else 0. */
    val marksValue: Int
        get() = marks.filter { it.isDigit() }.toIntOrNull() ?: 0
}
