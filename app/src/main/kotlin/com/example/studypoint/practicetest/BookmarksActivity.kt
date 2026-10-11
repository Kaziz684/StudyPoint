package com.example.studypoint.practicetest

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studypoint.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BookmarksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvCount: TextView
    private lateinit var adapter: BookmarkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)

        supportActionBar?.apply {
            title = "Saved Questions"
            setDisplayHomeAsUpEnabled(true)
        }

        recyclerView = findViewById(R.id.rvBookmarks)
        tvEmpty = findViewById(R.id.tvEmptyBookmarks)
        tvCount = findViewById(R.id.tvBookmarkCount)

        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btnClearBookmarks)?.setOnClickListener {
            confirmClearAll()
        }

        loadBookmarks()
    }

    override fun onResume() {
        super.onResume()
        loadBookmarks()
    }

    private fun loadBookmarks() {
        val bookmarks = BookmarkManager.getAll(this)
        tvCount.text = "${bookmarks.size} saved"

        if (bookmarks.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter = BookmarkAdapter(
                bookmarks,
                onRemove = { question ->
                    BookmarkManager.removeBookmark(this, question)
                    loadBookmarks()
                },
                onReview = { question ->
                    showQuestionDetail(question)
                }
            )
            recyclerView.adapter = adapter
        }
    }

    private fun showQuestionDetail(q: Question) {
        val optionsText = q.options.mapIndexed { i, opt ->
            val marker = if (i == q.correctAnswerIndex) "✓ " else "   "
            "$marker${('A' + i)}. $opt"
        }.joinToString("\n")

        val message = "${q.questionText}\n\n$optionsText" +
            (if (q.explanation.isNotBlank()) "\n\nExplanation: ${q.explanation}" else "")

        MaterialAlertDialogBuilder(this)
            .setTitle("${q.subject} • ${q.chapter}")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .setNeutralButton("Remove") { _, _ ->
                BookmarkManager.removeBookmark(this, q)
                loadBookmarks()
            }
            .show()
    }

    private fun confirmClearAll() {
        if (BookmarkManager.count(this) == 0) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear all bookmarks?")
            .setMessage("This will remove all saved questions.")
            .setPositiveButton("Clear") { _, _ ->
                BookmarkManager.clearAll(this)
                loadBookmarks()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
