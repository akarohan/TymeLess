package com.example.diaryapp.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.diaryapp.R
import com.example.diaryapp.data.Note

class NotesAdapter(
    private var notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteDelete: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {
    override fun getItemViewType(position: Int): Int {
        return when (notes[position].noteType) {
            "P" -> 1
            "A" -> 2
            else -> 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val layout = when (viewType) {
            1 -> R.layout.item_pnote_card
            2 -> R.layout.item_anote_card
            else -> R.layout.item_note_card
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return NoteViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        Log.d("NotesAdapter", "Binding position $position, title=${notes[position].title}")
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    inner class NoteViewHolder(itemView: View, private val viewType: Int) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = when (viewType) {
            1 -> itemView.findViewById(R.id.pnoteTitle)
            2 -> itemView.findViewById(R.id.anoteTitle)
            else -> itemView.findViewById(R.id.noteTitle)
        }
        private val content: TextView = when (viewType) {
            1 -> itemView.findViewById(R.id.pnoteContent)
            2 -> itemView.findViewById(R.id.anoteContent)
            else -> itemView.findViewById(R.id.noteContent)
        }
        private val btnDelete: ImageButton = when (viewType) {
            1 -> itemView.findViewById(R.id.btnDeletePNote)
            2 -> itemView.findViewById(R.id.btnDeleteANote)
            else -> itemView.findViewById(R.id.btnDeleteNote)
        }
        // For A note: single image
        private val anoteImage: ImageView? = if (viewType == 2) itemView.findViewById(R.id.anoteImage) else null

        fun bind(note: Note) {
            title.text = note.title
            // Hide content for password protected cards (A and P)
            if (note.noteType == "A" || note.noteType == "P") {
                content.text = "••••••••••••••••••••"
                content.setTextColor(android.graphics.Color.GRAY)
            } else {
                content.text = note.content
                content.setTextColor(android.graphics.Color.parseColor("#222222"))
            }
            // Show only one image for A notes
            if (note.noteType == "A" && anoteImage != null) {
                if (note.imagePaths.isNotEmpty()) {
                    anoteImage.visibility = View.VISIBLE
                    val path = note.imagePaths[0]
                    val file = java.io.File(path)
                    val uri = when {
                        file.exists() -> android.net.Uri.fromFile(file)
                        path.startsWith("content://") -> android.net.Uri.parse(path)
                        else -> null
                    }
                    if (uri != null) {
                        com.bumptech.glide.Glide.with(anoteImage.context)
                            .load(uri)
                            .placeholder(com.example.diaryapp.R.drawable.bg_image_rounded)
                            .error(com.example.diaryapp.R.drawable.bg_image_rounded)
                            .centerCrop()
                            .into(anoteImage)
                    } else {
                        anoteImage.setImageResource(com.example.diaryapp.R.drawable.bg_image_rounded)
                    }
                } else {
                    anoteImage.visibility = View.GONE
                }
            }
            itemView.setOnClickListener { onNoteClick(note) }
            btnDelete.setOnClickListener { onNoteDelete(note) }
        }
    }
} 