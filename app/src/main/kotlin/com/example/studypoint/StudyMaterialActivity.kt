package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class StudyMaterialActivity : AppCompatActivity() {

    private lateinit var selectedClass: String
    private lateinit var selectedSubject: String

    // 🔹 Chapter buttons for Notes (up to 9 chapters)
    private val noteButtons = listOf(
        R.id.btnCh1, R.id.btnCh2, R.id.btnCh3,
        R.id.btnCh4, R.id.btnCh5, R.id.btnCh6,
        R.id.btnCh7, R.id.btnCh8, R.id.btnCh9
    )

    // 🔹 PDF Links map
    private val pdfLinks = mapOf(
        "9" to mapOf(
            "English" to mapOf(
                "Notes" to listOf(
                    "https://drive.google.com/uc?export=download&id=9_ENGLISH_NOTES_CH1",
                    "https://drive.google.com/uc?export=download&id=9_ENGLISH_NOTES_CH2",
                    "https://drive.google.com/uc?export=download&id=9_ENGLISH_NOTES_CH3"
                ),
                "Handwritten" to "https://drive.google.com/uc?export=download&id=9_ENGLISH_HANDWRITTEN",
                "Book" to "https://drive.google.com/uc?export=download&id=9_ENGLISH_BOOK",
                "Keybook" to "https://drive.google.com/uc?export=download&id=9_ENGLISH_KEYBOOK"
            ),
            "Biology" to mapOf(
                "Notes" to listOf(
                    "https://drive.google.com/uc?export=download&id=9_BIO_NOTES_CH1",
                    "https://drive.google.com/uc?export=download&id=9_BIO_NOTES_CH2"
                ),
                "Handwritten" to "https://drive.google.com/uc?export=download&id=9_BIO_HANDWRITTEN",
                "Book" to "https://drive.google.com/uc?export=download&id=9_BIO_BOOK",
                "Keybook" to "https://drive.google.com/uc?export=download&id=163NTXZm7xBUo0qVEmJ-S0dUcEyy1__HR"
            )
            // 🔹 Add more 9th subjects here
        ),
        "10" to mapOf(
            "English" to mapOf(
                "Notes" to listOf(
                    "https://drive.google.com/uc?export=download&id=10_ENGLISH_NOTES_CH1",
                    "https://drive.google.com/uc?export=download&id=10_ENGLISH_NOTES_CH2"
                ),
                "Handwritten" to "https://drive.google.com/uc?export=download&id=10_ENGLISH_HANDWRITTEN",
                "Book" to "https://drive.google.com/uc?export=download&id=10_ENGLISH_BOOK",
                "Keybook" to "https://drive.google.com/uc?export=download&id=10_ENGLISH_KEYBOOK"
            )
            // 🔹 Add more 10th subjects
        )
        // 🔹 11th & 12th similarly
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study_material)

        selectedClass = intent.getStringExtra("class") ?: "9"
        selectedSubject = intent.getStringExtra("subject") ?: "English"

        // Buttons
        val btnNotes = findViewById<Button>(R.id.btnNotes)
        val btnHandwritten = findViewById<Button>(R.id.btnHandwritten)
        val btnBook = findViewById<Button>(R.id.btnBook)
        val btnKeybook = findViewById<Button>(R.id.btnKeybook)

        btnNotes.setOnClickListener { openNotes() }
        btnHandwritten.setOnClickListener { openSinglePdf("Handwritten") }
        btnBook.setOnClickListener { openSinglePdf("Book") }
        btnKeybook.setOnClickListener { openSinglePdf("Keybook") }
    }

    // 🔹 Open Notes chapter-wise
    private fun openNotes() {
        val linksAny = pdfLinks[selectedClass]?.get(selectedSubject)?.get("Notes")
        val links = linksAny as? List<String> ?: return

        noteButtons.forEachIndexed { index, id ->
            val btn = findViewById<MaterialButton>(id)
            if (index < links.size) {
                btn.visibility = View.VISIBLE
                btn.text = "Chapter ${index + 1}"
                btn.setOnClickListener { openPdf(links[index]) }
            } else {
                btn.visibility = View.GONE
            }
        }
    }

    // 🔹 Open single PDF for Book / Handwritten / Keybook
    private fun openSinglePdf(type: String) {
        val linkAny = pdfLinks[selectedClass]?.get(selectedSubject)?.get(type)
        val link = linkAny as? String ?: return
        openPdf(link)
    }

    // 🔹 Intent to PdfViewActivity
    private fun openPdf(url: String) {
        val intent = Intent(this, PdfViewActivity::class.java)
        intent.putExtra("pdfUrl", url)
        startActivity(intent)
    }
}
