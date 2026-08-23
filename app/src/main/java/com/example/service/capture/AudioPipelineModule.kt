package com.example.service.capture

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.SystemClock
import android.util.Log
import com.example.model.AudioSourceType

/**
 * Módulo modular de captura, procesamiento DSP en C++ (Noise Gate, Ducking automático, Soft Limiter)
 * y codificación AAC de audio para juegos y voz.
 * Orquesta concurrentemente [InternalAudioWorker], [MicAudioWorker] y [AudioDspMixer].
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
        private const val AUDIO_BIT_RATE = 192000
        private const val TIMEOUT_USEC = 10000L
    }

    private var internalAudioWorker: InternalAudioWorker? = null
    private var micAudioWorker: MicAudioWorker? = null
    private var dspMixer: AudioDspMixer? = null
    private var audioEncoder: MediaCodec? = null

    private var currentGameGain: Float = 1.0f
    private var currentMicGain: Float = 1.25f
    private var currentNoiseGate: Boolean = true
    private var currentDucking: Boolean = true

    private var encoderWorkerThread: Thread? = null

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
            val aFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            val aEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            aEncoder.configure(aFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            aEncoder.start()
            audioEncoder = aEncoder
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
            Log.w(TAG, "Ningún AudioRecord disponible, continuando sin audio")
            hasAudio = false
        }
    }

    fun startWorker() {
        val encoder = audioEncoder ?: return
        val mixer = dspMixer ?: return

        internalAudioWorker?.start()
        micAudioWorker?.start()

        // Hilo consumidor y codificador MediaCodec AAC
        encoderWorkerThread = Thread({
            val bufferInfo = MediaCodec.BufferInfo()

            while (isRecordingProvider()) {
                try {
                    val isPaused = isPausedProvider()
                    val internalData = internalAudioWorker?.audioQueue?.poll()
                    val micData = if (micAudioWorker?.isMicMuted == false) {
                        micAudioWorker?.audioQueue?.poll()
                    } else null

                    if (internalData == null && micData == null) {
                        // Si no hay datos inmediatos, drenar buffers de salida pendientes del codificador
                        drainAudioEncoder(encoder, bufferInfo, isPaused)
                        SystemClock.sleep(5)
                        continue
                    }

                    var finalBytes: ByteArray? = null
                    var finalSize = 0

                    if (internalData != null && micData != null) {
                        val (mixed, size) = mixer.mixDualAudio(internalData, micData)
                        finalBytes = mixed
                        finalSize = size
                    } else if (internalData != null) {
                        finalBytes = internalData
                        finalSize = internalData.size
                    } else if (micData != null) {
                        val (proc, size) = mixer.processSingleMicAudio(micData)
                        finalBytes = proc
                        finalSize = size
                    }

                    if (finalBytes != null && finalSize > 0 && !isPaused) {
                        val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                            inputBuffer?.clear()
                            inputBuffer?.put(finalBytes, 0, finalSize)

                            val pts = System.nanoTime() / 1000
                            encoder.queueInputBuffer(inputBufferIndex, 0, finalSize, pts, 0)
                        }
                    }

                    // Drenar encoder de audio continuamente
                    drainAudioEncoder(encoder, bufferInfo, isPaused)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en bucle de audio: ${e.message}")
                    break
                }
            }
        }, "OBS_AudioEncoderWorker").apply { start() }
    }

    private fun drainAudioEncoder(encoder: MediaCodec, bufferInfo: MediaCodec.BufferInfo, isPaused: Boolean) {
        while (true) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                muxerManager.addAudioTrack(newFormat)
            } else if (outputBufferIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if (!isPaused) {
                        muxerManager.writeAudioSample(outputBuffer, bufferInfo)
                    }
                }
                encoder.releaseOutputBuffer(outputBufferIndex, false)
            } else {
                break
            }
        }
    }

    fun stopWorker() {
        internalAudioWorker?.stop()
        micAudioWorker?.stop()

        try {
            encoderWorkerThread?.join(500)
        } catch (_: Exception) {}
        encoderWorkerThread = null
    }

    fun release() {
        stopWorker()

        internalAudioWorker?.release()
        internalAudioWorker = null

        micAudioWorker?.release()
        micAudioWorker = null

        try {
            audioEncoder?.stop()
            audioEncoder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando audioEncoder: ${e.message}")
        }
        audioEncoder = null

        dspMixer?.release()
        dspMixer = null
    }
}
