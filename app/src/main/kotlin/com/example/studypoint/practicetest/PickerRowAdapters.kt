package com.example.studypoint.practicetest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studypoint.R

/** One row of the Paper Generator class picker (vertical list). */
data class ClassRow(
    val digits: String, // "9".."12"
    val label: String, // "CLASS 9"
    val countText: String, // "No. of Questions: 7,459" or "Loading…"
    val selected: Boolean
)

/** One row of the Paper Generator subject picker (vertical list). */
data class SubjectRow(
    val name: String,
    val countText: String, // "No. of Questions: 1,256" or "Loading…"
    val selected: Boolean
)

class ClassRowAdapter(
    private var items: List<ClassRow>,
    private val onClick: (ClassRow) -> Unit
) : RecyclerView.Adapter<ClassRowAdapter.Holder>() {

    fun updateItems(newItems: List<ClassRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val circle: TextView = view.findViewById(R.id.tvRowCircle)
        val title: TextView = view.findViewById(R.id.tvRowTitle)
        val count: TextView = view.findViewById(R.id.tvRowCount)
        val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class_row, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = items[position]
        holder.circle.text = row.digits.padStart(2, '0')
        holder.title.text = row.label
        holder.count.text = row.countText
        holder.root.setBackgroundResource(
            if (row.selected) R.drawable.bg_row_selected else R.drawable.bg_role_card
        )
        holder.root.setOnClickListener { onClick(row) }
    }
}

class SubjectRowAdapter(
    private var items: List<SubjectRow>,
    private val onClick: (SubjectRow) -> Unit
) : RecyclerView.Adapter<SubjectRowAdapter.Holder>() {

    fun updateItems(newItems: List<SubjectRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val circle: TextView = view.findViewById(R.id.tvRowCircle)
        val title: TextView = view.findViewById(R.id.tvRowTitle)
        val count: TextView = view.findViewById(R.id.tvRowCount)
        val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject_row, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = items[position]
        holder.circle.text = "▤"
        holder.title.text = row.name
        holder.count.text = row.countText
        holder.root.setBackgroundResource(
            if (row.selected) R.drawable.bg_row_selected else R.drawable.bg_role_card
        )
        holder.root.setOnClickListener { onClick(row) }
    }
}
