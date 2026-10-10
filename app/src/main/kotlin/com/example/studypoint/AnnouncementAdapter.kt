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
        holder.btnReadMore.visibility = if (hasLink) View.VISIBLE else View.GONE
        if (hasLink) {
            holder.btnReadMore.setOnClickListener {
                val context = it.context
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link_url))
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<BoardAnnouncement>) {
        items = newItems
        notifyDataSetChanged()
    }
}
