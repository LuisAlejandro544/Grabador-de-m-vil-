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
import com.example.nativecore.NativeAudioDSPBridge
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
    private var internalAudioRecord: AudioRecord? = null
    private var micAudioRecord: AudioRecord? = null
    private var audioEncoder: MediaCodec? = null
    private var audioThread: Thread? = null
    private val isMicMutedInternal = AtomicBoolean(false)

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
    val isMicrophoneMuted: Boolean get() = isMicMutedInternal.get()
    val outputFile: File? get() = currentOutputFile
    val activeProjection: MediaProjection? get() = mediaProjection

    /**
     * Activa o silencia el micrófono en tiempo real sin cortar la grabación ni el audio del juego.
     */
    fun setMicrophoneMuted(muted: Boolean) {
        isMicMutedInternal.set(muted)
        Log.i(TAG, "Micrófono en vivo cambiado a: ${if (muted) "SILENCIADO (Solo Audio Juego)" else "ACTIVO (Juego + Voz)"}")
    }

    fun toggleMicrophoneMuted(): Boolean {
        val newMuted = !isMicMutedInternal.get()
        setMicrophoneMuted(newMuted)
        return newMuted
    }

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
            if (hasAudio && audioEncoder != null && (internalAudioRecord != null || micAudioRecord != null)) {
                startAudioWorker()
            }

            Log.i(TAG, "Grabación iniciada exitosamente: ${width}x${height} @ $fps FPS (Audio: $audioSource, Mic Muted: ${isMicMutedInternal.get()})")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Excepción crítica iniciando ScreenCaptureEngine: ${e.message}", e)
            release()
            onError(e.message ?: "Error desconocido al inicializar el codificador")
            return false
        }
    }

    /**
     * Configura el pipeline de audio dual: AudioPlaybackCapture (Juego) + MIC (Voz en vivo).
     */
    private fun setupAudioPipeline(audioSource: String) {
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val channelCount = 2

        val minBufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, channelConfig, audioEncoding)
            .coerceAtLeast(8192)

        val wantsInternal = audioSource == AudioSourceType.INTERNAL_GAME.name || audioSource == AudioSourceType.INTERNAL_AND_MIC.name
        val wantsMic = audioSource == AudioSourceType.MIC.name || audioSource == AudioSourceType.INTERNAL_AND_MIC.name || audioSource == AudioSourceType.INTERNAL_GAME.name

        // Si se seleccionó solo audio del juego, el micrófono se inicializa muteado para permitir activación dinámica
        isMicMutedInternal.set(audioSource == AudioSourceType.INTERNAL_GAME.name)

        // 1. Audio Interno del Juego (Android 10+)
        if (wantsInternal && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
            try {
                val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                val record = AudioRecord.Builder()
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

                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    internalAudioRecord = record
                    Log.i(TAG, "AudioRecord para Audio Interno del Juego inicializado exitosamente")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallo al inicializar AudioPlaybackCapture: ${e.message}")
            }
        }

        // 2. Micrófono para comentarios / voz del jugador
        if (wantsMic) {
            try {
                val micRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_RATE,
                    channelConfig,
                    audioEncoding,
                    minBufferSize * 2
                )
                if (micRecord.state == AudioRecord.STATE_INITIALIZED) {
                    micAudioRecord = micRecord
                    Log.i(TAG, "AudioRecord para Micrófono inicializado exitosamente (Muted: ${isMicMutedInternal.get()})")
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo inicializar AudioRecord para MIC: ${e.message}")
            }
        }

        // Fallback a MIC simple si el audio interno falló
        if (internalAudioRecord == null && micAudioRecord == null && audioSource != AudioSourceType.NONE.name) {
            try {
                val fallback = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_RATE,
                    channelConfig,
                    audioEncoding,
                    minBufferSize * 2
                )
                if (fallback.state == AudioRecord.STATE_INITIALIZED) {
                    micAudioRecord = fallback
                    isMicMutedInternal.set(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback de audio falló: ${e.message}")
            }
        }

        val hasAnyAudioRecord = (internalAudioRecord != null && internalAudioRecord?.state == AudioRecord.STATE_INITIALIZED) ||
                (micAudioRecord != null && micAudioRecord?.state == AudioRecord.STATE_INITIALIZED)

        if (hasAnyAudioRecord) {
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
            hasAudio = true

            // Inicializar motor de Audio DSP Pro en C++ (Noise Gate, Ducking automático, Soft Limiter)
            NativeAudioDSPBridge.initAudioDsp(AUDIO_SAMPLE_RATE, channelCount)
            NativeAudioDSPBridge.configureAudioDsp(
                noiseGateThresholdDb = -38.0f,
                duckingAttenuation = 0.35f,
                micGain = 1.25f,
                gameGain = 1.0f,
                noiseGateEnabled = true,
                duckingEnabled = true,
                peakLimiterEnabled = true
            )
        } else {
            Log.w(TAG, "Ningún AudioRecord disponible, continuando sin pista de audio")
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
     * Hilo de lectura PCM y codificación AAC con soporte de mezcla dinámica en vivo (Juego + Micrófono).
     */
    private fun startAudioWorker() {
        audioThread = Thread({
            val intRec = internalAudioRecord
            val micRec = micAudioRecord
            val encoder = audioEncoder ?: return@Thread
            val bufferInfo = MediaCodec.BufferInfo()

            try {
                intRec?.startRecording()
            } catch (e: Exception) {
                Log.w(TAG, "Error iniciando internalAudioRecord: ${e.message}")
            }
            try {
                micRec?.startRecording()
            } catch (e: Exception) {
                Log.w(TAG, "Error iniciando micAudioRecord: ${e.message}")
            }

            val bufferSize = 4096
            val internalBuf = ByteArray(bufferSize)
            val micBuf = ByteArray(bufferSize)
            val mixBuf = ByteArray(bufferSize)

            while (isRecordingInternal.get()) {
                if (isPausedInternal.get()) {
                    SystemClock.sleep(20)
                    continue
                }

                try {
                    val readInternal = intRec?.read(internalBuf, 0, bufferSize) ?: -1
                    val readMic = micRec?.read(micBuf, 0, bufferSize) ?: -1

                    var finalBytes: ByteArray? = null
                    var finalSize = 0

                    if (intRec != null && micRec != null) {
                        // Caso 1: Mezcla Dual Activa (Audio interno + Micrófono dinámico)
                        if (readInternal > 0) {
                            finalSize = readInternal
                            val isMicMuted = isMicMutedInternal.get()

                            // Procesar mediante C++ Audio DSP (Noise Gate, Audio Ducking y Soft Limiter)
                            val processedBytes = if (NativeAudioDSPBridge.isNativeReady()) {
                                val pcmCount = if (readMic > 0 && !isMicMuted) minOf(readInternal, readMic) else readInternal
                                NativeAudioDSPBridge.processAndMixAudio(
                                    internalAudio = internalBuf,
                                    micAudio = if (readMic > 0 && !isMicMuted) micBuf else null,
                                    outputMix = mixBuf,
                                    byteCount = pcmCount,
                                    isMicMuted = isMicMuted
                                )
                            } else 0

                            if (processedBytes > 0) {
                                if (readInternal > processedBytes) {
                                    System.arraycopy(internalBuf, processedBytes, mixBuf, processedBytes, readInternal - processedBytes)
                                }
                                finalBytes = mixBuf
                            } else if (!isMicMuted && readMic > 0) {
                                // Fallback a mezcla PCM directa en Kotlin
                                val sampleCount = minOf(readInternal, readMic) / 2
                                for (i in 0 until sampleCount) {
                                    val idx = i * 2
                                    val sInternal = (internalBuf[idx].toInt() and 0xFF) or (internalBuf[idx + 1].toInt() shl 8)
                                    val sMic = (micBuf[idx].toInt() and 0xFF) or (micBuf[idx + 1].toInt() shl 8)
                                    val mixed = (sInternal.toShort() + sMic.toShort()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                    mixBuf[idx] = (mixed and 0xFF).toByte()
                                    mixBuf[idx + 1] = ((mixed shr 8) and 0xFF).toByte()
                                }
                                if (readInternal > readMic) {
                                    System.arraycopy(internalBuf, readMic, mixBuf, readMic, readInternal - readMic)
                                }
                                finalBytes = mixBuf
                            } else {
                                // Micrófono silenciado: Graba exclusivamente audio del juego
                                finalBytes = internalBuf
                            }
                        }
                    } else if (intRec != null) {
                        // Caso 2: Solo audio interno del juego disponible
                        if (readInternal > 0) {
                            finalSize = readInternal
                            finalBytes = internalBuf
                        }
                    } else if (micRec != null) {
                        // Caso 3: Solo micrófono disponible (con filtrado DSP en C++)
                        if (readMic > 0) {
                            finalSize = readMic
                            if (isMicMutedInternal.get()) {
                                micBuf.fill(0)
                                finalBytes = micBuf
                            } else {
                                val processed = if (NativeAudioDSPBridge.isNativeReady()) {
                                    NativeAudioDSPBridge.processAndMixAudio(
                                        internalAudio = null,
                                        micAudio = micBuf,
                                        outputMix = mixBuf,
                                        byteCount = readMic,
                                        isMicMuted = false
                                    )
                                } else 0

                                finalBytes = if (processed > 0) mixBuf else micBuf
                            }
                        }
                    }

                    if (finalBytes != null && finalSize > 0 && !isPausedInternal.get()) {
                        val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                            inputBuffer?.clear()
                            inputBuffer?.put(finalBytes, 0, finalSize)

                            val pts = System.nanoTime() / 1000
                            encoder.queueInputBuffer(inputBufferIndex, 0, finalSize, pts, 0)
                        }
                    }

                    // Drenar encoder de audio
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
                intRec?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error deteniendo internalAudioRecord: ${e.message}")
            }
            try {
                micRec?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error deteniendo micAudioRecord: ${e.message}")
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
            internalAudioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando internalAudioRecord: ${e.message}")
        }
        internalAudioRecord = null

        try {
            micAudioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando micAudioRecord: ${e.message}")
        }
        micAudioRecord = null

        try {
            audioEncoder?.stop()
            audioEncoder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando audioEncoder: ${e.message}")
        }
        audioEncoder = null

        NativeAudioDSPBridge.releaseAudioDsp()

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
