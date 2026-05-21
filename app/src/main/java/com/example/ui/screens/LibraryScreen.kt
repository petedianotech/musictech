package com.example.ui.screens

import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeDown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylist: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVideoPlayer: (String) -> Unit
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (selectedTab == 0) {
                        val sleepTimer by viewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
                        if (sleepTimer > 0) {
                            Text("${sleepTimer}m", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp).align(Alignment.CenterVertically))
                        } else {
                            var showSleepMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showSleepMenu = true }) {
                                Icon(Icons.Default.Timer, contentDescription = "Sleep Timer")
                            }
                            DropdownMenu(expanded = showSleepMenu, onDismissRequest = { showSleepMenu = false }) {
                                listOf(15, 30, 45, 60).forEach { mins ->
                                    DropdownMenuItem(text = { Text("$mins minutes") }, onClick = { viewModel.setSleepTimer(mins); showSleepMenu = false })
                                }
                            }
                        }

                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { viewModel.loadTracks(force = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan Music")
                        }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Title") }, onClick = { viewModel.setSortOrder(com.example.ui.viewmodel.SortOrder.TITLE); expanded = false })
                            DropdownMenuItem(text = { Text("Artist") }, onClick = { viewModel.setSortOrder(com.example.ui.viewmodel.SortOrder.ARTIST); expanded = false })
                            DropdownMenuItem(text = { Text("Duration") }, onClick = { viewModel.setSortOrder(com.example.ui.viewmodel.SortOrder.DURATION); expanded = false })
                        }
                    }
                    if (selectedTab == 2) {
                        IconButton(onClick = { showAddPlaylistDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Playlist")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val currentTrack by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()
        val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
        
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            var trackToEdit by remember { mutableStateOf<AudioTrack?>(null) }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            
            Column(modifier = Modifier.fillMaxSize().padding(bottom = if (currentTrack != null) 72.dp else 0.dp)) {
                TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Tracks", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Favorites", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("Playlists", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                    Text("Videos", modifier = Modifier.padding(16.dp))
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
                            }, onHide = {
                                viewModel.toggleHiddenTrack(track.uri)
                            }, onEdit = { trackToEdit = track },
                            onPlayNext = { viewModel.playNext(track) })
                        }
                    }
                }
            } else if (selectedTab == 1) {
                val dbFavorites by viewModel.favorites.collectAsStateWithLifecycle()
                val favTracks = tracks.filter { t -> dbFavorites.any { it.mediaUri == t.uri } }
                if (favTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No favorites yet", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(favTracks) { track ->
                            TrackItem(track = track, onClick = {
                                viewModel.tryPlayTrack(track.uri)
                            }, onHide = {
                                viewModel.toggleHiddenTrack(track.uri)
                            }, onEdit = { trackToEdit = track },
                            onPlayNext = { viewModel.playNext(track) })
                        }
                    }
                }
            } else if (selectedTab == 3) {
                val dbVideos by viewModel.allVideos.collectAsStateWithLifecycle()
                if (dbVideos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No videos found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(dbVideos) { video ->
                            ListItem(
                                modifier = Modifier.clickable { 
                                    onNavigateToVideoPlayer(video.uri)
                                },
                                headlineContent = { Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text("Size: ${video.size / (1024 * 1024)} MB") },
                                leadingContent = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Video")
                                },
                                trailingContent = {
                                    Text("${video.duration / 60000}:${String.format("%02d", (video.duration % 60000) / 1000)}")
                                }
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(playlists) { playlist ->
                        val gradient = when (playlist.name.lowercase()) {
                            "amapiano" -> Brush.horizontalGradient(listOf(Color(0xFFF093FB), Color(0xFFF5576C)))
                            "afrobeats" -> Brush.horizontalGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))
                            "pop" -> Brush.horizontalGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)))
                            "hip hop" -> Brush.horizontalGradient(listOf(Color(0xFF30CFD0), Color(0xFF330867)))
                            "r&b" -> Brush.horizontalGradient(listOf(Color(0xFFE65C00), Color(0xFFF9D423)))
                            else -> Brush.horizontalGradient(listOf(Color(0xFF021B79), Color(0xFF0575E6)))
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .clickable { onNavigateToPlaylist(playlist.id) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(gradient)
                                    .padding(16.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .align(Alignment.TopEnd)
                                )
                                Column {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Sub-genre",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } // close Column
            
            if (currentTrack != null) {
                MiniPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onClick = onNavigateToPlayer,
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            
            trackToEdit?.let { track ->
                var title by remember { mutableStateOf(track.title) }
                var artist by remember { mutableStateOf(track.artist) }
                var album by remember { mutableStateOf(track.album) }
                var coverUri by remember { mutableStateOf(track.customArtUri ?: "") }
                
                AlertDialog(
                    onDismissRequest = { trackToEdit = null },
                    title = { Text("Edit Metadata") },
                    text = {
                        Column {
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                            OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, singleLine = true)
                            OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") }, singleLine = true)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.saveTrackMetadata(track.uri, title, artist, album, if (coverUri.isNotBlank()) coverUri else null)
                            trackToEdit = null
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { trackToEdit = null }) { Text("Cancel") }
                    }
                )
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
fun MiniPlayer(
    track: AudioTrack,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), track.albumId)
    val volumeState by viewModel.volume.collectAsStateWithLifecycle()
    var showVolumeSlider by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .error(R.drawable.img_album_placeholder_1779303397262) // fallback
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (showVolumeSlider) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = volumeState,
                        onValueChange = { viewModel.setVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f).padding(vertical = 0.dp)
                    )
                    Text(
                        text = "${(volumeState * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            val volumeIcon = if (volumeState == 0f) Icons.Default.VolumeOff else if (volumeState < 0.5f) Icons.Default.VolumeDown else Icons.Default.VolumeUp
            IconButton(onClick = { showVolumeSlider = !showVolumeSlider }) {
                Icon(
                    imageVector = volumeIcon,
                    contentDescription = "Volume"
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause"
                )
            }
        }
    }
}

@Composable
fun TrackItem(track: AudioTrack, onClick: () -> Unit, onHide: () -> Unit = {}, onEdit: () -> Unit = {}, onPlayNext: () -> Unit = {}) {
    val context = LocalContext.current
    val albumArtUri = if (track.customArtUri != null) Uri.parse(track.customArtUri) else ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), track.albumId)
    var expanded by remember { mutableStateOf(false) }
    
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
        },
        trailingContent = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Play next") }, onClick = { expanded = false; onPlayNext() })
                    DropdownMenuItem(text = { Text("Edit metadata") }, onClick = { expanded = false; onEdit() })
                    DropdownMenuItem(text = { Text("Hide track") }, onClick = { expanded = false; onHide() })
                }
            }
        }
    )
}
