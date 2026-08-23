package com.example.service.capture

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.SystemClock
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Worker especializado en la codificación por hardware AAC y sincronización de búferes PCM de audio.
 * Desacopla la lógica de temporización, cola de codificación y drenado hacia [MuxerManager].
 */
class AudioEncoderWorker(
    private val sampleRate: Int,
    private val channelCount: Int,
    private val muxerManager: MuxerManager,
    private val isRecordingProvider: () -> Boolean,
    private val isPausedProvider: () -> Boolean
) {

    companion object {
        private const val TAG = "AudioEncoderWorker"
        private const val AUDIO_BIT_RATE = 192000
        private const val TIMEOUT_USEC = 10000L
    }

    private var audioEncoder: MediaCodec? = null
    private var encoderWorkerThread: Thread? = null

    val isInitialized: Boolean
        get() = audioEncoder != null

    fun initialize(): Boolean {
        return try {
            val aFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            val aEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            aEncoder.configure(aFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            aEncoder.start()
            audioEncoder = aEncoder
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando encoder AAC: ${e.message}")
            audioEncoder = null
            false
        }
    }

    fun start(
        internalWorker: InternalAudioWorker?,
        micWorker: MicAudioWorker?,
        mixer: AudioDspMixer
    ) {
        val encoder = audioEncoder ?: return
        val isDualAudioMode = internalWorker != null && micWorker != null

        encoderWorkerThread = Thread({
            val bufferInfo = MediaCodec.BufferInfo()
            val micAccumulator = ByteArrayOutputStream(16384)
            var lastInternalDataTime = SystemClock.uptimeMillis()

            while (isRecordingProvider()) {
                try {
                    val isPaused = isPausedProvider()

                    if (isDualAudioMode) {
                        // --- MODO DUAL SINCRONIZADO (AUDIO DEL JUEGO + MICRÓFONO) ---
                        if (micWorker != null && !micWorker.isMicMuted) {
                            while (true) {
                                val chunk = micWorker.audioQueue.poll() ?: break
                                micAccumulator.write(chunk)
                            }
                        }

                        val internalData = internalWorker?.audioQueue?.poll()
                        var finalBytes: ByteArray? = null
                        var finalSize = 0

                        if (internalData != null) {
                            lastInternalDataTime = SystemClock.uptimeMillis()
                            val neededBytes = internalData.size
                            val micData = ByteArray(neededBytes)

                            if (micWorker != null && !micWorker.isMicMuted && micAccumulator.size() > 0) {
                                val currentMicBytes = micAccumulator.toByteArray()
                                val bytesToCopy = minOf(neededBytes, currentMicBytes.size)
                                System.arraycopy(currentMicBytes, 0, micData, 0, bytesToCopy)

                                micAccumulator.reset()
                                if (currentMicBytes.size > bytesToCopy) {
                                    val remaining = currentMicBytes.size - bytesToCopy
                                    val safeRemaining = minOf(remaining, 16384)
                                    val startOffset = currentMicBytes.size - safeRemaining
                                    micAccumulator.write(currentMicBytes, startOffset, safeRemaining)
                                }
                            }

                            val (mixed, size) = mixer.mixDualAudio(internalData, micData)
                            finalBytes = mixed
                            finalSize = size
                        } else {
                            // Fallback si la fuente del juego tarda más de 50ms (ej. pantalla de carga o silencio completo)
                            val elapsedSinceInternal = SystemClock.uptimeMillis() - lastInternalDataTime
                            if (elapsedSinceInternal > 50L && micAccumulator.size() >= 4096) {
                                val currentMicBytes = micAccumulator.toByteArray()
                                val chunkSize = 4096
                                val micChunk = currentMicBytes.copyOf(chunkSize)
                                micAccumulator.reset()
                                if (currentMicBytes.size > chunkSize) {
                                    micAccumulator.write(currentMicBytes, chunkSize, currentMicBytes.size - chunkSize)
                                }

                                val silentInternal = ByteArray(chunkSize)
                                val (mixed, size) = mixer.mixDualAudio(silentInternal, micChunk)
                                finalBytes = mixed
                                finalSize = size
                            }
                        }

                        if (finalBytes != null && finalSize > 0 && !isPaused) {
                            feedEncoder(encoder, finalBytes, finalSize)
                        }

                        drainAudioEncoder(encoder, bufferInfo, isPaused)

                        if (internalData == null) {
                            SystemClock.sleep(4)
                        }
                    } else {
                        // --- MODO FUENTE ÚNICA (SOLO JUEGO O SOLO MICRÓFONO) ---
                        val internalData = internalWorker?.audioQueue?.poll()
                        val micData = if (micWorker?.isMicMuted == false) {
                            micWorker.audioQueue.poll()
                        } else null

                        if (internalData == null && micData == null) {
                            drainAudioEncoder(encoder, bufferInfo, isPaused)
                            SystemClock.sleep(5)
                            continue
                        }

                        var finalBytes: ByteArray? = null
                        var finalSize = 0

                        if (internalData != null) {
                            finalBytes = internalData
                            finalSize = internalData.size
                        } else if (micData != null) {
                            val (proc, size) = mixer.processSingleMicAudio(micData)
                            finalBytes = proc
                            finalSize = size
                        }

                        if (finalBytes != null && finalSize > 0 && !isPaused) {
                            feedEncoder(encoder, finalBytes, finalSize)
                        }

                        drainAudioEncoder(encoder, bufferInfo, isPaused)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en bucle de audio: ${e.message}")
                    break
                }
            }
        }, "OBS_AudioEncoderWorker").apply { start() }
    }

    private fun feedEncoder(encoder: MediaCodec, bytes: ByteArray, size: Int) {
        val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
        if (inputBufferIndex >= 0) {
            val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(bytes, 0, size)

            val pts = System.nanoTime() / 1000
            encoder.queueInputBuffer(inputBufferIndex, 0, size, pts, 0)
        }
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

    fun stop() {
        try {
            encoderWorkerThread?.join(500)
        } catch (_: Exception) {}
        encoderWorkerThread = null
    }

    fun release() {
        stop()
        try {
            audioEncoder?.stop()
            audioEncoder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando audioEncoder: ${e.message}")
        }
        audioEncoder = null
    }
}
