package com.example.service.capture

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Worker desacoplado responsable exclusivamente de capturar el audio interno del juego / sistema
 * utilizando la API [AudioPlaybackCaptureConfiguration] en Android 10+ (API 29+).
 * Diseñado con arquitectura Zero-Allocation para eliminar pausas de Garbage Collector (GC)
 * y prioridad de hilo secundaria para no competir con los núcleos principales de CPU del juego.
 */
class InternalAudioWorker(
    private val mediaProjection: MediaProjection?,
    private val sampleRate: Int,
    private val isRecordingProvider: () -> Boolean,
    private val isPausedProvider: () -> Boolean
) {
    companion object {
        private const val TAG = "InternalAudioWorker"
        private const val BUFFER_SIZE = AudioFrameBuffer.DEFAULT_CAPACITY
        private const val MAX_QUEUE_CAPACITY = 16
        private const val POOL_CAPACITY = 24
    }

    private var audioRecord: AudioRecord? = null
    private var workerThread: Thread? = null
    private val bufferPool = ConcurrentLinkedQueue<AudioFrameBuffer>()
    val audioQueue = ConcurrentLinkedQueue<AudioFrameBuffer>()

    init {
        for (i in 0 until POOL_CAPACITY) {
            bufferPool.offer(AudioFrameBuffer())
        }
    }

    val isInitialized: Boolean
        get() = audioRecord != null && audioRecord?.state == AudioRecord.STATE_INITIALIZED

    fun recycleBuffer(buffer: AudioFrameBuffer) {
        if (bufferPool.size < POOL_CAPACITY) {
            bufferPool.offer(buffer)
        }
    }

    @SuppressLint("MissingPermission")
    fun initialize(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || mediaProjection == null) {
            return false
        }

        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = maxOf(
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding),
            BUFFER_SIZE * 2
        )

        return try {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
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
                audioRecord = record
                Log.i(TAG, "AudioPlaybackCapture inicializado para audio interno a ${sampleRate}Hz")
                true
            } else {
                record.release()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al inicializar captura interna: ${e.message}", e)
            false
        }
    }

    fun start() {
        val record = audioRecord ?: return
        audioQueue.clear()

        workerThread = Thread({
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            } catch (_: Exception) {}

            try {
                record.startRecording()
            } catch (e: Exception) {
                Log.w(TAG, "Error iniciando AudioRecord interno: ${e.message}")
            }

            while (isRecordingProvider()) {
                try {
                    val frameBuffer = bufferPool.poll() ?: AudioFrameBuffer()
                    val readBytes = record.read(frameBuffer.data, 0, BUFFER_SIZE)
                    if (readBytes > 0) {
                        frameBuffer.size = readBytes
                        if (!isPausedProvider()) {
                            if (audioQueue.size >= MAX_QUEUE_CAPACITY) {
                                val dropped = audioQueue.poll()
                                if (dropped != null) recycleBuffer(dropped)
                            }
                            audioQueue.offer(frameBuffer)
                        } else {
                            recycleBuffer(frameBuffer)
                        }
                    } else {
                        recycleBuffer(frameBuffer)
                        SystemClock.sleep(4)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Excepción leyendo audio interno: ${e.message}")
                    break
                }
            }

            try {
                record.stop()
            } catch (_: Exception) {}
        }, "OBS_InternalAudioReader").apply { start() }
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
            Log.w(TAG, "Error liberando audioRecord interno: ${e.message}")
        }
        audioRecord = null
        audioQueue.clear()
        bufferPool.clear()
    }
}
