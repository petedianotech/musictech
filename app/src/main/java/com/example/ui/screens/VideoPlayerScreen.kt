package com.example.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.viewmodel.MusicViewModel

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUri: String,
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val videos = viewModel.allVideos.value
    val startIndex = videos.indexOfFirst { it.uri == java.net.URLDecoder.decode(videoUri, "UTF-8") }.coerceAtLeast(0)
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItems = videos.map { MediaItem.fromUri(Uri.parse(it.uri)) }
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            playWhenReady = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        DisposableEffect(Unit) {
            // Optional: Pause music playback if it's running
            viewModel.togglePlayPause() 
            onDispose {
                exoPlayer.release()
            }
        }
        
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowPreviousButton(true)
                    setShowNextButton(true)
                }
            }
        )
        
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}
