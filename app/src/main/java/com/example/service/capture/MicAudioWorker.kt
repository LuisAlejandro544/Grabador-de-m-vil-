package com.example.service.capture

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Worker desacoplado responsable de capturar la voz y el micrófono del usuario.
 * Soporta control de silenciamiento dinámico ("Mute/Unmute") en caliente sin alterar la
 * sincronización de timestamps del codificador.
 */
class MicAudioWorker(
    private val sampleRate: Int,
    private val isRecordingProvider: () -> Boolean,
    private val isPausedProvider: () -> Boolean,
    var onAmplitudeMeasured: ((Float) -> Unit)? = null
) {
    companion object {
        private const val TAG = "MicAudioWorker"
        private const val BUFFER_SIZE = 4096
        private const val MAX_QUEUE_CAPACITY = 20
    }

    private var audioRecord: AudioRecord? = null
    private var workerThread: Thread? = null
    val audioQueue = ConcurrentLinkedQueue<ByteArray>()

    private val isMicMutedInternal = AtomicBoolean(false)

    val isInitialized: Boolean
        get() = audioRecord != null && audioRecord?.state == AudioRecord.STATE_INITIALIZED

    val isMicMuted: Boolean
        get() = isMicMutedInternal.get()

    fun setMicMuted(muted: Boolean) {
        isMicMutedInternal.set(muted)
        if (muted) {
            audioQueue.clear()
            onAmplitudeMeasured?.invoke(0f)
        }
        Log.d(TAG, "Micrófono: ${if (muted) "SILENCIADO" else "ACTIVO"}")
    }

    fun toggleMicMute(): Boolean {
        val newState = !isMicMutedInternal.get()
        setMicMuted(newState)
        return newState
    }

    @SuppressLint("MissingPermission")
    fun initialize(): Boolean {
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = maxOf(
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding),
            BUFFER_SIZE * 2
        )

        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioEncoding,
                minBufferSize * 2
            )
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord = record
                Log.i(TAG, "AudioRecord MIC inicializado a ${sampleRate}Hz")
                true
            } else {
                record.release()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al inicializar AudioRecord MIC: ${e.message}", e)
            false
        }
    }

    fun start() {
        val record = audioRecord ?: return
        audioQueue.clear()

        workerThread = Thread({
            try {
                record.startRecording()
            } catch (e: Exception) {
                Log.w(TAG, "Error iniciando micAudioRecord: ${e.message}")
            }

            val buf = ByteArray(BUFFER_SIZE)
            var sampleCounter = 0

            while (isRecordingProvider()) {
                try {
                    val readBytes = record.read(buf, 0, BUFFER_SIZE)
                    if (readBytes > 0) {
                        val isPaused = isPausedProvider()
                        if (!isMicMutedInternal.get() && !isPaused) {
                            val data = buf.copyOf(readBytes)
                            if (audioQueue.size >= MAX_QUEUE_CAPACITY) {
                                audioQueue.poll() // Dropear buffer más antiguo
                            }
                            audioQueue.offer(data)

                            // Medir amplitud (RMS) para animación reactiva de PNGtuber
                            sampleCounter++
                            if (sampleCounter % 2 == 0) { // Reducir overhead de cálculo
                                var sumSq = 0.0
                                val numSamples = readBytes / 2
                                for (i in 0 until readBytes step 2) {
                                    val sample = ((data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xFF)).toShort()
                                    sumSq += sample * sample
                                }
                                val rms = kotlin.math.sqrt(sumSq / numSamples)
                                val normalized = (rms / 15000.0).toFloat().coerceIn(0f, 1f)
                                onAmplitudeMeasured?.invoke(normalized)
                            }
                        } else {
                            onAmplitudeMeasured?.invoke(0f)
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
                record.stop()
            } catch (_: Exception) {}
        }, "OBS_MicAudioReader").apply { start() }
    }

    fun stop() {
        try {
            workerThread?.join(300)
        } catch (_: Exception) {}
        workerThread = null
    }

    fun release() {
        stop()
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando micAudioRecord: ${e.message}")
        }
        audioRecord = null
        audioQueue.clear()
    }
}
