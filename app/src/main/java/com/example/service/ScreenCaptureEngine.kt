package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.example.model.AudioSourceType
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Motor modular de captura de pantalla y codificación en tiempo real por hardware.
 * Utiliza [MediaCodec] + [MediaMuxer] y [AudioRecord] con soporte para captura de audio interno del juego
 * mediante [AudioPlaybackCaptureConfiguration] en Android 10+ y capturas instantáneas con [ScreenshotHelper].
 */
class ScreenCaptureEngine(private val context: Context) {

    companion object {
        private const val TAG = "ScreenCaptureEngine"
        private const val VIRTUAL_DISPLAY_NAME = "OBSMobile_VirtualDisplay"
        private const val AUDIO_SAMPLE_RATE = 44100
        private const val AUDIO_BIT_RATE = 192000
        private const val TIMEOUT_USEC = 10000L
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    // Codificación de Video por Hardware
    private var videoEncoder: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var videoThread: Thread? = null

    // Captura y Codificación de Audio
    private var audioRecord: AudioRecord? = null
    private var audioEncoder: MediaCodec? = null
    private var audioThread: Thread? = null

    // Muxer del contenedor MP4
    private var mediaMuxer: MediaMuxer? = null
    private val muxerLock = Object()
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false
    private var hasAudio = false

    private var currentOutputFile: File? = null
    private val isRecordingInternal = AtomicBoolean(false)
    private val isPausedInternal = AtomicBoolean(false)

    val isRecording: Boolean get() = isRecordingInternal.get()
    val isPaused: Boolean get() = isPausedInternal.get()
    val outputFile: File? get() = currentOutputFile
    val activeProjection: MediaProjection? get() = mediaProjection

    init {
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    /**
     * Inicializa y arranca la sesión de grabación de pantalla y audio (interno de juego o micrófono).
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
        onError: (String) -> Unit,
        onSystemStop: (() -> Unit)? = null
    ): Boolean {
        try {
            this.currentOutputFile = outputFile
            this.hasAudio = audioSource != AudioSourceType.NONE.name
            this.videoTrackIndex = -1
            this.audioTrackIndex = -1
            this.muxerStarted = false

            // 1. Inicializar MediaProjection
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
            if (mediaProjection == null) {
                onError("No se pudo obtener el permiso de MediaProjection del sistema")
                return false
            }

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.w(TAG, "MediaProjection detenido por el sistema o por el usuario")
                    onSystemStop?.invoke() ?: stopCapture()
                }
            }, Handler(Looper.getMainLooper()))

            // 2. Inicializar MediaMuxer
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // 3. Configurar codificador de video por hardware (H.264)
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // Keyframe cada 1 segundo
            }

            val vEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            vEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = vEncoder.createInputSurface()
            vEncoder.start()
            videoEncoder = vEncoder

            // 4. Configurar AudioRecord y codificador AAC si el audio está activado
            if (hasAudio) {
                setupAudioPipeline(audioSource)
            }

            // 5. Crear VirtualDisplay sobre la superficie del encoder
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface,
                null,
                null
            )

            isRecordingInternal.set(true)
            isPausedInternal.set(false)

            // 6. Lanzar hilos de procesamiento asíncrono no bloqueantes
            startVideoWorker()
            if (hasAudio && audioEncoder != null && audioRecord != null) {
                startAudioWorker()
            }

            Log.i(TAG, "Grabación iniciada exitosamente: ${width}x${height} @ $fps FPS (Audio: $audioSource)")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Excepción crítica iniciando ScreenCaptureEngine: ${e.message}", e)
            release()
            onError(e.message ?: "Error desconocido al inicializar el codificador")
            return false
        }
    }

    /**
     * Configura AudioRecord para audio interno (AudioPlaybackCaptureConfiguration) o micrófono.
     */
    private fun setupAudioPipeline(audioSource: String) {
        val isInternalOnly = audioSource == AudioSourceType.INTERNAL_GAME.name
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val channelCount = 2

        val minBufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, channelConfig, audioEncoding)
            .coerceAtLeast(8192)

        var record: AudioRecord? = null

        if (isInternalOnly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
            try {
                val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                record = AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(playbackConfig)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioEncoding)
                            .setSampleRate(AUDIO_SAMPLE_RATE)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize * 2)
                    .build()

                Log.i(TAG, "AudioRecord configurado exitosamente con AudioPlaybackCapture para Audio Interno del Juego")
            } catch (e: Exception) {
                Log.w(TAG, "Fallo al inicializar AudioPlaybackCapture, fallback a MIC: ${e.message}")
            }
        }

        // Fallback o modo micrófono normal
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_RATE,
                    channelConfig,
                    audioEncoding,
                    minBufferSize * 2
                )
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo inicializar AudioRecord para MIC: ${e.message}")
            }
        }

        if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord = record

            // Configurar encoder AAC
            val aFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, AUDIO_SAMPLE_RATE, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            val aEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            aEncoder.configure(aFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            aEncoder.start()
            audioEncoder = aEncoder
        } else {
            Log.w(TAG, "AudioRecord no inicializado, continuando sin pista de audio")
            hasAudio = false
        }
    }

    /**
     * Hilo de drenado y muxing del codificador de video H.264.
     */
    private fun startVideoWorker() {
        videoThread = Thread({
            val encoder = videoEncoder ?: return@Thread
            val bufferInfo = MediaCodec.BufferInfo()

            while (isRecordingInternal.get()) {
                if (isPausedInternal.get()) {
                    SystemClock.sleep(20)
                    continue
                }

                try {
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    when (outputBufferIndex) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            synchronized(muxerLock) {
                                if (videoTrackIndex == -1) {
                                    val newFormat = encoder.outputFormat
                                    videoTrackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                                    Log.d(TAG, "Pista de video agregada a MediaMuxer (index: $videoTrackIndex)")
                                    checkAndStartMuxer()
                                }
                            }
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // Sin datos aún, continuar ciclo
                        }
                        else -> {
                            if (outputBufferIndex >= 0) {
                                val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                                if (outputBuffer != null && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    synchronized(muxerLock) {
                                        if (!muxerStarted) {
                                            try {
                                                muxerLock.wait(100)
                                            } catch (_: InterruptedException) {}
                                        }
                                        if (muxerStarted && videoTrackIndex != -1 && !isPausedInternal.get()) {
                                            outputBuffer.position(bufferInfo.offset)
                                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                            mediaMuxer?.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                                        }
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

    /**
     * Hilo de lectura PCM y codificación AAC para audio interno/micrófono.
     */
    private fun startAudioWorker() {
        audioThread = Thread({
            val record = audioRecord ?: return@Thread
            val encoder = audioEncoder ?: return@Thread
            val bufferInfo = MediaCodec.BufferInfo()
            val byteBuffer = ByteBuffer.allocateDirect(4096)

            try {
                record.startRecording()
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar grabación de AudioRecord: ${e.message}")
                return@Thread
            }

            var presentationTimeUs = 0L

            while (isRecordingInternal.get()) {
                if (isPausedInternal.get()) {
                    SystemClock.sleep(20)
                    continue
                }

                try {
                    // 1. Leer PCM desde AudioRecord
                    byteBuffer.clear()
                    val readBytes = record.read(byteBuffer, byteBuffer.capacity())
                    if (readBytes > 0 && !isPausedInternal.get()) {
                        val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                            inputBuffer?.clear()
                            byteBuffer.position(0)
                            byteBuffer.limit(readBytes)
                            inputBuffer?.put(byteBuffer)

                            val pts = System.nanoTime() / 1000
                            encoder.queueInputBuffer(inputBufferIndex, 0, readBytes, pts, 0)
                        }
                    }

                    // 2. Drenar encoder de audio
                    while (true) {
                        val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            synchronized(muxerLock) {
                                if (audioTrackIndex == -1) {
                                    val newFormat = encoder.outputFormat
                                    audioTrackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                                    Log.d(TAG, "Pista de audio agregada a MediaMuxer (index: $audioTrackIndex)")
                                    checkAndStartMuxer()
                                }
                            }
                        } else if (outputBufferIndex >= 0) {
                            val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                            if (outputBuffer != null && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                synchronized(muxerLock) {
                                    if (!muxerStarted) {
                                        try {
                                            muxerLock.wait(100)
                                        } catch (_: InterruptedException) {}
                                    }
                                    if (muxerStarted && audioTrackIndex != -1 && !isPausedInternal.get()) {
                                        outputBuffer.position(bufferInfo.offset)
                                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                        mediaMuxer?.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                                    }
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

            try {
                record.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error deteniendo AudioRecord: ${e.message}")
            }
        }, "OBS_AudioWorker").apply { start() }
    }

    private fun checkAndStartMuxer() {
        synchronized(muxerLock) {
            if (muxerStarted) return
            val videoReady = videoTrackIndex != -1
            val audioReady = !hasAudio || audioTrackIndex != -1

            if (videoReady && audioReady) {
                try {
                    mediaMuxer?.start()
                    muxerStarted = true
                    Log.i(TAG, "MediaMuxer iniciado con éxito (Video: $videoTrackIndex, Audio: $audioTrackIndex)")
                    muxerLock.notifyAll()
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al iniciar MediaMuxer: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Pausa la grabación.
     */
    fun pauseCapture(): Boolean {
        if (!isRecordingInternal.get() || isPausedInternal.get()) return false
        isPausedInternal.set(true)
        Log.d(TAG, "Grabación pausada")
        return true
    }

    /**
     * Reanuda la grabación.
     */
    fun resumeCapture(): Boolean {
        if (!isRecordingInternal.get() || !isPausedInternal.get()) return false
        isPausedInternal.set(false)
        Log.d(TAG, "Grabación reanudada")
        return true
    }

    /**
     * Toma una captura de pantalla instantánea usando ImageReader sobre la proyección activa.
     */
    fun takeScreenshot(
        context: Context,
        width: Int,
        height: Int,
        densityDpi: Int,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        // Si la grabación está activa en Android 14+, no podemos crear un segundo VirtualDisplay en la misma proyección
        if (isRecordingInternal.get()) {
            val videoFile = currentOutputFile
            if (videoFile != null && videoFile.exists() && videoFile.length() > 0L) {
                ScreenshotHelper.captureFrameFromVideo(
                    context = context,
                    videoFile = videoFile,
                    onSuccess = onSuccess,
                    onError = {
                        onError("Fotograma en procesamiento: $it")
                    }
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

    /**
     * Detiene la captura y finaliza el archivo contenedor MP4 con el muxer de hardware.
     */
    fun stopCapture(): File? {
        val savedFile = currentOutputFile
        try {
            isRecordingInternal.set(false)
            isPausedInternal.set(false)

            // Esperar que los workers terminen limpiamente
            try {
                videoThread?.join(500)
                audioThread?.join(500)
            } catch (_: Exception) {}

            synchronized(muxerLock) {
                if (muxerStarted) {
                    try {
                        mediaMuxer?.stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "MediaMuxer stop excepción: ${e.message}")
                    }
                    muxerStarted = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo captura: ${e.message}", e)
        } finally {
            release()
        }
        return savedFile
    }

    /**
     * Libera de forma ordenada todos los recursos de hardware y proyecciones.
     */
    fun release() {
        isRecordingInternal.set(false)
        isPausedInternal.set(false)

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando VirtualDisplay: ${e.message}")
        }
        virtualDisplay = null

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

        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando audioRecord: ${e.message}")
        }
        audioRecord = null

        try {
            audioEncoder?.stop()
            audioEncoder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando audioEncoder: ${e.message}")
        }
        audioEncoder = null

        try {
            mediaMuxer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando MediaMuxer: ${e.message}")
        }
        mediaMuxer = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error deteniendo MediaProjection: ${e.message}")
        }
        mediaProjection = null

        Log.d(TAG, "Recursos de ScreenCaptureEngine liberados con éxito")
    }
}
