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
import com.example.data.SettingsRepository
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
        const val ACTION_TOGGLE_MIC = "com.example.service.TOGGLE_MIC"
        const val ACTION_TOGGLE_FACECAM = "com.example.service.TOGGLE_FACECAM"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_RES_WIDTH = "extra_res_width"
        const val EXTRA_RES_HEIGHT = "extra_res_height"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_AUDIO_SOURCE = "extra_audio_source"
        const val EXTRA_SHOW_FLOATING_BUBBLE = "extra_show_floating_bubble"
        const val EXTRA_SHOW_FACECAM = "extra_show_facecam"

        private val _recordingState = MutableStateFlow(RecordingStatus.IDLE)
        val recordingState = _recordingState.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0)
        val elapsedSeconds = _elapsedSeconds.asStateFlow()

        private val _isMicMuted = MutableStateFlow(false)
        val isMicMuted = _isMicMuted.asStateFlow()

        private val _isFacecamActive = MutableStateFlow(false)
        val isFacecamActive = _isFacecamActive.asStateFlow()

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
    private var facecamOverlayManager: FacecamOverlayManager? = null

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
        observeSettingsChanges()
    }

    private fun observeSettingsChanges() {
        serviceScope.launch {
            SettingsRepository(this@ScreenRecordService).configFlow.collect { config ->
                if (facecamOverlayManager?.isShowing == true) {
                    facecamOverlayManager?.setShape(config.facecamShape)
                    facecamOverlayManager?.setSize(config.facecamSize)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStartAction(intent)
            ACTION_STOP -> handleStopAction()
            ACTION_PAUSE -> handlePauseAction()
            ACTION_RESUME -> handleResumeAction()
            ACTION_SCREENSHOT -> handleScreenshotAction()
            ACTION_TOGGLE_MIC -> handleToggleMicAction()
            ACTION_TOGGLE_FACECAM -> handleToggleFacecamAction()
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

        // Cargar configuración guardada persistentemente como base y respaldo
        val savedConfig = SettingsRepository(this).getConfig()
        val defaultDims = savedConfig.resolution.getDimensions(isPortrait = true)

        val width = intent.getIntExtra(EXTRA_RES_WIDTH, defaultDims.first)
        val height = intent.getIntExtra(EXTRA_RES_HEIGHT, defaultDims.second)
        val fps = intent.getIntExtra(EXTRA_FPS, savedConfig.fps.fps)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, savedConfig.bitrate.bps)
        val audioSource = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: savedConfig.audioSource.name
        val showFloatingBubble = intent.getBooleanExtra(EXTRA_SHOW_FLOATING_BUBBLE, savedConfig.showFloatingBubble)
        val showFacecam = intent.getBooleanExtra(EXTRA_SHOW_FACECAM, savedConfig.showFacecam)

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

        // 1. Iniciar Foreground Service con tipo MEDIA_PROJECTION, MICROPHONE y CAMERA
        val initialNotification = notificationHelper.buildForegroundNotification(0, isPaused = false, isMicrophoneEnabled = currentAudioEnabled)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (currentAudioEnabled) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            if (showFacecam) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            }
            startForeground(
                RecordNotificationHelper.NOTIFICATION_ID,
                initialNotification,
                serviceType
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                Log.e(TAG, "Error en captura: $errorMsg")
                _errorMessage.value = errorMsg
                _recordingState.value = RecordingStatus.ERROR
                cleanupAndStop()
            },
            onSystemStop = {
                Log.w(TAG, "MediaProjection detenido por el sistema")
                handleStopAction()
            }
        )

        if (started) {
            _recordingState.value = RecordingStatus.RECORDING
            _elapsedSeconds.value = 0
            _errorMessage.value = null
            _isMicMuted.value = captureEngine.isMicrophoneMuted

            // 4. Activar Facecam si estaba solicitado
            if (showFacecam) {
                launchFacecam(savedConfig)
            } else {
                _isFacecamActive.value = false
            }

            // 5. Activar burbuja flotante opcional con submenú de herramientas, Facecam y botón de voz
            if (showFloatingBubble) {
                floatingBubbleManager?.dismiss()
                floatingBubbleManager = FloatingBubbleManager(
                    context = this,
                    onPauseClicked = { handlePauseAction() },
                    onResumeClicked = { handleResumeAction() },
                    onStopClicked = { handleStopAction() },
                    onMicToggleClicked = { handleToggleMicAction() },
                    onScreenshotRequested = { handleScreenshotAction() },
                    onFacecamToggleClicked = { handleToggleFacecamAction() }
                ).apply {
                    show()
                    updateMicStatus(captureEngine.isMicrophoneMuted)
                    updateFacecamStatus(_isFacecamActive.value)
                }
            }

            startChronometerTimer()
        } else {
            cleanupAndStop()
        }
    }

    private fun launchFacecam(config: com.example.model.RecordingConfig) {
        facecamOverlayManager?.dismiss()
        facecamOverlayManager = FacecamOverlayManager(
            context = this,
            shape = config.facecamShape,
            size = config.facecamSize,
            isFrontCamera = config.isFrontCamera,
            onCloseClicked = {
                _isFacecamActive.value = false
                floatingBubbleManager?.updateFacecamStatus(false)
                SettingsRepository(this).toggleFacecam(false)
            },
            onShapeChanged = { newShape ->
                SettingsRepository(this).updateFacecamShape(newShape)
            },
            onCameraFlipped = { isFront ->
                SettingsRepository(this).setFacecamCamera(isFront)
            }
        ).apply {
            show()
        }
        _isFacecamActive.value = facecamOverlayManager?.isShowing == true
        floatingBubbleManager?.updateFacecamStatus(_isFacecamActive.value)
    }

    private fun handleToggleFacecamAction() {
        if (facecamOverlayManager?.isShowing == true) {
            facecamOverlayManager?.dismiss()
            facecamOverlayManager = null
            _isFacecamActive.value = false
            floatingBubbleManager?.updateFacecamStatus(false)
            SettingsRepository(this).toggleFacecam(false)
            Log.i(TAG, "Facecam desactivada desde la burbuja flotante")
        } else {
            val config = SettingsRepository(this).getConfig()
            launchFacecam(config)
            SettingsRepository(this).toggleFacecam(true)
            Log.i(TAG, "Facecam activada desde la burbuja flotante")
        }
    }

    private fun handleToggleMicAction() {
        val newMuted = captureEngine.toggleMicrophoneMuted()
        _isMicMuted.value = newMuted
        floatingBubbleManager?.updateMicStatus(newMuted)
        notificationHelper.updateNotification(
            _elapsedSeconds.value.toLong(),
            isPaused = captureEngine.isPaused,
            isMicrophoneEnabled = !newMuted
        )
        Log.i(TAG, "Conmutador de micrófono ejecutado: ${if (newMuted) "Silenciado (Solo Juego)" else "Activo (Juego + Voz)"}")
    }

    private fun handlePauseAction() {
        if (captureEngine.pauseCapture()) {
            _recordingState.value = RecordingStatus.PAUSED
            floatingBubbleManager?.updateStatus(true)
            notificationHelper.updateNotification(
                _elapsedSeconds.value.toLong(),
                isPaused = true,
                isMicrophoneEnabled = !captureEngine.isMicrophoneMuted
            )
        }
    }

    private fun handleResumeAction() {
        if (captureEngine.resumeCapture()) {
            _recordingState.value = RecordingStatus.RECORDING
            floatingBubbleManager?.updateStatus(false)
            notificationHelper.updateNotification(
                _elapsedSeconds.value.toLong(),
                isPaused = false,
                isMicrophoneEnabled = !captureEngine.isMicrophoneMuted
            )
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
                    notificationHelper.updateNotification(currentSec.toLong(), isPaused = false, isMicrophoneEnabled = !captureEngine.isMicrophoneMuted)
                }
            }
        }
    }

    private fun cleanupAndStop() {
        timerJob?.cancel()
        floatingBubbleManager?.dismiss()
        floatingBubbleManager = null
        facecamOverlayManager?.dismiss()
        facecamOverlayManager = null
        _isFacecamActive.value = false
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
