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
import com.example.service.state.ServiceStateManager
import com.example.service.timer.ServiceChronometerTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Servicio en primer plano modular y desacoplado para la grabación de pantalla.
 * Coordina:
 * - [ScreenCaptureEngine]: Motor de codificación y captura multimedia.
 * - [ServiceStateManager]: Gestor centralizado del estado reactivo del servicio.
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

        // Delegación transparente hacia ServiceStateManager para retrocompatibilidad total
        val recordingState: StateFlow<RecordingStatus> get() = ServiceStateManager.recordingState
        val elapsedSeconds: StateFlow<Int> get() = ServiceStateManager.elapsedSeconds
        val isMicMuted: StateFlow<Boolean> get() = ServiceStateManager.isMicMuted
        val isFacecamActive: StateFlow<Boolean> get() = ServiceStateManager.isFacecamActive
        val isVtuberActive: StateFlow<Boolean> get() = ServiceStateManager.isVtuberActive
        val isVuMeterActive: StateFlow<Boolean> get() = ServiceStateManager.isVuMeterActive
        val isBeautyActive: StateFlow<Boolean> get() = ServiceStateManager.isBeautyActive
        val isRgbActive: StateFlow<Boolean> get() = ServiceStateManager.isRgbActive
        val isTouchActive: StateFlow<Boolean> get() = ServiceStateManager.isTouchActive
        val isWatermarkActive: StateFlow<Boolean> get() = ServiceStateManager.isWatermarkActive
        val isSceneOverlayActive: StateFlow<Boolean> get() = ServiceStateManager.isSceneOverlayActive
        val lastSavedFilePath: StateFlow<String?> get() = ServiceStateManager.lastSavedFilePath
        val errorMessage: StateFlow<String?> get() = ServiceStateManager.errorMessage

        fun isRecording(): Boolean = ServiceStateManager.isRecording()
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
            onFacecamStateChanged = { ServiceStateManager.setFacecamActive(it) },
            onBeautyStateChanged = { ServiceStateManager.setBeautyActive(it) },
            onRgbStateChanged = { ServiceStateManager.setRgbActive(it) },
            onTouchStateChanged = { ServiceStateManager.setTouchActive(it) },
            onWatermarkStateChanged = { ServiceStateManager.setWatermarkActive(it) },
            onSceneOverlayStateChanged = { ServiceStateManager.setSceneOverlayActive(it) },
            onVtuberStateChanged = { ServiceStateManager.setVtuberActive(it) },
            onVuMeterStateChanged = { ServiceStateManager.setVuMeterActive(it) },
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
            elapsedSecondsFlow = ServiceStateManager.elapsedSecondsMutableFlow,
            onEmergencyStorageStop = { errorMsg ->
                ServiceStateManager.setErrorMessage(errorMsg)
                handleStopAction()
            }
        )

        emergencyReceiver = ServiceEmergencyReceiver(
            onEmergencyBatteryLow = {
                ServiceStateManager.setErrorMessage("Grabación salvaguardada por batería baja del dispositivo.")
                handleStopAction()
            },
            onEmergencyStorageLow = {
                ServiceStateManager.setErrorMessage("Grabación salvaguardada por falta de espacio en almacenamiento.")
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
                ServiceStateManager.setErrorMessage(errorMsg)
                ServiceStateManager.setRecordingState(RecordingStatus.ERROR)
                cleanupAndStop()
            },
            onSystemStop = {
                Log.w(TAG, "MediaProjection detenido por el sistema")
                handleStopAction()
            }
        )

        if (started) {
            ServiceStateManager.setRecordingState(RecordingStatus.RECORDING)
            ServiceStateManager.setElapsedSeconds(0)
            ServiceStateManager.setErrorMessage(null)
            ServiceStateManager.setMicMuted(captureEngine.isMicrophoneMuted)

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
        ServiceStateManager.setMicMuted(newMuted)
        overlayCoordinator.updateBubbleMicStatus(newMuted)
        notificationHelper.updateNotification(
            ServiceStateManager.elapsedSeconds.value.toLong(),
            isPaused = captureEngine.isPaused,
            isMicrophoneEnabled = !newMuted
        )
        Log.i(TAG, "Micrófono conmutado: ${if (newMuted) "Silenciado" else "Activo"}")
    }

    private fun handlePauseAction() {
        if (captureEngine.pauseCapture()) {
            ServiceStateManager.setRecordingState(RecordingStatus.PAUSED)
            overlayCoordinator.updateBubbleStatus(isPaused = true)
            notificationHelper.updateNotification(
                ServiceStateManager.elapsedSeconds.value.toLong(),
                isPaused = true,
                isMicrophoneEnabled = !captureEngine.isMicrophoneMuted
            )
        }
    }

    private fun handleResumeAction() {
        if (captureEngine.resumeCapture()) {
            ServiceStateManager.setRecordingState(RecordingStatus.RECORDING)
            overlayCoordinator.updateBubbleStatus(isPaused = false)
            notificationHelper.updateNotification(
                ServiceStateManager.elapsedSeconds.value.toLong(),
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
        ServiceStateManager.setRecordingState(RecordingStatus.SAVING)
        chronometerTimer.stop()
        overlayCoordinator.dismissAll()

        val savedFile = captureEngine.stopCapture()

        if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
            ServiceStateManager.setLastSavedFilePath(savedFile.absolutePath)
            RecordStorageHelper.scanFileToMediaStore(this, savedFile)
            Log.i(TAG, "Grabación finalizada y guardada en: ${savedFile.absolutePath}")
        } else {
            savedFile?.delete()
            ServiceStateManager.setErrorMessage("La grabación se canceló o no generó contenido válido.")
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
        ServiceStateManager.reset()
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

