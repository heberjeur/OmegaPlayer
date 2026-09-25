package com.arslandaim.omegaplayer.media

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqManager @Inject constructor() {
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentBoostScale: Float = 1.0f
    
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    val bands: StateFlow<List<EqBand>> = _bands.asStateFlow()

    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets.asStateFlow()

    fun setupEqualizer(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            val previousBands = _bands.value
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _enabled.value
            }
            if (previousBands.isNotEmpty()) {
                previousBands.forEach { band ->
                    try {
                        equalizer?.setBandLevel(band.id, band.level)
                    } catch (e: IllegalArgumentException) {
                        equalizer = null
                    } catch (e: IllegalStateException) {
                        equalizer = null
                    }
                }
            }
            loadBands()
            loadPresets()
            
            loudnessEnhancer?.release()
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(calculateGainMb(currentBoostScale))
                enabled = true
            }
        } catch (e: IllegalStateException) {
            equalizer = null
            loudnessEnhancer = null
        } catch (e: IllegalArgumentException) {
            equalizer = null
            loudnessEnhancer = null
        } catch (e: UnsupportedOperationException) {
            equalizer = null
            loudnessEnhancer = null
        } catch (e: RuntimeException) {
            equalizer = null
            loudnessEnhancer = null
        }
    }

    private fun calculateGainMb(scale: Float): Int {
        return if (scale > 1.0f) {
            ((scale - 1.0f) * 1000f).toInt()
        } else {
            0
        }
    }

    fun setVolumeBoostScale(scale: Float) {
        currentBoostScale = scale.coerceIn(1.0f, 2.0f)
        try {
            loudnessEnhancer?.setTargetGain(calculateGainMb(currentBoostScale))
        } catch (e: IllegalStateException) {
            loudnessEnhancer = null
        } catch (e: IllegalArgumentException) {
            loudnessEnhancer = null
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        equalizer?.enabled = enabled
    }

    fun setBandLevel(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
        val currentBands = _bands.value.toMutableList()
        val index = currentBands.indexOfFirst { it.id == band }
        if (index != -1) {
            currentBands[index] = currentBands[index].copy(level = level)
            _bands.value = currentBands
        }
    }

    fun usePreset(preset: Short) {
        equalizer?.usePreset(preset)
        loadBands()
    }

    private fun loadBands() {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands
        val newBands = mutableListOf<EqBand>()
        val range = eq.bandLevelRange
        for (i in 0 until numBands) {
            val band = i.toShort()
            newBands.add(
                EqBand(
                    id = band,
                    centerFreq = eq.getCenterFreq(band),
                    minLevel = range[0],
                    maxLevel = range[1],
                    level = eq.getBandLevel(band)
                )
            )
        }
        _bands.value = newBands
    }

    private fun loadPresets() {
        val eq = equalizer ?: return
        val numPresets = eq.numberOfPresets
        val newPresets = mutableListOf<String>()
        for (i in 0 until numPresets) {
            newPresets.add(eq.getPresetName(i.toShort()))
        }
        _presets.value = newPresets
    }
}

data class EqBand(
    val id: Short,
    val centerFreq: Int,
    val minLevel: Short,
    val maxLevel: Short,
    val level: Short
)
