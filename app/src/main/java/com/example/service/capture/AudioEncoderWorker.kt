package com.example.service.capture

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Process
import android.os.SystemClock
import android.util.Log

/**
 * Worker especializado en la codificación por hardware AAC y sincronización de búferes PCM de audio.
 * Desacopla la lógica de temporización, cola de codificación y drenado hacia [MuxerManager].
 * Diseñado con arquitectura Zero-Allocation con búfer circular pre-asignado y reciclaje de objetos
 * para no interferir en el rendimiento de los juegos pesados.
 */
class AudioEncoderWorker(
    private val sampleRate: Int,
    private val channelCount: Int,
    private val muxerManager: MuxerManager,
    private val isRecordingProvider: () -> Boolean,
    private val isPausedProvider: () -> Boolean,
    private var avSyncOffsetMs: Int = 0
) {

    companion object {
        private const val TAG = "AudioEncoderWorker"
        private const val AUDIO_BIT_RATE = 192000
        private const val TIMEOUT_USEC = 10000L
        private const val RING_BUFFER_SIZE = 32768
    }

    private var audioEncoder: MediaCodec? = null
    private var encoderWorkerThread: Thread? = null
    private var lastQueuedPtsUs: Long = -1L

    val isInitialized: Boolean
        get() = audioEncoder != null

    fun setAvSyncOffsetMs(offsetMs: Int) {
        this.avSyncOffsetMs = offsetMs
    }

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
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            } catch (_: Exception) {}

            val bufferInfo = MediaCodec.BufferInfo()

            // Búferes pre-asignados reutilizables para evitar instanciación dinámica en el bucle
            val micRingBuffer = ByteArray(RING_BUFFER_SIZE)
            var micRingCount = 0

            val micHoldingBuffer = ByteArray(AudioFrameBuffer.DEFAULT_CAPACITY)
            val silentHoldingBuffer = ByteArray(AudioFrameBuffer.DEFAULT_CAPACITY)

            var lastInternalDataTime = SystemClock.uptimeMillis()

            while (isRecordingProvider()) {
                try {
                    val isPaused = isPausedProvider()

                    if (isDualAudioMode) {
                        // --- MODO DUAL SINCRONIZADO (AUDIO DEL JUEGO + MICRÓFONO) ---
                        if (micWorker != null && !micWorker.isMicMuted) {
                            while (true) {
                                val micFrame = micWorker.audioQueue.poll() ?: break
                                val bytesToAdd = micFrame.size
                                val spaceLeft = RING_BUFFER_SIZE - micRingCount
                                val copySize = minOf(bytesToAdd, spaceLeft)
                                if (copySize > 0) {
                                    System.arraycopy(micFrame.data, 0, micRingBuffer, micRingCount, copySize)
                                    micRingCount += copySize
                                }
                                micWorker.recycleBuffer(micFrame)
                            }
                        }

                        val internalFrame = internalWorker?.audioQueue?.poll()
                        var finalBytes: ByteArray? = null
                        var finalSize = 0

                        if (internalFrame != null) {
                            lastInternalDataTime = SystemClock.uptimeMillis()
                            val neededBytes = minOf(internalFrame.size, micHoldingBuffer.size)

                            if (micWorker != null && !micWorker.isMicMuted && micRingCount > 0) {
                                val bytesFromMic = minOf(neededBytes, micRingCount)
                                System.arraycopy(micRingBuffer, 0, micHoldingBuffer, 0, bytesFromMic)
                                if (bytesFromMic < neededBytes) {
                                    java.util.Arrays.fill(micHoldingBuffer, bytesFromMic, neededBytes, 0.toByte())
                                }

                                val remaining = micRingCount - bytesFromMic
                                if (remaining > 0) {
                                    System.arraycopy(micRingBuffer, bytesFromMic, micRingBuffer, 0, remaining)
                                }
                                micRingCount = remaining
                            } else {
                                java.util.Arrays.fill(micHoldingBuffer, 0, neededBytes, 0.toByte())
                            }

                            val (mixed, size) = mixer.mixDualAudio(
                                internalData = internalFrame.data,
                                internalSize = internalFrame.size,
                                micData = micHoldingBuffer,
                                micSize = neededBytes
                            )
                            finalBytes = mixed
                            finalSize = size

                            internalWorker.recycleBuffer(internalFrame)
                        } else {
                            // Fallback si la fuente del juego tarda más de 50ms
                            val elapsedSinceInternal = SystemClock.uptimeMillis() - lastInternalDataTime
                            if (elapsedSinceInternal > 50L && micRingCount >= 4096) {
                                val chunkSize = 4096
                                System.arraycopy(micRingBuffer, 0, micHoldingBuffer, 0, chunkSize)
                                val remaining = micRingCount - chunkSize
                                if (remaining > 0) {
                                    System.arraycopy(micRingBuffer, chunkSize, micRingBuffer, 0, remaining)
                                }
                                micRingCount = remaining

                                val (mixed, size) = mixer.mixDualAudio(
                                    internalData = silentHoldingBuffer,
                                    internalSize = chunkSize,
                                    micData = micHoldingBuffer,
                                    micSize = chunkSize
                                )
                                finalBytes = mixed
                                finalSize = size
                            }
                        }

                        if (finalBytes != null && finalSize > 0 && !isPaused) {
                            feedEncoder(encoder, finalBytes, finalSize)
                        }

                        drainAudioEncoder(encoder, bufferInfo, isPaused)

                        if (internalFrame == null) {
                            SystemClock.sleep(4)
                        }
                    } else {
                        // --- MODO FUENTE ÚNICA (SOLO JUEGO O SOLO MICRÓFONO) ---
                        val internalFrame = internalWorker?.audioQueue?.poll()
                        val micFrame = if (micWorker?.isMicMuted == false) {
                            micWorker.audioQueue.poll()
                        } else null

                        if (internalFrame == null && micFrame == null) {
                            drainAudioEncoder(encoder, bufferInfo, isPaused)
                            SystemClock.sleep(4)
                            continue
                        }

                        var finalBytes: ByteArray? = null
                        var finalSize = 0

                        if (internalFrame != null) {
                            finalBytes = internalFrame.data
                            finalSize = internalFrame.size
                            if (!isPaused && finalSize > 0) {
                                feedEncoder(encoder, finalBytes, finalSize)
                            }
                            internalWorker.recycleBuffer(internalFrame)
                        } else if (micFrame != null) {
                            val (proc, size) = mixer.processSingleMicAudio(micFrame.data, micFrame.size)
                            finalBytes = proc
                            finalSize = size
                            if (!isPaused && finalSize > 0) {
                                feedEncoder(encoder, finalBytes, finalSize)
                            }
                            micWorker?.recycleBuffer(micFrame)
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

            val nowUs = System.nanoTime() / 1000L
            val bytesPerFrame = channelCount * 2L // 16-bit PCM = 2 bytes por muestra por canal
            val bufferDurationUs = if (bytesPerFrame > 0 && sampleRate > 0) {
                (size * 1_000_000L) / (bytesPerFrame * sampleRate)
            } else {
                0L
            }

            val pts = if (lastQueuedPtsUs == -1L) {
                nowUs
            } else {
                val continuousPts = lastQueuedPtsUs + bufferDurationUs
                // Si la desviación es menor a 80ms, mantener continuidad estricta por conteo de muestras PCM
                if (kotlin.math.abs(nowUs - continuousPts) < 80_000L) {
                    continuousPts
                } else {
                    nowUs
                }
            }
            lastQueuedPtsUs = pts
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
