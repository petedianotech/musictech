package com.example.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getInt("theme_mode", 0))
    val themeMode: StateFlow<Int> = _themeMode

    private val _crossfadeDuration = MutableStateFlow(prefs.getInt("crossfade_duration", 0))
    val crossfadeDuration: StateFlow<Int> = _crossfadeDuration

    fun setThemeMode(mode: Int) {
        prefs.edit { putInt("theme_mode", mode) }
        _themeMode.value = mode
    }

    fun setCrossfadeDuration(seconds: Int) {
        prefs.edit { putInt("crossfade_duration", seconds) }
        _crossfadeDuration.value = seconds
    }
}

