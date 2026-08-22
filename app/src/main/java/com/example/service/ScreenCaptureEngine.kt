package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import com.example.model.AudioSourceType
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.service.capture.AudioPipelineModule
import com.example.service.capture.MuxerManager
import com.example.service.capture.VideoEncoderModule
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fachada orquestadora modular del motor de captura y grabación de pantalla.
 * Coordina [MediaProjection], [VirtualDisplay], [VideoEncoderModule], [AudioPipelineModule] y [MuxerManager].
 */
class ScreenCaptureEngine(private val context: Context) {

    companion object {
        private const val TAG = "ScreenCaptureEngine"
        private const val VIRTUAL_DISPLAY_NAME = "OBS_ScreenCapture_Display"
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var muxerManager: MuxerManager? = null
    private var videoEncoderModule: VideoEncoderModule? = null
    private var audioPipelineModule: AudioPipelineModule? = null

    private var currentOutputFile: File? = null
    private val isRecordingInternal = AtomicBoolean(false)
    private val isPausedInternal = AtomicBoolean(false)

    val isRecording: Boolean get() = isRecordingInternal.get()
    val isPaused: Boolean get() = isPausedInternal.get()
    val isMicMuted: Boolean get() = audioPipelineModule?.isMicMuted ?: false
    val isMicrophoneMuted: Boolean get() = isMicMuted
    val hasAudioTrack: Boolean get() = audioPipelineModule?.hasAudio ?: false
    val activeProjection: MediaProjection? get() = mediaProjection

    init {
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    fun toggleMicMute(): Boolean {
        return audioPipelineModule?.toggleMicMute() ?: false
    }

    fun toggleMicrophoneMuted(): Boolean {
        return toggleMicMute()
    }

    fun setMicMuted(muted: Boolean) {
        audioPipelineModule?.setMicMuted(muted)
    }

    fun setAudioGains(gameGain: Float, micGain: Float) {
        audioPipelineModule?.setAudioGains(gameGain, micGain)
    }

    fun setAudioFilters(noiseGate: Boolean, ducking: Boolean) {
        audioPipelineModule?.setAudioFilters(noiseGate, ducking)
    }

    fun getAudioLevels(): FloatArray {
        return audioPipelineModule?.getAudioLevels() ?: floatArrayOf(0f, 0f, 0f, 1f)
    }

    /**
     * Inicia la captura de pantalla por hardware, codificación H.264 / AAC y guardado en MP4.
     */
    fun startCapture(
        resultCode: Int,
        resultData: Intent,
        width: Int = VideoResolution.RES_1080P.width,
        height: Int = VideoResolution.RES_1080P.height,
        densityDpi: Int = 320,
        fps: Int = VideoFps.FPS_60.fps,
        bitrate: Int = VideoBitrate.BITRATE_8M.bps,
        audioSource: String = AudioSourceType.INTERNAL_AND_MIC.name,
        sampleRate: Int = 48000,
        outputFile: File? = null,
        onAudioAmplitude: ((Float) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        onSystemStop: (() -> Unit)? = null
    ): Boolean {
        if (isRecordingInternal.get()) {
            Log.w(TAG, "La captura ya se encuentra activa")
            return false
        }

        try {
            // 1. Obtener MediaProjection
            val proj = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
            if (proj == null) {
                val msg = "No se pudo obtener MediaProjection con el token provisto"
                Log.e(TAG, msg)
                onError?.invoke(msg)
                return false
            }
            this.mediaProjection = proj

            // Registrar callback para detectar detención del sistema
            proj.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.w(TAG, "Callback del sistema: MediaProjection detenido")
                    onSystemStop?.invoke()
                }
            }, null)

            // 2. Preparar archivo de destino
            val targetFile = if (outputFile != null) {
                outputFile
            } else {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
                File(outputDir, "GAMEPLAY_$timeStamp.mp4")
            }
            this.currentOutputFile = targetFile

            // 3. Inicializar Muxer
            val muxer = MuxerManager(
                outputFile = targetFile,
                hasAudioProvider = { audioPipelineModule?.hasAudio == true }
            )
            this.muxerManager = muxer

            // 4. Inicializar Codificador de Video
            val vEncoder = VideoEncoderModule(
                width = width,
                height = height,
                fps = fps,
                bitrate = bitrate,
                muxerManager = muxer,
                isRecordingProvider = { isRecordingInternal.get() },
                isPausedProvider = { isPausedInternal.get() }
            )
            vEncoder.initialize()
            this.videoEncoderModule = vEncoder

            // 5. Inicializar Pipeline de Audio
            val aPipeline = AudioPipelineModule(
                audioSource = audioSource,
                mediaProjection = proj,
                muxerManager = muxer,
                sampleRate = sampleRate,
                isRecordingProvider = { isRecordingInternal.get() },
                isPausedProvider = { isPausedInternal.get() },
                onAmplitudeMeasured = onAudioAmplitude
            )
            aPipeline.initialize()
            this.audioPipelineModule = aPipeline

            // 6. Crear VirtualDisplay
            val surface = vEncoder.inputSurface
            if (surface == null) {
                val msg = "Input Surface del encoder de video es nulo"
                Log.e(TAG, msg)
                onError?.invoke(msg)
                release()
                return false
            }

            val vDisplay = proj.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )
            this.virtualDisplay = vDisplay

            isRecordingInternal.set(true)
            isPausedInternal.set(false)

            // 7. Lanzar Workers en segundo plano
            vEncoder.startWorker()
            if (aPipeline.hasAudio) {
                aPipeline.startWorker()
            }

            Log.i(TAG, "Captura iniciada exitosamente (${width}x$height @ ${fps}fps, Audio: $audioSource, Destino: ${targetFile.name})")
            return true
        } catch (e: Exception) {
            val msg = e.message ?: "Error desconocido al iniciar captura"
            Log.e(TAG, "Error iniciando captura de pantalla: $msg", e)
            onError?.invoke(msg)
            release()
            return false
        }
    }

    fun pauseCapture(): Boolean {
        if (!isRecordingInternal.get() || isPausedInternal.get()) return false
        isPausedInternal.set(true)
        Log.d(TAG, "Grabación pausada")
        return true
    }

    fun resumeCapture(): Boolean {
        if (!isRecordingInternal.get() || !isPausedInternal.get()) return false
        isPausedInternal.set(false)
        Log.d(TAG, "Grabación reanudada")
        return true
    }

    fun takeScreenshot(
        context: Context,
        width: Int,
        height: Int,
        densityDpi: Int,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isRecordingInternal.get()) {
            val videoFile = currentOutputFile
            if (videoFile != null && videoFile.exists() && videoFile.length() > 0L) {
                ScreenshotHelper.captureFrameFromVideo(
                    context = context,
                    videoFile = videoFile,
                    onSuccess = onSuccess,
                    onError = { onError("Fotograma en procesamiento: $it") }
                )
            } else {
                onError("Grabación en curso: fotograma no disponible aún")
            }
            return
        }

        val proj = mediaProjection
        if (proj == null) {
            onError("No hay una proyección de pantalla activa para tomar la captura")
            return
        }

        ScreenshotHelper.captureFromMediaProjection(
            context = context,
            mediaProjection = proj,
            width = width,
            height = height,
            densityDpi = densityDpi,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun stopCapture(): File? {
        val savedFile = currentOutputFile
        try {
            isRecordingInternal.set(false)
            isPausedInternal.set(false)

            videoEncoderModule?.stopWorker()
            audioPipelineModule?.stopWorker()
            muxerManager?.stopAndRelease()
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo captura: ${e.message}", e)
        } finally {
            release()
        }
        return savedFile
    }

    fun release() {
        isRecordingInternal.set(false)
        isPausedInternal.set(false)

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando VirtualDisplay: ${e.message}")
        }
        virtualDisplay = null

        videoEncoderModule?.release()
        videoEncoderModule = null

        audioPipelineModule?.release()
        audioPipelineModule = null

        muxerManager = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error deteniendo MediaProjection: ${e.message}")
        }
        mediaProjection = null

        Log.d(TAG, "Recursos de ScreenCaptureEngine liberados con éxito")
    }
}
