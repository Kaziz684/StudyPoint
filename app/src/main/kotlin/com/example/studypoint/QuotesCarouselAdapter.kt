package com.example.studypoint

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuotesCarouselAdapter(private val quotes: List<QuoteItem>) :
    RecyclerView.Adapter<QuotesCarouselAdapter.QuoteViewHolder>() {

    class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuoteText: TextView = itemView.findViewById(R.id.tvQuoteText)
        val tvQuoteAuthor: TextView = itemView.findViewById(R.id.tvQuoteAuthor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quote_card, parent, false)
        return QuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val item = quotes[position]
        holder.tvQuoteText.text = item.quote
        holder.tvQuoteAuthor.text = item.author
    }

    override fun getItemCount(): Int = quotes.size
}
