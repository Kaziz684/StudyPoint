package com.example.studypoint

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(
    private val items: List<Pair<String,String>>,
    private val onClick: (String,String) -> Unit
): RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(view: View): RecyclerView.ViewHolder(view){
        val textView: TextView = view.findViewById(R.id.textViewChapter)
        val badge: TextView? = view.findViewById(R.id.itemBadge)
        val subtitle: TextView? = view.findViewById(R.id.itemSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note,parent,false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val (chapter,link) = items[position]
        holder.textView.text = chapter
        holder.badge?.text = (position + 1).toString()
        holder.subtitle?.text = "Notes, MCQs and short questions"
        holder.itemView.setOnClickListener {
            onClick(chapter, link)
        }
    }

    override fun getItemCount(): Int = items.size
}
