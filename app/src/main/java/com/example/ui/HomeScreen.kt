package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RecordingStatus
import com.example.ui.components.GameLauncherScreen
import com.example.ui.components.HomeModalsHost
import com.example.ui.components.RecordBottomBar
import com.example.ui.components.RecordTopBar
import com.example.ui.components.SettingsView
import com.example.ui.launcher.rememberScreenRecordLaunchers
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.tabs.GalleryTab
import com.example.ui.tabs.RecordTab
import kotlinx.coroutines.launch

/**
 * Pantalla principal modular de Vortex Studio.
 * Orquesta la barra superior, barra inferior y las pestañas [RecordTab], [GalleryTab],
 * [GameLauncherScreen] y [SettingsView], delegando los modales a [HomeModalsHost]
 * y los lanzadores de captura a [rememberScreenRecordLaunchers].
 */
@Composable
fun HomeScreen(
    viewModel: RecordViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(initialValue = UiState())
    val lifecycleOwner = LocalLifecycleOwner.current

    // Flujo de Bienvenida y Centro de Permisos en la primera apertura
    if (!uiState.isOnboardingCompleted) {
        OnboardingScreen(
            onCompleteOnboarding = {
                viewModel.completeOnboarding()
            },
            modifier = modifier
        )
        return
    }

    // Refrescar lista de videos automáticamente al volver a la app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadVideos()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Gestor modular de permisos y lanzadores de MediaProjection
    val recordLaunchersState = rememberScreenRecordLaunchers(
        viewModel = viewModel,
        config = uiState.config,
        onShowSnackbar = { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    )

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RecordTopBar(
                status = uiState.status,
                elapsedSeconds = uiState.elapsedSeconds,
                onRefresh = { viewModel.loadVideos() }
            )
        },
        bottomBar = {
            RecordBottomBar(
                activeTab = uiState.activeTab,
                videoCount = uiState.videos.size,
                onTabSelected = { tabIndex ->
                    viewModel.setActiveTab(tabIndex)
                    when (tabIndex) {
                        1 -> viewModel.loadVideos()
                        2 -> viewModel.loadInstalledGames()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState.activeTab) {
                0 -> RecordTab(
                    config = uiState.config,
                    status = uiState.status,
                    elapsedSeconds = uiState.elapsedSeconds,
                    countdownNumber = uiState.countdownNumber,
                    storageInfo = uiState.storageInfo,
                    videos = uiState.videos,
                    onStartClick = { recordLaunchersState.requestStartRecording(null) },
                    onStopClick = { viewModel.stopRecording() },
                    onPauseClick = { viewModel.pauseRecording() },
                    onResumeClick = { viewModel.resumeRecording() },
                    onCancelCountdown = { viewModel.cancelCountdown() },
                    onToggleGameMode = { viewModel.toggleGameMode(it) },
                    onRefreshStorage = { viewModel.refreshStorageInfo() },
                    onViewAllVideos = {
                        viewModel.setActiveTab(1)
                        viewModel.loadVideos()
                    },
                    onPlayVideo = { viewModel.playVideo(it) },
                    onShareVideo = { viewModel.shareVideo(context, it) },
                    onDeleteVideo = { viewModel.deleteVideo(it) },
                    onRenameVideo = { v, name -> viewModel.renameVideo(v, name) },
                    onEditVideo = { viewModel.openEditor(it) }
                )

                1 -> GalleryTab(
                    videos = uiState.videos,
                    isLoading = uiState.isLoadingVideos,
                    onPlayVideo = { viewModel.playVideo(it) },
                    onShareVideo = { viewModel.shareVideo(context, it) },
                    onDeleteVideo = { viewModel.deleteVideo(it) },
                    onRenameVideo = { v, name -> viewModel.renameVideo(v, name) },
                    onEditVideo = { viewModel.openEditor(it) }
                )

                2 -> GameLauncherScreen(
                    games = uiState.installedGames,
                    isLoading = uiState.isLoadingGames,
                    isRecording = uiState.status == RecordingStatus.RECORDING,
                    onStartRecordingWithGame = { pkg -> recordLaunchersState.requestStartRecording(pkg) },
                    onLaunchGameDirectly = { pkg -> viewModel.launchGame(pkg) },
                    onRefreshGames = { viewModel.loadInstalledGames() }
                )

                3 -> SettingsView(
                    config = uiState.config,
                    onUpdateResolution = { viewModel.updateResolution(it) },
                    onUpdateFps = { viewModel.updateFps(it) },
                    onUpdateBitrate = { viewModel.updateBitrate(it) },
                    onUpdateBitrateMbps = { viewModel.updateBitrateMbps(it) },
                    onUpdateImageFormat = { viewModel.updateImageFormat(it) },
                    onUpdateImageQuality = { viewModel.updateImageQuality(it) },
                    onToggleImageWebpLossless = { viewModel.toggleWebpLossless(it) },
                    onUpdateAudioSource = { viewModel.updateAudioSource(it) },
                    onUpdateAudioSampleRate = { viewModel.updateAudioSampleRate(it) },
                    onToggleFloatingVuMeter = { viewModel.toggleFloatingVuMeter(it) },
                    onUpdateGameGain = { viewModel.updateGameAudioGain(it) },
                    onUpdateMicGain = { viewModel.updateMicAudioGain(it) },
                    onToggleNoiseGate = { viewModel.toggleNoiseGate(it) },
                    onToggleAudioDucking = { viewModel.toggleAudioDucking(it) },
                    onUpdateAvSyncOffset = { viewModel.updateAvSyncOffset(it) },
                    onUpdateCountdown = { viewModel.updateCountdown(it) },
                    onToggleGameMode = { viewModel.toggleGameMode(it) },
                    onToggleFloatingBubble = { viewModel.toggleFloatingBubble(it) },
                    onToggleHideBubbleInFinalVideo = { viewModel.toggleHideBubbleInFinalVideo(it) },
                    onToggleFacecam = { enabled ->
                        if (enabled) {
                            recordLaunchersState.requestCameraPermission()
                        }
                        viewModel.toggleFacecam(enabled)
                    },
                    onUpdateFacecamShape = { viewModel.updateFacecamShape(it) },
                    onUpdateFacecamSize = { viewModel.updateFacecamSize(it) },
                    onUpdateFacecamFps = { viewModel.updateFacecamFps(it) },
                    onToggleFacecamCamera = { viewModel.toggleFacecamCamera() },
                    onToggleBeautyFilter = { viewModel.toggleBeautyFilter(it) },
                    onToggleFacecamRgbBorder = { viewModel.toggleFacecamRgbBorder(it) },
                    onToggleVtuber = { viewModel.toggleVtuber(it) },
                    onUpdateVtuberPreset = { viewModel.updateVtuberPreset(it) },
                    onUpdateVtuberTrackingMode = { mode ->
                        if (mode != com.example.model.VtuberTrackingMode.VOICE_ONLY) {
                            recordLaunchersState.requestCameraPermission()
                        }
                        viewModel.updateVtuberTrackingMode(mode)
                    },
                    onToggleVtuberHeadTilt = { viewModel.toggleVtuberHeadTilt(it) },
                    onUpdateVtuberEyeBlinkSensitivity = { viewModel.updateVtuberEyeBlinkSensitivity(it) },
                    onUpdateVtuberMouthSensitivity = { viewModel.updateVtuberMouthSensitivity(it) },
                    onUpdateVtuberSize = { viewModel.updateVtuberSize(it) },
                    onUpdateVtuberSensitivity = { viewModel.updateVtuberSensitivity(it) },
                    onToggleVtuberBounce = { viewModel.toggleVtuberBounce(it) },
                    onUpdateVtuberIdleImage = { viewModel.updateVtuberIdleImage(it) },
                    onUpdateVtuberTalkImage = { viewModel.updateVtuberTalkImage(it) },
                    onUpdateVtuberBlinkImage = { viewModel.updateVtuberBlinkImage(it) },
                    onUpdateVtuberBlinkTalkImage = { viewModel.updateVtuberBlinkTalkImage(it) },
                    onToggleTouchVisualizer = { viewModel.toggleTouchVisualizer(it) },
                    onUpdateTouchVisualizerColor = { viewModel.updateTouchVisualizerColor(it) },
                    onToggleWatermark = { viewModel.toggleWatermark(it) },
                    onUpdateWatermarkType = { viewModel.updateWatermarkType(it) },
                    onUpdateWatermarkText = { viewModel.updateWatermarkText(it) },
                    onUpdateWatermarkOpacity = { viewModel.updateWatermarkOpacity(it) },
                    onUpdateWatermarkSize = { viewModel.updateWatermarkSize(it) },
                    onUpdateWatermarkColor = { viewModel.updateWatermarkColor(it) },
                    onUpdateWatermarkImageUri = { viewModel.updateWatermarkImageUri(it) },
                    onToggleSceneOverlay = { viewModel.toggleSceneOverlay(it) },
                    onUpdateSceneOverlayType = { viewModel.updateSceneOverlayType(it) },
                    onUpdateSceneOverlayText = { viewModel.updateSceneOverlayText(it) },
                    onUpdateSceneOverlayOpacity = { viewModel.updateSceneOverlayOpacity(it) },
                    onUpdateSceneOverlayImageUri = { viewModel.updateSceneOverlayImageUri(it) },
                    onReopenOnboarding = { viewModel.resetOnboarding() },
                    onCheckForUpdates = { viewModel.checkForUpdates(force = true) },
                    onOpenGitHubReleases = { viewModel.openGitHubReleases() },
                    onOpenFeedbackSurvey = { viewModel.openFeedbackSurvey() },
                    updateInfo = uiState.updateInfo
                )
            }
        }
    }

    // Hospedaje modular de diálogos modales (Reproductor y Editor)
    HomeModalsHost(
        uiState = uiState,
        viewModel = viewModel,
        context = context
    )
}
