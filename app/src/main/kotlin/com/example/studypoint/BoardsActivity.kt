package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BoardsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boards)

        // Set Header
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.headerText).text = "Punjab Educational Boards"

        val boards = listOf(
            BoardModel("BISE Lahore", "https://www.biselahore.com/"),
            BoardModel("BISE Lahore AI ChatBot", "https://chatbot.biselahore.com"),
            BoardModel("BISE Bahawalpur", "http://www.bisebwp.edu.pk/"),
            BoardModel("BISE D.G. Khan", "https://www.bisedgkhan.edu.pk/"),
            BoardModel("BISE Faisalabad", "https://www.bisefsd.edu.pk/"),
            BoardModel("BISE Gujranwala", "http://www.bisegrw.com/"),
            BoardModel("BISE Multan", "https://web.bisemultan.edu.pk/"),
            BoardModel("BISE Rawalpindi", "https://biserawalpindi.edu.pk/"),
            BoardModel("BISE Sahiwal", "https://bisesahiwal.edu.pk/"),
            BoardModel("BISE Sargodha", "https://www.bisesargodha.edu.pk/")
        )

        val recyclerView = findViewById<RecyclerView>(R.id.boardsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = BoardsAdapter(boards) { board ->
            val intent = Intent(this, BoardWebViewActivity::class.java)
            intent.putExtra("url", board.url)
            intent.putExtra("title", board.name)
            startActivity(intent)
        }
    }
}
