package com.example.diaryapp

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DiaryTypeConverters {
    @TypeConverter
    fun fromImagePaths(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toImagePaths(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")

    @TypeConverter
    fun fromAudioList(value: List<AudioItem>): String = Gson().toJson(value)

    @TypeConverter
    fun toAudioList(value: String): List<AudioItem> =
        if (value.isEmpty()) emptyList()
        else Gson().fromJson(value, object : TypeToken<List<AudioItem>>() {}.type)
} 