package com.example.ui.screens

import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.domain.AudioTrack
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylist: (Int) -> Unit
) {
    val tracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Musictech", fontWeight = FontWeight.Bold) },
                actions = {
                    if (selectedTab == 1) {
                        IconButton(onClick = { showAddPlaylistDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Playlist")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val currentTrack by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()
            if (currentTrack != null) {
                FloatingActionButton(onClick = onNavigateToPlayer) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Now Playing")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Tracks", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Playlists", modifier = Modifier.padding(16.dp))
                }
            }
            
            if (selectedTab == 0) {
                if (tracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tracks found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(tracks) { track ->
                            TrackItem(track = track, onClick = {
                                viewModel.playTrack(track)
                                onNavigateToPlayer()
                            })
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(playlists) { playlist ->
                        ListItem(
                            modifier = Modifier.clickable { onNavigateToPlaylist(playlist.id) },
                            headlineContent = { Text(playlist.name) },
                            leadingContent = {
                                Icon(painterResource(R.drawable.img_album_placeholder_1779303397262), contentDescription = null, modifier = Modifier.size(48.dp))
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddPlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPlaylistDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createPlaylist(playlistName)
                    showAddPlaylistDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TrackItem(track: AudioTrack, onClick: () -> Unit) {
    val context = LocalContext.current
    val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), track.albumId)
    
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { 
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) 
        },
        supportingContent = { 
            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) 
        },
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .crossfade(true)
                    .error(R.drawable.img_album_placeholder_1779303397262) // fallback
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    )
}
