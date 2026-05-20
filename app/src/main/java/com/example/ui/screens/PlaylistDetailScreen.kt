package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AudioTrack
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val playlistTracks by viewModel.getPlaylistTracks(playlistId).collectAsStateWithLifecycle(emptyList())
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    
    val playlistName = playlists.find { it.id == playlistId }?.name ?: "Playlist"
    var showAddTrackModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deletePlaylist(playlistId); onNavigateBack() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Playlist")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTrackModal = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Track")
            }
        }
    ) { paddingValues ->
        if (playlistTracks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Playlist is empty")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                items(playlistTracks) { t ->
                    ListItem(
                        modifier = Modifier.clickable {
                            // play track logic
                            val track = allTracks.find { track -> track.uri == t.mediaUri }
                            if (track != null) {
                                viewModel.playTrack(track)
                                onNavigateToPlayer()
                            }
                        },
                        headlineContent = { Text(t.title) },
                        supportingContent = { Text(t.artist) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeTrackFromPlaylist(playlistId, t.mediaUri) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddTrackModal) {
        AlertDialog(
            onDismissRequest = { showAddTrackModal = false },
            title = { Text("Add Track") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(allTracks) { track ->
                        val isAdded = playlistTracks.any { it.mediaUri == track.uri }
                        ListItem(
                            modifier = Modifier.clickable {
                                if (!isAdded) {
                                    viewModel.addTrackToPlaylist(playlistId, track)
                                }
                            },
                            headlineContent = { Text(track.title) },
                            supportingContent = { Text(track.artist) },
                            trailingContent = {
                                if (isAdded) Text("Added", style = MaterialTheme.typography.labelSmall)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddTrackModal = false }) {
                    Text("Done")
                }
            }
        )
    }
}
