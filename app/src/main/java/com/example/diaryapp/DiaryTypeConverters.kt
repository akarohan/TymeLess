package com.example.diaryapp

import androidx.room.TypeConverter

class DiaryTypeConverters {
    @TypeConverter
    fun fromImagePaths(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toImagePaths(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")
} 