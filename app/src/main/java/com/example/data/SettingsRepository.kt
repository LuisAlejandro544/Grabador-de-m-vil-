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
            touchVisualizerColor = touchColor
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
    }
}
