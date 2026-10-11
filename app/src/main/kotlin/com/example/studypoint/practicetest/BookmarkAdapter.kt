package com.example.studypoint.practicetest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studypoint.R

class BookmarkAdapter(
    private val questions: List<Question>,
    private val onRemove: (Question) -> Unit,
    private val onReview: (Question) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubject: TextView = view.findViewById(R.id.tvBookmarkSubject)
        val tvQuestion: TextView = view.findViewById(R.id.tvBookmarkQuestion)
        val tvMeta: TextView = view.findViewById(R.id.tvBookmarkMeta)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveBookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val q = questions[position]
        holder.tvSubject.text = q.subject.ifBlank { "General" }
        holder.tvQuestion.text = q.questionText
        holder.tvQuestion.maxLines = 2
        val meta = listOf(q.class_num, q.chapter).filter { it.isNotBlank() }.joinToString(" • ")
        holder.tvMeta.text = meta
        holder.tvMeta.visibility = if (meta.isBlank()) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onReview(q) }
        holder.btnRemove.setOnClickListener { onRemove(q) }
    }

    override fun getItemCount() = questions.size
}
