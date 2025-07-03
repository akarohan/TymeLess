package com.example.diaryapp

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AudioChipAdapter(
    val audioList: MutableList<AudioItem>,
    private val onPlayPause: (AudioItem, Int) -> Unit,
    private val onDelete: (AudioItem, Int) -> Unit
) : RecyclerView.Adapter<AudioChipAdapter.AudioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audio_chip, parent, false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.bind(audioList[position], position)
    }

    override fun getItemCount(): Int = audioList.size

    inner class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playArea: View = itemView.findViewById(R.id.audioPlayArea)
        private val icon: ImageView = itemView.findViewById(R.id.audioIcon)
        private val waveform: ImageView = itemView.findViewById(R.id.audioWaveform)
        private val duration: TextView = itemView.findViewById(R.id.audioDuration)
        private val delete: ImageButton = itemView.findViewById(R.id.audioDelete)

        fun bind(item: AudioItem, position: Int) {
            duration.text = item.duration
            // Set icon and background based on play state
            if (item.isPlaying) {
                playArea.setBackgroundResource(R.drawable.ic_stop_circle_white)
                icon.setImageDrawable(null)
            } else {
                playArea.setBackgroundResource(R.drawable.circle_white_bg)
                icon.setImageResource(R.drawable.ic_mic_filled)
                icon.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.black))
            }
            playArea.setOnClickListener { onPlayPause(item, adapterPosition) }
            delete.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDelete(item, pos)
                }
            }
        }
    }
} 