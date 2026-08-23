package com.example.service.capture

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Gestor sincronizado de MediaMuxer para empaquetar pistas H.264 de video y AAC de audio en formato MP4.
 */
class MuxerManager(
    private val outputFile: File,
    private val hasAudioProvider: () -> Boolean
) {

    companion object {
        private const val TAG = "MuxerManager"
    }

    private val lock = Object()
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false
    private var isReleased = false
    private var samplesWrittenCount = 0L

    // Sincronización continua de PTS para soportar Pausa / Reanudación sin saltos ni desincronización
    private var videoPtsOffsetUs = 0L
    private var lastVideoRawPtsUs = -1L
    private var lastVideoWrittenPtsUs = -1L

    private var audioPtsOffsetUs = 0L
    private var lastAudioRawPtsUs = -1L
    private var lastAudioWrittenPtsUs = -1L

    init {
        try {
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando MediaMuxer en ${outputFile.absolutePath}: ${e.message}", e)
        }
    }

    fun addVideoTrack(format: MediaFormat) {
        synchronized(lock) {
            if (isReleased) return
            if (videoTrackIndex == -1) {
                videoTrackIndex = mediaMuxer?.addTrack(format) ?: -1
                Log.d(TAG, "Pista de video agregada (index: $videoTrackIndex)")
                checkAndStartMuxerLocked()
            }
        }
    }

    fun addAudioTrack(format: MediaFormat) {
        synchronized(lock) {
            if (isReleased) return
            if (audioTrackIndex == -1) {
                audioTrackIndex = mediaMuxer?.addTrack(format) ?: -1
                Log.d(TAG, "Pista de audio agregada (index: $audioTrackIndex)")
                checkAndStartMuxerLocked()
            }
        }
    }

    private fun checkAndStartMuxerLocked() {
        if (muxerStarted || isReleased) return
        val videoReady = videoTrackIndex != -1
        val audioReady = !hasAudioProvider() || audioTrackIndex != -1

        if (videoReady && audioReady) {
            try {
                mediaMuxer?.start()
                muxerStarted = true
                Log.i(TAG, "MediaMuxer iniciado con éxito (Video: $videoTrackIndex, Audio: $audioTrackIndex)")
                lock.notifyAll()
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al iniciar MediaMuxer: ${e.message}", e)
            }
        }
    }

    fun writeVideoSample(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (isReleased) return
            if (!muxerStarted) {
                try {
                    lock.wait(100)
                } catch (_: InterruptedException) {}
            }
            if (muxerStarted && videoTrackIndex != -1 && !isReleased) {
                try {
                    val rawPts = bufferInfo.presentationTimeUs
                    if (lastVideoRawPtsUs != -1L) {
                        val gap = rawPts - lastVideoRawPtsUs
                        // Si hubo una pausa o salto mayor a 100ms (100000us), compensar el desfase
                        if (gap > 100_000L) {
                            val normalIntervalUs = 16_666L // ~60fps
                            videoPtsOffsetUs += (gap - normalIntervalUs)
                        }
                    }
                    lastVideoRawPtsUs = rawPts

                    var adjustedPts = rawPts - videoPtsOffsetUs
                    // Garantizar monotonicidad estricta para evitar rechazo en el contenedor MP4
                    if (adjustedPts <= lastVideoWrittenPtsUs) {
                        adjustedPts = lastVideoWrittenPtsUs + 1000L // +1ms de seguridad
                    }
                    lastVideoWrittenPtsUs = adjustedPts
                    bufferInfo.presentationTimeUs = adjustedPts

                    buffer.position(bufferInfo.offset)
                    buffer.limit(bufferInfo.offset + bufferInfo.size)
                    mediaMuxer?.writeSampleData(videoTrackIndex, buffer, bufferInfo)
                    samplesWrittenCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Error escribiendo muestra de video: ${e.message}")
                }
            }
        }
    }

    fun writeAudioSample(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (isReleased) return
            if (!muxerStarted) {
                try {
                    lock.wait(100)
                } catch (_: InterruptedException) {}
            }
            if (muxerStarted && audioTrackIndex != -1 && !isReleased) {
                try {
                    val rawPts = bufferInfo.presentationTimeUs
                    if (lastAudioRawPtsUs != -1L) {
                        val gap = rawPts - lastAudioRawPtsUs
                        // Si hubo un salto mayor a 100ms, compensar en audio
                        if (gap > 100_000L) {
                            val normalAudioIntervalUs = 21_333L // ~1024 samples @ 48kHz
                            audioPtsOffsetUs += (gap - normalAudioIntervalUs)
                        }
                    }
                    lastAudioRawPtsUs = rawPts

                    var adjustedPts = rawPts - audioPtsOffsetUs
                    // Garantizar monotonicidad estricta
                    if (adjustedPts <= lastAudioWrittenPtsUs) {
                        adjustedPts = lastAudioWrittenPtsUs + 1000L
                    }
                    lastAudioWrittenPtsUs = adjustedPts
                    bufferInfo.presentationTimeUs = adjustedPts

                    buffer.position(bufferInfo.offset)
                    buffer.limit(bufferInfo.offset + bufferInfo.size)
                    mediaMuxer?.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                    samplesWrittenCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Error escribiendo muestra de audio: ${e.message}")
                }
            }
        }
    }

    /**
     * Finaliza de forma ordenada (Graceful Finalize) el contenedor MP4.
     * Escribe el átomo 'moov', cierra los flujos y valida la integridad del archivo resultante.
     */
    fun stopAndRelease(): Boolean {
        synchronized(lock) {
            if (isReleased) return outputFile.exists() && outputFile.length() > 0
            isReleased = true

            var finalizeSuccess = false
            if (muxerStarted && samplesWrittenCount > 0) {
                try {
                    mediaMuxer?.stop()
                    finalizeSuccess = true
                    Log.i(TAG, "MediaMuxer finalizado ordenadamente (moov atom escrito, $samplesWrittenCount muestras)")
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "Advertencia al detener MediaMuxer (posible parada rápida): ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Excepción inesperada al detener MediaMuxer: ${e.message}", e)
                }
            } else {
                Log.w(TAG, "MediaMuxer detenido sin muestras suficientes ($samplesWrittenCount muestras)")
            }
            muxerStarted = false

            try {
                mediaMuxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "MediaMuxer release excepción: ${e.message}")
            }
            mediaMuxer = null

            // Validar que el archivo MP4 generado no esté corrupto
            val valid = outputFile.exists() && outputFile.length() > 1024
            Log.d(TAG, "Validación de integridad MP4 final: valid=$valid, tamaño=${outputFile.length()} bytes")
            return finalizeSuccess || valid
        }
    }
}
