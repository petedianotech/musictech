package com.example.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.db.MusicDatabase
import com.example.data.db.Playlist
import com.example.data.db.PlaylistTrack
import com.example.data.db.TrackLyrics
import com.example.data.db.FavoriteTrack
import com.example.data.repository.SettingsRepository
import com.example.data.repository.MediaRepository
import com.example.domain.AudioTrack
import com.example.playback.MusicPlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { TITLE, ARTIST, DURATION }

class MusicViewModel(
    private val context: Context,
    private val mediaRepository: MediaRepository,
    private val database: MusicDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var mediaController: MediaController? = null

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _allTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val customMetadata: StateFlow<List<com.example.data.db.CustomTrackMetadata>> = database.customMetadataDao().getAllMetadata()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTracks: StateFlow<List<AudioTrack>> = combine(
        _allTracks, 
        _sortOrder,
        settingsRepository.minDurationAtLeast,
        settingsRepository.hiddenTracks,
        customMetadata
    ) { tracks, order, minSec, hidden, metadataList ->
        val metaMap = metadataList.associateBy { it.mediaUri }
        var filtered = tracks.filter { it.duration >= minSec * 1000L && !hidden.contains(it.uri) }
            .map { track ->
                metaMap[track.uri]?.let { custom ->
                    track.copy(
                        title = custom.title,
                        artist = custom.artist,
                        album = custom.album,
                        customArtUri = custom.coverArtUri
                    )
                } ?: track
            }
        
        when (order) {
            SortOrder.TITLE -> filtered = filtered.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> filtered = filtered.sortedBy { it.artist.lowercase() }
            SortOrder.DURATION -> filtered = filtered.sortedByDescending { it.duration }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentPlayingTrack = MutableStateFlow<AudioTrack?>(null)
    val currentPlayingTrack: StateFlow<AudioTrack?> = _currentPlayingTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow(0)
    val sleepTimerRemaining: StateFlow<Int> = _sleepTimerRemaining.asStateFlow()
    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    val favorites: StateFlow<List<FavoriteTrack>> = database.favoriteDao().getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = database.playlistDao().getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        seedDefaultGenres()
        initializeController()
        loadTracks()
        startPositionUpdater()
        setupHeadsetReceiver()
    }

    private fun setupHeadsetReceiver() {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(curContext: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.content.Intent.ACTION_HEADSET_PLUG) {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1 && settingsRepository.playOnHeadsetConnect.value) {
                        mediaController?.play()
                    } else if (state == 0 && settingsRepository.pauseOnHeadsetDisconnect.value) {
                        mediaController?.pause()
                    }
                }
            }
        }
        context.applicationContext.registerReceiver(receiver, android.content.IntentFilter(android.content.Intent.ACTION_HEADSET_PLUG))
    }

    private fun seedDefaultGenres() {
        viewModelScope.launch {
            try {
                val playlists = database.playlistDao().getAllPlaylists().first()
                if (playlists.isEmpty()) {
                    val genres = listOf("Amapiano", "Afrobeats", "Pop", "Hip Hop", "R&B")
                    genres.forEach {
                        database.playlistDao().insertPlaylist(Playlist(name = it))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller

                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentTrack(mediaItem)
                        mediaController?.let { controller ->
                            val cfSec = settingsRepository.crossfadeDuration.value
                            if (cfSec > 0) {
                                isFadingOut = false
                                isFadingIn = true
                                controller.volume = 0f
                                viewModelScope.launch {
                                    val steps = 20
                                    val stepTime = (cfSec * 1000L) / steps
                                    var vol = 0f
                                    for (i in 1..steps) {
                                        vol += 1f / steps
                                        controller.volume = minOf(1f, vol)
                                        delay(stepTime)
                                    }
                                    controller.volume = 1f
                                    isFadingIn = false
                                }
                            } else {
                                controller.volume = 1f
                            }
                        }
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _shuffleModeEnabled.value = shuffleModeEnabled
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                    }
                })
                // initial fetch
                _isPlaying.value = controller.isPlaying
                _shuffleModeEnabled.value = controller.shuffleModeEnabled
                _repeatMode.value = controller.repeatMode
                updateCurrentTrack(controller.currentMediaItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun updateCurrentTrack(mediaItem: MediaItem?) {
        val uri = mediaItem?.mediaId
        if (uri != null) {
            val track = _allTracks.value.find { it.uri == uri }
            _currentPlayingTrack.value = track
        } else {
            _currentPlayingTrack.value = null
        }
    }

    private var isFadingOut = false
    private var isFadingIn = false

    private fun startPositionUpdater() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _currentPosition.value = controller.currentPosition
                        
                        val cfSec = settingsRepository.crossfadeDuration.value
                        if (cfSec > 0) {
                            val remain = controller.duration - controller.currentPosition
                            if (remain in 1..(cfSec * 1000L) && !isFadingOut) {
                                isFadingOut = true
                                viewModelScope.launch {
                                    val steps = 20
                                    val stepTime = remain / steps
                                    var vol = 1f
                                    for (i in 1..steps) {
                                        vol -= 1f / steps
                                        controller.volume = maxOf(0f, vol)
                                        delay(stepTime)
                                    }
                                }
                            }
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    fun loadTracks() {
        viewModelScope.launch {
            _allTracks.value = mediaRepository.getAllAudioFiles()
        }
    }

    fun tryPlayTrack(trackUri: String) {
        val tracks = allTracks.value
        val index = tracks.indexOfFirst { it.uri == trackUri }
        if (index != -1) {
            playTrackList(tracks, index)
        }
    }

    fun playTrack(track: AudioTrack) {
        val tracks = allTracks.value
        val trackIndex = tracks.indexOf(track)
        if (trackIndex != -1) {
            playTrackList(tracks, trackIndex)
        }
    }

    fun playNext(track: AudioTrack) {
        mediaController?.let { controller ->
            val mediaItem = MediaItem.Builder()
                .setMediaId(track.uri)
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(if (track.customArtUri != null) android.net.Uri.parse(track.customArtUri) else android.content.ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), track.albumId))
                        .build()
                )
                .build()
            val nextIndex = if (controller.mediaItemCount > 0) controller.currentMediaItemIndex + 1 else 0
            controller.addMediaItem(nextIndex, mediaItem)
        }
    }

    fun setVolume(vol: Float) {
        _volume.value = vol
        mediaController?.volume = vol
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerRemaining.value = minutes
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                while (_sleepTimerRemaining.value > 0) {
                    kotlinx.coroutines.delay(60 * 1000L)
                    _sleepTimerRemaining.value -= 1
                }
                mediaController?.let { fadeOutAndPause(it) }
            }
        }
    }

    fun playTrackList(tracks: List<AudioTrack>, startIndex: Int = 0) {
        mediaController?.let { controller ->
            val mediaItems = tracks.map { track ->
                MediaItem.Builder()
                    .setMediaId(track.uri)
                    .setUri(track.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .setArtworkUri(if (track.customArtUri != null) android.net.Uri.parse(track.customArtUri) else android.content.ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), track.albumId))
                            .build()
                    )
                    .build()
            }
            controller.setMediaItems(mediaItems, startIndex, 0)
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                fadeOutAndPause(controller)
            } else {
                fadeInAndPlay(controller)
            }
        }
    }

    private fun fadeOutAndPause(controller: MediaController) {
        viewModelScope.launch {
            val originalVolume = controller.volume
            var vol = originalVolume
            while (vol > 0f) {
                vol -= 0.1f
                controller.volume = maxOf(0f, vol)
                delay(50)
            }
            controller.pause()
            controller.volume = originalVolume
        }
    }

    private fun fadeInAndPlay(controller: MediaController) {
        viewModelScope.launch {
            val targetVolume = controller.volume
            controller.volume = 0f
            controller.play()
            var vol = 0f
            while (vol < targetVolume) {
                vol += 0.1f
                controller.volume = minOf(targetVolume, vol)
                delay(50)
            }
            controller.volume = targetVolume
        }
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(uri: String, isFav: Boolean) {
        viewModelScope.launch {
            if (isFav) {
                database.favoriteDao().removeFavorite(uri)
            } else {
                database.favoriteDao().addFavorite(FavoriteTrack(uri))
            }
        }
    }

    fun toggleShuffleMode() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun cycleRepeatMode() {
        mediaController?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun getLyrics(uri: String): Flow<String?> = database.trackLyricsDao().getLyrics(uri)

    fun saveLyrics(uri: String, lyrics: String) {
        viewModelScope.launch {
            database.trackLyricsDao().saveLyrics(TrackLyrics(uri, lyrics))
        }
    }

    // Playlist Operations
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if(name.isNotBlank()){
                database.playlistDao().insertPlaylist(Playlist(name = name))
            }
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch {
            database.playlistDao().deletePlaylist(id)
        }
    }

    fun addTrackToPlaylist(playlistId: Int, track: AudioTrack) {
        viewModelScope.launch {
            database.playlistDao().insertTrack(
                PlaylistTrack(
                    playlistId = playlistId,
                    mediaUri = track.uri,
                    title = track.title,
                    artist = track.artist,
                    albumId = track.albumId,
                    duration = track.duration
                )
            )
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, uri: String) {
        viewModelScope.launch {
            database.playlistDao().deleteTrack(playlistId, uri)
        }
    }
    
    fun getPlaylistTracks(playlistId: Int) = database.playlistDao().getTracksForPlaylist(playlistId)

    fun toggleHiddenTrack(uri: String) {
        viewModelScope.launch {
            settingsRepository.toggleHiddenTrack(uri)
        }
    }

    fun saveTrackMetadata(uri: String, title: String, artist: String, album: String, coverArtUri: String? = null) {
        viewModelScope.launch {
            database.customMetadataDao().saveMetadata(
                com.example.data.db.CustomTrackMetadata(
                    mediaUri = uri,
                    title = title,
                    artist = artist,
                    album = album,
                    coverArtUri = coverArtUri
                )
            )
        }
    }
}

class MusicViewModelFactory(
    private val context: Context,
    private val repository: MediaRepository,
    private val database: MusicDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(context, repository, database, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
