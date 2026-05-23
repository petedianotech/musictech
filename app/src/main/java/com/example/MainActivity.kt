package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.Song
import com.example.ui.theme.MusictechTheme
import com.example.ui.theme.WorkspaceBlue
import com.example.ui.theme.WorkspaceGreen
import com.example.ui.theme.WorkspaceRed
import com.example.ui.theme.WorkspaceYellow
import com.example.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusictechTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicAppRoot()
                }
            }
        }
    }
}

// Helper to convert hexadecimal color strings safely to Compose Colors
fun String.toComposeColor(): Color {
    return try {
        Color(AndroidColor.parseColor(this))
    } catch (e: Exception) {
        WorkspaceBlue // Default fallback is beautiful blue
    }
}

// Formats duration milliseconds into neat MM:SS string
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes.coerceAtMost(99L), seconds.coerceAtMost(59L))
}

data class LyricLine(val timestampMs: Long, val text: String)

fun parseLyrics(lyricsData: String): List<LyricLine> {
    if (lyricsData.isBlank()) return emptyList()
    return try {
        lyricsData.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 2) {
                LyricLine(parts[0].toLong(), parts[1])
            } else null
        }.sortedBy { it.timestampMs }
    } catch (e: Exception) {
        emptyList()
    }
}

enum class AppTheme(val displayName: String, val midColor: Color, val bottomColor: Color, val surfaceColor: Color, val accentColor: Color) {
    MIDNIGHT("Midnight", Color(0xFF121422), Color(0xFF060912), Color(0xFF1A1D2E), Color(0xFF4D88FF)),
    SUNSET("Sunset", Color(0xFF2A0845), Color(0xFF150426), Color(0xFF3B0B60), Color(0xFFFF5E8E)),
    FOREST("Forest", Color(0xFF0A2E1C), Color(0xFF05170E), Color(0xFF15402B), Color(0xFF48D18D)),
    ROYAL("Royal", Color(0xFF1B1B4B), Color(0xFF0F0F2D), Color(0xFF2B2B70), Color(0xFFFFD700))
}

val LocalAppTheme = androidx.compose.runtime.compositionLocalOf { AppTheme.MIDNIGHT }

@Composable
fun AppDrawerContent(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    closeDrawer: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = currentTheme.surfaceColor,
        drawerContentColor = Color.White
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "MUSICTECH",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = currentTheme.accentColor)
        )
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Theme",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall.copy(color = Color.White.copy(alpha = 0.5f))
        )
        
        AppTheme.values().forEach { theme ->
            val isSelected = currentTheme == theme
            NavigationDrawerItem(
                label = { Text(theme.displayName) },
                selected = isSelected,
                onClick = { 
                    onThemeChange(theme)
                    closeDrawer()
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = theme.accentColor.copy(alpha = 0.2f),
                    unselectedContainerColor = Color.Transparent,
                    selectedTextColor = theme.accentColor,
                    unselectedTextColor = Color.White
                )
            )
        }
        
        Spacer(Modifier.weight(1f))
        
        NavigationDrawerItem(
            label = { Text("About / Settings") },
            selected = false,
            onClick = { 
                showAboutDialog = true
                closeDrawer()
            },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = Color.White, unselectedIconColor = Color.White)
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showAboutDialog) {
        val context = LocalContext.current
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceColor),
                border = borderHelper(Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = currentTheme.accentColor, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("About MUSICTECH", color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The app is free to use.", color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Developed by Peter Damiano", color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://peterdamiano.vercel.app"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Web: peterdamiano.vercel.app", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/PetedianoTech"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GitHub: PetedianoTech", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun MusicAppRoot() {
    var currentTheme by remember { mutableStateOf(AppTheme.MIDNIGHT) }

    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Storage permission is required to load local music files.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            hasPermission = true
        } else {
            permissionLauncher.launch(permission)
        }
    }
    
    androidx.compose.runtime.CompositionLocalProvider(LocalAppTheme provides currentTheme) {
        if (hasPermission) {
            val viewModel: MusicViewModel = viewModel()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    AppDrawerContent(currentTheme, onThemeChange = { currentTheme = it }) {
                        scope.launch { drawerState.close() }
                    }
                }
            ) {
                MusicAppScreen(
                    viewModel = viewModel, 
                    currentTheme = currentTheme, 
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(currentTheme.midColor)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = currentTheme.accentColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Storage Permission Required",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "To find and play all music files on your device seamlessly, we need access to your local media.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { 
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            permissionLauncher.launch(permission) 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor)
                    ) {
                        Text("Grant Permission", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MusicAppScreen(viewModel: MusicViewModel, currentTheme: AppTheme, onOpenDrawer: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Core database collections
    val allSongs by viewModel.allSongs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsState()
    val smartPlaylists by viewModel.smartPlaylists.collectAsState()
    
    // Player settings state
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    
    // Add song form trigger state
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Search query state
    var searchQuery by remember { mutableStateOf("") }
    
    // Deletion confirmation trigger state
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    
    // Edit details trigger state
    var songToEdit by remember { mutableStateOf<Song?>(null) }
    
    // Multi-select state
    var selectedSongIds by remember { mutableStateOf(setOf<Int>()) }
    var showBulkPlaylistDialog by remember { mutableStateOf(false) }
    
    // Now playing screen state
    var showNowPlaying by remember { mutableStateOf(false) }
    
    // Sort logic
    val sortOption by viewModel.sortOption.collectAsState()

    // Dynamic theme background blending
    val activeColor = currentSong?.colorHex?.toComposeColor() ?: WorkspaceBlue
    val animatedBgColor by animateColorAsState(
        targetValue = activeColor.copy(alpha = 0.15f),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "DynamicAmbientColor"
    )

    val filteredSongs = remember(allSongs, searchQuery, sortOption) {
        val filtered = if (searchQuery.isBlank()) allSongs
        else allSongs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
        
        when (sortOption) {
            "TITLE" -> filtered.sortedBy { it.title.lowercase() }
            "ARTIST" -> filtered.sortedBy { it.artist.lowercase() }
            "DURATION" -> filtered.sortedByDescending { it.durationMs }
            else -> filtered.sortedByDescending { it.id } // RECENT (default by id since it's auto-generated)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedBgColor,
                        currentTheme.midColor,
                        currentTheme.bottomColor
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // ________________ TOP NAVIGATION / HEADER REGION ________________
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Menu",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (currentSong == null) {
                    Text(
                        "MUSICTECH",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color.White)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }
            
            if (currentSong == null) {
                Spacer(modifier = Modifier.height(30.dp))
                // Default Visual Brand Logo when nothing is active
                BrandLogoWidget()
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // ________________ MAIN VIEWPORT ________________
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main visual player hub (At top of viewport)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        currentSong?.let { song ->
                            CoreVisualizerCard(
                                song = song,
                                isPlaying = isPlaying,
                                durationMs = durationMs,
                                currentPositionMs = currentPositionMs,
                                onSeek = { progress -> viewModel.seekTo(progress) },
                                onPrev = { viewModel.playPrevious() },
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onNext = { viewModel.playNext() },
                                onFavToggle = { viewModel.toggleFavorite(song) }
                            )
                        }
                    }
                }
                
                // Live Lyrics expansion pane
                if (currentSong?.lyrics?.isNotBlank() == true) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            LyricsCard(song = currentSong!!, currentPositionMs = currentPositionMs)
                        }
                    }
                }

                // Recently Played Section
                if (recentlyPlayedSongs.isNotEmpty()) {
                    item {
                        RecentlyPlayedSection(
                            recentSongs = recentlyPlayedSongs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onSongClick = { viewModel.playSong(it) }
                        )
                    }
                }

                // Smart Playlists Generator (Genre-Based)
                if (smartPlaylists.isNotEmpty()) {
                    item {
                        SmartPlaylistsSection(
                            smartPlaylists = smartPlaylists,
                            onPlaylistClick = { viewModel.playSmartPlaylist(it) }
                        )
                    }
                }

                // Library title bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Your Sound Library",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "${allSongs.size} tracks active • Tap to stream",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            var showSortMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Sort", tint = Color.White)
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Recent") },
                                        onClick = { viewModel.setSortOption("RECENT"); showSortMenu = false }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Title") },
                                        onClick = { viewModel.setSortOption("TITLE"); showSortMenu = false }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Artist") },
                                        onClick = { viewModel.setSortOption("ARTIST"); showSortMenu = false }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Duration") },
                                        onClick = { viewModel.setSortOption("DURATION"); showSortMenu = false }
                                    )
                                }
                            }
                            
                            // Add track button triggering modal
                            IconButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                currentTheme.accentColor,
                                                currentTheme.accentColor.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                                    .testTag("add_song_trigger_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Track",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Song feed

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = { Text("Search songs or artists...", color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear Search", tint = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentTheme.accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                            cursorColor = currentTheme.accentColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                }

                if (filteredSongs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.03f)
                            ),
                            border = borderHelper(Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (allSongs.isEmpty()) "No local music found." else "No results found for '$searchQuery'",
                                    color = Color.White.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSongs) { song ->
                        val isSelected = selectedSongIds.contains(song.id)
                        val isSelectionMode = selectedSongIds.isNotEmpty()
                        
                        SongGlassCard(
                            song = song,
                            isActive = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onPlayClick = { 
                                if (isSelectionMode) {
                                    selectedSongIds = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                                } else {
                                    viewModel.playSong(song)
                                }
                            },
                            onLongPress = {
                                selectedSongIds = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                            },
                            onFavToggle = { viewModel.toggleFavorite(song) },
                            onDeleteClick = { songToDelete = song },
                            onEditClick = { songToEdit = song },
                            onAddToPlaylistClick = {
                                selectedSongIds = setOf(song.id)
                                showBulkPlaylistDialog = true
                            },
                            onPlayNextClick = {
                                viewModel.setNextSong(song)
                                android.widget.Toast.makeText(context, "Added to Play Next", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // ________________ CONTEXTUAL ACTION BAR ________________
        AnimatedVisibility(
            visible = selectedSongIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WorkspaceBlue),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedSongIds.size} Selected",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { viewModel.applyToSelection(selectedSongIds, "FAVORITE"); selectedSongIds = emptySet() }) {
                            Icon(Icons.Default.Favorite, contentDescription = "Favorite All", tint = Color.White)
                        }
                        IconButton(onClick = { showBulkPlaylistDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add to Playlist", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.applyToSelection(selectedSongIds, "DELETE"); selectedSongIds = emptySet() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete All", tint = Color.White)
                        }
                        IconButton(onClick = { selectedSongIds = emptySet() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear Selection", tint = Color.White)
                        }
                    }
                }
            }
        }
        
        // ________________ MODALS & DIALOGS ________________
        
        // Form dialog to add custom tracks
        if (showAddDialog) {
            AddSongDialog(
                onDismiss = { showAddDialog = false },
                onAddSong = { title, artist, url, genre, color, lyrics, albumArtUrl ->
                    viewModel.addNewSong(title, artist, url, genre, color, lyrics, albumArtUrl)
                    showAddDialog = false
                }
            )
        }

        // Deletion confirmation trigger state
        songToDelete?.let { song ->
            DeleteConfirmationDialog(
                song = song,
                onDismiss = { songToDelete = null },
                onConfirm = {
                    viewModel.deleteSong(song)
                    songToDelete = null
                }
            )
        }

        // Edit details trigger state
        songToEdit?.let { song ->
            EditSongDialog(
                song = song,
                onDismiss = { songToEdit = null },
                onSave = { newTitle, newArtist, newPlaylist ->
                    viewModel.updateSongDetails(song, newTitle, newArtist, newPlaylist)
                    songToEdit = null
                }
            )
        }

        if (showBulkPlaylistDialog) {
            BulkPlaylistDialog(
                onDismiss = { showBulkPlaylistDialog = false },
                onSave = { playlistName ->
                    viewModel.applyToSelection(selectedSongIds, "PLAYLIST", playlistName)
                    selectedSongIds = emptySet()
                    showBulkPlaylistDialog = false
                }
            )
        }

        AnimatedVisibility(
            visible = currentSong != null && !showNowPlaying && selectedSongIds.isEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            currentSong?.let { song ->
                HeadsUpNotificationBar(
                    song = song,
                    isPlaying = isPlaying,
                    currentPos = currentPositionMs,
                    duration = durationMs,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onClick = { showNowPlaying = true }
                )
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize().zIndex(100f)
        ) {
            currentSong?.let { song ->
                NowPlayingScreen(
                    song = song,
                    isPlaying = isPlaying,
                    durationMs = durationMs,
                    currentPositionMs = currentPositionMs,
                    onSeek = { progress -> viewModel.seekTo(progress) },
                    onPrev = { viewModel.playPrevious() },
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNext() },
                    onFavToggle = { viewModel.toggleFavorite(song) },
                    onClose = { showNowPlaying = false },
                    onEditLyrics = { lyrics -> viewModel.updateLyrics(song, lyrics) }
                )
            }
        }
    }
}

@Composable
fun EditSongDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var playlist by remember { mutableStateOf(song.genre) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LocalAppTheme.current.surfaceColor),
            border = borderHelper(Color.White.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Edit Track", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist, onValueChange = { artist = it }, label = { Text("Artist") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = playlist, onValueChange = { playlist = it }, label = { Text("Playlist Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(title, artist, playlist) }, colors = ButtonDefaults.buttonColors(containerColor = LocalAppTheme.current.accentColor)) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun BulkPlaylistDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var playlist by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LocalAppTheme.current.surfaceColor),
            border = borderHelper(Color.White.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Add to Playlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = playlist, onValueChange = { playlist = it }, label = { Text("Playlist Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(playlist) }, colors = ButtonDefaults.buttonColors(containerColor = LocalAppTheme.current.accentColor)) { Text("Save") }
                }
            }
        }
    }
}

// Detailed Live Lyrics Integration card
@Composable
fun LyricsCard(
    song: Song,
    currentPositionMs: Long
) {
    val lyrics = remember(song.lyrics) { parseLyrics(song.lyrics) }
    if (lyrics.isEmpty()) return

    // Find the current line dynamically sync'd
    val currentLineIndex = lyrics.indexOfLast { it.timestampMs <= currentPositionMs }.coerceAtLeast(0)
    
    val themeColor = song.colorHex.toComposeColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .testTag("lyrics_panel"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.04f)
        ),
        border = borderHelper(Color.White.copy(alpha = 0.08f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                if (currentLineIndex > 0) {
                    Text(
                        text = lyrics[currentLineIndex - 1].text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.2f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = lyrics.getOrNull(currentLineIndex)?.text ?: "...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                    color = themeColor,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
                
                if (currentLineIndex < lyrics.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lyrics[currentLineIndex + 1].text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.2f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Detailed Play History horizontally
@Composable
fun RecentlyPlayedSection(
    recentSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Recently Played",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(recentSongs) { song ->
                val isActive = currentSong?.id == song.id
                val themeColor = song.colorHex.toComposeColor()
                
                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .clickable { onSongClick(song) }
                        .testTag("recent_song_${song.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = if (isActive) 0.08f else 0.03f)
                    ),
                    border = borderHelper(if (isActive) themeColor else Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(themeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.albumArtUrl.isNotBlank()) {
                                AsyncImage(
                                    model = song.albumArtUrl,
                                    contentDescription = "Album Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = themeColor, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Smart Playlist Generator (Genre-Based)
@Composable
fun SmartPlaylistsSection(
    smartPlaylists: Map<String, List<Song>>,
    onPlaylistClick: (List<Song>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Smart Mixes",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(smartPlaylists.entries.toList()) { entry ->
                val genre = entry.key
                val songs = entry.value
                val dominantColor = songs.firstOrNull()?.colorHex?.toComposeColor() ?: WorkspaceBlue
                
                Card(
                     modifier = Modifier
                         .width(180.dp)
                         .height(110.dp)
                         .clickable { onPlaylistClick(songs) }
                         .testTag("smart_playlist_$genre"),
                     shape = RoundedCornerShape(20.dp),
                     colors = CardDefaults.cardColors(containerColor = dominantColor.copy(alpha = 0.15f)),
                     border = borderHelper(dominantColor.copy(alpha = 0.4f))
                 ) {
                     Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                         Column(modifier = Modifier.align(Alignment.BottomStart)) {
                             Text(
                                 text = "$genre Vibes",
                                 style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
                                 maxLines = 2
                             )
                             Text(
                                 text = "${songs.size} curated tracks",
                                 style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                             )
                         }
                     }
                 }
            }
        }
    }
}

// Simple border stroke drawer helper
fun borderHelper(color: Color): BorderStroke = BorderStroke(
    width = 1.dp,
    color = color
)

@Composable
fun BrandLogoWidget() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.sweepGradient(
                        colors = listOf(WorkspaceBlue, WorkspaceGreen, WorkspaceYellow, WorkspaceRed, WorkspaceBlue)
                    )
                )
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(LocalAppTheme.current.surfaceColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Musictech app icon logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "MUSICTECH",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = Color.White
            )
        )
        Text(
            text = "Workspace Core Player",
            style = MaterialTheme.typography.bodySmall.copy(
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        )
    }
}

// Beautiful heads-up status notification bar widget (Dynamic pill styled like system notification cap overlay)
@Composable
fun HeadsUpNotificationBar(
    song: Song,
    isPlaying: Boolean,
    currentPos: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onClick: () -> Unit = {}
) {
    val progress = if (duration > 0) currentPos.toFloat() / duration.coerceAtLeast(1L) else 0f
    val themeColor = song.colorHex.toComposeColor()
    
    // Disc rotation loop
    val infiniteTransition = rememberInfiniteTransition(label = "DiscRotationTransition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiscRotationAngle"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag("status_notification_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spinning disk cover visual
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .rotate(if (isPlaying) rotationAngle else 0f)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(themeColor, themeColor.copy(alpha = 0.3f), themeColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(LocalAppTheme.current.surfaceColor)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Track details scrolling text
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Now Streaming",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = themeColor
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Visual wave indicator
                        if (isPlaying) {
                            VisualWavePulsator(themeColor)
                        }
                    }
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Small circular toggle button
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .testTag("notification_bar_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Toggle play capsule",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Embedded micro progress slider rail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(themeColor, themeColor.copy(alpha = 0.5f))
                            )
                        )
                )
            }
        }
    }
}

// Pulsating live sound-wave visualizer bar indicator
@Composable
fun VisualWavePulsator(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTrans")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "WaveS1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "WaveS2"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "WaveS3"
    )

    Row(
        modifier = Modifier.height(10.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.width(2.dp).fillMaxHeight(scale1).background(color))
        Box(modifier = Modifier.width(2.dp).fillMaxHeight(scale2).background(color))
        Box(modifier = Modifier.width(2.dp).fillMaxHeight(scale3).background(color))
    }
}

// Gorgeous central visualization controller card representation of active playback
@Composable
fun CoreVisualizerCard(
    song: Song,
    isPlaying: Boolean,
    durationMs: Long,
    currentPositionMs: Long,
    onSeek: (Float) -> Unit,
    onPrev: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onFavToggle: () -> Unit
) {
    val themeColor = song.colorHex.toComposeColor()
    val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.coerceAtLeast(1L) else 0f
    
    // Wave visual pulse variables
    val infiniteTransition = rememberInfiniteTransition(label = "CircleWaveTrans")
    val ringMultiplier1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VisualRing1"
    )
    val ringMultiplier2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VisualRing2"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("core_player_panel"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.04f)
        ),
        border = borderHelper(Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // central spinning concentric soundwave simulator
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulsators
                if (isPlaying) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = themeColor,
                            radius = (size.minDimension / 2) * ringMultiplier1,
                            style = Stroke(width = 1.5.dp.toPx()),
                            alpha = (1f - (ringMultiplier1 - 0.9f) / 0.35f).coerceIn(0f, 1f)
                        )
                        drawCircle(
                            color = themeColor,
                            radius = (size.minDimension / 2) * ringMultiplier2,
                            style = Stroke(width = 1.dp.toPx()),
                            alpha = (1f - (ringMultiplier2 - 1.0f) / 0.45f).coerceIn(0f, 1f)
                        )
                    }
                }

                // Center visual artwork plate with Google workspace overlapping accent borders
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(LocalAppTheme.current.surfaceColor)
                        .border(
                            width = 4.dp,
                            brush = Brush.sweepGradient(
                                listOf(WorkspaceBlue, WorkspaceGreen, WorkspaceYellow, WorkspaceRed, WorkspaceBlue)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.albumArtUrl.isNotBlank()) {
                        AsyncImage(
                            model = song.albumArtUrl,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Active Track Note vector",
                            tint = themeColor,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Genre: ${song.genre}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = themeColor,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Favorite control
                IconButton(
                    onClick = onFavToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .testTag("player_favorite_toggle")
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle favorite state",
                        tint = if (song.isFavorite) WorkspaceRed else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progressive Seek Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(
                        thumbColor = themeColor,
                        activeTrackColor = themeColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("player_progress_seek_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPositionMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = formatDuration(durationMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player central circular transport controllers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.03f))
                        .testTag("player_skip_backward")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = "Previous Song",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Big play capsule
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(themeColor, themeColor.copy(alpha = 0.7f))
                            )
                        )
                        .testTag("player_play_pause_bubble")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Toggle Play State",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.03f))
                        .testTag("player_skip_forward")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Next Song",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    durationMs: Long,
    currentPositionMs: Long,
    onSeek: (Float) -> Unit,
    onPrev: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onFavToggle: () -> Unit,
    onClose: () -> Unit,
    onEditLyrics: (String) -> Unit
) {
    val themeColor = song.colorHex.toComposeColor()
    var showLyricsEdit by remember { mutableStateOf(false) }
    var lyricsInput by remember { mutableStateOf(song.lyrics) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppTheme.current.bottomColor)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("Now Playing", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showLyricsEdit = true }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Lyrics", tint = Color.White)
                }
                IconButton(onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH)
                    intent.putExtra(android.app.SearchManager.QUERY, "${song.title} ${song.artist} lyrics")
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search Google", tint = Color.White)
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    CoreVisualizerCard(
                        song = song,
                        isPlaying = isPlaying,
                        durationMs = durationMs,
                        currentPositionMs = currentPositionMs,
                        onSeek = onSeek,
                        onPrev = onPrev,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onFavToggle = onFavToggle
                    )
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
                if (song.lyrics.isNotBlank()) {
                    item {
                        LyricsCard(song = song, currentPositionMs = currentPositionMs)
                    }
                }
                item { Spacer(modifier = Modifier.height(48.dp)) }

                // Volume slider placeholder or simple volume control
                // For a real volume control we'd need an AudioManager
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White.copy(0.6f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volume Control via System Buttons", color = Color.White.copy(0.4f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (showLyricsEdit) {
            Dialog(onDismissRequest = { showLyricsEdit = false }) {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = LocalAppTheme.current.surfaceColor), modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Edit Lyrics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = lyricsInput,
                            onValueChange = { lyricsInput = it },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            placeholder = { Text("Paste lyrics here...") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { showLyricsEdit = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { onEditLyrics(lyricsInput); showLyricsEdit = false }, colors = ButtonDefaults.buttonColors(containerColor = LocalAppTheme.current.accentColor)) { Text("Save") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongGlassCard(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onPlayClick: () -> Unit,
    onLongPress: () -> Unit,
    onFavToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit = {},
    onPlayNextClick: () -> Unit = {}
) {
    val themeColor = song.colorHex.toComposeColor()
    val animatedStrokeColor by animateColorAsState(
        targetValue = if (isSelected) WorkspaceRed else if (isActive) themeColor else Color.White.copy(alpha = 0.06f),
        label = "CardBorderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("song_item_card_${song.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) WorkspaceRed.copy(alpha = 0.15f) else if (isActive) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.03f)
        ),
        border = borderHelper(animatedStrokeColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onPlayClick,
                    onLongClick = onLongPress
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            // Left Workspace themed geometric indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColor.copy(alpha = 0.15f))
                    .border(width = 1.dp, color = themeColor.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUrl.isNotBlank()) {
                    AsyncImage(
                        model = song.albumArtUrl,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isActive || isSelected) themeColor else Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick heart play action
            IconButton(
                onClick = onFavToggle,
                modifier = Modifier.size(34.dp).testTag("list_item_favicon_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite track icon button",
                    tint = if (song.isFavorite) WorkspaceRed else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            
            var showMenu by remember { mutableStateOf(false) }
            
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Play Next") },
                        onClick = { onPlayNextClick(); showMenu = false }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        onClick = { onAddToPlaylistClick(); showMenu = false }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Edit Details") },
                        onClick = { onEditClick(); showMenu = false }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Delete", color = WorkspaceRed) },
                        onClick = { onDeleteClick(); showMenu = false }
                    )
                }
            }
        }
    }
}

// Dialog to add custom sound tracks to local Room DB
@Composable
fun AddSongDialog(
    onDismiss: () -> Unit,
    onAddSong: (String, String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var uriOrUrl by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var albumArtUrl by remember { mutableStateOf("") }
    
    // Choose which Google color theme to link
    var selectedColorCode by remember { mutableStateOf("#4285F4") } // Default is Blue
    
    val workspaceColorsList = listOf(
        Pair("#4285F4", "Workspace Blue (Docs/Drive)"),
        Pair("#0F9D58", "Workspace Green (Sheets)"),
        Pair("#F4B400", "Workspace Yellow (Slides/Keep)"),
        Pair("#DB4437", "Workspace Red (Gmail)")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("add_song_dialog_panel"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = LocalAppTheme.current.surfaceColor
            ),
            border = borderHelper(Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Custom Beat",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel model",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Standard clean input drawers
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title") },
                    colors = textFieldColorsHelper(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_song_input_title")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist Name") },
                    colors = textFieldColorsHelper(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_song_input_artist")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uriOrUrl,
                    onValueChange = { uriOrUrl = it },
                    placeholder = { Text("e.g. https://www.soundhelix.com/...") },
                    label = { Text("Streaming Audio URL (Leave empty for demo)") },
                    colors = textFieldColorsHelper(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_song_input_url")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre (e.g. Synthwave, Acoustic)") },
                    colors = textFieldColorsHelper(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_song_input_genre")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    placeholder = { Text("0|First line\n5000|Second line") },
                    label = { Text("Live Lyrics (timestamp_ms|text)") },
                    colors = textFieldColorsHelper(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("add_song_input_lyrics")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = albumArtUrl,
                    onValueChange = { albumArtUrl = it },
                    placeholder = { Text("e.g. https://images.unsplash.com/...") },
                    label = { Text("Album Art URL (Optional)") },
                    colors = textFieldColorsHelper(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_song_input_album_art")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Workspace Color selector chips
                Text(
                    text = "Select Google Workspace Theme Color",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    workspaceColorsList.forEach { pair ->
                        val hex = pair.first
                        val isSelected = selectedColorCode == hex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(hex.toComposeColor())
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorCode = hex }
                                .testTag("color_chip_$hex")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddSong(title, artist, uriOrUrl, genre, selectedColorCode, lyrics, albumArtUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("add_song_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedColorCode.toComposeColor(),
                        contentColor = Color.White
                    ),
                    enabled = title.isNotBlank()
                ) {
                    Text(
                        text = "Save Track to Database",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun textFieldColorsHelper() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = LocalAppTheme.current.accentColor,
    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
    focusedLabelColor = LocalAppTheme.current.accentColor,
    unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
    focusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f)
)

// Deletion affirmation alert popup
@Composable
fun DeleteConfirmationDialog(
    song: Song,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("delete_confirmation_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = LocalAppTheme.current.surfaceColor
            ),
            border = borderHelper(WorkspaceRed.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(WorkspaceRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = WorkspaceRed,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Delete Song?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want to completely delete \"${song.title}\"? This will delete it permanently from your database.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    ),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("delete_cancel_action"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            contentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("delete_confirm_action"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WorkspaceRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
