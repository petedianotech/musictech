package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_lyrics")
data class TrackLyrics(
    @PrimaryKey val mediaUri: String,
    val lyrics: String
)
