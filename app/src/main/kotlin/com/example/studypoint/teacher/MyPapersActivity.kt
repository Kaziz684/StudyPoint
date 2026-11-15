package com.example.studypoint.teacher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studypoint.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * My Papers — every paper this teacher generated, newest first. Records
 * live in Firestore (collection: teacher_papers); the files themselves stay
 * in the app papers folder, exactly where the generator wrote them.
 */
class MyPapersActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var adapter: PapersAdapter
    private lateinit var rvPapers: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvLoading: TextView
    private var pendingSaveFile: File? = null

    private val saveLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            val src = pendingSaveFile
            pendingSaveFile = null
            if (uri != null && src != null) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            contentResolver.openOutputStream(uri)?.use { out ->
                                src.inputStream().use { it.copyTo(out) }
                            }
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    Toast.makeText(
                        this@MyPapersActivity,
                        if (ok) "Saved" else "Save failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_papers)
        title = "My Papers"
        findViewById<TextView>(R.id.btnBackPapers).setOnClickListener { finish() }
        rvPapers = findViewById(R.id.rvPapers)
        tvEmpty = findViewById(R.id.tvEmptyPapers)
        tvLoading = findViewById(R.id.tvLoadingPapers)
        rvPapers.layoutManager = LinearLayoutManager(this)
        adapter = PapersAdapter(
            onView = { paper -> viewPaper(paper) },
            onDownload = { paper -> downloadPaper(paper) },
            onShare = { paper -> sharePaper(paper) },
            onInfo = { paper -> showPaperInfo(paper) }
        )
        rvPapers.adapter = adapter
        TeacherBottomNav.setup(this, TeacherBottomNav.Tab.PAPERS)
        loadPapers()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun loadPapers() {
        tvLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        TeacherRepository.loadGeneratedPapers(
            onOk = { list ->
                tvLoading.visibility = View.GONE
                adapter.submit(list)
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            },
            onErr = { msg ->
                tvLoading.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Could not load saved papers. $msg"
            }
        )
    }

    /** App-private external papers folder — same place the generator writes. */
    private fun papersDir(): File {
        val base = getExternalFilesDir(null) ?: cacheDir
        return File(base, "papers").apply { mkdirs() }
    }

    private fun fileFor(paper: SavedPaper): File? {
        if (paper.fileName.isBlank()) return null
        val file = File(papersDir(), paper.fileName)
        return if (file.exists()) file else null
    }

    private fun mimeFor(file: File): String =
        if (file.extension.equals("docx", ignoreCase = true)) {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        } else {
            "application/pdf"
        }

    private fun viewPaper(paper: SavedPaper) {
        val file = fileFor(paper)
        if (file == null) {
            Toast.makeText(this, "File not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeFor(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri(null, uri)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No viewer app found for this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadPaper(paper: SavedPaper) {
        val file = fileFor(paper)
        if (file == null) {
            Toast.makeText(this, "File not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        pendingSaveFile = file
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeFor(file)
            putExtra(Intent.EXTRA_TITLE, file.name)
        }
        saveLauncher.launch(intent)
    }

    private fun sharePaper(paper: SavedPaper) {
        val file = fileFor(paper)
        if (file == null) {
            Toast.makeText(this, "File not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeFor(file)
                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this@MyPapersActivity, "$packageName.fileprovider", file))
                putExtra(Intent.EXTRA_SUBJECT, "StudyPoint Question Paper")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Paper"))
        } catch (e: Exception) {
            Toast.makeText(this, "Sharing failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPaperInfo(paper: SavedPaper) {
        val date = if (paper.createdAt > 0) {
            SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(paper.createdAt))
        } else {
            "—"
        }
        val message = StringBuilder()
        message.append("Paper No:  ${paper.paperNo.ifBlank { "—" }}\n")
        message.append("Date:  $date\n")
        message.append("Board:  ${boardLabel(paper.board)}\n")
        message.append("Class:  ${paper.classLabel}\n")
        message.append("Subject:  ${paper.subject}\n")
        message.append("Chapters:  ${paper.chaptersLabel}\n")
        message.append("Paper Type:  ${paper.paperType}\n")
        message.append("Total Marks:  ${paper.totalMarks}\n")
        message.append("Questions:  ${paper.totalQuestions} " +
            "(${paper.mcqCount} MCQs, ${paper.shortCount} Short, ${paper.longCount} Long)\n")
        message.append("Answers:  ${if (paper.solved) "Solved" else "Unsolved"}\n")
        message.append("Format:  ${paper.format}")
        AlertDialog.Builder(this)
            .setTitle("Paper Details")
            .setMessage(message.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun boardLabel(board: String): String = when (board) {
        "federal" -> "Federal Board (FBISE)"
        "sindh" -> "Sindh Text Book Board"
        "kp" -> "KP Text Book Board"
        else -> "Punjab Text Book Board"
    }

    private class PapersAdapter(
        private val onView: (SavedPaper) -> Unit,
        private val onDownload: (SavedPaper) -> Unit,
        private val onShare: (SavedPaper) -> Unit,
        private val onInfo: (SavedPaper) -> Unit
    ) : RecyclerView.Adapter<PapersAdapter.Holder>() {

        private var items: List<SavedPaper> = emptyList()

        fun submit(list: List<SavedPaper>) {
            items = list
            notifyDataSetChanged()
        }

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvPaperTitle)
            val tvMeta: TextView = view.findViewById(R.id.tvPaperMeta)
            val tvChapters: TextView = view.findViewById(R.id.tvPaperChapters)
            val tvCounts: TextView = view.findViewById(R.id.tvPaperCounts)
            val btnView: Button = view.findViewById(R.id.btnPaperView)
            val btnDownload: Button = view.findViewById(R.id.btnPaperDownload)
            val btnShare: Button = view.findViewById(R.id.btnPaperShare)
            val btnInfo: Button = view.findViewById(R.id.btnPaperInfo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_saved_paper, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val paper = items[position]
            holder.tvTitle.text = if (paper.paperNo.isNotBlank()) {
                "Paper #${paper.paperNo} — ${paper.subject}"
            } else {
                paper.subject
            }
            val date = if (paper.createdAt > 0) {
                SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(paper.createdAt))
            } else {
                ""
            }
            holder.tvMeta.text = listOf(date, paper.classLabel, paper.paperType)
                .filter { it.isNotBlank() }.joinToString("  •  ")
            holder.tvChapters.text = paper.chaptersLabel.ifBlank { "All Chapters" }
            holder.tvCounts.text = "${paper.totalQuestions} questions  •  " +
                "${paper.mcqCount} MCQs, ${paper.shortCount} Short, ${paper.longCount} Long  •  " +
                "${paper.totalMarks} marks"
            holder.btnView.setOnClickListener { onView(paper) }
            holder.btnDownload.setOnClickListener { onDownload(paper) }
            holder.btnShare.setOnClickListener { onShare(paper) }
            holder.btnInfo.setOnClickListener { onInfo(paper) }
        }
    }
}
