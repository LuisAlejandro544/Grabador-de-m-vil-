package com.example.ui.components.settings

import com.example.model.AudioSampleRate
import com.example.model.AudioSourceType
import com.example.model.FacecamFps
import com.example.model.FacecamShape
import com.example.model.FacecamSize
import com.example.model.ImageFormatOption
import com.example.model.SceneOverlayType
import com.example.model.TouchColorOption
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.model.VtuberPreset
import com.example.model.VtuberSize
import com.example.model.WatermarkSize
import com.example.model.WatermarkType

/**
 * Agrupación de eventos de configuración de video e imagen.
 */
data class VideoSettingsEvents(
    val onUpdateResolution: (VideoResolution) -> Unit = {},
    val onUpdateFps: (VideoFps) -> Unit = {},
    val onUpdateBitrate: (VideoBitrate) -> Unit = {},
    val onUpdateBitrateMbps: (Int) -> Unit = {},
    val onUpdateImageFormat: (ImageFormatOption) -> Unit = {},
    val onUpdateImageQuality: (Int) -> Unit = {},
    val onToggleImageWebpLossless: (Boolean) -> Unit = {}
)

/**
 * Agrupación de eventos de configuración de audio DSP y vúmetro.
 */
data class AudioSettingsEvents(
    val onUpdateAudioSource: (AudioSourceType) -> Unit = {},
    val onUpdateAudioSampleRate: (AudioSampleRate) -> Unit = {},
    val onToggleFloatingVuMeter: (Boolean) -> Unit = {},
    val onUpdateGameGain: (Float) -> Unit = {},
    val onUpdateMicGain: (Float) -> Unit = {},
    val onToggleNoiseGate: (Boolean) -> Unit = {},
    val onToggleAudioDucking: (Boolean) -> Unit = {},
    val onUpdateAvSyncOffset: (Int) -> Unit = {}
)

/**
 * Agrupación de eventos de Facecam y cámara flotante.
 */
data class FacecamSettingsEvents(
    val onToggleFacecam: (Boolean) -> Unit = {},
    val onUpdateFacecamShape: (FacecamShape) -> Unit = {},
    val onUpdateFacecamSize: (FacecamSize) -> Unit = {},
    val onUpdateFacecamFps: (FacecamFps) -> Unit = {},
    val onToggleFacecamCamera: () -> Unit = {},
    val onToggleBeautyFilter: (Boolean) -> Unit = {},
    val onToggleFacecamRgbBorder: (Boolean) -> Unit = {}
)

/**
 * Agrupación de eventos de Avatar 2D / PNGtuber reactivo por voz.
 */
data class VtuberSettingsEvents(
    val onToggleVtuber: (Boolean) -> Unit = {},
    val onUpdateVtuberPreset: (VtuberPreset) -> Unit = {},
    val onUpdateVtuberSize: (VtuberSize) -> Unit = {},
    val onUpdateVtuberSensitivity: (Float) -> Unit = {},
    val onToggleVtuberBounce: (Boolean) -> Unit = {},
    val onUpdateVtuberIdleImage: (String?) -> Unit = {},
    val onUpdateVtuberTalkImage: (String?) -> Unit = {},
    val onUpdateVtuberBlinkImage: (String?) -> Unit = {},
    val onUpdateVtuberBlinkTalkImage: (String?) -> Unit = {}
)

/**
 * Agrupación de eventos de toques, marca de agua y overlays de escena.
 */
data class OverlaySettingsEvents(
    val onToggleTouchVisualizer: (Boolean) -> Unit = {},
    val onUpdateTouchVisualizerColor: (TouchColorOption) -> Unit = {},
    val onToggleWatermark: (Boolean) -> Unit = {},
    val onUpdateWatermarkType: (WatermarkType) -> Unit = {},
    val onUpdateWatermarkText: (String) -> Unit = {},
    val onUpdateWatermarkOpacity: (Float) -> Unit = {},
    val onUpdateWatermarkSize: (WatermarkSize) -> Unit = {},
    val onUpdateWatermarkColor: (TouchColorOption) -> Unit = {},
    val onUpdateWatermarkImageUri: (String?) -> Unit = {},
    val onToggleSceneOverlay: (Boolean) -> Unit = {},
    val onUpdateSceneOverlayType: (SceneOverlayType) -> Unit = {},
    val onUpdateSceneOverlayText: (String) -> Unit = {},
    val onUpdateSceneOverlayOpacity: (Float) -> Unit = {},
    val onUpdateSceneOverlayImageUri: (String?) -> Unit = {}
)

/**
 * Agrupación de eventos generales (Modo juego, burbuja, onboarding, cuenta atrás, comprobador de actualizaciones).
 */
data class GeneralSettingsEvents(
    val onToggleGameMode: (Boolean) -> Unit = {},
    val onToggleFloatingBubble: (Boolean) -> Unit = {},
    val onToggleHideBubbleInFinalVideo: (Boolean) -> Unit = {},
    val onUpdateCountdown: (Int) -> Unit = {},
    val onReopenOnboarding: () -> Unit = {},
    val onCheckForUpdates: () -> Unit = {},
    val onOpenGitHubReleases: () -> Unit = {},
    val onOpenFeedbackSurvey: () -> Unit = {}
)
