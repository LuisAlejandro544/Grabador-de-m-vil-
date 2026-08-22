package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AudioSourceType
import com.example.model.FacecamShape
import com.example.model.FacecamSize
import com.example.model.RecordingConfig
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio de persistencia local para la configuración de grabación de OBS Mobile.
 * Garantiza que las resoluciones, FPS, bitrate, fuentes de audio, cuenta atrás,
 * modo de juego, burbuja flotante y Facecam persistan entre reinicios de la aplicación y
 * se transmitan de forma 100% fiel al motor de captura [ScreenRecordService] y [ScreenCaptureEngine].
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<RecordingConfig> = _configFlow.asStateFlow()

    fun getConfig(): RecordingConfig = _configFlow.value

    fun loadConfig(): RecordingConfig {
        val resName = prefs.getString(KEY_RESOLUTION, VideoResolution.RES_1080P.name)
        val resolution = try {
            VideoResolution.valueOf(resName ?: VideoResolution.RES_1080P.name)
        } catch (e: Exception) {
            VideoResolution.RES_1080P
        }

        val fpsName = prefs.getString(KEY_FPS, VideoFps.FPS_60.name)
        val fps = try {
            VideoFps.valueOf(fpsName ?: VideoFps.FPS_60.name)
        } catch (e: Exception) {
            VideoFps.FPS_60
        }

        val bitrateName = prefs.getString(KEY_BITRATE, VideoBitrate.BITRATE_8M.name)
        val bitrate = try {
            VideoBitrate.valueOf(bitrateName ?: VideoBitrate.BITRATE_8M.name)
        } catch (e: Exception) {
            VideoBitrate.BITRATE_8M
        }

        val audioName = prefs.getString(KEY_AUDIO_SOURCE, AudioSourceType.INTERNAL_AND_MIC.name)
        val audioSource = try {
            AudioSourceType.valueOf(audioName ?: AudioSourceType.INTERNAL_AND_MIC.name)
        } catch (e: Exception) {
            AudioSourceType.INTERNAL_AND_MIC
        }

        val countdown = prefs.getInt(KEY_COUNTDOWN, 3)
        val isGameMode = prefs.getBoolean(KEY_GAME_MODE, true)
        val showFloatingBubble = prefs.getBoolean(KEY_FLOATING_BUBBLE, true)

        val showFacecam = prefs.getBoolean(KEY_SHOW_FACECAM, false)
        val shapeName = prefs.getString(KEY_FACECAM_SHAPE, FacecamShape.CIRCLE.name)
        val facecamShape = try {
            FacecamShape.valueOf(shapeName ?: FacecamShape.CIRCLE.name)
        } catch (e: Exception) {
            FacecamShape.CIRCLE
        }

        val sizeName = prefs.getString(KEY_FACECAM_SIZE, FacecamSize.MEDIUM.name)
        val facecamSize = try {
            FacecamSize.valueOf(sizeName ?: FacecamSize.MEDIUM.name)
        } catch (e: Exception) {
            FacecamSize.MEDIUM
        }

        val isFrontCamera = prefs.getBoolean(KEY_FACECAM_FRONT, true)
        val beautyFilter = prefs.getBoolean(KEY_BEAUTY_FILTER, false)
        val facecamRgbBorder = prefs.getBoolean(KEY_FACECAM_RGB, false)
        val showTouchVisualizer = prefs.getBoolean(KEY_SHOW_TOUCH_VISUALIZER, false)
        val touchColorName = prefs.getString(KEY_TOUCH_COLOR, com.example.model.TouchColorOption.CYAN.name)
        val touchColor = try {
            com.example.model.TouchColorOption.valueOf(touchColorName ?: com.example.model.TouchColorOption.CYAN.name)
        } catch (e: Exception) {
            com.example.model.TouchColorOption.CYAN
        }

        val showWatermark = prefs.getBoolean(KEY_SHOW_WATERMARK, false)
        val wmTypeName = prefs.getString(KEY_WATERMARK_TYPE, com.example.model.WatermarkType.TEXT.name)
        val watermarkType = try {
            com.example.model.WatermarkType.valueOf(wmTypeName ?: com.example.model.WatermarkType.TEXT.name)
        } catch (e: Exception) {
            com.example.model.WatermarkType.TEXT
        }
        val watermarkText = prefs.getString(KEY_WATERMARK_TEXT, "🌪️ Vortex Studio") ?: "🌪️ Vortex Studio"
        val watermarkOpacity = prefs.getFloat(KEY_WATERMARK_OPACITY, 0.85f)
        val wmSizeName = prefs.getString(KEY_WATERMARK_SIZE, com.example.model.WatermarkSize.MEDIUM.name)
        val watermarkSize = try {
            com.example.model.WatermarkSize.valueOf(wmSizeName ?: com.example.model.WatermarkSize.MEDIUM.name)
        } catch (e: Exception) {
            com.example.model.WatermarkSize.MEDIUM
        }
        val wmColorName = prefs.getString(KEY_WATERMARK_COLOR, com.example.model.TouchColorOption.CYAN.name)
        val watermarkColor = try {
            com.example.model.TouchColorOption.valueOf(wmColorName ?: com.example.model.TouchColorOption.CYAN.name)
        } catch (e: Exception) {
            com.example.model.TouchColorOption.CYAN
        }
        val watermarkCustomImageUri = prefs.getString(KEY_WATERMARK_IMAGE_URI, null)

        val showSceneOverlay = prefs.getBoolean(KEY_SHOW_SCENE_OVERLAY, false)
        val sceneTypeName = prefs.getString(KEY_SCENE_OVERLAY_TYPE, com.example.model.SceneOverlayType.GAMER_NEON_FRAME.name)
        val sceneOverlayType = try {
            com.example.model.SceneOverlayType.valueOf(sceneTypeName ?: com.example.model.SceneOverlayType.GAMER_NEON_FRAME.name)
        } catch (e: Exception) {
            com.example.model.SceneOverlayType.GAMER_NEON_FRAME
        }
        val sceneOverlayText = prefs.getString(KEY_SCENE_OVERLAY_TEXT, "🔴 EN VIVO | @TuCanal") ?: "🔴 EN VIVO | @TuCanal"
        val sceneOverlayOpacity = prefs.getFloat(KEY_SCENE_OVERLAY_OPACITY, 0.90f)
        val sceneOverlayImageUri = prefs.getString(KEY_SCENE_OVERLAY_IMAGE_URI, null)

        return RecordingConfig(
            resolution = resolution,
            fps = fps,
            bitrate = bitrate,
            audioSource = audioSource,
            countdownSeconds = countdown,
            isGameMode = isGameMode,
            showFloatingBubble = showFloatingBubble,
            showFacecam = showFacecam,
            facecamShape = facecamShape,
            facecamSize = facecamSize,
            isFrontCamera = isFrontCamera,
            beautyFilterEnabled = beautyFilter,
            facecamRgbBorder = facecamRgbBorder,
            showTouchVisualizer = showTouchVisualizer,
            touchVisualizerColor = touchColor,
            showWatermark = showWatermark,
            watermarkType = watermarkType,
            watermarkText = watermarkText,
            watermarkOpacity = watermarkOpacity,
            watermarkSize = watermarkSize,
            watermarkColor = watermarkColor,
            watermarkCustomImageUri = watermarkCustomImageUri,
            showSceneOverlay = showSceneOverlay,
            sceneOverlayType = sceneOverlayType,
            sceneOverlayText = sceneOverlayText,
            sceneOverlayOpacity = sceneOverlayOpacity,
            sceneOverlayImageUri = sceneOverlayImageUri
        )
    }

    fun saveConfig(config: RecordingConfig) {
        prefs.edit().apply {
            putString(KEY_RESOLUTION, config.resolution.name)
            putString(KEY_FPS, config.fps.name)
            putString(KEY_BITRATE, config.bitrate.name)
            putString(KEY_AUDIO_SOURCE, config.audioSource.name)
            putInt(KEY_COUNTDOWN, config.countdownSeconds)
            putBoolean(KEY_GAME_MODE, config.isGameMode)
            putBoolean(KEY_FLOATING_BUBBLE, config.showFloatingBubble)
            putBoolean(KEY_SHOW_FACECAM, config.showFacecam)
            putString(KEY_FACECAM_SHAPE, config.facecamShape.name)
            putString(KEY_FACECAM_SIZE, config.facecamSize.name)
            putBoolean(KEY_FACECAM_FRONT, config.isFrontCamera)
            putBoolean(KEY_BEAUTY_FILTER, config.beautyFilterEnabled)
            putBoolean(KEY_FACECAM_RGB, config.facecamRgbBorder)
            putBoolean(KEY_SHOW_TOUCH_VISUALIZER, config.showTouchVisualizer)
            putString(KEY_TOUCH_COLOR, config.touchVisualizerColor.name)
            putBoolean(KEY_SHOW_WATERMARK, config.showWatermark)
            putString(KEY_WATERMARK_TYPE, config.watermarkType.name)
            putString(KEY_WATERMARK_TEXT, config.watermarkText)
            putFloat(KEY_WATERMARK_OPACITY, config.watermarkOpacity)
            putString(KEY_WATERMARK_SIZE, config.watermarkSize.name)
            putString(KEY_WATERMARK_COLOR, config.watermarkColor.name)
            putString(KEY_WATERMARK_IMAGE_URI, config.watermarkCustomImageUri)
            putBoolean(KEY_SHOW_SCENE_OVERLAY, config.showSceneOverlay)
            putString(KEY_SCENE_OVERLAY_TYPE, config.sceneOverlayType.name)
            putString(KEY_SCENE_OVERLAY_TEXT, config.sceneOverlayText)
            putFloat(KEY_SCENE_OVERLAY_OPACITY, config.sceneOverlayOpacity)
            putString(KEY_SCENE_OVERLAY_IMAGE_URI, config.sceneOverlayImageUri)
            apply()
        }
        _configFlow.value = config
    }

    fun updateResolution(resolution: VideoResolution) {
        val updated = _configFlow.value.copy(resolution = resolution)
        saveConfig(updated)
    }

    fun updateFps(fps: VideoFps) {
        val updated = _configFlow.value.copy(fps = fps)
        saveConfig(updated)
    }

    fun updateBitrate(bitrate: VideoBitrate) {
        val updated = _configFlow.value.copy(bitrate = bitrate)
        saveConfig(updated)
    }

    fun updateAudioSource(source: AudioSourceType) {
        val updated = _configFlow.value.copy(audioSource = source)
        saveConfig(updated)
    }

    fun updateCountdown(seconds: Int) {
        val updated = _configFlow.value.copy(countdownSeconds = seconds)
        saveConfig(updated)
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        val updated = _configFlow.value.copy(showFloatingBubble = enabled)
        saveConfig(updated)
    }

    fun toggleFacecam(enabled: Boolean) {
        val updated = _configFlow.value.copy(showFacecam = enabled)
        saveConfig(updated)
    }

    fun updateFacecamShape(shape: FacecamShape) {
        val updated = _configFlow.value.copy(facecamShape = shape)
        saveConfig(updated)
    }

    fun updateFacecamSize(size: FacecamSize) {
        val updated = _configFlow.value.copy(facecamSize = size)
        saveConfig(updated)
    }

    fun toggleFacecamCamera() {
        val current = _configFlow.value.isFrontCamera
        val updated = _configFlow.value.copy(isFrontCamera = !current)
        saveConfig(updated)
    }

    fun setFacecamCamera(isFront: Boolean) {
        val updated = _configFlow.value.copy(isFrontCamera = isFront)
        saveConfig(updated)
    }

    fun toggleBeautyFilter(enabled: Boolean) {
        val updated = _configFlow.value.copy(beautyFilterEnabled = enabled)
        saveConfig(updated)
    }

    fun toggleFacecamRgbBorder(enabled: Boolean) {
        val updated = _configFlow.value.copy(facecamRgbBorder = enabled)
        saveConfig(updated)
    }

    fun toggleTouchVisualizer(enabled: Boolean) {
        val updated = _configFlow.value.copy(showTouchVisualizer = enabled)
        saveConfig(updated)
    }

    fun updateTouchVisualizerColor(color: com.example.model.TouchColorOption) {
        val updated = _configFlow.value.copy(touchVisualizerColor = color)
        saveConfig(updated)
    }

    fun toggleWatermark(enabled: Boolean) {
        val updated = _configFlow.value.copy(showWatermark = enabled)
        saveConfig(updated)
    }

    fun updateWatermarkType(type: com.example.model.WatermarkType) {
        val updated = _configFlow.value.copy(watermarkType = type)
        saveConfig(updated)
    }

    fun updateWatermarkText(text: String) {
        val updated = _configFlow.value.copy(watermarkText = text)
        saveConfig(updated)
    }

    fun updateWatermarkOpacity(opacity: Float) {
        val updated = _configFlow.value.copy(watermarkOpacity = opacity)
        saveConfig(updated)
    }

    fun updateWatermarkSize(size: com.example.model.WatermarkSize) {
        val updated = _configFlow.value.copy(watermarkSize = size)
        saveConfig(updated)
    }

    fun updateWatermarkColor(color: com.example.model.TouchColorOption) {
        val updated = _configFlow.value.copy(watermarkColor = color)
        saveConfig(updated)
    }

    fun updateWatermarkImageUri(uri: String?) {
        val updated = _configFlow.value.copy(watermarkCustomImageUri = uri)
        saveConfig(updated)
    }

    fun toggleSceneOverlay(enabled: Boolean) {
        val updated = _configFlow.value.copy(showSceneOverlay = enabled)
        saveConfig(updated)
    }

    fun updateSceneOverlayType(type: com.example.model.SceneOverlayType) {
        val updated = _configFlow.value.copy(sceneOverlayType = type)
        saveConfig(updated)
    }

    fun updateSceneOverlayText(text: String) {
        val updated = _configFlow.value.copy(sceneOverlayText = text)
        saveConfig(updated)
    }

    fun updateSceneOverlayOpacity(opacity: Float) {
        val updated = _configFlow.value.copy(sceneOverlayOpacity = opacity)
        saveConfig(updated)
    }

    fun updateSceneOverlayImageUri(uri: String?) {
        val updated = _configFlow.value.copy(sceneOverlayImageUri = uri)
        saveConfig(updated)
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
        private const val KEY_RESOLUTION = "pref_resolution"
        private const val KEY_FPS = "pref_fps"
        private const val KEY_BITRATE = "pref_bitrate"
        private const val KEY_AUDIO_SOURCE = "pref_audio_source"
        private const val KEY_COUNTDOWN = "pref_countdown"
        private const val KEY_GAME_MODE = "pref_game_mode"
        private const val KEY_FLOATING_BUBBLE = "pref_floating_bubble"
        private const val KEY_SHOW_FACECAM = "pref_show_facecam"
        private const val KEY_FACECAM_SHAPE = "pref_facecam_shape"
        private const val KEY_FACECAM_SIZE = "pref_facecam_size"
        private const val KEY_FACECAM_FRONT = "pref_facecam_front"
        private const val KEY_BEAUTY_FILTER = "pref_beauty_filter"
        private const val KEY_FACECAM_RGB = "pref_facecam_rgb"
        private const val KEY_SHOW_TOUCH_VISUALIZER = "pref_show_touch_visualizer"
        private const val KEY_TOUCH_COLOR = "pref_touch_color"
        private const val KEY_SHOW_WATERMARK = "pref_show_watermark"
        private const val KEY_WATERMARK_TYPE = "pref_watermark_type"
        private const val KEY_WATERMARK_TEXT = "pref_watermark_text"
        private const val KEY_WATERMARK_OPACITY = "pref_watermark_opacity"
        private const val KEY_WATERMARK_SIZE = "pref_watermark_size"
        private const val KEY_WATERMARK_COLOR = "pref_watermark_color"
        private const val KEY_WATERMARK_IMAGE_URI = "pref_watermark_image_uri"
        private const val KEY_SHOW_SCENE_OVERLAY = "pref_show_scene_overlay"
        private const val KEY_SCENE_OVERLAY_TYPE = "pref_scene_overlay_type"
        private const val KEY_SCENE_OVERLAY_TEXT = "pref_scene_overlay_text"
        private const val KEY_SCENE_OVERLAY_OPACITY = "pref_scene_overlay_opacity"
        private const val KEY_SCENE_OVERLAY_IMAGE_URI = "pref_scene_overlay_image_uri"
    }
}
