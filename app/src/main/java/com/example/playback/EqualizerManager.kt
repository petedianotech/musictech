package com.example.playback

import android.media.audiofx.Equalizer

object EqualizerManager {
    var equalizer: Equalizer? = null
        private set

    fun setup(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            if (equalizer != null) {
                equalizer?.release()
            }
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }

    fun getBandLevel(band: Short): Short {
        return equalizer?.getBandLevel(band) ?: 0
    }

    fun setBandLevel(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
    }

    fun getNumberOfBands(): Short {
        return equalizer?.numberOfBands ?: 0
    }

    fun getBandFreqRange(band: Short): IntArray {
        return equalizer?.getBandFreqRange(band) ?: intArrayOf(0, 0)
    }

    fun getBandLevelRange(): ShortArray {
        return equalizer?.bandLevelRange ?: shortArrayOf(0, 0)
    }
}
