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

    init {
        try {
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando MediaMuxer en ${outputFile.absolutePath}: ${e.message}", e)
        }
    }

    fun addVideoTrack(format: MediaFormat) {
        synchronized(lock) {
            if (videoTrackIndex == -1) {
                videoTrackIndex = mediaMuxer?.addTrack(format) ?: -1
                Log.d(TAG, "Pista de video agregada (index: $videoTrackIndex)")
                checkAndStartMuxerLocked()
            }
        }
    }

    fun addAudioTrack(format: MediaFormat) {
        synchronized(lock) {
            if (audioTrackIndex == -1) {
                audioTrackIndex = mediaMuxer?.addTrack(format) ?: -1
                Log.d(TAG, "Pista de audio agregada (index: $audioTrackIndex)")
                checkAndStartMuxerLocked()
            }
        }
    }

    private fun checkAndStartMuxerLocked() {
        if (muxerStarted) return
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
            if (!muxerStarted) {
                try {
                    lock.wait(100)
                } catch (_: InterruptedException) {}
            }
            if (muxerStarted && videoTrackIndex != -1) {
                buffer.position(bufferInfo.offset)
                buffer.limit(bufferInfo.offset + bufferInfo.size)
                mediaMuxer?.writeSampleData(videoTrackIndex, buffer, bufferInfo)
            }
        }
    }

    fun writeAudioSample(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (!muxerStarted) {
                try {
                    lock.wait(100)
                } catch (_: InterruptedException) {}
            }
            if (muxerStarted && audioTrackIndex != -1) {
                buffer.position(bufferInfo.offset)
                buffer.limit(bufferInfo.offset + bufferInfo.size)
                mediaMuxer?.writeSampleData(audioTrackIndex, buffer, bufferInfo)
            }
        }
    }

    fun stopAndRelease() {
        synchronized(lock) {
            if (muxerStarted) {
                try {
                    mediaMuxer?.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "MediaMuxer stop excepción: ${e.message}")
                }
                muxerStarted = false
            }
            try {
                mediaMuxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "MediaMuxer release excepción: ${e.message}")
            }
            mediaMuxer = null
        }
    }
}
