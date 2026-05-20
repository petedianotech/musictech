package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteTrack>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(track: FavoriteTrack)

    @Query("DELETE FROM favorites WHERE mediaUri = :uri")
    suspend fun removeFavorite(uri: String)
}
