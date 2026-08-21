package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.model.AudioSourceType
import com.example.model.RecordingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Servicio en primer plano desacoplado para la grabación de pantalla.
 * Orquesta [ScreenCaptureEngine], [RecordNotificationHelper], [RecordStorageHelper],
 * [FloatingBubbleManager], [ScreenshotHelper] y [ScreenDrawingOverlay] sin acoplamiento monolítico.
 */
class ScreenRecordService : Service() {

    companion object {
        const val TAG = "ScreenRecordService"

        const val ACTION_START = "com.example.service.START"
        const val ACTION_STOP = "com.example.service.STOP"
        const val ACTION_PAUSE = "com.example.service.PAUSE"
        const val ACTION_RESUME = "com.example.service.RESUME"
        const val ACTION_SCREENSHOT = "com.example.service.SCREENSHOT"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_RES_WIDTH = "extra_res_width"
        const val EXTRA_RES_HEIGHT = "extra_res_height"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_AUDIO_SOURCE = "extra_audio_source"
        const val EXTRA_SHOW_FLOATING_BUBBLE = "extra_show_floating_bubble"

        private val _recordingState = MutableStateFlow(RecordingStatus.IDLE)
        val recordingState = _recordingState.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0)
        val elapsedSeconds = _elapsedSeconds.asStateFlow()

        private val _lastSavedFilePath = MutableStateFlow<String?>(null)
        val lastSavedFilePath = _lastSavedFilePath.asStateFlow()

        private val _errorMessage = MutableStateFlow<String?>(null)
        val errorMessage = _errorMessage.asStateFlow()

        fun isRecording(): Boolean {
            val state = _recordingState.value
            return state == RecordingStatus.RECORDING || state == RecordingStatus.PAUSED
        }
    }

    private lateinit var captureEngine: ScreenCaptureEngine
    private lateinit var notificationHelper: RecordNotificationHelper
    private var floatingBubbleManager: FloatingBubbleManager? = null

    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var currentAudioEnabled = true
    private var currentActiveFile: File? = null
    private var currentRecWidth = 1080
    private var currentRecHeight = 1920
    private var currentDensityDpi = 480

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        captureEngine = ScreenCaptureEngine(this)
        notificationHelper = RecordNotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStartAction(intent)
            ACTION_STOP -> handleStopAction()
            ACTION_PAUSE -> handlePauseAction()
            ACTION_RESUME -> handleResumeAction()
            ACTION_SCREENSHOT -> handleScreenshotAction()
        }
        return START_NOT_STICKY
    }

    private fun handleStartAction(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        val width = intent.getIntExtra(EXTRA_RES_WIDTH, 1080)
        val height = intent.getIntExtra(EXTRA_RES_HEIGHT, 1920)
        val fps = intent.getIntExtra(EXTRA_FPS, 60)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 8_000_000)
        val audioSource = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: AudioSourceType.MIC.name
        val showFloatingBubble = intent.getBooleanExtra(EXTRA_SHOW_FLOATING_BUBBLE, true)

        if (resultCode == 0 || resultData == null) {
            Log.e(TAG, "Parámetros de proyección inválidos")
            stopSelf()
            return
        }

        // Determinar orientación y dimensiones reales
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val isPortrait = metrics.heightPixels >= metrics.widthPixels
        val recWidth = if (isPortrait) minOf(width, height) else maxOf(width, height)
        val recHeight = if (isPortrait) maxOf(width, height) else minOf(width, height)

        currentRecWidth = recWidth
        currentRecHeight = recHeight
        currentDensityDpi = metrics.densityDpi
        currentAudioEnabled = audioSource != AudioSourceType.NONE.name

        // 1. Iniciar Foreground Service con tipo MEDIA_PROJECTION
        val initialNotification = notificationHelper.buildForegroundNotification(0, isPaused = false, isMicrophoneEnabled = currentAudioEnabled)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                RecordNotificationHelper.NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(RecordNotificationHelper.NOTIFICATION_ID, initialNotification)
        }

        // 2. Preparar archivo de destino
        val outputFile = RecordStorageHelper.prepareOutputFile(this)
        currentActiveFile = outputFile

        // 3. Arrancar motor de captura
        val started = captureEngine.startCapture(
            resultCode = resultCode,
            resultData = resultData,
            width = recWidth,
            height = recHeight,
            densityDpi = metrics.densityDpi,
            fps = fps,
            bitrate = bitrate,
            audioSource = audioSource,
            outputFile = outputFile,
            onError = { errorMsg ->
                _errorMessage.value = errorMsg
                _recordingState.value = RecordingStatus.ERROR
                cleanupAndStop()
            }
        )

        if (started) {
            _recordingState.value = RecordingStatus.RECORDING
            _elapsedSeconds.value = 0
            _errorMessage.value = null

            // 4. Activar burbuja flotante opcional con submenú de herramientas
            if (showFloatingBubble) {
                floatingBubbleManager?.dismiss()
                floatingBubbleManager = FloatingBubbleManager(
                    context = this,
                    onPauseClicked = { handlePauseAction() },
                    onResumeClicked = { handleResumeAction() },
                    onStopClicked = { handleStopAction() },
                    onScreenshotRequested = { handleScreenshotAction() }
                ).apply { show() }
            }

            startChronometerTimer()
        } else {
            cleanupAndStop()
        }
    }

    private fun handlePauseAction() {
        if (captureEngine.pauseCapture()) {
            _recordingState.value = RecordingStatus.PAUSED
            floatingBubbleManager?.updateStatus(true)
            notificationHelper.updateNotification(_elapsedSeconds.value.toLong(), isPaused = true, isMicrophoneEnabled = currentAudioEnabled)
        }
    }

    private fun handleResumeAction() {
        if (captureEngine.resumeCapture()) {
            _recordingState.value = RecordingStatus.RECORDING
            floatingBubbleManager?.updateStatus(false)
            notificationHelper.updateNotification(_elapsedSeconds.value.toLong(), isPaused = false, isMicrophoneEnabled = currentAudioEnabled)
        }
    }

    private fun handleScreenshotAction() {
        if (captureEngine.activeProjection != null) {
            captureEngine.takeScreenshot(
                context = this,
                width = currentRecWidth,
                height = currentRecHeight,
                densityDpi = currentDensityDpi,
                onSuccess = { shotFile ->
                    Log.i(TAG, "Screenshot tomado exitosamente con ImageReader: ${shotFile.absolutePath}")
                },
                onError = { err ->
                    Log.w(TAG, "Fallo al capturar con ImageReader, intentando fallback: $err")
                    currentActiveFile?.let { videoFile ->
                        ScreenshotHelper.captureFrameFromVideo(
                            context = this,
                            videoFile = videoFile,
                            onSuccess = { f -> Log.i(TAG, "Screenshot fallback OK: ${f.absolutePath}") },
                            onError = { e -> Log.e(TAG, "Screenshot fallback falló: $e") }
                        )
                    }
                }
            )
        } else {
            currentActiveFile?.let { videoFile ->
                ScreenshotHelper.captureFrameFromVideo(
                    context = this,
                    videoFile = videoFile,
                    onSuccess = { shotFile ->
                        Log.i(TAG, "Screenshot tomado desde video: ${shotFile.absolutePath}")
                    },
                    onError = { err ->
                        Log.w(TAG, "Screenshot no capturado: $err")
                    }
                )
            }
        }
    }

    private fun handleStopAction() {
        _recordingState.value = RecordingStatus.SAVING
        timerJob?.cancel()

        floatingBubbleManager?.dismiss()
        floatingBubbleManager = null

        val savedFile = captureEngine.stopCapture()

        if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
            _lastSavedFilePath.value = savedFile.absolutePath
            RecordStorageHelper.scanFileToMediaStore(this, savedFile)
            Log.i(TAG, "Grabación finalizada y guardada en: ${savedFile.absolutePath}")
        } else {
            savedFile?.delete()
            _errorMessage.value = "La grabación se canceló o no generó contenido válido."
            Log.w(TAG, "Archivo de grabación vacío o nulo")
        }

        cleanupAndStop()
    }

    private fun startChronometerTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (captureEngine.isRecording && !captureEngine.isPaused) {
                    _elapsedSeconds.value += 1
                    val currentSec = _elapsedSeconds.value
                    floatingBubbleManager?.updateTime(currentSec)
                    notificationHelper.updateNotification(currentSec.toLong(), isPaused = false, isMicrophoneEnabled = currentAudioEnabled)
                }
            }
        }
    }

    private fun cleanupAndStop() {
        timerJob?.cancel()
        floatingBubbleManager?.dismiss()
        floatingBubbleManager = null
        captureEngine.release()
        _recordingState.value = RecordingStatus.IDLE
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cleanupAndStop()
        super.onDestroy()
    }
}
