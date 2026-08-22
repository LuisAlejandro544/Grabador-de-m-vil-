package com.example.service.capture

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Módulo de codificación de video por hardware (H.264 / AVC).
 * Proporciona el [Surface] de entrada para el [android.hardware.display.VirtualDisplay]
 * y corre el worker de drenado que escribe en [MuxerManager].
 */
class VideoEncoderModule(
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitrate: Int,
    private val muxerManager: MuxerManager,
    private val isRecordingProvider: () -> Boolean,
    private val isPausedProvider: () -> Boolean
) {

    companion object {
        private const val TAG = "VideoEncoderModule"
        private const val TIMEOUT_USEC = 10000L
        private const val I_FRAME_INTERVAL = 1
    }

    private var videoEncoder: MediaCodec? = null
    var inputSurface: Surface? = null
        private set

    private var videoThread: Thread? = null

    fun initialize() {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_CAPTURE_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
            setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel41)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()
        encoder.start()
        videoEncoder = encoder
        Log.i(TAG, "VideoEncoder configurado con éxito (${width}x$height, ${fps}fps, ${bitrate}bps)")
    }

    fun startWorker() {
        videoThread = Thread({
            val encoder = videoEncoder ?: return@Thread
            val bufferInfo = MediaCodec.BufferInfo()

            while (isRecordingProvider()) {
                if (isPausedProvider()) {
                    SystemClock.sleep(20)
                    continue
                }

                try {
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    when (outputBufferIndex) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val newFormat = encoder.outputFormat
                            muxerManager.addVideoTrack(newFormat)
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // Sin datos aún
                        }
                        else -> {
                            if (outputBufferIndex >= 0) {
                                val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                                if (outputBuffer != null && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    if (!isPausedProvider()) {
                                        muxerManager.writeVideoSample(outputBuffer, bufferInfo)
                                    }
                                }
                                encoder.releaseOutputBuffer(outputBufferIndex, false)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en bucle de video: ${e.message}")
                    break
                }
            }
        }, "OBS_VideoWorker").apply { start() }
    }

    fun stopWorker() {
        try {
            videoThread?.join(500)
        } catch (_: Exception) {}
        videoThread = null
    }

    fun release() {
        try {
            videoEncoder?.stop()
            videoEncoder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando videoEncoder: ${e.message}")
        }
        videoEncoder = null

        try {
            inputSurface?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando inputSurface: ${e.message}")
        }
        inputSurface = null
    }
}
