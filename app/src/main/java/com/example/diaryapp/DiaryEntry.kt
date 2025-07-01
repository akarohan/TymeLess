package com.example.diaryapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.Index
import java.util.Date

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // Store as epoch millis for easier querying
    val htmlContent: String,
    val imagePaths: List<String>,
    val title: String? = null
) 