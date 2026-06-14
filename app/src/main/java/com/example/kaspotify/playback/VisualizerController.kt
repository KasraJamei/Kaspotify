package com.example.kaspotify.playback

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns a [Visualizer] bound to the ExoPlayer audio session, same attach/release pattern as
 * [EqualizerController]. Captures waveform (PCM) frames for rendering an animated visualizer.
 */
@Singleton
class VisualizerController @Inject constructor() {

    private var visualizer: Visualizer? = null
    private var sessionId: Int = 0

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _waveform = MutableStateFlow<ByteArray>(ByteArray(0))
    val waveform: StateFlow<ByteArray> = _waveform.asStateFlow()

    /**
     * Called from the service when the audio session id is known. We only remember the id here:
     * actually constructing the [Visualizer] requires RECORD_AUDIO, which isn't granted until the
     * user enables the visualizer, so creation is deferred to [setEnabled].
     */
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        sessionId = audioSessionId
    }

    private fun createVisualizer() {
        val audioSessionId = sessionId
        if (audioSessionId == 0 || visualizer != null) return
        try {
            val viz = Visualizer(audioSessionId)
            viz.setCaptureSize(CAPTURE_SIZE)
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { _waveform.value = it.copyOf() }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) = Unit
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false
            )
            visualizer = viz
            _available.value = true
        } catch (t: Throwable) {
            visualizer = null
            _available.value = false
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            // Created lazily here: by now RECORD_AUDIO has been granted by the UI.
            createVisualizer()
        }
        val viz = visualizer ?: run {
            _enabled.value = false
            return
        }
        try {
            viz.enabled = enabled
            _enabled.value = viz.enabled
            if (!enabled) _waveform.value = ByteArray(0)
        } catch (_: Throwable) {
            _enabled.value = false
        }
    }

    fun release() {
        visualizer?.runCatching {
            enabled = false
            release()
        }
        visualizer = null
        _available.value = false
        _enabled.value = false
        _waveform.value = ByteArray(0)
    }

    companion object {
        private const val CAPTURE_SIZE = 256
    }
}
