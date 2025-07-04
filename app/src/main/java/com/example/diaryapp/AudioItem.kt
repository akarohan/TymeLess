package com.example.diaryapp

data class AudioItem(
    val filePath: String,
    val duration: String,
    val isPlaying: Boolean = false
) 