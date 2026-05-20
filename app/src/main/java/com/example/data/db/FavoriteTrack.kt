package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteTrack(
    @PrimaryKey val mediaUri: String,
    val addedAt: Long = System.currentTimeMillis()
)
