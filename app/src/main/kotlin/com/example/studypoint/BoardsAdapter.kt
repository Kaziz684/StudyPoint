package com.example.studypoint

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BoardsAdapter(
    private val boards: List<BoardModel>,
    private val onItemClick: (BoardModel) -> Unit
) : RecyclerView.Adapter<BoardsAdapter.BoardViewHolder>() {

    class BoardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logo: ImageView = itemView.findViewById(R.id.boardLogo)
        val name: TextView = itemView.findViewById(R.id.boardName)
        val subtitle: TextView = itemView.findViewById(R.id.boardSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_board, parent, false)
        return BoardViewHolder(view)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val board = boards[position]
        holder.name.text = board.name
        holder.subtitle.text = board.subtitle
        holder.logo.setImageResource(board.logoRes)
        holder.itemView.setOnClickListener { onItemClick(board) }
    }

    override fun getItemCount(): Int = boards.size
}
