package com.example.studypoint

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class RecentItem(
    val title: String,
    val grade: String,
    val subject: String,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis()
)

object RecentReadManager {
    private const val PREF_NAME = "RecentReads"
    private const val KEY_RECENT = "recent_items"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val MAX_ITEMS = 10

    private val gson = Gson()

    fun addRecent(context: Context, item: RecentItem) {
        val recent = getRecent(context).toMutableList()
        recent.removeAll { it.fileName == item.fileName }
        recent.add(0, item)
        if (recent.size > MAX_ITEMS) {
            recent.removeAt(recent.size - 1)
        }
        saveRecent(context, recent)
    }

    fun getRecent(context: Context): List<RecentItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RECENT, null) ?: return emptyList()
        val type = object : TypeToken<List<RecentItem>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    private fun saveRecent(context: Context, list: List<RecentItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECENT, gson.toJson(list)).apply()
    }

    fun addBookmark(context: Context, item: RecentItem) {
        val bookmarks = getBookmarks(context).toMutableList()
        if (bookmarks.none { it.fileName == item.fileName }) {
            bookmarks.add(0, item)
            saveBookmarks(context, bookmarks)
        }
    }

    fun getBookmarks(context: Context): List<RecentItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        val type = object : TypeToken<List<RecentItem>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    private fun saveBookmarks(context: Context, list: List<RecentItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BOOKMARKS, gson.toJson(list)).apply()
    }
}
