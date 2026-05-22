package com.example.data.repository

import com.example.data.db.Song
import com.example.data.db.SongDao
import kotlinx.coroutines.flow.Flow

class MusicRepository(private val songDao: SongDao) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val recentlyPlayed: Flow<List<Song>> = songDao.getRecentlyPlayed()

    suspend fun insertSong(song: Song) {
        songDao.insertSong(song)
    }

    suspend fun updateSong(song: Song) {
        songDao.updateSong(song)
    }

    suspend fun deleteSong(song: Song) {
        songDao.deleteSong(song)
    }
}
