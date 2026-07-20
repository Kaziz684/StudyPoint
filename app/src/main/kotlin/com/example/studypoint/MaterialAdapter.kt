package com.example.studypoint

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MaterialAdapter(
    private val context: Context,
    private val materials: List<Pair<String, Int>>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<MaterialAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.materialIcon)
        val title: TextView = itemView.findViewById(R.id.materialTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_material, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (title, iconRes) = materials[position]
        holder.title.text = title
        holder.icon.setImageResource(iconRes)

        // click listener
        holder.itemView.setOnClickListener {
            onItemClick(title)
        }
    }

    override fun getItemCount(): Int = materials.size
}
