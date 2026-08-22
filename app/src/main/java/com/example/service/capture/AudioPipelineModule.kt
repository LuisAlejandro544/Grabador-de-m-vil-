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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Módulo de captura, procesamiento DSP en C++ (Noise Gate, Ducking automático, Soft Limiter)
 * y codificación AAC de audio para juegos y voz.
 * Implementa lectura concurrente y desacoplada para audio interno y micrófono, permitiendo
 * alternar en tiempo real ("Voz ON" vs "Solo Juego") sin bloqueos ni pérdida de sincronización.
 */
class AudioPipelineModule(
    private val audioSource: String,
    private val mediaProjection: MediaProjection?,
    private val muxerManager: MuxerManager,
    private val sampleRate: Int = 48000,
    private val isRecordingProvider: () -> Boolean,
    private val isPausedProvider: () -> Boolean
) {

    companion object {
        private const val TAG = "AudioPipelineModule"
        private const val AUDIO_BIT_RATE = 192000
        private const val TIMEOUT_USEC = 10000L
        private const val BUFFER_SIZE = 4096
        private const val MAX_QUEUE_CAPACITY = 20
    }

    private var internalAudioRecord: AudioRecord? = null
    private var micAudioRecord: AudioRecord? = null
    private var audioEncoder: MediaCodec? = null

    private var internalAudioThread: Thread? = null
    private var micAudioThread: Thread? = null
    private var encoderWorkerThread: Thread? = null

    private val internalAudioQueue = ConcurrentLinkedQueue<ByteArray>()
    private val micAudioQueue = ConcurrentLinkedQueue<ByteArray>()

    private val isMicMutedInternal = AtomicBoolean(false)
    var hasAudio: Boolean = false
        private set

    val isMicMuted: Boolean get() = isMicMutedInternal.get()

    fun setMicMuted(muted: Boolean) {
        isMicMutedInternal.set(muted)
        if (muted) {
            micAudioQueue.clear()
        }
        Log.d(TAG, "Estado de captura de micrófono actualizado: ${if (muted) "SILENCIADO (Solo Juego)" else "ACTIVO (Juego + Voz)"}")
    }

    fun toggleMicMute(): Boolean {
        val newState = !isMicMutedInternal.get()
        setMicMuted(newState)
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
        val minBufferSize = maxOf(AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding), BUFFER_SIZE * 2)

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
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()

                    val record = AudioRecord.Builder()
                        .setAudioPlaybackCaptureConfig(config)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(minBufferSize * 2)
                        .build()

                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        internalAudioRecord = record
                        Log.i(TAG, "AudioPlaybackCapture inicializado para audio interno del juego a ${sampleRate}Hz")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al inicializar captura de audio interno: ${e.message}", e)
                }
            }
        }

        // 2. Micrófono / Voz (Inicializar siempre si audio != NONE para permitir conmutación dinámica en caliente)
        if (audioSource == AudioSourceType.MIC.name ||
            audioSource == AudioSourceType.INTERNAL_AND_MIC.name ||
            audioSource == AudioSourceType.INTERNAL_GAME.name
        ) {
            try {
                val micRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioEncoding,
                    minBufferSize * 2
                )
                if (micRecord.state == AudioRecord.STATE_INITIALIZED) {
                    micAudioRecord = micRecord
                    Log.i(TAG, "AudioRecord para MIC inicializado correctamente a ${sampleRate}Hz")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al inicializar AudioRecord MIC: ${e.message}", e)
            }
        }

        // Configurar estado inicial de silenciamiento según la fuente seleccionada
        if (audioSource == AudioSourceType.INTERNAL_GAME.name) {
            isMicMutedInternal.set(true)
        } else {
            isMicMutedInternal.set(false)
        }

        // Fallback a MIC si el audio interno falló
        if (internalAudioRecord == null && micAudioRecord == null && audioSource != AudioSourceType.NONE.name) {
            try {
                val fallback = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
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

            // Inicializar DSP C++
            NativeAudioDSPBridge.initAudioDsp(sampleRate, channelCount)
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
        val intRec = internalAudioRecord
        val micRec = micAudioRecord
        val encoder = audioEncoder ?: return

        internalAudioQueue.clear()
        micAudioQueue.clear()

        // 1. Hilo productor de Audio Interno (Juego / App)
        if (intRec != null) {
            internalAudioThread = Thread({
                try {
                    intRec.startRecording()
                } catch (e: Exception) {
                    Log.w(TAG, "Error iniciando internalAudioRecord: ${e.message}")
                }

                val buf = ByteArray(BUFFER_SIZE)
                while (isRecordingProvider()) {
                    if (isPausedProvider()) {
                        SystemClock.sleep(20)
                        continue
                    }
                    try {
                        val readBytes = intRec.read(buf, 0, BUFFER_SIZE)
                        if (readBytes > 0) {
                            val data = buf.copyOf(readBytes)
                            if (internalAudioQueue.size >= MAX_QUEUE_CAPACITY) {
                                internalAudioQueue.poll() // Dropear buffer más antiguo para evitar lag
                            }
                            internalAudioQueue.offer(data)
                        } else {
                            SystemClock.sleep(5)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Excepción leyendo internal audio: ${e.message}")
                        break
                    }
                }

                try {
                    intRec.stop()
                } catch (_: Exception) {}
            }, "OBS_InternalAudioReader").apply { start() }
        }

        // 2. Hilo productor de Micrófono (Voz)
        if (micRec != null) {
            micAudioThread = Thread({
                try {
                    micRec.startRecording()
                } catch (e: Exception) {
                    Log.w(TAG, "Error iniciando micAudioRecord: ${e.message}")
                }

                val buf = ByteArray(BUFFER_SIZE)
                while (isRecordingProvider()) {
                    if (isPausedProvider()) {
                        SystemClock.sleep(20)
                        continue
                    }
                    try {
                        val readBytes = micRec.read(buf, 0, BUFFER_SIZE)
                        if (readBytes > 0) {
                            if (!isMicMutedInternal.get()) {
                                val data = buf.copyOf(readBytes)
                                if (micAudioQueue.size >= MAX_QUEUE_CAPACITY) {
                                    micAudioQueue.poll() // Dropear buffer más antiguo
                                }
                                micAudioQueue.offer(data)
                            }
                        } else {
                            SystemClock.sleep(5)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Excepción leyendo mic audio: ${e.message}")
                        break
                    }
                }

                try {
                    micRec.stop()
                } catch (_: Exception) {}
            }, "OBS_MicAudioReader").apply { start() }
        }

        // 3. Hilo consumidor y mezclador DSP hacia el MediaCodec AAC
        encoderWorkerThread = Thread({
            val bufferInfo = MediaCodec.BufferInfo()
            val mixBuf = ByteArray(BUFFER_SIZE)

            while (isRecordingProvider()) {
                if (isPausedProvider()) {
                    SystemClock.sleep(20)
                    internalAudioQueue.clear()
                    micAudioQueue.clear()
                    continue
                }

                try {
                    val isMicMuted = isMicMutedInternal.get()
                    val internalData = internalAudioQueue.poll()
                    val micData = if (!isMicMuted) micAudioQueue.poll() else null

                    if (internalData == null && micData == null) {
                        SystemClock.sleep(5)
                        continue
                    }

                    var finalBytes: ByteArray? = null
                    var finalSize = 0

                    if (internalData != null && micData != null) {
                        val pcmCount = minOf(internalData.size, micData.size)
                        finalSize = maxOf(internalData.size, micData.size)

                        val processedBytes = if (NativeAudioDSPBridge.isNativeReady()) {
                            NativeAudioDSPBridge.processAndMixAudio(
                                internalAudio = internalData,
                                micAudio = micData,
                                outputMix = mixBuf,
                                byteCount = pcmCount,
                                isMicMuted = false
                            )
                        } else 0

                        if (processedBytes > 0) {
                            if (internalData.size > processedBytes) {
                                System.arraycopy(internalData, processedBytes, mixBuf, processedBytes, internalData.size - processedBytes)
                            }
                            finalBytes = mixBuf
                        } else {
                            // Mezclador PCM 16-bit con soft clipping
                            val sampleCount = pcmCount / 2
                            for (i in 0 until sampleCount) {
                                val idx = i * 2
                                val sInternal = (internalData[idx].toInt() and 0xFF) or (internalData[idx + 1].toInt() shl 8)
                                val sMic = (micData[idx].toInt() and 0xFF) or (micData[idx + 1].toInt() shl 8)
                                val mixed = (sInternal.toShort() + sMic.toShort()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                mixBuf[idx] = (mixed and 0xFF).toByte()
                                mixBuf[idx + 1] = ((mixed shr 8) and 0xFF).toByte()
                            }
                            if (internalData.size > micData.size) {
                                System.arraycopy(internalData, pcmCount, mixBuf, pcmCount, internalData.size - pcmCount)
                            } else if (micData.size > internalData.size) {
                                System.arraycopy(micData, pcmCount, mixBuf, pcmCount, micData.size - pcmCount)
                            }
                            finalBytes = mixBuf
                        }
                    } else if (internalData != null) {
                        finalBytes = internalData
                        finalSize = internalData.size
                    } else if (micData != null) {
                        finalSize = micData.size
                        val processed = if (NativeAudioDSPBridge.isNativeReady()) {
                            NativeAudioDSPBridge.processAndMixAudio(
                                internalAudio = null,
                                micAudio = micData,
                                outputMix = mixBuf,
                                byteCount = finalSize,
                                isMicMuted = false
                            )
                        } else 0
                        finalBytes = if (processed > 0) mixBuf else micData
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
        }, "OBS_AudioEncoderWorker").apply { start() }
    }

    fun stopWorker() {
        try {
            internalAudioThread?.join(300)
        } catch (_: Exception) {}
        internalAudioThread = null

        try {
            micAudioThread?.join(300)
        } catch (_: Exception) {}
        micAudioThread = null

        try {
            encoderWorkerThread?.join(500)
        } catch (_: Exception) {}
        encoderWorkerThread = null
    }

    fun release() {
        stopWorker()

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

