package com.example.studypoint

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ClassSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classes)

        val tvClassTitle = findViewById<TextView>(R.id.tvClassTitle)
        tvClassTitle.text = "📚 Select Your Class"

        val recyclerView = findViewById<RecyclerView>(R.id.classRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 👉 2 columns
        recyclerView.adapter = ClassAdapter(this, listOf("9", "10", "11", "12"))
    }
}
