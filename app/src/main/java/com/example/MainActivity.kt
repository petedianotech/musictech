package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.Song
import com.example.ui.theme.MusictechTheme
import com.example.ui.theme.SlateMidnight
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
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun MusicAppRoot() {
    val viewModel: MusicViewModel = viewModel()
    
    // Core database collections
    val allSongs by viewModel.allSongs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    
    // Player settings state
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    
    // Add song form trigger state
    var showAddDialog by mutableStateOf(false)
    
    // Deletion confirmation trigger state
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    
    // Dynamic theme background blending
    val activeColor = currentSong?.colorHex?.toComposeColor() ?: WorkspaceBlue
    val animatedBgColor by animateColorAsState(
        targetValue = activeColor.copy(alpha = 0.15f),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "DynamicAmbientColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedBgColor,
                        SlateMidnight,
                        Color(0xFF060912)
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
            
            // ________________ HEADER REGION & HEADS-UP MINI NOTIFICATION BAR ________________
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = currentSong != null,
                enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -50 }) + fadeOut()
            ) {
                currentSong?.let { song ->
                    HeadsUpNotificationBar(
                        song = song,
                        isPlaying = isPlaying,
                        currentPos = currentPositionMs,
                        duration = durationMs,
                        onTogglePlay = { viewModel.togglePlayPause() }
                    )
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

                        // Add track button triggering modal
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                               .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(WorkspaceBlue, WorkspaceGreen)
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

                // Song feed
                if (allSongs.isEmpty()) {
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
                                    text = "No songs added yet.\nTap '+' above to preload beats!",
                                    color = Color.White.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    items(allSongs) { song ->
                        SongGlassCard(
                            song = song,
                            isActive = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id,
                            onPlayClick = { viewModel.playSong(song) },
                            onFavToggle = { viewModel.toggleFavorite(song) },
                            onDeleteClick = { songToDelete = song }
                        )
                    }
                }
            }
        }
        
        // ________________ MODALS & DIALOGS ________________
        
        // Form dialog to add custom tracks
        if (showAddDialog) {
            AddSongDialog(
                onDismiss = { showAddDialog = false },
                onAddSong = { title, artist, url, genre, color ->
                    viewModel.addNewSong(title, artist, url, genre, color)
                    showAddDialog = false
                }
            )
        }

        // Deletion confirmation trigger state (To satisfy the "add delete songs button" securely)
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
                    .background(SlateMidnight),
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
    onTogglePlay: () -> Unit
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
                            .background(SlateMidnight)
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
                        .background(SlateMidnight)
                        .border(
                            width = 4.dp,
                            brush = Brush.sweepGradient(
                                listOf(WorkspaceBlue, WorkspaceGreen, WorkspaceYellow, WorkspaceRed, WorkspaceBlue)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Active Track Note vector",
                        tint = themeColor,
                        modifier = Modifier.size(54.dp)
                    )
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

// Gorgeous glassmorphic list row representing songs
@Composable
fun SongGlassCard(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onFavToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val themeColor = song.colorHex.toComposeColor()
    val animatedStrokeColor by animateColorAsState(
        targetValue = if (isActive) themeColor else Color.White.copy(alpha = 0.06f),
        label = "CardBorderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .testTag("song_item_card_${song.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.03f)
        ),
        border = borderHelper(animatedStrokeColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) themeColor else Color.White
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

            // Crucial requested DELETE button (In beautiful theme-aligned Workspace red outline/accent)
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(WorkspaceRed.copy(alpha = 0.1f))
                    .testTag("list_item_delete_button_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete track icon",
                    tint = WorkspaceRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Dialog to add custom sound tracks to local Room DB
@Composable
fun AddSongDialog(
    onDismiss: () -> Unit,
    onAddSong: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var uriOrUrl by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    
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
                containerColor = SlateMidnight
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
                            onAddSong(title, artist, uriOrUrl, genre, selectedColorCode)
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
    focusedBorderColor = WorkspaceBlue,
    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
    focusedLabelColor = WorkspaceBlue,
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
                containerColor = SlateMidnight
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
