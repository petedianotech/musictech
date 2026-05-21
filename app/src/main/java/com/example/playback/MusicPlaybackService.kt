package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.*

class MusicPlaybackService : MediaLibraryService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(), true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            
        val initialSessionId = exoPlayer?.audioSessionId ?: 0
        if (initialSessionId != 0) {
            EqualizerManager.setup(initialSessionId)
        }
            
        exoPlayer?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                super.onAudioSessionIdChanged(audioSessionId)
                EqualizerManager.setup(audioSessionId)
            }

            override fun onPositionDiscontinuity(
                oldPosition: androidx.media3.common.Player.PositionInfo,
                newPosition: androidx.media3.common.Player.PositionInfo,
                reason: Int
            ) {
                savePlaybackStateToPrefs()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                savePlaybackStateToPrefs()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                savePlaybackStateToPrefs()
            }
        })

        // Periodically track play head position to prefs
        serviceScope.launch {
            while (isActive) {
                delay(2000L)
                if (exoPlayer?.isPlaying == true) {
                    savePlaybackStateToPrefs()
                }
            }
        }

        // Restore saved queue
        restorePlaybackQueue()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer!!, Callback())
            .setSessionActivity(pendingIntent)
            .build()
            
        // ensure default app icon or notification logo is pulled correctly
        setMediaNotificationProvider(
            androidx.media3.session.DefaultMediaNotificationProvider.Builder(this)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun savePlaybackStateToPrefs() {
        val player = exoPlayer ?: return
        val count = player.mediaItemCount
        if (count > 0) {
            val list = mutableListOf<String>()
            for (i in 0 until count) {
                val item = player.getMediaItemAt(i)
                list.add(item.mediaId)
            }
            val index = player.currentMediaItemIndex
            val pos = player.currentPosition
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit()
                .putString("playback_queue", list.joinToString("|||"))
                .putInt("playback_index", index)
                .putLong("playback_position", pos)
                .apply()
        }
    }

    private fun restorePlaybackQueue() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val qStr = prefs.getString("playback_queue", null) ?: return
        val list = qStr.split("|||").filter { it.isNotEmpty() }
        if (list.isEmpty()) return
        
        val index = prefs.getInt("playback_index", 0)
        val pos = prefs.getLong("playback_position", 0L)
        
        val mediaRepository = MediaRepository(this)
        serviceScope.launch {
            val allTracks = withContext(Dispatchers.IO) {
                mediaRepository.getAllAudioFiles()
            }
            val trackMap = allTracks.associateBy { it.uri }
            
            val mediaItems = list.mapNotNull { uri ->
                val track = trackMap[uri]
                if (track != null) {
                    val artUri = if (track.customArtUri != null) {
                        try { android.net.Uri.parse(track.customArtUri) } catch (e: Exception) { null }
                    } else {
                        android.content.ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), track.albumId)
                    }

                    MediaItem.Builder()
                        .setMediaId(track.uri)
                        .setUri(android.net.Uri.parse(track.uri))
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .setAlbumTitle(track.album)
                                .setArtworkUri(artUri)
                                .build()
                        )
                        .build()
                } else null
            }
            
            if (mediaItems.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val safeIndex = if (index in mediaItems.indices) index else 0
                    exoPlayer?.let { player ->
                        if (player.mediaItemCount == 0) {
                            player.setMediaItems(mediaItems, safeIndex, pos)
                            player.prepare()
                        }
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    private inner class Callback : MediaLibrarySession.Callback {
        // Implement callbacks for browsing library if needed
        // For simple playback, the controller will send MediaItems directly.
    }
}
