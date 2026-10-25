package com.example.studypoint

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
    private val useWhiteCard: Boolean = false,
    private val onItemClick: (GridItem) -> Unit
) : RecyclerView.Adapter<GenericGridAdapter.GridViewHolder>() {

    class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: View = itemView.findViewById(R.id.cardContainer)
        // List rows (item_chapter_row) carry a number badge instead of an icon;
        // grid/glass cards carry an icon instead of a badge — both are nullable.
        val icon: ImageView? = itemView.findViewById(R.id.itemIcon)
        val badge: TextView? = itemView.findViewById(R.id.itemBadge)
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
        if (useListRow) {
            // Approved chapter-row design: teal badge with a white number
            // (the chapter number when the id is numeric, else the row order),
            // and a truthful generic subtitle when the item carries none.
            holder.badge?.visibility = View.VISIBLE
            holder.badge?.text = item.id.toIntOrNull()?.toString() ?: (position + 1).toString()
            holder.icon?.visibility = View.GONE
            holder.subtitle?.text = item.subtitle.ifBlank { "Notes, MCQs and short questions" }
            holder.subtitle?.visibility = View.VISIBLE
        } else {
            holder.subtitle?.text = item.subtitle
            holder.subtitle?.visibility = if (item.subtitle.isNotEmpty()) View.VISIBLE else View.GONE
            holder.icon?.setImageResource(item.iconRes)
            // White cards need a visible icon tint: the class icons are white
            // vectors, so tint them teal on white cards; other cards keep
            // their untinted icons.
            holder.icon?.imageTintList = if (useWhiteCard) {
                ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.context, R.color.edu_emerald)
                )
            } else {
                null
            }
        }
        // Soft pastel card tint (flowchart style) for square cards only;
        // white-card screens keep the layout's white card background.
        if (!useGlassCard && !useListRow && !useWhiteCard) {
            val pastel = listOf(
                0xFFEFF6FF, 0xFFECFDF5, 0xFFF5F3FF,
                0xFFFFF7ED, 0xFFFDF2F8, 0xFFF0FDFA
            )
            (holder.card as? MaterialCardView)?.setCardBackgroundColor(
                pastel[position % pastel.size].toInt()
            )
        }
        holder.card.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
