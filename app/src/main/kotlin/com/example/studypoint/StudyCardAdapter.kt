package com.example.studypoint

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load.load

/**
 * Slides for the home "Important Questions" card: the 2027 pairing / guess
 * cards, hosted on Drive like every other study material and loaded with
 * Coil. A slide is a teaser of the card; tapping it opens the full card
 * on screen, tap anywhere to close.
 */
class StudyCardAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<StudyCardAdapter.StudyCardViewHolder>() {

    class StudyCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCard: ImageView = itemView.findViewById(R.id.ivStudyCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudyCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_card, parent, false)
        return StudyCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudyCardViewHolder, position: Int) {
        val url = imageUrls[position]
        holder.ivCard.load(url) {
            crossfade(true)
        }
        holder.ivCard.setOnClickListener {
            showFullscreen(it.context, url)
        }
    }

    override fun getItemCount(): Int = imageUrls.size

    private fun showFullscreen(context: Context, url: String) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            setOnClickListener { dialog.dismiss() }
        }
        imageView.load(url) {
            crossfade(true)
        }
        dialog.setContentView(imageView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.show()
    }
}
