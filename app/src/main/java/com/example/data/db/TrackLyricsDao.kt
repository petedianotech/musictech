package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackLyricsDao {
    @Query("SELECT lyrics FROM track_lyrics WHERE mediaUri = :uri")
    fun getLyrics(uri: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(trackLyrics: TrackLyrics)
}
