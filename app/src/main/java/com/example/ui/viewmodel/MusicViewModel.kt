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

    // Smart playlists generic map (Genre -> List of Songs)
    val smartPlaylists: StateFlow<Map<String, List<Song>>> = repository.allSongs
        .map { songs -> songs.groupBy { it.genre } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private var progressUpdateJob: Job? = null

    init {
        // Initialize MediaController
        setupMediaController()

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
                    colorHex = "#4285F4", // Workspace Blue
                    lyrics = "0|Welcome to Azure Workspace\n5000|A soothing ambient soundscape\n10000|Focus and create\n15000|Let the ideas flow\n20000|Uninterrupted productivity\n40000|Feel the groove",
                    albumArtUrl = "https://images.unsplash.com/photo-1557672172-298e090bd0f1?w=600&h=600&fit=crop"
                ),
                Song(
                    title = "Emerald Sheets",
                    artist = "Chilledge",
                    uriOrUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    durationMs = 423000,
                    genre = "Docs Ambient",
                    colorHex = "#0F9D58", // Workspace Green
                    lyrics = "0|Entering the Emerald Sheets\n8000|Calculate your dreams\n16000|Data flows like a river\n24000|Structured and aligned\n32000|Harmony in logic",
                    albumArtUrl = "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=600&h=600&fit=crop"
                ),
                Song(
                    title = "Solar Keynote",
                    artist = "Golden Horizon",
                    uriOrUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    durationMs = 302000,
                    genre = "Slides Acoustic",
                    colorHex = "#F4B400", // Workspace Yellow
                    lyrics = "0|Presenting Solar Keynote\n10000|Transitions fade in\n20000|Bright ideas illuminated\n30000|Captivate the audience",
                    albumArtUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&h=600&fit=crop"
                ),
                Song(
                    title = "Crimson Drive",
                    artist = "Neon Matrix",
                    uriOrUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                    durationMs = 318000,
                    genre = "Drive Synth",
                    colorHex = "#DB4437", // Workspace Red
                    lyrics = "0|Welcome to the Crimson Drive\n6000|High octane storage\n12000|Secure and fast\n18000|Racing through the cloud\n24000|Limitless capacity",
                    albumArtUrl = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=600&h=600&fit=crop"
                )
            )

            for (track in defaultTracks) {
                repository.insertSong(track)
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
