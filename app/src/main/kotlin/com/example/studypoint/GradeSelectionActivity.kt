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
        // Dark premium background for glass design — must be applied to the layout
        // root and the list itself (android.R.id.content sits UNDER the layout's
        // own opaque background, so setting it there never shows).
        (recyclerView.parent as? android.view.View)?.setBackgroundResource(R.drawable.bg_class_dark)
        recyclerView.setBackgroundResource(R.drawable.bg_class_dark)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GenericGridAdapter(grades, useGlassCard = true) { item ->
            val intent = Intent(this, SubjectsActivity::class.java)
            intent.putExtra("grade", item.id)
            startActivity(intent)
        }
    }
}
