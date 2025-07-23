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
        // Set card background color based on mood
        val entry = entries[position]
        val moodColors = arrayOf(
            android.graphics.Color.parseColor("#D32F2F"), // 0: very sad (red)
            android.graphics.Color.parseColor("#E57373"), // 1: sad
            android.graphics.Color.parseColor("#FFB300"), // 2: less sad/orange
            android.graphics.Color.parseColor("#FFD54F"), // 3: neutral yellow
            android.graphics.Color.parseColor("#FFF176"), // 4: neutral
            androidx.core.content.ContextCompat.getColor(holder.itemView.context, com.example.diaryapp.R.color.greyback), // 5: neutral/black (original grey)
            android.graphics.Color.parseColor("#90EE90"), // 6: light green
            android.graphics.Color.parseColor("#66BB6A"), // 7: medium-light green
            android.graphics.Color.parseColor("#43A047"), // 8: medium green
            android.graphics.Color.parseColor("#388E3C"), // 9: grass green
            android.graphics.Color.parseColor("#228B22")  // 10: very happy (deep green)
        )
        val cardView = holder.itemView as androidx.cardview.widget.CardView
        val mood = entry.mood.coerceIn(0, 10)
        cardView.setCardBackgroundColor(moodColors[mood])
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

            // Images logic
            val imageViews = listOf(imageLeft, imageTopRight, imageBottomRight)
            val cornerRadiusPx = (16 * imageViews[0].context.resources.displayMetrics.density).toInt()
            if (entry.imagePaths.isNotEmpty()) {
                imagesRow.visibility = View.VISIBLE
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
                                .placeholder(R.drawable.bg_image_rounded)
                                .error(R.drawable.bg_image_rounded)
                                .into(imageViews[i])
                        } else {
                            imageViews[i].visibility = View.GONE
                        }
                    } else {
                        imageViews[i].visibility = View.GONE
                    }
                }
            } else {
                imagesRow.visibility = View.GONE
                imageViews.forEach { it.visibility = View.GONE }
            }

            // Audio chip logic: only show if there is at least one audio item
            val audioChip = itemView.findViewById<View>(R.id.audioChipPreview)
            val audioDuration = itemView.findViewById<TextView>(R.id.audioDuration)
            val audioDelete = itemView.findViewById<ImageButton>(R.id.audioDelete)
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