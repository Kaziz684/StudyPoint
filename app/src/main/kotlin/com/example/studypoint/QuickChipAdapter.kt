package com.example.studypoint

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuickChipAdapter(private val items: List<QuickChipItem>) :
    RecyclerView.Adapter<QuickChipAdapter.QuickChipViewHolder>() {

    class QuickChipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivChipIcon: ImageView = itemView.findViewById(R.id.ivChipIcon)
        val tvChipTitle: TextView = itemView.findViewById(R.id.tvChipTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quick_chip, parent, false)
        return QuickChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickChipViewHolder, position: Int) {
        val item = items[position]
        holder.tvChipTitle.text = item.title
        holder.ivChipIcon.setImageResource(item.iconRes)
        holder.itemView.setOnClickListener {
            item.onClick()
        }
    }

    override fun getItemCount(): Int = items.size
}
