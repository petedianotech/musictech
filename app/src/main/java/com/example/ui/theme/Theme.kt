package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WorkspaceBlue,
    secondary = WorkspaceGreen,
    tertiary = WorkspaceYellow,
    background = SlateMidnight,
    surface = ObsidianDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = OffWhite,
    onSurface = OffWhite
)

@Composable
fun MusictechTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
