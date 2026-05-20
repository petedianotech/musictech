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
