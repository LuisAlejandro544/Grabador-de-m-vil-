package com.example.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.data.SettingsRepository
import com.example.model.AudioSourceType
import com.example.model.RecordingStatus
import com.example.service.dispatcher.OverlayActionCallbacks
import com.example.service.dispatcher.ServiceActionDispatcher
import com.example.service.overlay.ServiceOverlayCoordinator
import com.example.service.receiver.ServiceEmergencyReceiver
import com.example.service.timer.ServiceChronometerTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Servicio en primer plano modular y desacoplado para la grabación de pantalla.
 * Coordina:
 * - [ScreenCaptureEngine]: Motor de codificación y captura multimedia.
 * - [ServiceChronometerTimer]: Cronómetro en tiempo real y salvaguarda de espacio en disco.
 * - [ServiceEmergencyReceiver]: Manejador de eventos de batería y almacenamiento crítico.
 * - [ServiceActionDispatcher]: Enrutamiento y lanzamiento de tipos de Foreground Service y Overlays.
 * - [ServiceOverlayCoordinator]: Coordinador de interfaces flotantes y vistas en pantalla.
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
        const val ACTION_TOGGLE_BEAUTY = "com.example.service.TOGGLE_BEAUTY"
        const val ACTION_TOGGLE_RGB = "com.example.service.TOGGLE_RGB"
        const val ACTION_TOGGLE_TOUCH = "com.example.service.TOGGLE_TOUCH"
        const val ACTION_TOGGLE_WATERMARK = "com.example.service.TOGGLE_WATERMARK"
        const val ACTION_TOGGLE_SCENE_OVERLAY = "com.example.service.TOGGLE_SCENE_OVERLAY"
        const val ACTION_TOGGLE_VTUBER = "com.example.service.TOGGLE_VTUBER"
        const val ACTION_TOGGLE_VUMETER = "com.example.service.TOGGLE_VUMETER"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_RES_WIDTH = "extra_res_width"
        const val EXTRA_RES_HEIGHT = "extra_res_height"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_AUDIO_SOURCE = "extra_audio_source"
        const val EXTRA_SAMPLE_RATE = "extra_sample_rate"
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

        private val _isVtuberActive = MutableStateFlow(false)
        val isVtuberActive = _isVtuberActive.asStateFlow()

        private val _isVuMeterActive = MutableStateFlow(false)
        val isVuMeterActive = _isVuMeterActive.asStateFlow()

        private val _isBeautyActive = MutableStateFlow(false)
        val isBeautyActive = _isBeautyActive.asStateFlow()

        private val _isRgbActive = MutableStateFlow(false)
        val isRgbActive = _isRgbActive.asStateFlow()

        private val _isTouchActive = MutableStateFlow(false)
        val isTouchActive = _isTouchActive.asStateFlow()

        private val _isWatermarkActive = MutableStateFlow(false)
        val isWatermarkActive = _isWatermarkActive.asStateFlow()

        private val _isSceneOverlayActive = MutableStateFlow(false)
        val isSceneOverlayActive = _isSceneOverlayActive.asStateFlow()

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
    private lateinit var overlayCoordinator: ServiceOverlayCoordinator
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var emergencyReceiver: ServiceEmergencyReceiver
    private lateinit var chronometerTimer: ServiceChronometerTimer
    private lateinit var actionDispatcher: ServiceActionDispatcher

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var currentActiveFile: File? = null
    private var currentRecWidth = 1080
    private var currentRecHeight = 1920
    private var currentDensityDpi = 480

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        captureEngine = ScreenCaptureEngine(this)
        notificationHelper = RecordNotificationHelper(this)
        settingsRepository = SettingsRepository(this)

        overlayCoordinator = ServiceOverlayCoordinator(
            context = this,
            settingsRepository = settingsRepository,
            onFacecamStateChanged = { _isFacecamActive.value = it },
            onBeautyStateChanged = { _isBeautyActive.value = it },
            onRgbStateChanged = { _isRgbActive.value = it },
            onTouchStateChanged = { _isTouchActive.value = it },
            onWatermarkStateChanged = { _isWatermarkActive.value = it },
            onSceneOverlayStateChanged = { _isSceneOverlayActive.value = it },
            onVtuberStateChanged = { _isVtuberActive.value = it },
            onVuMeterStateChanged = { _isVuMeterActive.value = it },
            onAudioGainsChanged = { gameGain, micGain ->
                captureEngine.setAudioGains(gameGain, micGain)
            },
            onAudioFiltersChanged = { noiseGate, ducking ->
                captureEngine.setAudioFilters(noiseGate, ducking)
            },
            onMicMuteToggled = { handleToggleMicAction() },
            isMicMutedProvider = { captureEngine.isMicrophoneMuted },
            audioLevelsProvider = { captureEngine.getAudioLevels() }
        )

        actionDispatcher = ServiceActionDispatcher(
            service = this,
            captureEngine = captureEngine,
            overlayCoordinator = overlayCoordinator,
            notificationHelper = notificationHelper
        )

        chronometerTimer = ServiceChronometerTimer(
            context = this,
            scope = serviceScope,
            captureEngine = captureEngine,
            overlayCoordinator = overlayCoordinator,
            notificationHelper = notificationHelper,
            settingsRepository = settingsRepository,
            elapsedSecondsFlow = _elapsedSeconds,
            onEmergencyStorageStop = { errorMsg ->
                _errorMessage.value = errorMsg
                handleStopAction()
            }
        )

        emergencyReceiver = ServiceEmergencyReceiver(
            onEmergencyBatteryLow = {
                _errorMessage.value = "Grabación salvaguardada por batería baja del dispositivo."
                handleStopAction()
            },
            onEmergencyStorageLow = {
                _errorMessage.value = "Grabación salvaguardada por falta de espacio en almacenamiento."
                handleStopAction()
            }
        )

        observeSettingsChanges()
        emergencyReceiver.register(this)
    }

    private fun observeSettingsChanges() {
        serviceScope.launch {
            settingsRepository.configFlow.collect { config ->
                overlayCoordinator.onConfigUpdated(config)
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
            ACTION_TOGGLE_FACECAM -> overlayCoordinator.toggleFacecam()
            ACTION_TOGGLE_BEAUTY -> overlayCoordinator.toggleBeauty()
            ACTION_TOGGLE_RGB -> overlayCoordinator.toggleRgbBorder()
            ACTION_TOGGLE_TOUCH -> overlayCoordinator.toggleTouchVisualizer()
            ACTION_TOGGLE_WATERMARK -> overlayCoordinator.toggleWatermark()
            ACTION_TOGGLE_SCENE_OVERLAY -> overlayCoordinator.toggleSceneOverlay()
            ACTION_TOGGLE_VTUBER -> overlayCoordinator.toggleVtuber()
            ACTION_TOGGLE_VUMETER -> overlayCoordinator.toggleVuMeter()
        }
        return START_NOT_STICKY
    }

    private fun handleStartAction(intent: Intent) {
        val params = ServiceParamsExtractor.extractParams(this, intent)
        if (params == null) {
            Log.e(TAG, "Parámetros de proyección inválidos")
            stopSelf()
            return
        }

        currentRecWidth = params.width
        currentRecHeight = params.height
        currentDensityDpi = params.densityDpi
        val isAudioEnabled = params.audioSource != AudioSourceType.NONE.name

        // 1. Iniciar Foreground Service con tipos específicos (Android 14+)
        val initialNotification = notificationHelper.buildForegroundNotification(
            0,
            isPaused = false,
            isMicrophoneEnabled = isAudioEnabled
        )
        actionDispatcher.startForegroundWithType(initialNotification, isAudioEnabled, params.showFacecam)

        // 2. Preparar archivo de destino
        val outputFile = RecordStorageHelper.prepareOutputFile(this)
        currentActiveFile = outputFile

        // 3. Arrancar motor de captura
        val started = captureEngine.startCapture(
            resultCode = params.resultCode,
            resultData = params.resultData,
            width = params.width,
            height = params.height,
            densityDpi = params.densityDpi,
            fps = params.fps,
            bitrate = params.bitrate,
            audioSource = params.audioSource,
            sampleRate = params.sampleRate,
            outputFile = outputFile,
            onAudioAmplitude = { amp -> overlayCoordinator.onAudioAmplitude(amp) },
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

            // 4. Lanzar interfaces y overlays configurados
            actionDispatcher.launchActiveOverlays(
                params = params,
                onActionCallbacks = OverlayActionCallbacks(
                    onPause = { handlePauseAction() },
                    onResume = { handleResumeAction() },
                    onStop = { handleStopAction() },
                    onToggleMic = { handleToggleMicAction() },
                    onScreenshot = { handleScreenshotAction() }
                )
            )

            chronometerTimer.start()
        } else {
            cleanupAndStop()
        }
    }

    private fun handleToggleMicAction() {
        val newMuted = captureEngine.toggleMicrophoneMuted()
        _isMicMuted.value = newMuted
        overlayCoordinator.updateBubbleMicStatus(newMuted)
        notificationHelper.updateNotification(
            _elapsedSeconds.value.toLong(),
            isPaused = captureEngine.isPaused,
            isMicrophoneEnabled = !newMuted
        )
        Log.i(TAG, "Micrófono conmutado: ${if (newMuted) "Silenciado" else "Activo"}")
    }

    private fun handlePauseAction() {
        if (captureEngine.pauseCapture()) {
            _recordingState.value = RecordingStatus.PAUSED
            overlayCoordinator.updateBubbleStatus(isPaused = true)
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
            overlayCoordinator.updateBubbleStatus(isPaused = false)
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
                    Log.i(TAG, "Screenshot tomado exitosamente: ${shotFile.absolutePath}")
                },
                onError = { err ->
                    Log.w(TAG, "Fallo ImageReader, intentando fallback: $err")
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
                    onSuccess = { shotFile -> Log.i(TAG, "Screenshot tomado desde video: ${shotFile.absolutePath}") },
                    onError = { err -> Log.w(TAG, "Screenshot no capturado: $err") }
                )
            }
        }
    }

    private fun handleStopAction() {
        _recordingState.value = RecordingStatus.SAVING
        chronometerTimer.stop()
        overlayCoordinator.dismissAll()

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

    private fun cleanupAndStop() {
        chronometerTimer.stop()
        emergencyReceiver.unregister(this)
        if (::overlayCoordinator.isInitialized) {
            overlayCoordinator.dismissAll()
        }
        if (::captureEngine.isInitialized) {
            captureEngine.release()
        }
        _recordingState.value = RecordingStatus.IDLE
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "onTaskRemoved invocado: salvaguardando grabación activa...")
        if (isRecording()) {
            handleStopAction()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            Log.w(TAG, "onTrimMemory crítico ($level): protegiendo búferes de grabación")
        }
    }

    override fun onDestroy() {
        emergencyReceiver.unregister(this)
        if (isRecording()) {
            handleStopAction()
        } else {
            cleanupAndStop()
        }
        super.onDestroy()
    }
}
