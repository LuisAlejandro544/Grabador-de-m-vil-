package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import com.example.model.AudioSourceType
import java.io.File

/**
 * Motor modular de captura de pantalla y codificación de video por hardware.
 * Encapsula la gestión de MediaProjection, VirtualDisplay y MediaRecorder.
 */
class ScreenCaptureEngine(private val context: Context) {

    companion object {
        private const val TAG = "ScreenCaptureEngine"
        private const val VIRTUAL_DISPLAY_NAME = "OBSMobile_VirtualDisplay"
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private var currentOutputFile: File? = null
    private var isRecordingInternal = false
    private var isPausedInternal = false

    val isRecording: Boolean get() = isRecordingInternal
    val isPaused: Boolean get() = isPausedInternal
    val outputFile: File? get() = currentOutputFile

    init {
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    /**
     * Inicializa y arranca la sesión de grabación de pantalla con los parámetros de resolución, tasa de cuadros y tasa de bits provistos.
     */
    fun startCapture(
        resultCode: Int,
        resultData: Intent,
        width: Int,
        height: Int,
        densityDpi: Int,
        fps: Int,
        bitrate: Int,
        audioSource: String,
        outputFile: File,
        onError: (String) -> Unit
    ): Boolean {
        try {
            this.currentOutputFile = outputFile

            // 1. Inicializar MediaProjection
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
            if (mediaProjection == null) {
                onError("No se pudo obtener el permiso de MediaProjection del sistema")
                return false
            }

            // Registrar callback para detectar terminación por el sistema
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        Log.w(TAG, "MediaProjection detenido por el sistema o por el usuario")
                        stopCapture()
                    }
                }, null)
            }

            // 2. Inicializar y configurar MediaRecorder
            mediaRecorder = createAndConfigureMediaRecorder(
                width = width,
                height = height,
                fps = fps,
                bitrate = bitrate,
                audioSource = audioSource,
                outputFile = outputFile
            )
            mediaRecorder?.prepare()

            // 3. Crear VirtualDisplay enlazado a la superficie del codificador
            val surface = mediaRecorder?.surface
            if (surface == null) {
                onError("No se pudo obtener la superficie de codificación de MediaRecorder")
                release()
                return false
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )

            // 4. Iniciar grabación
            mediaRecorder?.start()
            isRecordingInternal = true
            isPausedInternal = false

            Log.i(TAG, "Grabación iniciada exitosamente: ${width}x${height} @ $fps FPS, Bitrate=$bitrate")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Excepción crítica iniciando ScreenCaptureEngine: ${e.message}", e)
            release()
            onError(e.message ?: "Error desconocido al inicializar el codificador de video")
            return false
        }
    }

    /**
     * Pausa la grabación si está soportado en la versión de Android (API 24+).
     */
    fun pauseCapture(): Boolean {
        if (!isRecordingInternal || isPausedInternal) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                isPausedInternal = true
                Log.d(TAG, "Grabación pausada")
                true
            } else {
                Log.w(TAG, "La pausa de grabación no está soportada en versiones previas a Android 7.0")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al pausar grabación: ${e.message}", e)
            false
        }
    }

    /**
     * Reanuda la grabación en pausa (API 24+).
     */
    fun resumeCapture(): Boolean {
        if (!isRecordingInternal || !isPausedInternal) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                isPausedInternal = false
                Log.d(TAG, "Grabación reanudada")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al reanudar grabación: ${e.message}", e)
            false
        }
    }

    /**
     * Detiene la captura y asegura el cierre correcto del archivo contenedor MP4.
     */
    fun stopCapture(): File? {
        val savedFile = currentOutputFile
        try {
            if (isRecordingInternal) {
                try {
                    mediaRecorder?.stop()
                } catch (e: RuntimeException) {
                    Log.w(TAG, "MediaRecorder stop falló (posiblemente detenido inmediatamente): ${e.message}")
                    if (savedFile != null && savedFile.length() == 0L) {
                        savedFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo MediaRecorder: ${e.message}", e)
        } finally {
            release()
        }
        return savedFile
    }

    /**
     * Libera de forma ordenada todos los recursos de hardware y proyecciones.
     */
    fun release() {
        isRecordingInternal = false
        isPausedInternal = false

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando VirtualDisplay: ${e.message}")
        }
        virtualDisplay = null

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando MediaRecorder: ${e.message}")
        }
        mediaRecorder = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error deteniendo MediaProjection: ${e.message}")
        }
        mediaProjection = null

        Log.d(TAG, "Recursos de ScreenCaptureEngine liberados con éxito")
    }

    /**
     * Configuración de MediaRecorder optimizada para bajo consumo de batería y 60 FPS fluidos.
     */
    private fun createAndConfigureMediaRecorder(
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        audioSource: String,
        outputFile: File
    ): MediaRecorder {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        val isAudioEnabled = audioSource != AudioSourceType.NONE.name
        val isInternalOnly = audioSource == AudioSourceType.INTERNAL_GAME.name

        if (isAudioEnabled) {
            try {
                if (isInternalOnly) {
                    recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                } else {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo asignar fuente de audio específica, fallback a MIC: ${e.message}")
                try {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                } catch (e2: Exception) {
                    Log.w(TAG, "Audio no disponible, continuando mudo: ${e2.message}")
                }
            }
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setOutputFile(outputFile.absolutePath)

        recorder.setVideoSize(width, height)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

        if (isAudioEnabled) {
            try {
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(192000)
                recorder.setAudioSamplingRate(48000)
                recorder.setAudioChannels(2)
            } catch (e: Exception) {
                Log.w(TAG, "Error configurando codificador de audio AAC: ${e.message}")
            }
        }

        recorder.setVideoEncodingBitRate(bitrate)
        recorder.setVideoFrameRate(fps)

        recorder.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MediaRecorder error callback: what=$what, extra=$extra")
        }

        recorder.setOnInfoListener { _, what, extra ->
            Log.i(TAG, "MediaRecorder info callback: what=$what, extra=$extra")
        }

        return recorder
    }
}
