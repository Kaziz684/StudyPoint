package com.example.studypoint

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class RecentItem(
    val title: String,
    val grade: String,
    val subject: String,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** 1-based page number for PDF bookmarks / last-read; 0 = unknown. */
    val page: Int = 0
)

data class CollectionItem(
    val title: String,
    val grade: String,
    val subject: String,
    val fileName: String,
    val page: Int = 0,
    val collection: String,
    val timestamp: Long = System.currentTimeMillis()
)

object RecentReadManager {
    private const val PREF_NAME = "RecentReads"
    private const val KEY_RECENT = "recent_items"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val KEY_LAST_PAGES = "last_pages"
    private const val KEY_COLLECTIONS = "collections"
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
        // Re-bookmarking replaces the old entry so the saved page updates.
        bookmarks.removeAll { it.fileName == item.fileName }
        bookmarks.add(0, item)
        saveBookmarks(context, bookmarks)
    }

    fun removeBookmark(context: Context, fileName: String) {
        val bookmarks = getBookmarks(context).toMutableList()
        if (bookmarks.removeAll { it.fileName == fileName }) {
            saveBookmarks(context, bookmarks)
        }
    }

    fun isBookmarked(context: Context, fileName: String): Boolean {
        return getBookmarks(context).any { it.fileName == fileName }
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

    /** Last-read page (1-based) per PDF, keyed by the PDF url. 0 = none saved. */
    fun saveLastPage(context: Context, fileName: String, page: Int) {
        val pages = getLastPages(context).toMutableMap()
        pages[fileName] = page
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_PAGES, gson.toJson(pages)).apply()
    }

    fun getLastPage(context: Context, fileName: String): Int {
        return getLastPages(context)[fileName] ?: 0
    }

    private fun getLastPages(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LAST_PAGES, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyMap() }
    }

    fun addToCollection(context: Context, item: CollectionItem) {
        val items = getCollections(context).toMutableList()
        items.removeAll { it.fileName == item.fileName && it.collection == item.collection }
        items.add(0, item)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_COLLECTIONS, gson.toJson(items)).apply()
    }

    fun getCollections(context: Context): List<CollectionItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_COLLECTIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<CollectionItem>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun getCollectionNames(context: Context): List<String> {
        return getCollections(context).map { it.collection }.distinct()
    }
}
