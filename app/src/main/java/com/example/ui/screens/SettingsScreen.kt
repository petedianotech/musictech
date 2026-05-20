package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )

            val options = listOf("System Default", "Light", "Dark")
            
            Column(Modifier.selectableGroup()) {
                options.forEachIndexed { index, text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (themeMode == index),
                                onClick = { settingsRepository.setThemeMode(index) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (themeMode == index),
                            onClick = null 
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Text(
                text = "Audio Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            val crossfade by settingsRepository.crossfadeDuration.collectAsStateWithLifecycle()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (crossfade > 0) "Crossfade: $crossfade s" else "Crossfade: Off")
                Slider(
                    value = crossfade.toFloat(),
                    onValueChange = { settingsRepository.setCrossfadeDuration(it.toInt()) },
                    valueRange = 0f..15f,
                    steps = 14,
                    modifier = Modifier.width(180.dp)
                )
            }

            Text(
                text = "Library Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            val minDuration by settingsRepository.minDurationAtLeast.collectAsStateWithLifecycle()
            val hiddenTracks by settingsRepository.hiddenTracks.collectAsStateWithLifecycle()
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (minDuration > 0) "Hide shorter than $minDuration s" else "Show all tracks")
                Slider(
                    value = minDuration.toFloat(),
                    onValueChange = { settingsRepository.setMinDuration(it.toInt()) },
                    valueRange = 0f..120f,
                    steps = 119,
                    modifier = Modifier.width(180.dp)
                )
            }
            if (hiddenTracks.isNotEmpty()) {
                TextButton(
                    onClick = { hiddenTracks.forEach { settingsRepository.toggleHiddenTrack(it) } },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Clear ${hiddenTracks.size} hidden tracks")
                }
            }

            Text(
                text = "Headset & Visuals",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )

            val playOnHeadset by settingsRepository.playOnHeadsetConnect.collectAsStateWithLifecycle()
            val pauseOnHeadset by settingsRepository.pauseOnHeadsetDisconnect.collectAsStateWithLifecycle()
            val spectrum by settingsRepository.spectrumStyle.collectAsStateWithLifecycle()

            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Play on headset connect")
                Switch(checked = playOnHeadset, onCheckedChange = { settingsRepository.setPlayOnHeadsetConnect(it) })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Pause on headset disconnect")
                Switch(checked = pauseOnHeadset, onCheckedChange = { settingsRepository.setPauseOnHeadsetDisconnect(it) })
            }

            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Spectrum Style", modifier = Modifier.weight(1f))
                val spectrumOptions = listOf("Random Rotating", "Bars", "Waveform", "Circular", "Pulsating", "Particles")
                var expandedSpectrum by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expandedSpectrum = true }) {
                        Text(spectrumOptions[spectrum])
                    }
                    DropdownMenu(expanded = expandedSpectrum, onDismissRequest = { expandedSpectrum = false }) {
                        spectrumOptions.forEachIndexed { index, name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { settingsRepository.setSpectrumStyle(index); expandedSpectrum = false })
                        }
                    }
                }
            }
            
            Text(
                text = "Equalizer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            val eq = com.example.playback.EqualizerManager.equalizer
            if (eq != null) {
                val numBands = eq.numberOfBands
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                
                for (i in 0 until numBands) {
                    val band = i.toShort()
                    val freqRange = eq.getCenterFreq(band) / 1000 // Convert mHz to Hz
                    var level by remember { mutableFloatStateOf(eq.getBandLevel(band).toFloat()) }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "$freqRange Hz", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(50.dp))
                        Slider(
                            value = level,
                            onValueChange = { level = it; eq.setBandLevel(band, it.toInt().toShort()) },
                            valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                        )
                        Text(text = "${(level / 100).toInt()} dB", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                    }
                }
            } else {
                Text(
                    text = "Start playing music to enable equalizer",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
