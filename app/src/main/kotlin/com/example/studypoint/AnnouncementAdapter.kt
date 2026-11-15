package com.example.studypoint

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class AnnouncementAdapter(private var items: List<BoardAnnouncement>) :
    RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {

    class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chipCategory: Chip = itemView.findViewById(R.id.chipCategory)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val btnReadMore: MaterialButton = itemView.findViewById(R.id.btnReadMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement_card, parent, false)
        return AnnouncementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val item = items[position]
        holder.chipCategory.text = item.category.ifBlank { "Announcement" }
        holder.tvDate.text = item.date.ifBlank { "" }
        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description

        val hasLink = !item.link_url.isBlank()
        val isVideo = item.link_url.contains("youtu.be") || item.link_url.contains("youtube.com")
        holder.btnReadMore.visibility = if (hasLink) View.VISIBLE else View.GONE
        // Recycled views keep old state, so reset the button text on every bind.
        holder.btnReadMore.text = if (isVideo) "Watch Video" else "Read More / View Notice"
        if (hasLink) {
            val openLink = View.OnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link_url))
                it.context.startActivity(intent)
            }
            holder.btnReadMore.setOnClickListener(openLink)
            // Tapping the card itself opens the link too (e.g. the news video).
            holder.itemView.setOnClickListener(openLink)
        } else {
            holder.btnReadMore.setOnClickListener(null)
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<BoardAnnouncement>) {
        items = newItems
        notifyDataSetChanged()
    }
}
