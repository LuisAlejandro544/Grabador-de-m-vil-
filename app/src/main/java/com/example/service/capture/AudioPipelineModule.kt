package com.example.service.capture

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.model.AudioSourceType
import com.example.nativecore.NativeAudioDSPBridge
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Módulo de captura, procesamiento DSP en C++ (Noise Gate, Ducking automático, Soft Limiter)
 * y codificación AAC de audio para juegos y voz.
 */
class AudioPipelineModule(
    private val audioSource: String,
    private val mediaProjection: MediaProjection?,
    private val muxerManager: MuxerManager,
    private val isRecordingProvider: () -> Boolean,
    private val isPausedProvider: () -> Boolean
) {

    companion object {
        private const val TAG = "AudioPipelineModule"
        private const val AUDIO_SAMPLE_RATE = 48000
        private const val AUDIO_BIT_RATE = 192000
        private const val TIMEOUT_USEC = 10000L
    }

    private var internalAudioRecord: AudioRecord? = null
    private var micAudioRecord: AudioRecord? = null
    private var audioEncoder: MediaCodec? = null
    private var audioThread: Thread? = null

    private val isMicMutedInternal = AtomicBoolean(false)
    var hasAudio: Boolean = false
        private set

    val isMicMuted: Boolean get() = isMicMutedInternal.get()

    fun setMicMuted(muted: Boolean) {
        isMicMutedInternal.set(muted)
        Log.d(TAG, "Estado de captura de micrófono actualizado: ${if (muted) "SILENCIADO" else "ACTIVO"}")
    }

    fun toggleMicMute(): Boolean {
        val newState = !isMicMutedInternal.get()
        isMicMutedInternal.set(newState)
        Log.d(TAG, "Micrófono alternado: ${if (newState) "SILENCIADO" else "ACTIVO"}")
        return newState
    }

    @SuppressLint("MissingPermission")
    fun initialize() {
        if (audioSource == AudioSourceType.NONE.name) {
            hasAudio = false
            return
        }

        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val channelCount = 2
        val minBufferSize = maxOf(AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, channelConfig, audioEncoding), 4096)

        // 1. Audio Interno del Juego (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            (audioSource == AudioSourceType.INTERNAL_GAME.name || audioSource == AudioSourceType.INTERNAL_AND_MIC.name)
        ) {
            val proj = mediaProjection
            if (proj != null) {
                try {
                    val config = AudioPlaybackCaptureConfiguration.Builder(proj)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .build()

                    val format = AudioFormat.Builder()
                        .setEncoding(audioEncoding)
                        .setSampleRate(AUDIO_SAMPLE_RATE)
                        .setChannelMask(channelConfig)
                        .build()

                    val record = AudioRecord.Builder()
                        .setAudioPlaybackCaptureConfig(config)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(minBufferSize * 2)
                        .build()

                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        internalAudioRecord = record
                        Log.i(TAG, "AudioPlaybackCapture inicializado para audio interno del juego")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al inicializar captura de audio interno: ${e.message}", e)
                }
            }
        }

        // 2. Micrófono / Voz
        if (audioSource == AudioSourceType.MIC.name || audioSource == AudioSourceType.INTERNAL_AND_MIC.name) {
            try {
                val micRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_RATE,
                    channelConfig,
                    audioEncoding,
                    minBufferSize * 2
                )
                if (micRecord.state == AudioRecord.STATE_INITIALIZED) {
                    micAudioRecord = micRecord
                    isMicMutedInternal.set(false)
                    Log.i(TAG, "AudioRecord para MIC inicializado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al inicializar AudioRecord MIC: ${e.message}", e)
            }
        }

        // Fallback a MIC si el audio interno falló
        if (internalAudioRecord == null && micAudioRecord == null && audioSource != AudioSourceType.NONE.name) {
            try {
                val fallback = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_RATE,
                    channelConfig,
                    audioEncoding,
                    minBufferSize * 2
                )
                if (fallback.state == AudioRecord.STATE_INITIALIZED) {
                    micAudioRecord = fallback
                    isMicMutedInternal.set(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback de audio falló: ${e.message}")
            }
        }

        val hasAnyAudioRecord = (internalAudioRecord != null && internalAudioRecord?.state == AudioRecord.STATE_INITIALIZED) ||
                (micAudioRecord != null && micAudioRecord?.state == AudioRecord.STATE_INITIALIZED)

        if (hasAnyAudioRecord) {
            val aFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, AUDIO_SAMPLE_RATE, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            val aEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            aEncoder.configure(aFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            aEncoder.start()
            audioEncoder = aEncoder
            hasAudio = true

            // Inicializar DSP C++
            NativeAudioDSPBridge.initAudioDsp(AUDIO_SAMPLE_RATE, channelCount)
            NativeAudioDSPBridge.configureAudioDsp(
                noiseGateThresholdDb = -38.0f,
                duckingAttenuation = 0.35f,
                micGain = 1.25f,
                gameGain = 1.0f,
                noiseGateEnabled = true,
                duckingEnabled = true,
                peakLimiterEnabled = true
            )
        } else {
            Log.w(TAG, "Ningún AudioRecord disponible, continuando sin audio")
            hasAudio = false
        }
    }

    fun startWorker() {
        audioThread = Thread({
            val intRec = internalAudioRecord
            val micRec = micAudioRecord
            val encoder = audioEncoder ?: return@Thread
            val bufferInfo = MediaCodec.BufferInfo()

            try {
                intRec?.startRecording()
            } catch (e: Exception) {
                Log.w(TAG, "Error iniciando internalAudioRecord: ${e.message}")
            }
            try {
                micRec?.startRecording()
            } catch (e: Exception) {
                Log.w(TAG, "Error iniciando micAudioRecord: ${e.message}")
            }

            val bufferSize = 4096
            val internalBuf = ByteArray(bufferSize)
            val micBuf = ByteArray(bufferSize)
            val mixBuf = ByteArray(bufferSize)

            while (isRecordingProvider()) {
                if (isPausedProvider()) {
                    SystemClock.sleep(20)
                    continue
                }

                try {
                    val readInternal = intRec?.read(internalBuf, 0, bufferSize) ?: -1
                    val readMic = micRec?.read(micBuf, 0, bufferSize) ?: -1

                    var finalBytes: ByteArray? = null
                    var finalSize = 0

                    if (intRec != null && micRec != null) {
                        if (readInternal > 0) {
                            finalSize = readInternal
                            val isMicMuted = isMicMutedInternal.get()

                            val processedBytes = if (NativeAudioDSPBridge.isNativeReady()) {
                                val pcmCount = if (readMic > 0 && !isMicMuted) minOf(readInternal, readMic) else readInternal
                                NativeAudioDSPBridge.processAndMixAudio(
                                    internalAudio = internalBuf,
                                    micAudio = if (readMic > 0 && !isMicMuted) micBuf else null,
                                    outputMix = mixBuf,
                                    byteCount = pcmCount,
                                    isMicMuted = isMicMuted
                                )
                            } else 0

                            if (processedBytes > 0) {
                                if (readInternal > processedBytes) {
                                    System.arraycopy(internalBuf, processedBytes, mixBuf, processedBytes, readInternal - processedBytes)
                                }
                                finalBytes = mixBuf
                            } else if (!isMicMuted && readMic > 0) {
                                val sampleCount = minOf(readInternal, readMic) / 2
                                for (i in 0 until sampleCount) {
                                    val idx = i * 2
                                    val sInternal = (internalBuf[idx].toInt() and 0xFF) or (internalBuf[idx + 1].toInt() shl 8)
                                    val sMic = (micBuf[idx].toInt() and 0xFF) or (micBuf[idx + 1].toInt() shl 8)
                                    val mixed = (sInternal.toShort() + sMic.toShort()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                    mixBuf[idx] = (mixed and 0xFF).toByte()
                                    mixBuf[idx + 1] = ((mixed shr 8) and 0xFF).toByte()
                                }
                                if (readInternal > readMic) {
                                    System.arraycopy(internalBuf, readMic, mixBuf, readMic, readInternal - readMic)
                                }
                                finalBytes = mixBuf
                            } else {
                                finalBytes = internalBuf
                            }
                        }
                    } else if (intRec != null) {
                        if (readInternal > 0) {
                            finalSize = readInternal
                            finalBytes = internalBuf
                        }
                    } else if (micRec != null) {
                        if (readMic > 0) {
                            finalSize = readMic
                            if (isMicMutedInternal.get()) {
                                micBuf.fill(0)
                                finalBytes = micBuf
                            } else {
                                val processed = if (NativeAudioDSPBridge.isNativeReady()) {
                                    NativeAudioDSPBridge.processAndMixAudio(
                                        internalAudio = null,
                                        micAudio = micBuf,
                                        outputMix = mixBuf,
                                        byteCount = readMic,
                                        isMicMuted = false
                                    )
                                } else 0

                                finalBytes = if (processed > 0) mixBuf else micBuf
                            }
                        }
                    }

                    if (finalBytes != null && finalSize > 0 && !isPausedProvider()) {
                        val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                            inputBuffer?.clear()
                            inputBuffer?.put(finalBytes, 0, finalSize)

                            val pts = System.nanoTime() / 1000
                            encoder.queueInputBuffer(inputBufferIndex, 0, finalSize, pts, 0)
                        }
                    }

                    // Drenar encoder de audio
                    while (true) {
                        val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            val newFormat = encoder.outputFormat
                            muxerManager.addAudioTrack(newFormat)
                        } else if (outputBufferIndex >= 0) {
                            val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                            if (outputBuffer != null && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                if (!isPausedProvider()) {
                                    muxerManager.writeAudioSample(outputBuffer, bufferInfo)
                                }
                            }
                            encoder.releaseOutputBuffer(outputBufferIndex, false)
                        } else {
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en bucle de audio: ${e.message}")
                    break
                }
            }

            try {
                intRec?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error deteniendo internalAudioRecord: ${e.message}")
            }
            try {
                micRec?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error deteniendo micAudioRecord: ${e.message}")
            }
        }, "OBS_AudioWorker").apply { start() }
    }

    fun stopWorker() {
        try {
            audioThread?.join(500)
        } catch (_: Exception) {}
        audioThread = null
    }

    fun release() {
        try {
            internalAudioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando internalAudioRecord: ${e.message}")
        }
        internalAudioRecord = null

        try {
            micAudioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando micAudioRecord: ${e.message}")
        }
        micAudioRecord = null

        try {
            audioEncoder?.stop()
            audioEncoder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando audioEncoder: ${e.message}")
        }
        audioEncoder = null

        NativeAudioDSPBridge.releaseAudioDsp()
    }
}
