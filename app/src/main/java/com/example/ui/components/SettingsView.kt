package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSampleRate
import com.example.model.AudioSourceType
import com.example.model.FacecamFps
import com.example.model.FacecamShape
import com.example.model.FacecamSize
import com.example.model.ImageFormatOption
import com.example.model.RecordingConfig
import com.example.model.SceneOverlayType
import com.example.model.TouchColorOption
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.model.VtuberPreset
import com.example.model.VtuberSize
import com.example.model.WatermarkSize
import com.example.model.WatermarkType
import com.example.ui.components.settings.AudioSettingsCard
import com.example.ui.components.settings.AudioSettingsEvents
import com.example.ui.components.settings.CountdownSettingsCard
import com.example.ui.components.settings.FacecamSettingsCard
import com.example.ui.components.settings.FacecamSettingsEvents
import com.example.ui.components.settings.FloatingBubbleSettingsCard
import com.example.ui.components.settings.GameModeCard
import com.example.ui.components.settings.GeneralSettingsEvents
import com.example.ui.components.settings.ImageFormatSettingsCard
import com.example.ui.components.settings.NativeModulesStatusCard
import com.example.ui.components.settings.OnboardingTutorialCard
import com.example.ui.components.settings.OverlaySettingsEvents
import com.example.ui.components.settings.SceneOverlaySettingsCard
import com.example.ui.components.settings.TouchVisualizerSettingsCard
import com.example.ui.components.settings.VideoQualitySettingsCard
import com.example.ui.components.settings.VideoSettingsEvents
import com.example.ui.components.settings.VtuberSettingsCard
import com.example.ui.components.settings.VtuberSettingsEvents
import com.example.ui.components.settings.WatermarkSettingsCard

/**
 * Vista modular y desacoplada de Ajustes de Grabación y Streaming.
 * Ensambla secciones mediante tarjetas modulares y contenedores de eventos tipados.
 */
@Composable
fun SettingsView(
    config: RecordingConfig,
    videoEvents: VideoSettingsEvents,
    audioEvents: AudioSettingsEvents,
    facecamEvents: FacecamSettingsEvents,
    vtuberEvents: VtuberSettingsEvents,
    overlayEvents: OverlaySettingsEvents,
    generalEvents: GeneralSettingsEvents,
    modifier: Modifier = Modifier,
    updateInfo: com.example.model.AppUpdateInfo = com.example.model.AppUpdateInfo()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración de Grabación",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Modo Juego (Optimización de latencia)
        GameModeCard(
            isGameMode = config.isGameMode,
            onToggleGameMode = generalEvents.onToggleGameMode
        )

        // Burbuja Flotante y Permiso de Superposición
        FloatingBubbleSettingsCard(
            showFloatingBubble = config.showFloatingBubble,
            onToggleFloatingBubble = generalEvents.onToggleFloatingBubble
        )

        // Guía de Inicio y Centro de Permisos
        OnboardingTutorialCard(
            onReopenOnboarding = generalEvents.onReopenOnboarding
        )

        // Marca de Agua / Logo Personalizado Superpuesto
        WatermarkSettingsCard(
            config = config,
            onToggleWatermark = overlayEvents.onToggleWatermark,
            onTypeSelected = overlayEvents.onUpdateWatermarkType,
            onTextChanged = overlayEvents.onUpdateWatermarkText,
            onOpacityChanged = overlayEvents.onUpdateWatermarkOpacity,
            onSizeSelected = overlayEvents.onUpdateWatermarkSize,
            onColorSelected = overlayEvents.onUpdateWatermarkColor,
            onImageSelected = overlayEvents.onUpdateWatermarkImageUri
        )

        // Overlays de Escena Personalizados
        SceneOverlaySettingsCard(
            config = config,
            onToggleSceneOverlay = overlayEvents.onToggleSceneOverlay,
            onTypeSelected = overlayEvents.onUpdateSceneOverlayType,
            onTextChanged = overlayEvents.onUpdateSceneOverlayText,
            onOpacityChanged = overlayEvents.onUpdateSceneOverlayOpacity,
            onImageSelected = overlayEvents.onUpdateSceneOverlayImageUri
        )

        // Facecam (Cámara Flotante, Filtros y Formas)
        FacecamSettingsCard(
            showFacecam = config.showFacecam,
            isFrontCamera = config.isFrontCamera,
            beautyFilterEnabled = config.beautyFilterEnabled,
            facecamRgbBorder = config.facecamRgbBorder,
            facecamShape = config.facecamShape,
            facecamSize = config.facecamSize,
            facecamFps = config.facecamFps,
            onToggleFacecam = facecamEvents.onToggleFacecam,
            onToggleFacecamCamera = facecamEvents.onToggleFacecamCamera,
            onToggleBeautyFilter = facecamEvents.onToggleBeautyFilter,
            onToggleFacecamRgbBorder = facecamEvents.onToggleFacecamRgbBorder,
            onUpdateFacecamShape = facecamEvents.onUpdateFacecamShape,
            onUpdateFacecamSize = facecamEvents.onUpdateFacecamSize,
            onUpdateFacecamFps = facecamEvents.onUpdateFacecamFps
        )

        // Avatar 2D / PNGtuber Reactivo (Modo VTuber)
        VtuberSettingsCard(
            config = config,
            onToggleVtuber = vtuberEvents.onToggleVtuber,
            onPresetSelected = vtuberEvents.onUpdateVtuberPreset,
            onSizeSelected = vtuberEvents.onUpdateVtuberSize,
            onSensitivityChanged = vtuberEvents.onUpdateVtuberSensitivity,
            onToggleBounce = vtuberEvents.onToggleVtuberBounce,
            onIdleImageSelected = vtuberEvents.onUpdateVtuberIdleImage,
            onTalkImageSelected = vtuberEvents.onUpdateVtuberTalkImage,
            onBlinkImageSelected = vtuberEvents.onUpdateVtuberBlinkImage,
            onBlinkTalkImageSelected = vtuberEvents.onUpdateVtuberBlinkTalkImage
        )

        // Visualizador de Toques Táctiles
        TouchVisualizerSettingsCard(
            showTouchVisualizer = config.showTouchVisualizer,
            touchVisualizerColor = config.touchVisualizerColor,
            onToggleTouchVisualizer = overlayEvents.onToggleTouchVisualizer,
            onUpdateTouchVisualizerColor = overlayEvents.onUpdateTouchVisualizerColor
        )

        // Calidad de Video (Resolución, FPS, Bitrate Personalizado)
        VideoQualitySettingsCard(
            resolution = config.resolution,
            fps = config.fps,
            bitrate = config.bitrate,
            bitrateMbps = config.bitrateMbps,
            onUpdateResolution = videoEvents.onUpdateResolution,
            onUpdateFps = videoEvents.onUpdateFps,
            onUpdateBitrate = videoEvents.onUpdateBitrate,
            onUpdateBitrateMbps = videoEvents.onUpdateBitrateMbps
        )

        // Formato y Compresión de Imagen / Screenshots
        ImageFormatSettingsCard(
            imageFormat = config.imageFormat,
            imageQuality = config.imageQuality,
            imageWebpLossless = config.imageWebpLossless,
            onUpdateImageFormat = videoEvents.onUpdateImageFormat,
            onUpdateImageQuality = videoEvents.onUpdateImageQuality,
            onToggleWebpLossless = videoEvents.onToggleImageWebpLossless
        )

        // Fuente de Audio y Frecuencia de Muestreo (DSP)
        AudioSettingsCard(
            audioSource = config.audioSource,
            audioSampleRate = config.audioSampleRate,
            showFloatingVuMeter = config.showFloatingVuMeter,
            gameAudioGain = config.gameAudioGain,
            micAudioGain = config.micAudioGain,
            noiseGateEnabled = config.noiseGateEnabled,
            audioDuckingEnabled = config.audioDuckingEnabled,
            avSyncOffsetMs = config.avSyncOffsetMs,
            onUpdateAudioSource = audioEvents.onUpdateAudioSource,
            onUpdateAudioSampleRate = audioEvents.onUpdateAudioSampleRate,
            onToggleFloatingVuMeter = audioEvents.onToggleFloatingVuMeter,
            onUpdateGameGain = audioEvents.onUpdateGameGain,
            onUpdateMicGain = audioEvents.onUpdateMicGain,
            onToggleNoiseGate = audioEvents.onToggleNoiseGate,
            onToggleAudioDucking = audioEvents.onToggleAudioDucking,
            onUpdateAvSyncOffset = audioEvents.onUpdateAvSyncOffset
        )

        // Cuenta Atrás
        CountdownSettingsCard(
            countdownSeconds = config.countdownSeconds,
            onUpdateCountdown = generalEvents.onUpdateCountdown
        )

        // Monitor de Estado de Módulos Nativos (C++ & Rust)
        NativeModulesStatusCard()

        // Canales de Versión y Distribución (Dev, Canary, Beta, Estable) y Comprobación de Actualizaciones
        com.example.ui.components.settings.ReleaseChannelInfoCard(
            updateInfo = updateInfo,
            onCheckForUpdates = generalEvents.onCheckForUpdates,
            onOpenGitHubReleases = generalEvents.onOpenGitHubReleases
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Sobrecarga modular para compatibilidad con parámetros individuales.
 */
@Composable
fun SettingsView(
    config: RecordingConfig,
    onUpdateResolution: (VideoResolution) -> Unit,
    onUpdateFps: (VideoFps) -> Unit,
    onUpdateBitrate: (VideoBitrate) -> Unit,
    onUpdateBitrateMbps: (Int) -> Unit = {},
    onUpdateImageFormat: (ImageFormatOption) -> Unit = {},
    onUpdateImageQuality: (Int) -> Unit = {},
    onToggleImageWebpLossless: (Boolean) -> Unit = {},
    onUpdateAudioSource: (AudioSourceType) -> Unit,
    onUpdateAudioSampleRate: (AudioSampleRate) -> Unit = {},
    onToggleFloatingVuMeter: (Boolean) -> Unit = {},
    onUpdateGameGain: (Float) -> Unit = {},
    onUpdateMicGain: (Float) -> Unit = {},
    onToggleNoiseGate: (Boolean) -> Unit = {},
    onToggleAudioDucking: (Boolean) -> Unit = {},
    onUpdateAvSyncOffset: (Int) -> Unit = {},
    onUpdateCountdown: (Int) -> Unit,
    onToggleGameMode: (Boolean) -> Unit,
    onToggleFloatingBubble: (Boolean) -> Unit = {},
    onToggleFacecam: (Boolean) -> Unit = {},
    onUpdateFacecamShape: (FacecamShape) -> Unit = {},
    onUpdateFacecamSize: (FacecamSize) -> Unit = {},
    onUpdateFacecamFps: (FacecamFps) -> Unit = {},
    onToggleFacecamCamera: () -> Unit = {},
    onToggleBeautyFilter: (Boolean) -> Unit = {},
    onToggleFacecamRgbBorder: (Boolean) -> Unit = {},
    onToggleVtuber: (Boolean) -> Unit = {},
    onUpdateVtuberPreset: (VtuberPreset) -> Unit = {},
    onUpdateVtuberSize: (VtuberSize) -> Unit = {},
    onUpdateVtuberSensitivity: (Float) -> Unit = {},
    onToggleVtuberBounce: (Boolean) -> Unit = {},
    onUpdateVtuberIdleImage: (String?) -> Unit = {},
    onUpdateVtuberTalkImage: (String?) -> Unit = {},
    onUpdateVtuberBlinkImage: (String?) -> Unit = {},
    onUpdateVtuberBlinkTalkImage: (String?) -> Unit = {},
    onToggleTouchVisualizer: (Boolean) -> Unit = {},
    onUpdateTouchVisualizerColor: (TouchColorOption) -> Unit = {},
    onToggleWatermark: (Boolean) -> Unit = {},
    onUpdateWatermarkType: (WatermarkType) -> Unit = {},
    onUpdateWatermarkText: (String) -> Unit = {},
    onUpdateWatermarkOpacity: (Float) -> Unit = {},
    onUpdateWatermarkSize: (WatermarkSize) -> Unit = {},
    onUpdateWatermarkColor: (TouchColorOption) -> Unit = {},
    onUpdateWatermarkImageUri: (String?) -> Unit = {},
    onToggleSceneOverlay: (Boolean) -> Unit = {},
    onUpdateSceneOverlayType: (SceneOverlayType) -> Unit = {},
    onUpdateSceneOverlayText: (String) -> Unit = {},
    onUpdateSceneOverlayOpacity: (Float) -> Unit = {},
    onUpdateSceneOverlayImageUri: (String?) -> Unit = {},
    onReopenOnboarding: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onOpenGitHubReleases: () -> Unit = {},
    updateInfo: com.example.model.AppUpdateInfo = com.example.model.AppUpdateInfo(),
    modifier: Modifier = Modifier
) {
    SettingsView(
        config = config,
        updateInfo = updateInfo,
        videoEvents = VideoSettingsEvents(
            onUpdateResolution = onUpdateResolution,
            onUpdateFps = onUpdateFps,
            onUpdateBitrate = onUpdateBitrate,
            onUpdateBitrateMbps = onUpdateBitrateMbps,
            onUpdateImageFormat = onUpdateImageFormat,
            onUpdateImageQuality = onUpdateImageQuality,
            onToggleImageWebpLossless = onToggleImageWebpLossless
        ),
        audioEvents = AudioSettingsEvents(
            onUpdateAudioSource = onUpdateAudioSource,
            onUpdateAudioSampleRate = onUpdateAudioSampleRate,
            onToggleFloatingVuMeter = onToggleFloatingVuMeter,
            onUpdateGameGain = onUpdateGameGain,
            onUpdateMicGain = onUpdateMicGain,
            onToggleNoiseGate = onToggleNoiseGate,
            onToggleAudioDucking = onToggleAudioDucking,
            onUpdateAvSyncOffset = onUpdateAvSyncOffset
        ),
        facecamEvents = FacecamSettingsEvents(
            onToggleFacecam = onToggleFacecam,
            onUpdateFacecamShape = onUpdateFacecamShape,
            onUpdateFacecamSize = onUpdateFacecamSize,
            onUpdateFacecamFps = onUpdateFacecamFps,
            onToggleFacecamCamera = onToggleFacecamCamera,
            onToggleBeautyFilter = onToggleBeautyFilter,
            onToggleFacecamRgbBorder = onToggleFacecamRgbBorder
        ),
        vtuberEvents = VtuberSettingsEvents(
            onToggleVtuber = onToggleVtuber,
            onUpdateVtuberPreset = onUpdateVtuberPreset,
            onUpdateVtuberSize = onUpdateVtuberSize,
            onUpdateVtuberSensitivity = onUpdateVtuberSensitivity,
            onToggleVtuberBounce = onToggleVtuberBounce,
            onUpdateVtuberIdleImage = onUpdateVtuberIdleImage,
            onUpdateVtuberTalkImage = onUpdateVtuberTalkImage,
            onUpdateVtuberBlinkImage = onUpdateVtuberBlinkImage,
            onUpdateVtuberBlinkTalkImage = onUpdateVtuberBlinkTalkImage
        ),
        overlayEvents = OverlaySettingsEvents(
            onToggleTouchVisualizer = onToggleTouchVisualizer,
            onUpdateTouchVisualizerColor = onUpdateTouchVisualizerColor,
            onToggleWatermark = onToggleWatermark,
            onUpdateWatermarkType = onUpdateWatermarkType,
            onUpdateWatermarkText = onUpdateWatermarkText,
            onUpdateWatermarkOpacity = onUpdateWatermarkOpacity,
            onUpdateWatermarkSize = onUpdateWatermarkSize,
            onUpdateWatermarkColor = onUpdateWatermarkColor,
            onUpdateWatermarkImageUri = onUpdateWatermarkImageUri,
            onToggleSceneOverlay = onToggleSceneOverlay,
            onUpdateSceneOverlayType = onUpdateSceneOverlayType,
            onUpdateSceneOverlayText = onUpdateSceneOverlayText,
            onUpdateSceneOverlayOpacity = onUpdateSceneOverlayOpacity,
            onUpdateSceneOverlayImageUri = onUpdateSceneOverlayImageUri
        ),
        generalEvents = GeneralSettingsEvents(
            onToggleGameMode = onToggleGameMode,
            onToggleFloatingBubble = onToggleFloatingBubble,
            onUpdateCountdown = onUpdateCountdown,
            onReopenOnboarding = onReopenOnboarding,
            onCheckForUpdates = onCheckForUpdates,
            onOpenGitHubReleases = onOpenGitHubReleases
        ),
        modifier = modifier
    )
}

