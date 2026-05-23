package com.example.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.service.PlaybackService
import com.example.data.db.MusicDatabase
import com.example.data.db.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = MusicDatabase.getDatabase(application).songDao()
    private val repository = MusicRepository(songDao)

    // MediaController Instance
    private var mediaController: MediaController? = null
    
    // Player State Flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // Combined database songs
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentlyPlayedSongs: StateFlow<List<Song>> = repository.recentlyPlayed
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Group by Genre (acts as Playlists)
    val smartPlaylists: StateFlow<Map<String, List<Song>>> = repository.allSongs
        .map { songs -> songs.groupBy { it.genre } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _sortOption = MutableStateFlow("RECENT")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    fun setSortOption(option: String) {
        _sortOption.value = option
    }

    private var progressUpdateJob: Job? = null

    init {
        // Initialize MediaController
        setupMediaController()

        // Sync and Pre-populate songs if completely empty
        viewModelScope.launch {
            repository.allSongs.collect { list ->
                if (list.isEmpty()) {
                    loadLocalMusicFiles()
                } else if (_currentSong.value == null) {
                    // Set the first song as default current if none selected
                    _currentSong.value = list.firstOrNull()
                    _currentSong.value?.let { loadSongInPlayer(it, playWhenReady = false) }
                }
            }
        }
    }

    private fun setupMediaController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                mediaController?.repeatMode = Player.REPEAT_MODE_ALL
                mediaController?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _durationMs.value = mediaController?.duration ?: 0L
                        } else if (playbackState == Player.STATE_ENDED) {
                            playNext()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) {
                            startProgressUpdateLoop()
                        } else {
                            stopProgressUpdateLoop()
                        }
                    }
                })
            },
            ContextCompat.getMainExecutor(getApplication())
        )
    }

    private fun loadLocalMusicFiles() {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    android.provider.MediaStore.Audio.Media._ID,
                    android.provider.MediaStore.Audio.Media.TITLE,
                    android.provider.MediaStore.Audio.Media.ARTIST,
                    android.provider.MediaStore.Audio.Media.DATA,
                    android.provider.MediaStore.Audio.Media.DURATION,
                    android.provider.MediaStore.Audio.Media.ALBUM_ID
                )
                
                val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
                
                val cursor = contentResolver.query(uri, projection, selection, null, null)
                
                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val dataColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                    val durationColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                    val albumIdColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM_ID)
                    
                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val title = it.getString(titleColumn) ?: "Unknown"
                        val artist = it.getString(artistColumn) ?: "Unknown"
                        val duration = it.getLong(durationColumn)
                        val albumId = it.getLong(albumIdColumn)
                        
                        val uriStr = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        ).toString()
                        
                        val albumArtUri = android.content.ContentUris.withAppendedId(
                            android.net.Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()

                        val colorHex = arrayOf("#4285F4", "#0F9D58", "#F4B400", "#DB4437").random()

                        val newSong = Song(
                            title = title,
                            artist = artist,
                            uriOrUrl = uriStr,
                            durationMs = duration,
                            genre = "Local Device",
                            colorHex = colorHex,
                            lyrics = "",
                            albumArtUrl = albumArtUri
                        )
                        
                        repository.insertSong(newSong)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Playback control
    fun playSong(song: Song) {
        val updatedSong = song.copy(lastPlayedTimestamp = System.currentTimeMillis())
        _currentSong.value = updatedSong
        viewModelScope.launch {
            repository.updateSong(updatedSong) // Persist play history
        }
        loadSongInPlayer(updatedSong, playWhenReady = true)
    }

    private fun loadSongInPlayer(song: Song, playWhenReady: Boolean) {
        mediaController?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setGenre(song.genre)
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(song.uriOrUrl)
                .setMediaMetadata(mediaMetadata)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = playWhenReady
        }
    }

    fun togglePlayPause() {
        mediaController?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                // If it was idle/ended and has a song, re-prepare
                if (player.playbackState == Player.STATE_IDLE) {
                    _currentSong.value?.let { loadSongInPlayer(it, playWhenReady = true) }
                } else {
                    player.play()
                }
            }
        }
    }

    fun seekTo(progress: Float) {
        mediaController?.let { player ->
            val targetPositionMs = (progress * _durationMs.value).toLong()
            player.seekTo(targetPositionMs)
            _currentPositionMs.value = targetPositionMs
        }
    }

    fun playNext() {
        if (scheduledNextSong != null) {
            val next = scheduledNextSong!!
            scheduledNextSong = null
            playSong(next)
            return
        }
        val list = allSongs.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentSong.value?.id }
        val nextIndex = if (currentIndex == -1 || currentIndex == list.lastIndex) 0 else currentIndex + 1
        playSong(list[nextIndex])
    }

    fun playPrevious() {
        val list = allSongs.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentSong.value?.id }
        val prevIndex = if (currentIndex <= 0) list.lastIndex else currentIndex - 1
        playSong(list[prevIndex])
    }
    
    // Feature: Smart Playlist invocation
    fun playSmartPlaylist(songs: List<Song>) {
        if(songs.isNotEmpty()) {
            playSong(songs.first())
        }
    }

    // Database CRUD actions
    fun deleteSong(song: Song) {
        viewModelScope.launch {
            // If the deleted song is the currently loaded song, switch track first
            if (_currentSong.value?.id == song.id) {
                val list = allSongs.value
                val remainingList = list.filter { it.id != song.id }
                if (remainingList.isNotEmpty()) {
                    _currentSong.value = remainingList.first()
                    loadSongInPlayer(remainingList.first(), playWhenReady = _isPlaying.value)
                } else {
                    mediaController?.stop()
                    _currentSong.value = null
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    _durationMs.value = 0L
                }
            }
            repository.deleteSong(song)
        }
    }

    fun addNewSong(title: String, artist: String, uriOrUrl: String, genre: String, colorHex: String, lyrics: String = "", albumArtUrl: String = "") {
        viewModelScope.launch {
            val newSong = Song(
                title = title,
                artist = artist,
                uriOrUrl = if (uriOrUrl.isNotBlank()) uriOrUrl else "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
                durationMs = 280000,
                genre = if (genre.isNotBlank()) genre else "Interactive Indie",
                colorHex = if (colorHex.startsWith("#") && colorHex.length == 7) colorHex else "#4285F4",
                lyrics = lyrics,
                albumArtUrl = albumArtUrl
            )
            repository.insertSong(newSong)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val updatedSong = song.copy(isFavorite = !song.isFavorite)
            repository.updateSong(updatedSong)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = updatedSong
            }
        }
    }

    fun updateSongDetails(song: Song, newTitle: String, newArtist: String, newPlaylist: String) {
        viewModelScope.launch {
            val updated = song.copy(title = newTitle, artist = newArtist, genre = newPlaylist.ifBlank { song.genre })
            repository.updateSong(updated)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = updated
                loadSongInPlayer(updated, playWhenReady = _isPlaying.value)
            }
        }
    }

    fun updateLyrics(song: Song, newLyrics: String) {
        viewModelScope.launch {
            val updated = song.copy(lyrics = newLyrics)
            repository.updateSong(updated)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = updated
            }
        }
    }

    // Since we don't have a real queue, we just simulate playNext by storing the song 
    // to be played when current finishes (or skipped)
    private var scheduledNextSong: Song? = null
    
    fun setNextSong(song: Song) {
        scheduledNextSong = song
    }

    fun applyToSelection(songIds: Set<Int>, action: String, payload: String = "") {
        viewModelScope.launch {
            val songsToUpdate = allSongs.value.filter { it.id in songIds }
            for (song in songsToUpdate) {
                when (action) {
                    "PLAYLIST" -> {
                        val updated = song.copy(genre = payload.ifBlank { song.genre })
                        repository.updateSong(updated)
                    }
                    "FAVORITE" -> {
                        val updated = song.copy(isFavorite = true)
                        repository.updateSong(updated)
                    }
                    "DELETE" -> {
                        if (_currentSong.value?.id == song.id) {
                            playNext()
                        }
                        repository.deleteSong(song)
                    }
                }
            }
        }
    }

    // Progress Loop
    private fun startProgressUpdateLoop() {
        stopProgressUpdateLoop()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                mediaController?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition
                        _durationMs.value = player.duration.coerceAtLeast(0L)
                    }
                }
                delay(100) // faster 100ms update for smooth lyrics syncing
            }
        }
    }

    private fun stopProgressUpdateLoop() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdateLoop()
        mediaController?.release()
        mediaController = null
    }
}
