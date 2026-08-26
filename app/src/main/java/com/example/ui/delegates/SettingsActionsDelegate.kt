package com.example.ui.delegates

import android.content.Context
import android.net.Uri
import com.example.data.SettingsRepository
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
import com.example.service.vtuber.VtuberPresetDrawables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Delegado modular para la gestión y persistencia de ajustes de video,
 * audio DSP, capturas de pantalla, overlays, facecam y avatares 2D PNGtuber.
 */
class SettingsActionsDelegate(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    fun updateResolution(resolution: VideoResolution) = settingsRepository.updateResolution(resolution)
    fun updateFps(fps: VideoFps) = settingsRepository.updateFps(fps)
    fun updateBitrate(bitrate: VideoBitrate) = settingsRepository.updateBitrate(bitrate)
    fun updateBitrateMbps(mbps: Int) = settingsRepository.updateBitrateMbps(mbps)
    fun updateFacecamFps(fps: FacecamFps) = settingsRepository.updateFacecamFps(fps)

    fun updateAudioSampleRate(sampleRate: AudioSampleRate) = settingsRepository.updateAudioSampleRate(sampleRate)
    fun updateAudioSource(source: AudioSourceType) = settingsRepository.updateAudioSource(source)
    fun toggleFloatingVuMeter(enabled: Boolean) = settingsRepository.toggleFloatingVuMeter(enabled)
    fun updateGameAudioGain(gain: Float) = settingsRepository.updateGameAudioGain(gain)
    fun updateMicAudioGain(gain: Float) = settingsRepository.updateMicAudioGain(gain)
    fun toggleNoiseGate(enabled: Boolean) = settingsRepository.toggleNoiseGate(enabled)
    fun toggleAudioDucking(enabled: Boolean) = settingsRepository.toggleAudioDucking(enabled)
    fun updateAvSyncOffset(offsetMs: Int) = settingsRepository.updateAvSyncOffset(offsetMs)

    fun updateCountdown(seconds: Int) = settingsRepository.updateCountdown(seconds)

    fun updateImageFormat(format: ImageFormatOption) = settingsRepository.updateImageFormat(format)
    fun updateImageQuality(quality: Int) = settingsRepository.updateImageQuality(quality)
    fun toggleWebpLossless(lossless: Boolean) = settingsRepository.toggleImageWebpLossless(lossless)

    fun toggleFloatingBubble(enabled: Boolean) = settingsRepository.toggleFloatingBubble(enabled)
    fun toggleFacecam(enabled: Boolean) = settingsRepository.toggleFacecam(enabled)
    fun updateFacecamShape(shape: FacecamShape) = settingsRepository.updateFacecamShape(shape)
    fun updateFacecamSize(size: FacecamSize) = settingsRepository.updateFacecamSize(size)
    fun toggleFacecamCamera() = settingsRepository.toggleFacecamCamera()
    fun toggleBeautyFilter(enabled: Boolean) = settingsRepository.toggleBeautyFilter(enabled)
    fun toggleFacecamRgbBorder(enabled: Boolean) = settingsRepository.toggleFacecamRgbBorder(enabled)

    fun toggleTouchVisualizer(enabled: Boolean) = settingsRepository.toggleTouchVisualizer(enabled)
    fun updateTouchVisualizerColor(color: TouchColorOption) = settingsRepository.updateTouchVisualizerColor(color)

    fun toggleWatermark(enabled: Boolean) = settingsRepository.toggleWatermark(enabled)
    fun updateWatermarkType(type: WatermarkType) = settingsRepository.updateWatermarkType(type)
    fun updateWatermarkText(text: String) = settingsRepository.updateWatermarkText(text)
    fun updateWatermarkOpacity(opacity: Float) = settingsRepository.updateWatermarkOpacity(opacity)
    fun updateWatermarkSize(size: WatermarkSize) = settingsRepository.updateWatermarkSize(size)
    fun updateWatermarkColor(color: TouchColorOption) = settingsRepository.updateWatermarkColor(color)
    fun updateWatermarkImageUri(uri: String?) = settingsRepository.updateWatermarkImageUri(uri)

    fun toggleSceneOverlay(enabled: Boolean) = settingsRepository.toggleSceneOverlay(enabled)
    fun updateSceneOverlayType(type: SceneOverlayType) = settingsRepository.updateSceneOverlayType(type)
    fun updateSceneOverlayText(text: String) = settingsRepository.updateSceneOverlayText(text)
    fun updateSceneOverlayOpacity(opacity: Float) = settingsRepository.updateSceneOverlayOpacity(opacity)
    fun updateSceneOverlayImageUri(uri: String?) = settingsRepository.updateSceneOverlayImageUri(uri)

    fun toggleVtuber(enabled: Boolean) = settingsRepository.toggleVtuber(enabled)
    fun updateVtuberPreset(preset: VtuberPreset) = settingsRepository.updateVtuberPreset(preset)
    fun updateVtuberTrackingMode(mode: com.example.model.VtuberTrackingMode) = settingsRepository.updateVtuberTrackingMode(mode)
    fun toggleVtuberHeadTilt(enabled: Boolean) = settingsRepository.toggleVtuberHeadTilt(enabled)
    fun updateVtuberEyeBlinkSensitivity(sensitivity: Float) = settingsRepository.updateVtuberEyeBlinkSensitivity(sensitivity)
    fun updateVtuberMouthSensitivity(sensitivity: Float) = settingsRepository.updateVtuberMouthSensitivity(sensitivity)
    fun updateVtuberSize(size: VtuberSize) = settingsRepository.updateVtuberSize(size)
    fun updateVtuberSensitivity(sensitivity: Float) = settingsRepository.updateVtuberSensitivity(sensitivity)
    fun toggleVtuberBounce(enabled: Boolean) = settingsRepository.toggleVtuberBounce(enabled)

    fun updateVtuberCustomImage(uriString: String?, stateType: String, onUpdated: (String?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val finalPath = if (!uriString.isNullOrBlank()) {
                val uri = Uri.parse(uriString)
                if (uri.scheme == "content") {
                    VtuberPresetDrawables.saveImageToInternalStorage(context, uri, stateType) ?: uriString
                } else {
                    uriString
                }
            } else {
                null
            }
            onUpdated(finalPath)
        }
    }

    fun updateVtuberIdleImage(uriString: String?) {
        updateVtuberCustomImage(uriString, "idle") { path ->
            settingsRepository.updateVtuberIdleUri(path)
        }
    }

    fun updateVtuberTalkImage(uriString: String?) {
        updateVtuberCustomImage(uriString, "talk") { path ->
            settingsRepository.updateVtuberTalkUri(path)
        }
    }

    fun updateVtuberBlinkImage(uriString: String?) {
        updateVtuberCustomImage(uriString, "blink") { path ->
            settingsRepository.updateVtuberBlinkUri(path)
        }
    }

    fun updateVtuberBlinkTalkImage(uriString: String?) {
        updateVtuberCustomImage(uriString, "blink_talk") { path ->
            settingsRepository.updateVtuberBlinkTalkUri(path)
        }
    }

    fun toggleGameMode(enabled: Boolean) = settingsRepository.toggleGameMode(enabled)
}
