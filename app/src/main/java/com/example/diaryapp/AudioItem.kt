package com.example.diaryapp

data class AudioItem(
    val filePath: String,
    val duration: String,
    var isPlaying: Boolean = false
) 