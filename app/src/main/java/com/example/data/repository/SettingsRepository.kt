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

    private val _minDurationAtLeast = MutableStateFlow(prefs.getInt("min_duration", 0))
    val minDurationAtLeast: StateFlow<Int> = _minDurationAtLeast

    private val _hiddenTracks = MutableStateFlow(prefs.getStringSet("hidden_tracks", emptySet()) ?: emptySet())
    val hiddenTracks: StateFlow<Set<String>> = _hiddenTracks

    private val _spectrumStyle = MutableStateFlow(prefs.getInt("spectrum_style", 0))
    val spectrumStyle: StateFlow<Int> = _spectrumStyle

    private val _playOnHeadsetConnect = MutableStateFlow(prefs.getBoolean("play_on_headset", false))
    val playOnHeadsetConnect: StateFlow<Boolean> = _playOnHeadsetConnect

    private val _pauseOnHeadsetDisconnect = MutableStateFlow(prefs.getBoolean("pause_on_headset", true))
    val pauseOnHeadsetDisconnect: StateFlow<Boolean> = _pauseOnHeadsetDisconnect

    private val _showLockscreenArt = MutableStateFlow(prefs.getBoolean("show_lockscreen_art", true))
    val showLockscreenArt: StateFlow<Boolean> = _showLockscreenArt

    fun setThemeMode(mode: Int) {
        prefs.edit { putInt("theme_mode", mode) }
        _themeMode.value = mode
    }

    fun setCrossfadeDuration(seconds: Int) {
        prefs.edit { putInt("crossfade_duration", seconds) }
        _crossfadeDuration.value = seconds
    }

    fun setMinDuration(seconds: Int) {
        prefs.edit { putInt("min_duration", seconds) }
        _minDurationAtLeast.value = seconds
    }

    fun toggleHiddenTrack(uri: String) {
        val current = _hiddenTracks.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        prefs.edit { putStringSet("hidden_tracks", current) }
        _hiddenTracks.value = current
    }

    fun setSpectrumStyle(style: Int) {
        prefs.edit { putInt("spectrum_style", style) }
        _spectrumStyle.value = style
    }

    fun setPlayOnHeadsetConnect(enable: Boolean) {
        prefs.edit { putBoolean("play_on_headset", enable) }
        _playOnHeadsetConnect.value = enable
    }

    fun setPauseOnHeadsetDisconnect(enable: Boolean) {
        prefs.edit { putBoolean("pause_on_headset", enable) }
        _pauseOnHeadsetDisconnect.value = enable
    }

    fun setShowLockscreenArt(enable: Boolean) {
        prefs.edit { putBoolean("show_lockscreen_art", enable) }
        _showLockscreenArt.value = enable
    }
}

