package com.example.studypoint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView




class ChaptersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters)  // layout set karna zaruri hai

        // RecyclerView ka reference
        val recyclerView = findViewById<RecyclerView>(R.id.chapterListLayout)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Example: chapters
        val chapters = listOf(
            "Chapter 1" to "https://drive.google.com/...",
            "Chapter 2" to "https://drive.google.com/...",
            "Chapter 3" to "https://drive.google.com/..."
        )

        // Adapter set karna
        recyclerView.adapter = NotesAdapter(chapters) { chapter, link ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            startActivity(intent)
        }
    }
}
