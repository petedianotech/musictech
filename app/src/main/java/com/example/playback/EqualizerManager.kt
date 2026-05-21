package com.example.playback

import android.media.audiofx.Equalizer

object EqualizerManager {
    var equalizer: Equalizer? = null
        private set

    fun setup(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            if (equalizer != null) {
                try { equalizer?.release() } catch(e: Exception) {}
            }
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try { equalizer?.release() } catch(e: Exception) {}
        equalizer = null
    }

    fun getBandLevel(band: Short): Short {
        return try { equalizer?.getBandLevel(band) ?: 0 } catch (e: Exception) { 0 }
    }

    fun setBandLevel(band: Short, level: Short) {
        try { equalizer?.setBandLevel(band, level) } catch (e: Exception) {}
    }

    fun getNumberOfBands(): Short {
        return try { equalizer?.numberOfBands ?: 0 } catch (e: Exception) { 0 }
    }

    fun getBandFreqRange(band: Short): IntArray {
        return try { equalizer?.getBandFreqRange(band) ?: intArrayOf(0, 0) } catch (e: Exception) { intArrayOf(0, 0) }
    }

    fun getBandLevelRange(): ShortArray {
        return try { equalizer?.bandLevelRange ?: shortArrayOf(0, 0) } catch (e: Exception) { shortArrayOf(0, 0) }
    }

    fun getCenterFreq(band: Short): Int {
        return try { equalizer?.getCenterFreq(band) ?: 0 } catch (e: Exception) { 0 }
    }
}
