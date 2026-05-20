package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.playback.EqualizerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onNavigateBack: () -> Unit) {
    var numBands by remember { mutableIntStateOf(0) }
    var bandLevels by remember { mutableStateOf<ShortArray>(shortArrayOf()) }
    var minLevel by remember { mutableIntStateOf(0) }
    var maxLevel by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val bands = EqualizerManager.getNumberOfBands()
        if (bands > 0) {
            numBands = bands.toInt()
            val levels = ShortArray(bands.toInt())
            for (i in 0 until bands) {
                levels[i] = EqualizerManager.getBandLevel(i.toShort())
            }
            bandLevels = levels
            
            val range = EqualizerManager.getBandLevelRange()
            minLevel = range[0].toInt()
            maxLevel = range[1].toInt()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (numBands > 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0 until numBands) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val freqRange = EqualizerManager.getBandFreqRange(i.toShort())
                            val freqHz = (freqRange[0] + freqRange[1]) / 2 / 1000 // roughly center freq in Hz
                            
                            Slider(
                                value = bandLevels[i].toFloat(),
                                onValueChange = { newVal ->
                                    val newShort = newVal.toInt().toShort()
                                    EqualizerManager.setBandLevel(i.toShort(), newShort)
                                    val newArray = bandLevels.clone()
                                    newArray[i] = newShort
                                    bandLevels = newArray
                                },
                                valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                modifier = Modifier.height(200.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${freqHz}Hz", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Equalizer not available or not playing.")
            }
        }
    }
}
