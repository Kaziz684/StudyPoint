package com.example.studypoint

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class ClassAdapter(
    private val context: Context,
    private val classList: List<String>
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.classCard)
        val classNumber: TextView = itemView.findViewById(R.id.tvClassNumber)
        val className: TextView = itemView.findViewById(R.id.tvClassName)
        val classSubtitle: TextView = itemView.findViewById(R.id.tvClassSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val className = classList[position]
        holder.classNumber.text = className
        holder.className.text = "Class ${className}th"
        holder.classSubtitle.text = when (className) {
            "9" -> "Matric Part 1"
            "10" -> "Matric Part 2"
            "11" -> "Inter Part 1"
            "12" -> "Inter Part 2"
            else -> ""
        }

        holder.cardView.setOnClickListener {
            val intent = Intent(context, SubjectsActivity::class.java)
            intent.putExtra("grade", className)
            intent.putExtra("class", className)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = classList.size
}
