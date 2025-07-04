package com.example.diaryapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.diaryapp.AudioItem

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val imagePaths: List<String> = emptyList(),
    val audioList: List<AudioItem> = emptyList(),
    val noteType: String = "N" // "N" for green, "P" for red, "A" for sky blue
) 