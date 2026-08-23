package com.example.service.capture

import android.media.projection.MediaProjection
import android.util.Log
import com.example.model.AudioSourceType

/**
 * Módulo de captura, procesamiento DSP en C++ (Noise Gate, Ducking automático, Soft Limiter)
 * y orquestación de audio para juegos y voz.
 * Coordina de forma modular [InternalAudioWorker], [MicAudioWorker], [AudioDspMixer] y [AudioEncoderWorker].
 */
class AudioPipelineModule(
    private val audioSource: String,
    private val mediaProjection: MediaProjection?,
    private val muxerManager: MuxerManager,
    private val sampleRate: Int = 48000,
    private val isRecordingProvider: () -> Boolean,
    private val isPausedProvider: () -> Boolean,
    var onAmplitudeMeasured: ((Float) -> Unit)? = null
) {

    companion object {
        private const val TAG = "AudioPipelineModule"
    }

    private var internalAudioWorker: InternalAudioWorker? = null
    private var micAudioWorker: MicAudioWorker? = null
    private var dspMixer: AudioDspMixer? = null
    private var audioEncoderWorker: AudioEncoderWorker? = null

    private var currentGameGain: Float = 1.0f
    private var currentMicGain: Float = 1.25f
    private var currentNoiseGate: Boolean = true
    private var currentDucking: Boolean = true

    var hasAudio: Boolean = false
        private set

    val isMicMuted: Boolean
        get() = micAudioWorker?.isMicMuted ?: false

    fun setMicMuted(muted: Boolean) {
        micAudioWorker?.setMicMuted(muted)
    }

    fun toggleMicMute(): Boolean {
        return micAudioWorker?.toggleMicMute() ?: false
    }

    fun setGains(gameGain: Float, micGain: Float) {
        currentGameGain = gameGain
        currentMicGain = micGain
        dspMixer?.setGains(gameGain, micGain)
    }

    fun setAudioGains(gameGain: Float, micGain: Float) {
        setGains(gameGain, micGain)
    }

    fun setFilters(noiseGate: Boolean, ducking: Boolean, limiter: Boolean = true) {
        currentNoiseGate = noiseGate
        currentDucking = ducking
        dspMixer?.setFilters(noiseGate, ducking, limiter)
    }

    fun setAudioFilters(noiseGate: Boolean, ducking: Boolean, limiter: Boolean = true) {
        setFilters(noiseGate, ducking, limiter)
    }

    fun getAudioLevels(): FloatArray {
        return dspMixer?.getAudioLevels() ?: FloatArray(4)
    }

    fun initialize(
        gameGain: Float = 1.0f,
        micGain: Float = 1.25f,
        noiseGateEnabled: Boolean = true,
        duckingEnabled: Boolean = true
    ) {
        currentGameGain = gameGain
        currentMicGain = micGain
        currentNoiseGate = noiseGateEnabled
        currentDucking = duckingEnabled
        if (audioSource == AudioSourceType.NONE.name) {
            hasAudio = false
            return
        }

        val channelCount = 2

        // 1. Audio Interno del Juego (Android 10+)
        if (audioSource == AudioSourceType.INTERNAL_GAME.name || audioSource == AudioSourceType.INTERNAL_AND_MIC.name) {
            val intWorker = InternalAudioWorker(
                mediaProjection = mediaProjection,
                sampleRate = sampleRate,
                isRecordingProvider = isRecordingProvider,
                isPausedProvider = isPausedProvider
            )
            if (intWorker.initialize()) {
                internalAudioWorker = intWorker
            }
        }

        // 2. Micrófono / Voz
        if (audioSource == AudioSourceType.MIC.name ||
            audioSource == AudioSourceType.INTERNAL_AND_MIC.name ||
            audioSource == AudioSourceType.INTERNAL_GAME.name
        ) {
            val micWorker = MicAudioWorker(
                sampleRate = sampleRate,
                isRecordingProvider = isRecordingProvider,
                isPausedProvider = isPausedProvider,
                onAmplitudeMeasured = { amp ->
                    onAmplitudeMeasured?.invoke(amp)
                }
            )
            if (micWorker.initialize()) {
                micAudioWorker = micWorker
            }
        }

        // Configuración inicial de mute
        if (audioSource == AudioSourceType.INTERNAL_GAME.name) {
            micAudioWorker?.setMicMuted(true)
        } else {
            micAudioWorker?.setMicMuted(false)
        }

        val hasAnyRecord = (internalAudioWorker?.isInitialized == true) || (micAudioWorker?.isInitialized == true)

        if (hasAnyRecord) {
            val encoder = AudioEncoderWorker(
                sampleRate = sampleRate,
                channelCount = channelCount,
                muxerManager = muxerManager,
                isRecordingProvider = isRecordingProvider,
                isPausedProvider = isPausedProvider
            )

            if (encoder.initialize()) {
                audioEncoderWorker = encoder
                hasAudio = true

                // Inicializar DSP C++ con los parámetros configurados
                dspMixer = AudioDspMixer(sampleRate, channelCount).apply {
                    initializeDsp(
                        gameGain = currentGameGain,
                        micGain = currentMicGain,
                        noiseGateEnabled = currentNoiseGate,
                        duckingEnabled = currentDucking,
                        peakLimiterEnabled = true
                    )
                }
            } else {
                hasAudio = false
            }
        } else {
            Log.w(TAG, "Ningún AudioRecord disponible, continuando sin audio")
            hasAudio = false
        }
    }

    fun startWorker() {
        val mixer = dspMixer ?: return
        val encoderWorker = audioEncoderWorker ?: return

        internalAudioWorker?.start()
        micAudioWorker?.start()

        encoderWorker.start(
            internalWorker = internalAudioWorker,
            micWorker = micAudioWorker,
            mixer = mixer
        )
    }

    fun stopWorker() {
        internalAudioWorker?.stop()
        micAudioWorker?.stop()
        audioEncoderWorker?.stop()
    }

    fun release() {
        stopWorker()

        internalAudioWorker?.release()
        internalAudioWorker = null

        micAudioWorker?.release()
        micAudioWorker = null

        audioEncoderWorker?.release()
        audioEncoderWorker = null

        dspMixer?.release()
        dspMixer = null
    }
}
