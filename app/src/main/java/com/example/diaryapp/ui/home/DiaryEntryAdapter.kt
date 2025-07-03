package com.example.diaryapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.diaryapp.DiaryEntry
import com.example.diaryapp.R
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop

class DiaryEntryAdapter(
    var entries: List<DiaryEntry>,
    private val onEntryClick: (DiaryEntry) -> Unit,
    private val onDeleteClick: (DiaryEntry) -> Unit
) : RecyclerView.Adapter<DiaryEntryAdapter.EntryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diary_entry, parent, false)
        return EntryViewHolder(view, onEntryClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    fun updateEntries(newEntries: List<DiaryEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    class EntryViewHolder(itemView: View, val onEntryClick: (DiaryEntry) -> Unit, val onDeleteClick: (DiaryEntry) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.entryTitle)
        private val dateText: TextView = itemView.findViewById(R.id.entryDate)
        private val previewText: TextView = itemView.findViewById(R.id.entryPreview)
        private val imageLeft: ImageView = itemView.findViewById(R.id.imageLeft)
        private val imageTopRight: ImageView = itemView.findViewById(R.id.imageTopRight)
        private val imageBottomRight: ImageView = itemView.findViewById(R.id.imageBottomRight)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val imagesRow: View = itemView.findViewById(R.id.imagesRow)
        private var currentEntry: DiaryEntry? = null

        init {
            itemView.setOnClickListener {
                currentEntry?.let { onEntryClick(it) }
            }
            deleteButton.setOnClickListener {
                currentEntry?.let { onDeleteClick(it) }
            }
        }

        fun bind(entry: DiaryEntry) {
            currentEntry = entry
            titleText.text = entry.title ?: "(No Title)"
            val sdf = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
            dateText.text = sdf.format(Date(entry.date))
            // Show a styled preview using Html.fromHtml, truncated to 60 chars
            val spanned = android.text.Html.fromHtml(entry.htmlContent, android.text.Html.FROM_HTML_MODE_LEGACY)
            previewText.text = if (spanned.length > 60) spanned.subSequence(0, 60) else spanned

            // Bind up to 3 images
            val imageViews = listOf(imageLeft, imageTopRight, imageBottomRight)
            var hasImage = false
            val cornerRadiusPx = (16 * imageViews[0].context.resources.displayMetrics.density).toInt()
            for (i in imageViews.indices) {
                if (entry.imagePaths.size > i) {
                    val path = entry.imagePaths[i]
                    val uri = if (path.startsWith("/")) {
                        val file = java.io.File(path)
                        if (file.exists()) android.net.Uri.fromFile(file) else null
                    } else if (path.startsWith("content://")) {
                        android.net.Uri.parse(path)
                    } else null

                    if (uri != null) {
                        imageViews[i].visibility = View.VISIBLE
                        Glide.with(imageViews[i].context)
                            .load(uri)
                            .transform(CenterCrop(), RoundedCorners(cornerRadiusPx))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(imageViews[i])
                        hasImage = true
                    } else {
                        imageViews[i].visibility = View.GONE
                    }
                } else {
                    imageViews[i].visibility = View.GONE
                }
            }
            imagesRow.visibility = if (hasImage) View.VISIBLE else View.GONE

            // Audio chip logic: only show if there is at least one audio item
            val audioChip = itemView.findViewById<View>(com.example.diaryapp.R.id.audioChipPreview)
            val audioDuration = itemView.findViewById<TextView>(com.example.diaryapp.R.id.audioDuration)
            val audioDelete = itemView.findViewById<ImageButton>(com.example.diaryapp.R.id.audioDelete)
            val firstAudio = entry.audioList.firstOrNull()
            if (firstAudio != null) {
                audioChip?.visibility = View.VISIBLE
                audioDuration?.text = firstAudio.duration
            } else {
                audioChip?.visibility = View.GONE
            }
            // Hide the delete button on the audio chip preview (if present)
            audioDelete?.visibility = View.GONE
        }
    }
} 