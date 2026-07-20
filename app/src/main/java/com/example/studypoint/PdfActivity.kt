package com.example.studypoint

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView   // ✅ sahi import

class PdfActivity : AppCompatActivity() {

    // File picker (sirf PDF select karne ke liye)
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val pdfView = findViewById<PDFView>(R.id.pdfView)
            pdfView.fromUri(it)
                .enableSwipe(true)        // swipe enable
                .swipeHorizontal(false)   // vertical scroll
                .enableDoubletap(true)    // zoom support
                .spacing(8)               // page ke darmiyan thoda gap
                .load()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)

        // Jab Activity open hogi → file picker khulega
        pdfPickerLauncher.launch(arrayOf("application/pdf"))
    }
}
