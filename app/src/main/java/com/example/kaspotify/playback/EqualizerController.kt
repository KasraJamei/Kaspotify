package com.example.kaspotify.playback

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqualizerBand(
    val index: Int,
    val centerFreqMilliHz: Int,
    val level: Short
)

/**
 * Owns an [Equalizer] bound to the ExoPlayer audio session. The session id is supplied by
 * [PlaybackService] once the player is created; the UI reads/controls the bands through this
 * singleton via the ViewModel.
 */
@Singleton
class EqualizerController @Inject constructor() {

    private var equalizer: Equalizer? = null

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** [min, max] band level in millibels. */
    private val _levelRange = MutableStateFlow(-1500 to 1500)
    val levelRange: StateFlow<Pair<Int, Int>> = _levelRange.asStateFlow()

    private val _bands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val bands: StateFlow<List<EqualizerBand>> = _bands.asStateFlow()

    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets.asStateFlow()

    private val _currentPreset = MutableStateFlow(-1)
    val currentPreset: StateFlow<Int> = _currentPreset.asStateFlow()

    /** Called from the service when the audio session id is known. Safe to call repeatedly. */
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        try {
            val eq = Equalizer(EFFECT_PRIORITY, audioSessionId)
            equalizer = eq
            val range = eq.bandLevelRange
            _levelRange.value = range[0].toInt() to range[1].toInt()
            _presets.value = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
            _enabled.value = eq.enabled
            refreshBands()
            _currentPreset.value = eq.currentPreset.toInt()
            _available.value = true
        } catch (t: Throwable) {
            // Equalizer is unavailable on some devices/emulators.
            equalizer = null
            _available.value = false
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.let {
            it.enabled = enabled
            _enabled.value = it.enabled
        }
    }

    fun setBandLevel(bandIndex: Int, levelMilliBel: Short) {
        val eq = equalizer ?: return
        try {
            ensureEnabled(eq)
            eq.setBandLevel(bandIndex.toShort(), levelMilliBel)
            _currentPreset.value = -1 // custom
            refreshBands()
        } catch (_: Throwable) {
        }
    }

    fun usePreset(presetIndex: Int) {
        val eq = equalizer ?: return
        try {
            ensureEnabled(eq)
            eq.usePreset(presetIndex.toShort())
            _currentPreset.value = presetIndex
            refreshBands()
        } catch (_: Throwable) {
        }
    }

    /** Adjusting bands/presets only has an audible effect once the effect is enabled. */
    private fun ensureEnabled(eq: Equalizer) {
        if (!eq.enabled) {
            eq.enabled = true
            _enabled.value = true
        }
    }

    fun release() {
        equalizer?.runCatching { release() }
        equalizer = null
        _available.value = false
    }

    private fun refreshBands() {
        val eq = equalizer ?: return
        _bands.value = (0 until eq.numberOfBands).map { i ->
            val band = i.toShort()
            EqualizerBand(
                index = i,
                centerFreqMilliHz = eq.getCenterFreq(band),
                level = eq.getBandLevel(band)
            )
        }
    }

    companion object {
        private const val EFFECT_PRIORITY = 0
    }
}
