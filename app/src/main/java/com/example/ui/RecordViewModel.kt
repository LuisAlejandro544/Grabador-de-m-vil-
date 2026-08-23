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
import com.example.model.RecordedVideo
import com.example.model.RecordingConfig
import com.example.model.RecordingStatus
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.service.ScreenRecordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

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
    val activeTab: Int = 0 // 0: Grabar, 1: Galería, 2: Ajustes, 3: Juegos
)

/**
 * ViewModel modular y reactivo para la pantalla principal de Vortex Studio.
 * Desacopla la cuenta atrás en [RecordCountdownManager] y los comandos del servicio en [RecordServiceLauncher].
 */
class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecordingsRepository(application)
    private val gamesHelper = InstalledGamesHelper(application)
    private val settingsRepository = SettingsRepository(application)
    private val countdownManager = RecordCountdownManager(application, viewModelScope)

    private val _config = MutableStateFlow(settingsRepository.getConfig())
    private val _storageInfo = MutableStateFlow(
        StorageMonitorHelper.queryStorageInfo(application, _config.value.bitrate.bps)
    )
    private val _videos = MutableStateFlow<List<RecordedVideo>>(emptyList())
    private val _isLoadingVideos = MutableStateFlow(false)
    private val _selectedVideoForPlay = MutableStateFlow<RecordedVideo?>(null)
    private val _selectedVideoForEdit = MutableStateFlow<RecordedVideo?>(null)
    private val _installedGames = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    private val _isLoadingGames = MutableStateFlow(false)
    private val _activeTab = MutableStateFlow(0)
    private val _infoMessage = MutableStateFlow<String?>(null)

    private var pendingLaunchGamePackage: String? = null

    val uiState = combine(
        combine(_config, ScreenRecordService.recordingState, ScreenRecordService.elapsedSeconds, countdownManager.countdownNumber) { config, state, elapsed, cd ->
            Quadruple(config, state, elapsed, cd)
        },
        combine(countdownManager.isCountingDown, _storageInfo, _videos, combine(_isLoadingVideos, _selectedVideoForPlay, _selectedVideoForEdit) { lVids, play, edit -> Triple(lVids, play, edit) }) { isCd, storage, vids, extra ->
            Quadruple(isCd, storage, vids, extra)
        },
        combine(_installedGames, _isLoadingGames, ScreenRecordService.errorMessage, combine(_infoMessage, _activeTab) { info, tab -> Pair(info, tab) }) { games, loadingGames, err, infoTab ->
            Quadruple(games, loadingGames, err, infoTab)
        }
    ) { group1, group2, group3 ->
        val (config, serviceState, elapsed, countdown) = group1
        val (isCountingDown, storage, videos, extra) = group2
        val (isLoadingVideos, selectedVideo, selectedVideoForEdit) = extra
        val (games, loadingGames, serviceError, infoTab) = group3
        val (infoMessage, activeTab) = infoTab

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
            activeTab = activeTab
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

    fun loadVideos() {
        viewModelScope.launch {
            _isLoadingVideos.value = true
            try {
                _videos.value = repository.loadSavedRecordings()
            } catch (_: Exception) {
            } finally {
                _isLoadingVideos.value = false
            }
        }
    }

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

    fun updateResolution(resolution: VideoResolution) {
        settingsRepository.updateResolution(resolution)
    }

    fun updateFps(fps: VideoFps) {
        settingsRepository.updateFps(fps)
    }

    fun updateBitrate(bitrate: VideoBitrate) {
        settingsRepository.updateBitrate(bitrate)
    }

    fun updateBitrateMbps(mbps: Int) {
        settingsRepository.updateBitrateMbps(mbps)
    }

    fun updateFacecamFps(fps: FacecamFps) {
        settingsRepository.updateFacecamFps(fps)
    }

    fun updateAudioSampleRate(sampleRate: AudioSampleRate) {
        settingsRepository.updateAudioSampleRate(sampleRate)
    }

    fun updateAudioSource(source: AudioSourceType) {
        settingsRepository.updateAudioSource(source)
    }

    fun toggleFloatingVuMeter(enabled: Boolean) {
        settingsRepository.toggleFloatingVuMeter(enabled)
    }

    fun updateGameAudioGain(gain: Float) {
        settingsRepository.updateGameAudioGain(gain)
    }

    fun updateMicAudioGain(gain: Float) {
        settingsRepository.updateMicAudioGain(gain)
    }

    fun toggleNoiseGate(enabled: Boolean) {
        settingsRepository.toggleNoiseGate(enabled)
    }

    fun toggleAudioDucking(enabled: Boolean) {
        settingsRepository.toggleAudioDucking(enabled)
    }

    fun updateCountdown(seconds: Int) {
        settingsRepository.updateCountdown(seconds)
    }

    fun updateImageFormat(format: com.example.model.ImageFormatOption) {
        settingsRepository.updateImageFormat(format)
    }

    fun updateImageQuality(quality: Int) {
        settingsRepository.updateImageQuality(quality)
    }

    fun toggleWebpLossless(lossless: Boolean) {
        settingsRepository.toggleImageWebpLossless(lossless)
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        settingsRepository.toggleFloatingBubble(enabled)
    }

    fun toggleFacecam(enabled: Boolean) {
        settingsRepository.toggleFacecam(enabled)
    }

    fun updateFacecamShape(shape: FacecamShape) {
        settingsRepository.updateFacecamShape(shape)
    }

    fun updateFacecamSize(size: FacecamSize) {
        settingsRepository.updateFacecamSize(size)
    }

    fun toggleFacecamCamera() {
        settingsRepository.toggleFacecamCamera()
    }

    fun toggleBeautyFilter(enabled: Boolean) {
        settingsRepository.toggleBeautyFilter(enabled)
    }

    fun toggleFacecamRgbBorder(enabled: Boolean) {
        settingsRepository.toggleFacecamRgbBorder(enabled)
    }

    fun toggleTouchVisualizer(enabled: Boolean) {
        settingsRepository.toggleTouchVisualizer(enabled)
    }

    fun updateTouchVisualizerColor(color: com.example.model.TouchColorOption) {
        settingsRepository.updateTouchVisualizerColor(color)
    }

    fun toggleWatermark(enabled: Boolean) {
        settingsRepository.toggleWatermark(enabled)
    }

    fun updateWatermarkType(type: com.example.model.WatermarkType) {
        settingsRepository.updateWatermarkType(type)
    }

    fun updateWatermarkText(text: String) {
        settingsRepository.updateWatermarkText(text)
    }

    fun updateWatermarkOpacity(opacity: Float) {
        settingsRepository.updateWatermarkOpacity(opacity)
    }

    fun updateWatermarkSize(size: com.example.model.WatermarkSize) {
        settingsRepository.updateWatermarkSize(size)
    }

    fun updateWatermarkColor(color: com.example.model.TouchColorOption) {
        settingsRepository.updateWatermarkColor(color)
    }

    fun updateWatermarkImageUri(uri: String?) {
        settingsRepository.updateWatermarkImageUri(uri)
    }

    fun toggleSceneOverlay(enabled: Boolean) {
        settingsRepository.toggleSceneOverlay(enabled)
    }

    fun updateSceneOverlayType(type: com.example.model.SceneOverlayType) {
        settingsRepository.updateSceneOverlayType(type)
    }

    fun updateSceneOverlayText(text: String) {
        settingsRepository.updateSceneOverlayText(text)
    }

    fun updateSceneOverlayOpacity(opacity: Float) {
        settingsRepository.updateSceneOverlayOpacity(opacity)
    }

    fun updateSceneOverlayImageUri(uri: String?) {
        settingsRepository.updateSceneOverlayImageUri(uri)
    }

    fun toggleVtuber(enabled: Boolean) {
        settingsRepository.toggleVtuber(enabled)
    }

    fun updateVtuberPreset(preset: com.example.model.VtuberPreset) {
        settingsRepository.updateVtuberPreset(preset)
    }

    fun updateVtuberSize(size: com.example.model.VtuberSize) {
        settingsRepository.updateVtuberSize(size)
    }

    fun updateVtuberSensitivity(sensitivity: Float) {
        settingsRepository.updateVtuberSensitivity(sensitivity)
    }

    fun toggleVtuberBounce(enabled: Boolean) {
        settingsRepository.toggleVtuberBounce(enabled)
    }

    fun updateVtuberIdleImage(uriString: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPath = if (!uriString.isNullOrBlank()) {
                val uri = android.net.Uri.parse(uriString)
                if (uri.scheme == "content") {
                    com.example.service.vtuber.VtuberPresetDrawables.saveImageToInternalStorage(
                        getApplication(), uri, "idle"
                    ) ?: uriString
                } else {
                    uriString
                }
            } else {
                null
            }
            settingsRepository.updateVtuberIdleUri(finalPath)
        }
    }

    fun updateVtuberTalkImage(uriString: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPath = if (!uriString.isNullOrBlank()) {
                val uri = android.net.Uri.parse(uriString)
                if (uri.scheme == "content") {
                    com.example.service.vtuber.VtuberPresetDrawables.saveImageToInternalStorage(
                        getApplication(), uri, "talk"
                    ) ?: uriString
                } else {
                    uriString
                }
            } else {
                null
            }
            settingsRepository.updateVtuberTalkUri(finalPath)
        }
    }

    fun updateVtuberBlinkImage(uriString: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPath = if (!uriString.isNullOrBlank()) {
                val uri = android.net.Uri.parse(uriString)
                if (uri.scheme == "content") {
                    com.example.service.vtuber.VtuberPresetDrawables.saveImageToInternalStorage(
                        getApplication(), uri, "blink"
                    ) ?: uriString
                } else {
                    uriString
                }
            } else {
                null
            }
            settingsRepository.updateVtuberBlinkUri(finalPath)
        }
    }

    fun updateVtuberBlinkTalkImage(uriString: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPath = if (!uriString.isNullOrBlank()) {
                val uri = android.net.Uri.parse(uriString)
                if (uri.scheme == "content") {
                    com.example.service.vtuber.VtuberPresetDrawables.saveImageToInternalStorage(
                        getApplication(), uri, "blink_talk"
                    ) ?: uriString
                } else {
                    uriString
                }
            } else {
                null
            }
            settingsRepository.updateVtuberBlinkTalkUri(finalPath)
        }
    }

    fun toggleGameMode(enabled: Boolean) {
        settingsRepository.toggleGameMode(enabled)
    }

    fun startRecordingFlow(
        resultCode: Int,
        resultData: Intent,
        targetGamePackage: String? = null
    ) {
        val config = _config.value
        val countdown = config.countdownSeconds
        pendingLaunchGamePackage = targetGamePackage

        // Iniciar el Foreground Service de inmediato mientras la Activity está en primer plano
        RecordServiceLauncher.startService(
            context = getApplication(),
            resultCode = resultCode,
            resultData = resultData,
            config = config
        )

        countdownManager.startCountdown(countdown) {
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

    fun deleteVideo(video: RecordedVideo) {
        viewModelScope.launch {
            val success = repository.deleteRecording(video.filePath)
            if (success) {
                _infoMessage.value = "Video eliminado"
                loadVideos()
            }
        }
    }

    fun renameVideo(video: RecordedVideo, newTitle: String) {
        viewModelScope.launch {
            val success = repository.renameRecording(video.filePath, newTitle)
            if (success) {
                _infoMessage.value = "Video renombrado"
                loadVideos()
            } else {
                _infoMessage.value = "No se pudo renombrar el archivo"
            }
        }
    }

    fun playVideo(video: RecordedVideo) {
        _selectedVideoForPlay.value = video
    }

    fun closePlayer() {
        _selectedVideoForPlay.value = null
    }

    fun openEditor(video: RecordedVideo) {
        _selectedVideoForPlay.value = null
        _selectedVideoForEdit.value = video
    }

    fun closeEditor() {
        _selectedVideoForEdit.value = null
    }

    fun onVideoEdited(file: java.io.File) {
        _selectedVideoForEdit.value = null
        _infoMessage.value = "¡Clip recortado guardado: ${file.name}!"
        loadVideos()
    }

    fun onThumbnailExtracted(file: java.io.File) {
        _infoMessage.value = "¡Miniatura HD guardada en Pictures: ${file.name}!"
    }

    fun shareVideo(context: Context, video: RecordedVideo) {
        val shareIntent = repository.createShareIntent(video.filePath)
        if (shareIntent != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Compartir grabación"))
        } else {
            _infoMessage.value = "No se pudo preparar el video para compartir"
        }
    }

    fun launchGame(packageName: String) {
        gamesHelper.launchApp(packageName)
    }

    fun clearInfoMessage() {
        _infoMessage.value = null
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
