package com.example.ui.screens

import android.content.ContentUris
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    val track by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.currentPosition.collectAsStateWithLifecycle()
    var showLyrics by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
                    }
                },
                actions = {
                    val isFavorite = viewModel.favorites.collectAsStateWithLifecycle().value.any { it.mediaUri == track?.uri }
                    IconButton(onClick = { track?.let { viewModel.toggleFavorite(it.uri, isFavorite) } }) {
                        Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite")
                    }
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Lyrics")
                    }
                    IconButton(onClick = onNavigateToEqualizer) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (track != null) {
            val t = track!!
            val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), t.albumId)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                if (showLyrics) {
                    val lyricsText by viewModel.getLyrics(t.uri).collectAsStateWithLifecycle(initialValue = null)
                    var isEditing by remember { mutableStateOf(false) }
                    var editText by remember(lyricsText, isEditing) { mutableStateOf(lyricsText ?: "") }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Lyrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    if (isEditing) {
                                        viewModel.saveLyrics(t.uri, editText)
                                    }
                                    isEditing = !isEditing
                                }) {
                                    Icon(if(isEditing) Icons.Default.Check else Icons.Default.Edit, "Edit")
                                }
                            }
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    modifier = Modifier.fillMaxSize(),
                                    placeholder = { Text("Enter lyrics here...") }
                                )
                            } else {
                                val displayLyrics = if (lyricsText.isNullOrBlank()) "♪\n(No lyrics found. Tap edit to add)\n\nEnjoy the music..." else lyricsText!!
                                
                                val lrcRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)")
                                val lines = displayLyrics.split("\n")
                                val parsedLines = lines.mapNotNull { line ->
                                    val match = lrcRegex.find(line)
                                    if (match != null) {
                                        val m = match.groupValues[1].toLong()
                                        val s = match.groupValues[2].toLong()
                                        val ms = match.groupValues[3].toLong()
                                        val totalMs = (m * 60 + s) * 1000 + ms * 10
                                        totalMs to match.groupValues[4]
                                    } else null
                                }

                                if (parsedLines.isNotEmpty()) {
                                    val currentLineIndex = parsedLines.indexOfLast { it.first <= position }.coerceAtLeast(0)
                                    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
                                    
                                    LaunchedEffect(currentLineIndex) {
                                        if (currentLineIndex >= 0) {
                                            scrollState.animateScrollToItem(currentLineIndex)
                                        }
                                    }

                                    LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState, horizontalAlignment = Alignment.CenterHorizontally) {
                                        itemsIndexed(parsedLines) { index, (time, text) ->
                                            val isCurrent = index == currentLineIndex
                                            Text(
                                                text = text.ifBlank { "♪" },
                                                style = if (isCurrent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        item {
                                            Text(
                                                text = displayLyrics,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Album Art & Spectrum Visualizer Layer
                    val spectrumStyle by viewModel.settingsRepository.spectrumStyle.collectAsStateWithLifecycle()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(albumArtUri)
                                .crossfade(true)
                                .error(R.drawable.img_album_placeholder_1779303397262) // fallback
                                .build(),
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Semi-transparent overlay to make spectrum pop on top of album artwork
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                        
                        SpectrumVisualizer(
                            isPlaying = isPlaying,
                            spectrumStyle = spectrumStyle,
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Track Info
                Text(
                    text = t.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = t.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Seek Bar (Simplified)
                val duration = if(t.duration > 0) t.duration.toFloat() else 1f
                Slider(
                    value = position.toFloat().coerceAtMost(duration),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..duration,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMs(position), style = MaterialTheme.typography.labelMedium)
                    Text(formatMs(t.duration), style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Volume slider
                val volumeState by viewModel.volume.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val volumeIcon = if (volumeState == 0f) Icons.Default.VolumeOff else if (volumeState < 0.5f) Icons.Default.VolumeDown else Icons.Default.VolumeUp
                    IconButton(onClick = { viewModel.setVolume(if (volumeState > 0f) 0f else 1f) }) {
                        Icon(volumeIcon, contentDescription = "Volume")
                    }
                    Slider(
                        value = volumeState,
                        onValueChange = { viewModel.setVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(volumeState * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Controls
                val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
                val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::toggleShuffleMode, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = viewModel::skipToPrevious, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                    }
                    
                    FloatingActionButton(
                        onClick = viewModel::togglePlayPause,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    IconButton(onClick = viewModel::skipToNext, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = viewModel::cycleRepeatMode, modifier = Modifier.size(48.dp)) {
                        val icon = when(repeatMode) {
                            androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        }
                        Icon(icon, contentDescription = "Repeat", tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No track playing")
            }
        }
    }
}

fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

data class VisualizerParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val dist: Float
)

@Composable
fun SpectrumVisualizer(
    isPlaying: Boolean,
    spectrumStyle: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SpectrumAnim")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    val energy by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.05f,
        animationSpec = tween(1000),
        label = "Energy"
    )

    var activeCalculatedStyle by remember(spectrumStyle) { mutableIntStateOf(if (spectrumStyle == 0) kotlin.random.Random.nextInt(1, 6) else spectrumStyle) }
    if (spectrumStyle == 0) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(5000L)
                activeCalculatedStyle = kotlin.random.Random.nextInt(1, 6)
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    // Custom particles for style 5, generated once
    val particles = remember {
        List(30) {
            val angle = kotlin.random.Random.nextFloat() * 2f * 3.1415927f
            val speed = 10f + kotlin.random.Random.nextFloat() * 30f
            val size = 3f + kotlin.random.Random.nextFloat() * 9f
            val dist = 20f + kotlin.random.Random.nextFloat() * 130f
            VisualizerParticle(angle, speed, size, dist)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f

        when (activeCalculatedStyle) {
            1 -> { // Bars (Classic)
                val barsCount = 20
                val barSpacing = width / (barsCount * 2f + 1)
                val barWidth = barSpacing * 1.5f
                for (i in 0 until barsCount) {
                    val angleOffset = i * 0.35f
                    val baseFactor = sin(animTime * 2.2f + angleOffset) * cos(animTime * 1.1f + angleOffset * 0.5f)
                    val value = (abs(baseFactor) * 0.75f + 0.25f) * energy
                    val barHeight = (height * 0.65f) * value
                    val x = barSpacing + i * (barWidth + barSpacing)
                    val y = height - barHeight
                    
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
            2 -> { // Waveform
                val path1 = Path()
                val path2 = Path()
                val steps = 80
                val stepWidth = width / steps

                path1.moveTo(0f, cy)
                path2.moveTo(0f, cy)

                for (i in 0..steps) {
                    val x = i * stepWidth
                    val waveAmp1 = sin(animTime * 1.8f + i * 0.09f) * cos(animTime * 0.9f + i * 0.05f) * (height * 0.25f) * energy
                    val waveAmp2 = cos(animTime * 1.4f + i * 0.12f) * sin(animTime * 0.6f + i * 0.07f) * (height * 0.20f) * energy
                    path1.lineTo(x, cy + waveAmp1)
                    path2.lineTo(x, cy - waveAmp2)
                }

                drawPath(
                    path = path1,
                    color = primaryColor.copy(alpha = 0.7f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = path2,
                    color = secondaryColor.copy(alpha = 0.6f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
            3 -> { // Circular radial
                val rBase = minOf(width, height) * 0.22f
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = rBase,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.8f),
                    radius = rBase,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )

                val spikes = 40
                for (i in 0 until spikes) {
                    val angleDeg = i * (360f / spikes)
                    val angleRad = angleDeg * (Math.PI / 180f).toFloat()
                    val phaseFactor = sin(animTime * 2.5f + i * 0.3f) * cos(animTime * 1.2f + i * 0.15f)
                    val length = (abs(phaseFactor) * 0.5f + 0.15f) * (minOf(width, height) * 0.18f) * energy
                    
                    val startX = cx + cos(angleRad) * rBase
                    val startY = cy + sin(angleRad) * rBase
                    val endX = cx + cos(angleRad) * (rBase + length)
                    val endY = cy + sin(angleRad) * (rBase + length)

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor, secondaryColor),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }
            4 -> { // Pulsating rings
                val numCircles = 4
                val rMax = minOf(width, height) * 0.45f
                val rMin = minOf(width, height) * 0.12f
                for (i in 0 until numCircles) {
                    val progress = ((animTime * 0.18f + i * 0.25f) % 1.0f)
                    val radius = rMin + progress * (rMax - rMin)
                    val alpha = (1f - progress) * energy
                    drawCircle(
                        color = secondaryColor.copy(alpha = alpha * 0.8f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 6f - progress * 3f)
                    )
                }
                drawCircle(
                    color = primaryColor,
                    radius = rMin + 10f * sin(animTime * 4f) * energy,
                    center = Offset(cx, cy)
                )
            }
            5 -> { // Particles orbit
                particles.forEachIndexed { i, p ->
                    val dynamicAngle = p.angle + (animTime * 0.05f * (p.speed / 30f)) * energy
                    val osc = sin(animTime * 2f + i) * 15f * energy
                    val radius = p.dist + osc
                    val x = cx + cos(dynamicAngle) * radius
                    val y = cy + sin(dynamicAngle) * radius

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor, Color.Transparent),
                            center = Offset(x, y),
                            radius = p.size * 1.8f
                        ),
                        radius = p.size * 1.8f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f * energy),
                        radius = p.size * 0.6f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
