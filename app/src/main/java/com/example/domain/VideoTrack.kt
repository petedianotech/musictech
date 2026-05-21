package com.example.domain

data class VideoTrack(
    val id: Long,
    val uri: String,
    val title: String,
    val duration: Long,
    val size: Long
)
