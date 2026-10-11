package com.example.studypoint.practicetest

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages bookmarked quiz questions using SharedPreferences.
 * Stores full question data as JSON so bookmarks work offline.
 */
object BookmarkManager {

    private const val PREFS_NAME = "bookmarks_prefs"
    private const val KEY_BOOKMARKS = "bookmarked_questions"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun questionToJson(q: Question): JSONObject {
        return JSONObject().apply {
            put("id", q.id)
            put("class_num", q.class_num)
            put("subject", q.subject)
            put("chapter", q.chapter)
            put("questionText", q.questionText)
            put("options", JSONArray(q.options))
            put("correctAnswerIndex", q.correctAnswerIndex)
            put("explanation", q.explanation)
        }
    }

    private fun jsonToQuestion(obj: JSONObject): Question {
        val optionsArray = obj.getJSONArray("options")
        val options = mutableListOf<String>()
        for (i in 0 until optionsArray.length()) {
            options.add(optionsArray.getString(i))
        }
        return Question(
            id = obj.getInt("id"),
            class_num = obj.optString("class_num", ""),
            subject = obj.optString("subject", ""),
            chapter = obj.optString("chapter", ""),
            questionText = obj.getString("questionText"),
            options = options,
            correctAnswerIndex = obj.getInt("correctAnswerIndex"),
            explanation = obj.optString("explanation", "")
        )
    }

    /** Unique key for a question (id + subject + class to avoid collisions). */
    private fun questionKey(q: Question): String = "${q.class_num}_${q.subject}_${q.id}"

    fun isBookmarked(context: Context, q: Question): Boolean {
        return getAll(context).any { questionKey(it) == questionKey(q) }
    }

    fun toggleBookmark(context: Context, q: Question): Boolean {
        val current = getAll(context).toMutableList()
        val key = questionKey(q)
        val existing = current.indexOfFirst { questionKey(it) == key }
        return if (existing >= 0) {
            current.removeAt(existing)
            saveAll(context, current)
            false // removed
        } else {
            current.add(q)
            saveAll(context, current)
            true // added
        }
    }

    fun removeBookmark(context: Context, q: Question) {
        val current = getAll(context).toMutableList()
        val key = questionKey(q)
        current.removeAll { questionKey(it) == key }
        saveAll(context, current)
    }

    fun getAll(context: Context): List<Question> {
        val json = prefs(context).getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { jsonToQuestion(array.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun count(context: Context): Int = getAll(context).size

    private fun saveAll(context: Context, questions: List<Question>) {
        val array = JSONArray()
        questions.forEach { array.put(questionToJson(it)) }
        prefs(context).edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_BOOKMARKS).apply()
    }
}
