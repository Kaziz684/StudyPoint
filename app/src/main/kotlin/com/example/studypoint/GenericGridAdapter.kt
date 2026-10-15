package com.example.studypoint

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

data class GridItem(
    val id: String,
    val title: String,
    val iconRes: Int,
    val subtitle: String = ""
)

class GenericGridAdapter(
    private val items: List<GridItem>,
    private val useGlassCard: Boolean = false,
    private val useListRow: Boolean = false,
    private val onItemClick: (GridItem) -> Unit
) : RecyclerView.Adapter<GenericGridAdapter.GridViewHolder>() {

    class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: View = itemView.findViewById(R.id.cardContainer)
        val icon: ImageView = itemView.findViewById(R.id.itemIcon)
        val title: TextView = itemView.findViewById(R.id.itemTitle)
        val subtitle: TextView? = itemView.findViewById(R.id.itemSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val layoutRes = when {
            useGlassCard -> R.layout.item_glass_card
            useListRow -> R.layout.item_chapter_row
            else -> R.layout.item_square_card
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle?.text = item.subtitle
        holder.subtitle?.visibility = if (item.subtitle.isNotEmpty()) View.VISIBLE else View.GONE
        // Soft pastel card tint (flowchart style) for square cards only
        if (!useGlassCard && !useListRow) {
            val pastel = listOf(
                0xFFEFF6FF, 0xFFECFDF5, 0xFFF5F3FF,
                0xFFFFF7ED, 0xFFFDF2F8, 0xFFF0FDFA
            )
            (holder.card as? MaterialCardView)?.setCardBackgroundColor(
                pastel[position % pastel.size].toInt()
            )
        }
        holder.icon.setImageResource(item.iconRes)
        // No need for separate tint if we use rich icons with backgrounds
        holder.icon.imageTintList = null 
        holder.card.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
