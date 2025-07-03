package com.example.diaryapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

sealed class DiaryGridItem {
    data class Image(val uri: Uri) : DiaryGridItem()
    data class Audio(val audioItem: AudioItem) : DiaryGridItem()
}

class DiaryGridAdapter(
    private val items: List<DiaryGridItem>,
    private val onImageDelete: (Int) -> Unit,
    private val onAudioPlayPause: (AudioItem, Int) -> Unit,
    private val onAudioDelete: (AudioItem, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_AUDIO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DiaryGridItem.Image -> TYPE_IMAGE
            is DiaryGridItem.Audio -> TYPE_AUDIO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_block, parent, false)
                ImageViewHolder(view)
            }
            TYPE_AUDIO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_audio_chip, parent, false)
                AudioViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DiaryGridItem.Image -> (holder as ImageViewHolder).bind(item.uri, position)
            is DiaryGridItem.Audio -> (holder as AudioViewHolder).bind(item.audioItem, position)
        }
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val imageDelete: ImageButton = itemView.findViewById(R.id.imageDelete)
        fun bind(uri: Uri, position: Int) {
            Glide.with(imageView.context)
                .load(uri)
                .placeholder(R.drawable.bg_image_rounded)
                .error(R.drawable.bg_image_rounded)
                .into(imageView)
            imageDelete.setOnClickListener { onImageDelete(position) }
        }
    }

    inner class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val duration: TextView = itemView.findViewById(R.id.audioDuration)
        private val icon: ImageButton = itemView.findViewById(R.id.audioIcon)
        private val delete: ImageButton = itemView.findViewById(R.id.audioDelete)
        fun bind(item: AudioItem, position: Int) {
            duration.text = item.duration
            if (item.isPlaying) {
                icon.setImageResource(R.drawable.ic_stop_circle_white)
                icon.clearColorFilter()
            } else {
                icon.setImageResource(R.drawable.ic_mic_filled)
                icon.setColorFilter(android.graphics.Color.BLACK)
            }
            icon.setOnClickListener { onAudioPlayPause(item, position) }
            delete.setOnClickListener { onAudioDelete(item, position) }
        }
    }
} 