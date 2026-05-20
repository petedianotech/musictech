package com.example.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_metadata")
data class CustomTrackMetadata(
    @PrimaryKey val mediaUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverArtUri: String? = null
)

@Dao
interface CustomMetadataDao {
    @Query("SELECT * FROM custom_metadata")
    fun getAllMetadata(): Flow<List<CustomTrackMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(metadata: CustomTrackMetadata)
}
