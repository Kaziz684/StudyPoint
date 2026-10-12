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

        val grades = listOf(
            GridItem("9", "نہم کلاس (میٹرک)", R.drawable.ic_class_9_rich),
            GridItem("10", "دہم کلاس (بورڈ)", R.drawable.ic_class_10_rich),
            GridItem("11", "گیارہویں کلاس (انٹر)", R.drawable.ic_class_11_rich),
            GridItem("12", "بارہویں کلاس (پری-یو)", R.drawable.ic_class_12_rich)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GenericGridAdapter(grades) { item ->
            val intent = Intent(this, SubjectsActivity::class.java)
            intent.putExtra("grade", item.id)
            startActivity(intent)
        }
    }
}
