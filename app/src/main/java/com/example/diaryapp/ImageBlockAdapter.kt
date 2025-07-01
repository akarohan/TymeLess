package com.example.diaryapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.diaryapp.R
import com.bumptech.glide.Glide
import android.view.ViewGroup.LayoutParams

class ImageBlockAdapter(
    private val imageUris: List<Uri>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ImageBlockAdapter.ImageBlockViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageBlockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_block, parent, false)
        return ImageBlockViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageBlockViewHolder, position: Int) {
        val uri = imageUris[position]
        holder.bind(uri)
        holder.btnRemove.setOnClickListener {
            onRemove(position)
        }
    }

    override fun getItemCount(): Int = imageUris.size

    class ImageBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
        fun bind(uri: Uri) {
            // Use Glide to load the image and get its size
            Glide.with(imageView.context)
                .load(uri)
                .into(imageView)
            // Optionally, you can use Glide's .listener to get the bitmap and adjust layout params
        }
    }
} 