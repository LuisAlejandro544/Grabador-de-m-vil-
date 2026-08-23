package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.settings.GameAndImageSettingsStore
import com.example.data.settings.OverlaySettingsStore
import com.example.data.settings.VideoAudioSettingsStore
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio de persistencia local para la configuración de grabación de Vortex Studio.
 * Desacoplado modularmente en submódulos especializados:
 * - [VideoAudioSettingsStore]: Resoluciones, FPS, Bitrates y motor de audio DSP.
 * - [OverlaySettingsStore]: Overlays (Facecam, VTuber, Marcas de agua, Vúmetro, Marco Gamer).
 * - [GameAndImageSettingsStore]: Modo juego, cuenta atrás y formatos de captura de pantalla.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val videoAudioStore = VideoAudioSettingsStore(prefs)
    private val overlayStore = OverlaySettingsStore(prefs)
    private val gameAndImageStore = GameAndImageSettingsStore(prefs)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<RecordingConfig> = _configFlow.asStateFlow()

    fun getConfig(): RecordingConfig = _configFlow.value

    fun loadConfig(): RecordingConfig {
        val videoAudioSlice = videoAudioStore.load()
        val overlaySlice = overlayStore.load()
        val gameImageSlice = gameAndImageStore.load()

        return RecordingConfig(
            resolution = videoAudioSlice.resolution,
            fps = videoAudioSlice.fps,
            bitrate = videoAudioSlice.bitrate,
            bitrateMbps = videoAudioSlice.bitrateMbps,
            facecamFps = videoAudioSlice.facecamFps,
            audioSampleRate = videoAudioSlice.audioSampleRate,
            audioSource = videoAudioSlice.audioSource,
            countdownSeconds = gameImageSlice.countdownSeconds,
            isGameMode = gameImageSlice.isGameMode,
            showFloatingBubble = overlaySlice.showFloatingBubble,
            showFacecam = overlaySlice.showFacecam,
            facecamShape = overlaySlice.facecamShape,
            facecamSize = overlaySlice.facecamSize,
            isFrontCamera = overlaySlice.isFrontCamera,
            beautyFilterEnabled = overlaySlice.beautyFilterEnabled,
            facecamRgbBorder = overlaySlice.facecamRgbBorder,
            showTouchVisualizer = overlaySlice.showTouchVisualizer,
            touchVisualizerColor = overlaySlice.touchVisualizerColor,
            showWatermark = overlaySlice.showWatermark,
            watermarkType = overlaySlice.watermarkType,
            watermarkText = overlaySlice.watermarkText,
            watermarkOpacity = overlaySlice.watermarkOpacity,
            watermarkSize = overlaySlice.watermarkSize,
            watermarkColor = overlaySlice.watermarkColor,
            watermarkCustomImageUri = overlaySlice.watermarkCustomImageUri,
            showSceneOverlay = overlaySlice.showSceneOverlay,
            sceneOverlayType = overlaySlice.sceneOverlayType,
            sceneOverlayText = overlaySlice.sceneOverlayText,
            sceneOverlayOpacity = overlaySlice.sceneOverlayOpacity,
            sceneOverlayImageUri = overlaySlice.sceneOverlayImageUri,
            showVtuber = overlaySlice.showVtuber,
            vtuberPreset = overlaySlice.vtuberPreset,
            vtuberSize = overlaySlice.vtuberSize,
            vtuberSensitivity = overlaySlice.vtuberSensitivity,
            vtuberBounceEnabled = overlaySlice.vtuberBounceEnabled,
            vtuberIdleImageUri = overlaySlice.vtuberIdleImageUri,
            vtuberTalkImageUri = overlaySlice.vtuberTalkImageUri,
            vtuberBlinkImageUri = overlaySlice.vtuberBlinkImageUri,
            vtuberBlinkTalkImageUri = overlaySlice.vtuberBlinkTalkImageUri,
            gameAudioGain = videoAudioSlice.gameAudioGain,
            micAudioGain = videoAudioSlice.micAudioGain,
            audioDuckingEnabled = videoAudioSlice.audioDuckingEnabled,
            noiseGateEnabled = videoAudioSlice.noiseGateEnabled,
            showFloatingVuMeter = overlaySlice.showFloatingVuMeter,
            imageFormat = gameImageSlice.imageFormat,
            imageQuality = gameImageSlice.imageQuality,
            imageWebpLossless = gameImageSlice.imageWebpLossless
        )
    }

    fun saveConfig(config: RecordingConfig) {
        prefs.edit().apply {
            videoAudioStore.save(this, config)
            overlayStore.save(this, config)
            gameAndImageStore.save(this, config)
            apply()
        }
        _configFlow.value = config
    }

    // --- Métodos de mutación tipados ---

    fun updateImageFormat(format: ImageFormatOption) {
        saveConfig(_configFlow.value.copy(imageFormat = format))
    }

    fun updateImageQuality(quality: Int) {
        val clamped = quality.coerceIn(10, 100)
        saveConfig(_configFlow.value.copy(imageQuality = clamped))
    }

    fun toggleImageWebpLossless(lossless: Boolean) {
        saveConfig(_configFlow.value.copy(imageWebpLossless = lossless))
    }

    fun updateGameAudioGain(gain: Float) {
        saveConfig(_configFlow.value.copy(gameAudioGain = gain.coerceIn(0.0f, 2.5f)))
    }

    fun updateMicAudioGain(gain: Float) {
        saveConfig(_configFlow.value.copy(micAudioGain = gain.coerceIn(0.0f, 2.5f)))
    }

    fun toggleAudioDucking(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(audioDuckingEnabled = enabled))
    }

    fun toggleNoiseGate(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(noiseGateEnabled = enabled))
    }

    fun toggleFloatingVuMeter(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(showFloatingVuMeter = enabled))
    }

    fun toggleVtuber(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(showVtuber = enabled))
    }

    fun updateVtuberPreset(preset: VtuberPreset) {
        saveConfig(_configFlow.value.copy(vtuberPreset = preset))
    }

    fun updateVtuberSize(size: VtuberSize) {
        saveConfig(_configFlow.value.copy(vtuberSize = size))
    }

    fun updateVtuberSensitivity(sensitivity: Float) {
        saveConfig(_configFlow.value.copy(vtuberSensitivity = sensitivity))
    }

    fun toggleVtuberBounce(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(vtuberBounceEnabled = enabled))
    }

    fun updateVtuberIdleUri(uri: String?) {
        saveConfig(_configFlow.value.copy(vtuberIdleImageUri = uri, vtuberPreset = VtuberPreset.CUSTOM))
    }

    fun updateVtuberTalkUri(uri: String?) {
        saveConfig(_configFlow.value.copy(vtuberTalkImageUri = uri, vtuberPreset = VtuberPreset.CUSTOM))
    }

    fun updateVtuberBlinkUri(uri: String?) {
        saveConfig(_configFlow.value.copy(vtuberBlinkImageUri = uri, vtuberPreset = VtuberPreset.CUSTOM))
    }

    fun updateVtuberBlinkTalkUri(uri: String?) {
        saveConfig(_configFlow.value.copy(vtuberBlinkTalkImageUri = uri, vtuberPreset = VtuberPreset.CUSTOM))
    }

    fun updateVtuberCustomImages(
        idleUri: String?,
        talkUri: String?,
        blinkUri: String? = null,
        blinkTalkUri: String? = null
    ) {
        saveConfig(
            _configFlow.value.copy(
                vtuberIdleImageUri = idleUri,
                vtuberTalkImageUri = talkUri,
                vtuberBlinkImageUri = blinkUri,
                vtuberBlinkTalkImageUri = blinkTalkUri,
                vtuberPreset = VtuberPreset.CUSTOM
            )
        )
    }

    fun updateResolution(resolution: VideoResolution) {
        saveConfig(_configFlow.value.copy(resolution = resolution))
    }

    fun updateFps(fps: VideoFps) {
        saveConfig(_configFlow.value.copy(fps = fps))
    }

    fun updateBitrate(bitrate: VideoBitrate) {
        val mbps = when (bitrate) {
            VideoBitrate.BITRATE_12M -> 12
            VideoBitrate.BITRATE_8M -> 8
            VideoBitrate.BITRATE_4M -> 4
        }
        saveConfig(_configFlow.value.copy(bitrate = bitrate, bitrateMbps = mbps))
    }

    fun updateBitrateMbps(mbps: Int) {
        val clamped = mbps.coerceIn(1, 12)
        val legacy = when {
            clamped >= 12 -> VideoBitrate.BITRATE_12M
            clamped >= 8 -> VideoBitrate.BITRATE_8M
            else -> VideoBitrate.BITRATE_4M
        }
        saveConfig(_configFlow.value.copy(bitrateMbps = clamped, bitrate = legacy))
    }

    fun updateFacecamFps(fps: FacecamFps) {
        saveConfig(_configFlow.value.copy(facecamFps = fps))
    }

    fun updateAudioSampleRate(sampleRate: AudioSampleRate) {
        saveConfig(_configFlow.value.copy(audioSampleRate = sampleRate))
    }

    fun updateAudioSource(source: AudioSourceType) {
        saveConfig(_configFlow.value.copy(audioSource = source))
    }

    fun updateCountdown(seconds: Int) {
        saveConfig(_configFlow.value.copy(countdownSeconds = seconds))
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(showFloatingBubble = enabled))
    }

    fun toggleFacecam(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(showFacecam = enabled))
    }

    fun updateFacecamShape(shape: FacecamShape) {
        saveConfig(_configFlow.value.copy(facecamShape = shape))
    }

    fun updateFacecamSize(size: FacecamSize) {
        saveConfig(_configFlow.value.copy(facecamSize = size))
    }

    fun toggleFacecamCamera() {
        val current = _configFlow.value.isFrontCamera
        saveConfig(_configFlow.value.copy(isFrontCamera = !current))
    }

    fun setFacecamCamera(isFront: Boolean) {
        saveConfig(_configFlow.value.copy(isFrontCamera = isFront))
    }

    fun toggleBeautyFilter(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(beautyFilterEnabled = enabled))
    }

    fun toggleFacecamRgbBorder(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(facecamRgbBorder = enabled))
    }

    fun toggleTouchVisualizer(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(showTouchVisualizer = enabled))
    }

    fun updateTouchVisualizerColor(color: TouchColorOption) {
        saveConfig(_configFlow.value.copy(touchVisualizerColor = color))
    }

    fun toggleWatermark(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(showWatermark = enabled))
    }

    fun updateWatermarkType(type: WatermarkType) {
        saveConfig(_configFlow.value.copy(watermarkType = type))
    }

    fun updateWatermarkText(text: String) {
        saveConfig(_configFlow.value.copy(watermarkText = text))
    }

    fun updateWatermarkOpacity(opacity: Float) {
        saveConfig(_configFlow.value.copy(watermarkOpacity = opacity))
    }

    fun updateWatermarkSize(size: WatermarkSize) {
        saveConfig(_configFlow.value.copy(watermarkSize = size))
    }

    fun updateWatermarkColor(color: TouchColorOption) {
        saveConfig(_configFlow.value.copy(watermarkColor = color))
    }

    fun updateWatermarkImageUri(uri: String?) {
        saveConfig(_configFlow.value.copy(watermarkCustomImageUri = uri))
    }

    fun toggleSceneOverlay(enabled: Boolean) {
        saveConfig(_configFlow.value.copy(showSceneOverlay = enabled))
    }

    fun updateSceneOverlayType(type: SceneOverlayType) {
        saveConfig(_configFlow.value.copy(sceneOverlayType = type))
    }

    fun updateSceneOverlayText(text: String) {
        saveConfig(_configFlow.value.copy(sceneOverlayText = text))
    }

    fun updateSceneOverlayOpacity(opacity: Float) {
        saveConfig(_configFlow.value.copy(sceneOverlayOpacity = opacity))
    }

    fun updateSceneOverlayImageUri(uri: String?) {
        saveConfig(_configFlow.value.copy(sceneOverlayImageUri = uri))
    }

    fun toggleGameMode(enabled: Boolean) {
        val current = _configFlow.value
        val updated = if (enabled) {
            current.copy(
                isGameMode = true,
                fps = VideoFps.FPS_60,
                bitrate = VideoBitrate.BITRATE_12M
            )
        } else {
            current.copy(
                isGameMode = false,
                fps = VideoFps.FPS_30,
                bitrate = VideoBitrate.BITRATE_8M
            )
        }
        saveConfig(updated)
    }

    companion object {
        private const val PREFS_NAME = "obs_mobile_recording_prefs"
    }
}
