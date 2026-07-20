package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class NotesActivity : AppCompatActivity() {

    private fun open(url: String) {

        Log.d("StudyPoint", "Opening PDF URL: $url")

        // Check if URL contains placeholder text
        if (url.contains("REPLACE_WITH") || url.contains("COMING_SOON")) {
            Log.e("StudyPoint", "File not available yet: $url")
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_LONG).show()
            return
        }

        // Browser kholne ki bajaye PdfViewActivity call karenge
        val intent = Intent(this, PdfViewActivity::class.java)
        intent.putExtra("pdfUrl", url)

        try {
            Log.d("StudyPoint", "Starting PdfViewActivity with URL: $url")
            startActivity(intent)
            Log.d("StudyPoint", "Successfully started PdfViewActivity")
        } catch (e: Exception) {
            Log.e("StudyPoint", "Error starting PdfViewActivity: ${e.message}", e)
            Toast.makeText(this, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        Log.d("StudyPoint", "NotesActivity onCreate started")

        val grade = intent.getStringExtra("grade") ?: ""
        val subject = intent.getStringExtra("subject") ?: ""
        val material = intent.getStringExtra("material") ?: "Notes"

        Log.d("StudyPoint", "Grade: $grade, Subject: $subject, Material: $material")

        // 🔹 Links ka Map
        val notesLinks = mapOf(
            "9th" to mapOf(
                "Biology" to mapOf(
                    "Notes" to listOf(
                        "https://drive.google.com/uc?export=download&id=142qNCvfBp0b5oqfQ_IYUTcKKEZlUAsay",//chapter 1
                        "https://drive.google.com/uc?export=download&id=14Ak7EUXP5VDcFn-dfAQvBtZuIeGFSVXQ",//chapter 2
                        "https://drive.google.com/uc?export=download&id=14eL4HHECcG59gdy2nycK4wlqU3iCSE91",//chapter 3
                        "https://drive.google.com/uc?export=download&id=14QCjlQv62JEBm2f1CUH8HndRMQaqAwgu",//chapter 4
                        "https://drive.google.com/uc?export=download&id=14QM1omop7MSPxG-sP73HDjSvzZ2SsFTr",//chapter 5
                        "https://drive.google.com/uc?export=download&id=14GLa3mFvt-H-wuRR1Ngz9_UXpVHWh0Ax",//chapter 6
                        "https://drive.google.com/uc?export=download&id=14SsrZ8J69wPkydd7FKulYOhQknAD2Vwo",//chapter 7
                        "https://drive.google.com/uc?export=download&id=14_k3EZQG8f_PGY_51mSrMSyblyPkrEOG",//chapter 8
                        "https://drive.google.com/uc?export=download&id=14W5RphrsAwR8WJGuupqeqR77xUH-NpRJ",//chapter 9


                    ),
                    "Book" to listOf(
                        "https://drive.google.com/uc?export=download&id=1jBfqNH0ssvsZ-uD_Zq1OtzuE-fNSySBb"
                    ),
                    "Keybook" to listOf(
                        "Coming Soon"

                    //"https://drive.google.com/uc?export=download&id=14W5RphrsAwR8WJGuupqeqR77xUH-NpRJ"
                    ),
                    "Handwritten Notes" to listOf(
                      "Coming Soon"
                    //"https://drive.google.com/uc?export=download&id=14W5RphrsAwR8WJGuupqeqR77xUH-NpRJ"
                    )
                ),
                "Chemistry" to mapOf(
                    "Notes" to listOf(
                        "https://drive.google.com/uc?export=download&id=14W5RphrsAwR8WJGuupqeqR77xUH-NpRJ", //ch1
                        "COMING_SOON", //ch2
                        "COMING_SOON", //ch3
                        "COMING_SOON", //ch4
                        "COMING_SOON", //ch5
                        "COMING_SOON", //ch6
                        "COMING_SOON", //ch7
                        "COMING_SOON", //ch8
                        "COMING_SOON", //ch9
                    ),
                    "Book" to listOf(
                        "https://drive.google.com/uc?export=download&id=1j8RBN3gh7eHXhYHbhn2ywYcrz3M2visq"
                    ),
                    "Keybook" to listOf(
                        "https://drive.google.com/uc?export=download&id=15zybNRbmjPO7cIn0Te9gefo5GFPG1UeZ"
                    ),
                    "Handwritten Notes" to listOf(
                        "coming soon"
                    )
                ),
                "Computer" to mapOf(
                    "Notes" to listOf(
                        "https://drive.google.com/uc?export=download&id=1gpHD6s82CwEwa6Y0sFy0RCloDa6Fq29D", //ch1
                        "https://drive.google.com/uc?export=download&id=15w5O0bmxzP8Z8nR1Y20q_3MJowma-9gd", //ch2
                        "https://drive.google.com/uc?export=download&id=1VHAjtOd2QoZaP4lSlCYVDWupsOWUaaaa", //ch3
                        "https://drive.google.com/uc?export=download&id=17fQ06WJ_xA_4VY8kF0oYP_TZJHraDyu2", //ch4
                        "https://drive.google.com/uc?export=download&id=1j1oUPd7IMDyCrTZaHTdtt_hlbFaUI1Cc", //ch5
                        "https://drive.google.com/uc?export=download&id=1KXEQhZmDGB0pGHOMaVdBb0Qg5zFLNSFR", //ch6
                        "COMING_SOON", //ch7
                        "COMING_SOON", //ch8
                        "COMING_SOON", //ch9
                    ),
                    "Book" to listOf(
                        "https://drive.google.com/uc?export=download&id=1iFNnLpDjBaPoZamzpkj41W2UHiF-zvWD",

                    ),
                    "Keybook" to listOf(
                        "https://drive.google.com/uc?export=download&id=15vs4XvyilG4JiH1nNte6HQ0Kl4pYcdyi",
                    ),
                    "Handwritten Notes" to listOf(
                        "https://drive.google.com/uc?export=download&id=1KXEQhZmDGB0pGHOMaVdBb0Qg5zFLNSFR",
                    )
                ),
                "Physics" to mapOf(
                    "Notes" to listOf(
                        "https://drive.google.com/uc?export=download&id=1BThkZRcZ0fyS4kA0z7N3xghSeIWfPiP3", // Ch 1 - Real ID added
                        "COMING_SOON" , // Ch 2 - Coming soon
                        "COMING_SOON" , // Ch 3 - Coming soon
                        "COMING_SOON" , // Ch 4 - Coming soon
                        "COMING_SOON" , // Ch 5 - Coming soon
                        "COMING_SOON" , // Ch 6 - Coming soon
                        "COMING_SOON" , // Ch 7 - Coming soon
                        "COMING_SOON" , // Ch 8 - Coming soon
                        "COMING_SOON" , // Ch 9 - Coming soon

                    ),
                    "Book" to listOf(
                        "https://drive.google.com/uc?export=download&id=1i7lZ9oR2aoqgRp7tMi0lcgLQPsHLa6I1" // ✅ Physics Book - Real ID added
                    ),
                    "Keybook" to listOf(
                        "https://drive.google.com/uc?export=download&id=163NTXZm7xBUo0qVEmJ-S0dUcEyy1__HR" // ✅ Physics Keybook - Real ID added
                    ),
                    "Handwritten Notes" to listOf(
                        "COMING SOON"
                    )
                ),
                "Mathematics" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
//                        "https://drive.google.com/uc?export=download&id=16Ea59C8wi_MD8zrv1kuhZZgX7MZaIyC4"
                    ),
                    "Keybook" to listOf(
                        "https://drive.google.com/uc?export=download&id=1RtZZ5jE8w-CmLO2tVKbPczp-TO1Fc9Gk"
                    )
                ),
                "English" to mapOf(
                    "Notes" to listOf(
//                        "https://drive.google.com/uc?export=download&id=1ADXuBPdSxMrBbnwncCEOtrCfnzORWEK",//chapter 1
//                        "https://drive.google.com/uc?export=download&id=1AIqOhc2PoEw39g2SFn7wnfph9xMRbpmG",//chapter 2
//                        "https://drive.google.com/uc?export=download&id=1AHGfjSayeeIDDVqrmgJEiDZ4MPURAdEB",//chapter 3
//                        "https://drive.google.com/uc?export=download&id=1ADXuBPdSxMrBbnwncCEOtrCfnzORWEK",//chapter 4

                    ),
                    "Book" to listOf(
                        "https://drive.google.com/uc?export=download&id=1iEI7xTL7WQQeEb71swI2_5ujZxsgcp1f"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                )
            ),
            "10th" to mapOf(
                "Biology" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "Chemistry" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "Physics" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "Mathematics" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "English" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                )
            ),
            "11th" to mapOf(
                "Biology" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "Chemistry" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "Physics" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "Mathematics" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "English" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                )
            ),
            "12th" to mapOf(
                "Biology" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "Chemistry" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "Physics" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "Mathematics" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                ),
                "English" to mapOf(
                    "Notes" to listOf(
                        "COMING_SOON",
                        "COMING_SOON"
                    ),
                    "Book" to listOf(
                        "COMING_SOON"
                    ),
                    "Keybook" to listOf(
                        "COMING_SOON"
                    )
                )
            )
        )

        // 🔹 Subject aur material ke links pick karo
        val gradeKey = "${grade}th"
        Log.d("StudyPoint", "Looking up grade key: $gradeKey")

        val links = notesLinks[gradeKey]?.get(subject)?.get(material) ?: emptyList()

        Log.d("StudyPoint", "Found ${links.size} links for $gradeKey $subject $material")
        links.forEachIndexed { index, link ->
            Log.d("StudyPoint", "Link $index: $link")
        }

        // 🔹 Buttons list
        val ids = listOf(
            R.id.btnCh1, R.id.btnCh2, R.id.btnCh3,
            R.id.btnCh4, R.id.btnCh5, R.id.btnCh6,
            R.id.btnCh7, R.id.btnCh8, R.id.btnCh9
        )

        if (material == "Notes") {
            // 🔹 Notes → chapter-wise buttons show karo
            ids.zip(links).forEach { (id, url) ->
                findViewById<MaterialButton>(id).apply {
                    visibility = View.VISIBLE
                    setOnClickListener { open(url) }
                }
            }
        } else {
            // 🔹 Book / Keybook → sirf ek button show karo
            if (material == "Keybook" && links.size > 1) {
                // Show multiple buttons for keybook guides
                val buttonLabels = listOf("Guide 1", "Guide 2", "Guide 3", "Guide 4", "Guide 5")
                links.take(5).forEachIndexed { index, url ->
                    findViewById<MaterialButton>(ids[index]).apply {
                        visibility = View.VISIBLE
                        text = buttonLabels[index]
                        setOnClickListener { open(url) }
                    }
                }
                // Hide remaining buttons
                ids.drop(links.size).forEach { id ->
                    findViewById<MaterialButton>(id).visibility = View.GONE
                }
            } else {
                // Single button for Book or single Keybook
                val bookBtn = findViewById<MaterialButton>(R.id.btnCh1)
                bookBtn.visibility = View.VISIBLE
                bookBtn.text = material // button pe "Book" ya "Keybook" likh do
                if (links.isNotEmpty()) {
                    bookBtn.setOnClickListener { open(links[0]) }
                }
                // Baaki sab buttons hide kar do
                ids.drop(1).forEach { id ->
                    findViewById<MaterialButton>(id).visibility = View.GONE
                }
            }
        }
    }
}
