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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { TITLE, ARTIST, DURATION }

class MusicViewModel(
    private val context: Context,
    private val mediaRepository: MediaRepository,
    private val database: MusicDatabase
) : ViewModel() {

    private var mediaController: MediaController? = null

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _allTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val allTracks: StateFlow<List<AudioTrack>> = combine(_allTracks, _sortOrder) { tracks, order ->
        when (order) {
            SortOrder.TITLE -> tracks.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> tracks.sortedBy { it.artist.lowercase() }
            SortOrder.DURATION -> tracks.sortedByDescending { it.duration }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentPlayingTrack = MutableStateFlow<AudioTrack?>(null)
    val currentPlayingTrack: StateFlow<AudioTrack?> = _currentPlayingTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = database.playlistDao().getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        initializeController()
        loadTracks()
        startPositionUpdater()
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

    private fun startPositionUpdater() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition
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
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
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
}

class MusicViewModelFactory(
    private val context: Context,
    private val repository: MediaRepository,
    private val database: MusicDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(context, repository, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
