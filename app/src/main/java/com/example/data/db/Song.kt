package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val uriOrUrl: String,
    val durationMs: Long,
    val isFavorite: Boolean = false,
    val genre: String = "Unknown",
    val colorHex: String = "#3F51B5" // Default elegant Indigo
)
