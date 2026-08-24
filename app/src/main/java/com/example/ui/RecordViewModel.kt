package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InstalledAppItem
import com.example.data.InstalledGamesHelper
import com.example.data.RecordingsRepository
import com.example.data.SettingsRepository
import com.example.data.StorageMonitorHelper
import com.example.data.StorageSpaceInfo
import com.example.model.AudioSampleRate
import com.example.model.AudioSourceType
import com.example.model.FacecamFps
import com.example.model.FacecamShape
import com.example.model.FacecamSize
import com.example.model.ImageFormatOption
import com.example.model.RecordedVideo
import com.example.model.RecordingConfig
import com.example.model.RecordingStatus
import com.example.model.SceneOverlayType
import com.example.model.TouchColorOption
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.model.VtuberPreset
import com.example.model.VtuberSize
import com.example.model.WatermarkSize
import com.example.model.WatermarkType
import com.example.service.ScreenRecordService
import com.example.ui.delegates.SettingsActionsDelegate
import com.example.ui.delegates.VideoGalleryDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val config: RecordingConfig = RecordingConfig(),
    val status: RecordingStatus = RecordingStatus.IDLE,
    val elapsedSeconds: Int = 0,
    val countdownNumber: Int = 0,
    val isCountingDown: Boolean = false,
    val storageInfo: StorageSpaceInfo = StorageSpaceInfo(),
    val videos: List<RecordedVideo> = emptyList(),
    val isLoadingVideos: Boolean = false,
    val selectedVideoForPlay: RecordedVideo? = null,
    val selectedVideoForEdit: RecordedVideo? = null,
    val installedGames: List<InstalledAppItem> = emptyList(),
    val isLoadingGames: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val activeTab: Int = 0, // 0: Grabar, 1: Galería, 2: Juegos, 3: Ajustes
    val isOnboardingCompleted: Boolean = true
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * ViewModel modular y reactivo para la pantalla principal de Vortex Studio.
 * Desacopla la lógica en delegados especializados:
 * - [VideoGalleryDelegate]: Carga, eliminación, renombrado, reproducción y recorte de grabaciones.
 * - [SettingsActionsDelegate]: Persistencia de ajustes audiovisuales, overlays, compresión y avatares 2D.
 * - [RecordCountdownManager]: Temporizador de cuenta regresiva previo al inicio.
 * - [RecordServiceLauncher]: Despacho seguro de comandos hacia el servicio en primer plano.
 */
class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecordingsRepository(application)
    private val gamesHelper = InstalledGamesHelper(application)
    private val settingsRepository = SettingsRepository(application)
    private val countdownManager = RecordCountdownManager(application, viewModelScope)

    private val _infoMessage = MutableStateFlow<String?>(null)
    private val _activeTab = MutableStateFlow(0)
    private val _installedGames = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    private val _isLoadingGames = MutableStateFlow(false)

    // Delegados modulares
    val galleryDelegate = VideoGalleryDelegate(
        context = application,
        repository = repository,
        scope = viewModelScope,
        onMessageEmitted = { msg -> _infoMessage.value = msg }
    )

    val settingsDelegate = SettingsActionsDelegate(
        context = application,
        settingsRepository = settingsRepository,
        scope = viewModelScope
    )

    private val _config = MutableStateFlow(settingsRepository.getConfig())
    private val _storageInfo = MutableStateFlow(
        StorageMonitorHelper.queryStorageInfo(application, _config.value.bitrate.bps)
    )

    private var pendingLaunchGamePackage: String? = null

    val uiState = combine(
        combine(_config, ScreenRecordService.recordingState, ScreenRecordService.elapsedSeconds, countdownManager.countdownNumber) { config, state, elapsed, cd ->
            Quadruple(config, state, elapsed, cd)
        },
        combine(
            countdownManager.isCountingDown,
            _storageInfo,
            galleryDelegate.videos,
            combine(galleryDelegate.isLoadingVideos, galleryDelegate.selectedVideoForPlay, galleryDelegate.selectedVideoForEdit) { lVids, play, edit -> Triple(lVids, play, edit) }
        ) { isCd, storage, vids, extra ->
            Quadruple(isCd, storage, vids, extra)
        },
        combine(_installedGames, _isLoadingGames, ScreenRecordService.errorMessage, combine(_infoMessage, _activeTab, settingsRepository.onboardingCompletedFlow) { info, tab, onboarded -> Triple(info, tab, onboarded) }) { games, loadingGames, err, infoTabOnboard ->
            Quadruple(games, loadingGames, err, infoTabOnboard)
        }
    ) { group1, group2, group3 ->
        val (config, serviceState, elapsed, countdown) = group1
        val (isCountingDown, storage, videos, extra) = group2
        val (isLoadingVideos, selectedVideo, selectedVideoForEdit) = extra
        val (games, loadingGames, serviceError, infoTabOnboard) = group3
        val (infoMessage, activeTab, isOnboarded) = infoTabOnboard

        val effectiveStatus = if (isCountingDown) RecordingStatus.COUNTDOWN else serviceState

        UiState(
            config = config,
            status = effectiveStatus,
            elapsedSeconds = elapsed,
            countdownNumber = countdown,
            isCountingDown = isCountingDown,
            storageInfo = storage,
            videos = videos,
            isLoadingVideos = isLoadingVideos,
            selectedVideoForPlay = selectedVideo,
            selectedVideoForEdit = selectedVideoForEdit,
            installedGames = games,
            isLoadingGames = loadingGames,
            errorMessage = serviceError,
            infoMessage = infoMessage,
            activeTab = activeTab,
            isOnboardingCompleted = isOnboarded
        )
    }

    init {
        loadVideos()
        loadInstalledGames()
        refreshStorageInfo()

        // Sincronizar reactivamente los cambios en la configuración persistida
        viewModelScope.launch {
            settingsRepository.configFlow.collect { persistentConfig ->
                _config.value = persistentConfig
                refreshStorageInfo()
            }
        }

        viewModelScope.launch {
            ScreenRecordService.lastSavedFilePath.collect { savedPath ->
                if (savedPath != null) {
                    loadVideos()
                    refreshStorageInfo()
                    _infoMessage.value = "¡Grabación guardada con éxito!"
                }
            }
        }
    }

    fun refreshStorageInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = StorageMonitorHelper.queryStorageInfo(
                getApplication(),
                _config.value.bitrate.bps
            )
            _storageInfo.value = info
        }
    }

    fun setActiveTab(tabIndex: Int) {
        _activeTab.value = tabIndex
    }

    // Delegaciones de Galería
    fun loadVideos() = galleryDelegate.loadVideos()
    fun deleteVideo(video: RecordedVideo) = galleryDelegate.deleteVideo(video)
    fun renameVideo(video: RecordedVideo, newTitle: String) = galleryDelegate.renameVideo(video, newTitle)
    fun playVideo(video: RecordedVideo) = galleryDelegate.playVideo(video)
    fun closePlayer() = galleryDelegate.closePlayer()
    fun openEditor(video: RecordedVideo) = galleryDelegate.openEditor(video)
    fun closeEditor() = galleryDelegate.closeEditor()
    fun onVideoEdited(file: File) = galleryDelegate.onVideoEdited(file)
    fun onThumbnailExtracted(file: File) = galleryDelegate.onThumbnailExtracted(file)
    fun shareVideo(context: Context, video: RecordedVideo) = galleryDelegate.shareVideo(context, video)

    // Juegos y Lanzamiento
    fun loadInstalledGames() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingGames.value = true
            try {
                _installedGames.value = gamesHelper.getInstalledGamesAndApps()
            } catch (_: Exception) {
            } finally {
                _isLoadingGames.value = false
            }
        }
    }

    fun launchGame(packageName: String) {
        gamesHelper.launchApp(packageName)
    }

    // Delegaciones de Ajustes
    fun updateResolution(resolution: VideoResolution) = settingsDelegate.updateResolution(resolution)
    fun updateFps(fps: VideoFps) = settingsDelegate.updateFps(fps)
    fun updateBitrate(bitrate: VideoBitrate) = settingsDelegate.updateBitrate(bitrate)
    fun updateBitrateMbps(mbps: Int) = settingsDelegate.updateBitrateMbps(mbps)
    fun updateFacecamFps(fps: FacecamFps) = settingsDelegate.updateFacecamFps(fps)
    fun updateAudioSampleRate(sampleRate: AudioSampleRate) = settingsDelegate.updateAudioSampleRate(sampleRate)
    fun updateAudioSource(source: AudioSourceType) = settingsDelegate.updateAudioSource(source)
    fun toggleFloatingVuMeter(enabled: Boolean) = settingsDelegate.toggleFloatingVuMeter(enabled)
    fun updateGameAudioGain(gain: Float) = settingsDelegate.updateGameAudioGain(gain)
    fun updateMicAudioGain(gain: Float) = settingsDelegate.updateMicAudioGain(gain)
    fun toggleNoiseGate(enabled: Boolean) = settingsDelegate.toggleNoiseGate(enabled)
    fun toggleAudioDucking(enabled: Boolean) = settingsDelegate.toggleAudioDucking(enabled)
    fun updateCountdown(seconds: Int) = settingsDelegate.updateCountdown(seconds)
    fun updateImageFormat(format: ImageFormatOption) = settingsDelegate.updateImageFormat(format)
    fun updateImageQuality(quality: Int) = settingsDelegate.updateImageQuality(quality)
    fun toggleWebpLossless(lossless: Boolean) = settingsDelegate.toggleWebpLossless(lossless)
    fun toggleFloatingBubble(enabled: Boolean) = settingsDelegate.toggleFloatingBubble(enabled)
    fun toggleFacecam(enabled: Boolean) = settingsDelegate.toggleFacecam(enabled)
    fun updateFacecamShape(shape: FacecamShape) = settingsDelegate.updateFacecamShape(shape)
    fun updateFacecamSize(size: FacecamSize) = settingsDelegate.updateFacecamSize(size)
    fun toggleFacecamCamera() = settingsDelegate.toggleFacecamCamera()
    fun toggleBeautyFilter(enabled: Boolean) = settingsDelegate.toggleBeautyFilter(enabled)
    fun toggleFacecamRgbBorder(enabled: Boolean) = settingsDelegate.toggleFacecamRgbBorder(enabled)
    fun toggleTouchVisualizer(enabled: Boolean) = settingsDelegate.toggleTouchVisualizer(enabled)
    fun updateTouchVisualizerColor(color: TouchColorOption) = settingsDelegate.updateTouchVisualizerColor(color)
    fun toggleWatermark(enabled: Boolean) = settingsDelegate.toggleWatermark(enabled)
    fun updateWatermarkType(type: WatermarkType) = settingsDelegate.updateWatermarkType(type)
    fun updateWatermarkText(text: String) = settingsDelegate.updateWatermarkText(text)
    fun updateWatermarkOpacity(opacity: Float) = settingsDelegate.updateWatermarkOpacity(opacity)
    fun updateWatermarkSize(size: WatermarkSize) = settingsDelegate.updateWatermarkSize(size)
    fun updateWatermarkColor(color: TouchColorOption) = settingsDelegate.updateWatermarkColor(color)
    fun updateWatermarkImageUri(uri: String?) = settingsDelegate.updateWatermarkImageUri(uri)
    fun toggleSceneOverlay(enabled: Boolean) = settingsDelegate.toggleSceneOverlay(enabled)
    fun updateSceneOverlayType(type: SceneOverlayType) = settingsDelegate.updateSceneOverlayType(type)
    fun updateSceneOverlayText(text: String) = settingsDelegate.updateSceneOverlayText(text)
    fun updateSceneOverlayOpacity(opacity: Float) = settingsDelegate.updateSceneOverlayOpacity(opacity)
    fun updateSceneOverlayImageUri(uri: String?) = settingsDelegate.updateSceneOverlayImageUri(uri)
    fun toggleVtuber(enabled: Boolean) = settingsDelegate.toggleVtuber(enabled)
    fun updateVtuberPreset(preset: VtuberPreset) = settingsDelegate.updateVtuberPreset(preset)
    fun updateVtuberSize(size: VtuberSize) = settingsDelegate.updateVtuberSize(size)
    fun updateVtuberSensitivity(sensitivity: Float) = settingsDelegate.updateVtuberSensitivity(sensitivity)
    fun toggleVtuberBounce(enabled: Boolean) = settingsDelegate.toggleVtuberBounce(enabled)
    fun updateVtuberIdleImage(uriString: String?) = settingsDelegate.updateVtuberIdleImage(uriString)
    fun updateVtuberTalkImage(uriString: String?) = settingsDelegate.updateVtuberTalkImage(uriString)
    fun updateVtuberBlinkImage(uriString: String?) = settingsDelegate.updateVtuberBlinkImage(uriString)
    fun updateVtuberBlinkTalkImage(uriString: String?) = settingsDelegate.updateVtuberBlinkTalkImage(uriString)
    fun toggleGameMode(enabled: Boolean) = settingsDelegate.toggleGameMode(enabled)

    // Grabación y Control de Flujo
    fun startRecordingFlow(
        resultCode: Int,
        resultData: Intent,
        targetGamePackage: String? = null
    ) {
        val config = _config.value
        val countdown = config.countdownSeconds
        pendingLaunchGamePackage = targetGamePackage

        if (countdown > 0) {
            countdownManager.startCountdown(countdown) {
                // 1. Iniciar el Foreground Service al completar la cuenta atrás
                RecordServiceLauncher.startService(
                    context = getApplication(),
                    resultCode = resultCode,
                    resultData = resultData,
                    config = _config.value
                )

                // 2. Si se solicitó grabar un juego o aplicación específica, lanzarla ahora
                launchPendingGame()
            }
        } else {
            RecordServiceLauncher.startService(
                context = getApplication(),
                resultCode = resultCode,
                resultData = resultData,
                config = config
            )
            launchPendingGame()
        }
    }

    fun cancelCountdown() {
        countdownManager.cancelCountdown()
        pendingLaunchGamePackage = null
        stopRecording()
    }

    private fun launchPendingGame() {
        pendingLaunchGamePackage?.let { pkg ->
            gamesHelper.launchApp(pkg)
            pendingLaunchGamePackage = null
        }
    }

    fun stopRecording() {
        RecordServiceLauncher.sendAction(getApplication(), ScreenRecordService.ACTION_STOP)
    }

    fun pauseRecording() {
        RecordServiceLauncher.sendAction(getApplication(), ScreenRecordService.ACTION_PAUSE)
    }

    fun resumeRecording() {
        RecordServiceLauncher.sendAction(getApplication(), ScreenRecordService.ACTION_RESUME)
    }

    fun clearInfoMessage() {
        _infoMessage.value = null
    }

    fun completeOnboarding() {
        settingsRepository.setOnboardingCompleted(true)
    }

    fun resetOnboarding() {
        settingsRepository.setOnboardingCompleted(false)
    }
}
