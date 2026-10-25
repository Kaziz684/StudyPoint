package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GradeSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_selection)

        // Set Header
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.headerText).text = "Select Your Class"
        findViewById<TextView>(R.id.headerText).setTextColor(android.graphics.Color.WHITE)

        val grades = listOf(
            GridItem("9", "9th Class", R.drawable.ic_simple_book, "(Matric)"),
            GridItem("10", "10th Class", R.drawable.ic_simple_pen, "(Board)"),
            GridItem("11", "11th Class", R.drawable.ic_simple_science, "(Inter)"),
            GridItem("12", "12th Class", R.drawable.ic_simple_cap, "(Pre-U)")
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        // Light standard background comes from the layout itself (edu_bg);
        // the old hardcoded dark override is removed so it can show.
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GenericGridAdapter(grades, useWhiteCard = true) { item ->
            val intent = Intent(this, SubjectsActivity::class.java)
            intent.putExtra("grade", item.id)
            startActivity(intent)
        }
    }
}
