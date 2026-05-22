package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.db.MusicDatabase
import com.example.data.db.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = MusicDatabase.getDatabase(application).songDao()
    private val repository = MusicRepository(songDao)

    // ExoPlayer Instance
    private var exoPlayer: ExoPlayer? = null

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

    private var progressUpdateJob: Job? = null

    init {
        // Initialize ExoPlayer
        setupExoPlayer()

        // Sync and Pre-populate songs if completely empty
        viewModelScope.launch {
            repository.allSongs.collect { list ->
                if (list.isEmpty()) {
                    prepopulateDefaultSongs()
                } else if (_currentSong.value == null) {
                    // Set the first song as default current if none selected
                    _currentSong.value = list.firstOrNull()
                    _currentSong.value?.let { loadSongInPlayer(it, playWhenReady = false) }
                }
            }
        }
    }

    private fun setupExoPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(getApplication()).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _durationMs.value = duration
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
            }
        }
    }

    private fun prepopulateDefaultSongs() {
        viewModelScope.launch {
            // Google Workspace theme colors aligned items
            val defaultTracks = listOf(
                Song(
                    title = "Azure Workspace",
                    artist = "Waveform Core",
                    uriOrUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    durationMs = 372000,
                    genre = "Docs Ambient",
                    colorHex = "#4285F4" // Google Workspace Blue
                ),
                Song(
                    title = "Emerald Sheets",
                    artist = "Chilledge",
                    uriOrUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    durationMs = 423000,
                    genre = "Sheets Lo-Fi",
                    colorHex = "#0F9D58" // Google Workspace Green
                ),
                Song(
                    title = "Solar Keynote",
                    artist = "Golden Horizon",
                    uriOrUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    durationMs = 302000,
                    genre = "Slides Acoustic",
                    colorHex = "#F4B400" // Google Workspace Yellow
                ),
                Song(
                    title = "Crimson Drive",
                    artist = "Neon Matrix",
                    uriOrUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                    durationMs = 318000,
                    genre = "Drive Synth",
                    colorHex = "#DB4437" // Google Workspace Red
                )
            )

            for (track in defaultTracks) {
                repository.insertSong(track)
            }
        }
    }

    // Playback control
    fun playSong(song: Song) {
        _currentSong.value = song
        loadSongInPlayer(song, playWhenReady = true)
    }

    private fun loadSongInPlayer(song: Song, playWhenReady: Boolean) {
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(song.uriOrUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = playWhenReady
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
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
        exoPlayer?.let { player ->
            val targetPositionMs = (progress * _durationMs.value).toLong()
            player.seekTo(targetPositionMs)
            _currentPositionMs.value = targetPositionMs
        }
    }

    fun playNext() {
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
                    exoPlayer?.stop()
                    _currentSong.value = null
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    _durationMs.value = 0L
                }
            }
            repository.deleteSong(song)
        }
    }

    fun addNewSong(title: String, artist: String, uriOrUrl: String, genre: String, colorHex: String) {
        viewModelScope.launch {
            val newSong = Song(
                title = title,
                artist = artist,
                uriOrUrl = if (uriOrUrl.isNotBlank()) uriOrUrl else "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
                durationMs = 280000,
                genre = if (genre.isNotBlank()) genre else "Interactive Indie",
                colorHex = if (colorHex.startsWith("#") && colorHex.length == 7) colorHex else "#4285F4"
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

    // Progress Loop
    private fun startProgressUpdateLoop() {
        stopProgressUpdateLoop()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition
                        _durationMs.value = player.duration.coerceAtLeast(0L)
                    }
                }
                delay(1000)
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
        exoPlayer?.release()
        exoPlayer = null
    }
}
