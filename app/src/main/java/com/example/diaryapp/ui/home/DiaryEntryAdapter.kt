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
        private val imageView: ImageView = itemView.findViewById(R.id.entryImage)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
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
            previewText.text = android.text.Html.fromHtml(entry.htmlContent).toString().take(60)
            if (entry.imagePaths.isNotEmpty()) {
                imageView.visibility = View.VISIBLE
                imageView.setImageURI(android.net.Uri.parse(entry.imagePaths[0]))
            } else {
                imageView.visibility = View.GONE
            }
        }
    }
} 