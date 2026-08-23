package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AudioSourceType
import com.example.model.RecordingStatus
import com.example.ui.components.GameLauncherScreen
import com.example.ui.components.RecordBottomBar
import com.example.ui.components.RecordTopBar
import com.example.ui.components.SettingsView
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.editor.VideoEditorDialog
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.tabs.GalleryTab
import com.example.ui.tabs.RecordTab
import kotlinx.coroutines.launch

/**
 * Pantalla principal modular de Vortex Studio.
 * Orquesta la barra superior, barra inferior y las pestañas [RecordTab], [GalleryTab],
 * [GameLauncherScreen] y [SettingsView] desacopladas.
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

    // Refrescar lista de videos automáticamente al volver a la app (ej. tras parar grabación desde un juego)
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

    var pendingGameLaunchPackage by remember { mutableStateOf<String?>(null) }

    // Launcher dedicado para permiso de cámara (usado al activar Facecam en Ajustes)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Permiso de cámara denegado")
            }
        }
    }

    // Launcher de permisos de captura de pantalla MediaProjection
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startRecordingFlow(
                resultCode = result.resultCode,
                resultData = result.data!!,
                targetGamePackage = pendingGameLaunchPackage
            )
            pendingGameLaunchPackage = null
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Permiso de captura de pantalla cancelado")
            }
            pendingGameLaunchPackage = null
        }
    }

    // Launcher de permisos requeridos (solo se invoca si hay permisos pendientes)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = projectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(captureIntent)
    }

    fun requestStartRecording(gamePackage: String? = null) {
        pendingGameLaunchPackage = gamePackage
        val missingPermissions = mutableListOf<String>()

        // 1. Permiso de micrófono: Solo si la configuración requiere capturar audio y aún no está concedido
        if (uiState.config.audioSource != AudioSourceType.NONE) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.RECORD_AUDIO)
            }
        }

        // 2. Permiso de cámara: Solo si Facecam está encendido y aún no está concedido
        if (uiState.config.showFacecam) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.CAMERA)
            }
        }

        // 3. Notificaciones en Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (missingPermissions.isEmpty()) {
            // Todos los permisos ya fueron otorgados previamente: ir directo a la captura sin molestar
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val captureIntent = projectionManager.createScreenCaptureIntent()
            mediaProjectionLauncher.launch(captureIntent)
        } else {
            // Solo solicitar los permisos que realmente falten
            permissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

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
                    onStartClick = { requestStartRecording() },
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
                    onStartRecordingWithGame = { pkg -> requestStartRecording(pkg) },
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
                    onUpdateCountdown = { viewModel.updateCountdown(it) },
                    onToggleGameMode = { viewModel.toggleGameMode(it) },
                    onToggleFloatingBubble = { viewModel.toggleFloatingBubble(it) },
                    onToggleFacecam = { enabled ->
                        if (enabled && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                    onReopenOnboarding = { viewModel.resetOnboarding() }
                )
            }
        }
    }

    // Modal de reproducción de video integrado
    uiState.selectedVideoForPlay?.let { video ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { viewModel.closePlayer() },
            onShare = { viewModel.shareVideo(context, it) },
            onEdit = { viewModel.openEditor(it) }
        )
    }

    // Modal de Mini Editor de Video estilo CapCut
    uiState.selectedVideoForEdit?.let { video ->
        VideoEditorDialog(
            video = video,
            onDismiss = { viewModel.closeEditor() },
            onVideoEdited = { editedFile ->
                viewModel.onVideoEdited(editedFile)
            },
            onThumbnailExtracted = { thumbFile ->
                viewModel.onThumbnailExtracted(thumbFile)
            }
        )
    }
}
