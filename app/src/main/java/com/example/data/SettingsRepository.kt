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
 * Repositorio de persistencia local para la configuración de grabación de Vortex Studio.
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

        val bitrateMbps = prefs.getInt(KEY_BITRATE_MBPS, 8).coerceIn(1, 12)

        val facecamFpsName = prefs.getString(KEY_FACECAM_FPS, com.example.model.FacecamFps.FPS_30.name)
        val facecamFps = try {
            com.example.model.FacecamFps.valueOf(facecamFpsName ?: com.example.model.FacecamFps.FPS_30.name)
        } catch (e: Exception) {
            com.example.model.FacecamFps.FPS_30
        }

        val audioRateName = prefs.getString(KEY_AUDIO_SAMPLE_RATE, com.example.model.AudioSampleRate.RATE_48000.name)
        val audioSampleRate = try {
            com.example.model.AudioSampleRate.valueOf(audioRateName ?: com.example.model.AudioSampleRate.RATE_48000.name)
        } catch (e: Exception) {
            com.example.model.AudioSampleRate.RATE_48000
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

        val showVtuber = prefs.getBoolean(KEY_SHOW_VTUBER, false)
        val vtuberPresetName = prefs.getString(KEY_VTUBER_PRESET, com.example.model.VtuberPreset.CYBER_CAT.name)
        val vtuberPreset = try {
            com.example.model.VtuberPreset.valueOf(vtuberPresetName ?: com.example.model.VtuberPreset.CYBER_CAT.name)
        } catch (e: Exception) {
            com.example.model.VtuberPreset.CYBER_CAT
        }
        val vtuberSizeName = prefs.getString(KEY_VTUBER_SIZE, com.example.model.VtuberSize.MEDIUM.name)
        val vtuberSize = try {
            com.example.model.VtuberSize.valueOf(vtuberSizeName ?: com.example.model.VtuberSize.MEDIUM.name)
        } catch (e: Exception) {
            com.example.model.VtuberSize.MEDIUM
        }
        val vtuberSensitivity = prefs.getFloat(KEY_VTUBER_SENSITIVITY, 0.18f)
        val vtuberBounceEnabled = prefs.getBoolean(KEY_VTUBER_BOUNCE, true)
        val vtuberIdleUri = prefs.getString(KEY_VTUBER_IDLE_URI, null)
        val vtuberTalkUri = prefs.getString(KEY_VTUBER_TALK_URI, null)
        val vtuberBlinkUri = prefs.getString(KEY_VTUBER_BLINK_URI, null)
        val vtuberBlinkTalkUri = prefs.getString(KEY_VTUBER_BLINK_TALK_URI, null)
        val gameGain = prefs.getFloat(KEY_GAME_AUDIO_GAIN, 1.0f)
        val micGain = prefs.getFloat(KEY_MIC_AUDIO_GAIN, 1.25f)
        val audioDucking = prefs.getBoolean(KEY_AUDIO_DUCKING, true)
        val noiseGate = prefs.getBoolean(KEY_NOISE_GATE, true)
        val showVuMeter = prefs.getBoolean(KEY_SHOW_FLOATING_VU_METER, false)

        val imgFormatName = prefs.getString(KEY_IMAGE_FORMAT, com.example.model.ImageFormatOption.PNG.name)
        val imageFormat = try {
            com.example.model.ImageFormatOption.valueOf(imgFormatName ?: com.example.model.ImageFormatOption.PNG.name)
        } catch (e: Exception) {
            com.example.model.ImageFormatOption.PNG
        }
        val imageQuality = prefs.getInt(KEY_IMAGE_QUALITY, 80).coerceIn(10, 100)
        val imageWebpLossless = prefs.getBoolean(KEY_IMAGE_WEBP_LOSSLESS, false)

        return RecordingConfig(
            resolution = resolution,
            fps = fps,
            bitrate = bitrate,
            bitrateMbps = bitrateMbps,
            facecamFps = facecamFps,
            audioSampleRate = audioSampleRate,
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
            sceneOverlayImageUri = sceneOverlayImageUri,
            showVtuber = showVtuber,
            vtuberPreset = vtuberPreset,
            vtuberSize = vtuberSize,
            vtuberSensitivity = vtuberSensitivity,
            vtuberBounceEnabled = vtuberBounceEnabled,
            vtuberIdleImageUri = vtuberIdleUri,
            vtuberTalkImageUri = vtuberTalkUri,
            vtuberBlinkImageUri = vtuberBlinkUri,
            vtuberBlinkTalkImageUri = vtuberBlinkTalkUri,
            gameAudioGain = gameGain,
            micAudioGain = micGain,
            audioDuckingEnabled = audioDucking,
            noiseGateEnabled = noiseGate,
            showFloatingVuMeter = showVuMeter,
            imageFormat = imageFormat,
            imageQuality = imageQuality,
            imageWebpLossless = imageWebpLossless
        )
    }

    fun saveConfig(config: RecordingConfig) {
        prefs.edit().apply {
            putString(KEY_RESOLUTION, config.resolution.name)
            putString(KEY_FPS, config.fps.name)
            putString(KEY_BITRATE, config.bitrate.name)
            putInt(KEY_BITRATE_MBPS, config.bitrateMbps)
            putString(KEY_FACECAM_FPS, config.facecamFps.name)
            putString(KEY_AUDIO_SAMPLE_RATE, config.audioSampleRate.name)
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
            putBoolean(KEY_SHOW_VTUBER, config.showVtuber)
            putString(KEY_VTUBER_PRESET, config.vtuberPreset.name)
            putString(KEY_VTUBER_SIZE, config.vtuberSize.name)
            putFloat(KEY_VTUBER_SENSITIVITY, config.vtuberSensitivity)
            putBoolean(KEY_VTUBER_BOUNCE, config.vtuberBounceEnabled)
            putString(KEY_VTUBER_IDLE_URI, config.vtuberIdleImageUri)
            putString(KEY_VTUBER_TALK_URI, config.vtuberTalkImageUri)
            putString(KEY_VTUBER_BLINK_URI, config.vtuberBlinkImageUri)
            putString(KEY_VTUBER_BLINK_TALK_URI, config.vtuberBlinkTalkImageUri)
            putFloat(KEY_GAME_AUDIO_GAIN, config.gameAudioGain)
            putFloat(KEY_MIC_AUDIO_GAIN, config.micAudioGain)
            putBoolean(KEY_AUDIO_DUCKING, config.audioDuckingEnabled)
            putBoolean(KEY_NOISE_GATE, config.noiseGateEnabled)
            putBoolean(KEY_SHOW_FLOATING_VU_METER, config.showFloatingVuMeter)
            putString(KEY_IMAGE_FORMAT, config.imageFormat.name)
            putInt(KEY_IMAGE_QUALITY, config.imageQuality)
            putBoolean(KEY_IMAGE_WEBP_LOSSLESS, config.imageWebpLossless)
            apply()
        }
        _configFlow.value = config
    }

    fun updateImageFormat(format: com.example.model.ImageFormatOption) {
        val updated = _configFlow.value.copy(imageFormat = format)
        saveConfig(updated)
    }

    fun updateImageQuality(quality: Int) {
        val clamped = quality.coerceIn(10, 100)
        val updated = _configFlow.value.copy(imageQuality = clamped)
        saveConfig(updated)
    }

    fun toggleImageWebpLossless(lossless: Boolean) {
        val updated = _configFlow.value.copy(imageWebpLossless = lossless)
        saveConfig(updated)
    }

    fun updateGameAudioGain(gain: Float) {
        val clamped = gain.coerceIn(0.0f, 2.5f)
        val updated = _configFlow.value.copy(gameAudioGain = clamped)
        saveConfig(updated)
    }

    fun updateMicAudioGain(gain: Float) {
        val clamped = gain.coerceIn(0.0f, 2.5f)
        val updated = _configFlow.value.copy(micAudioGain = clamped)
        saveConfig(updated)
    }

    fun toggleAudioDucking(enabled: Boolean) {
        val updated = _configFlow.value.copy(audioDuckingEnabled = enabled)
        saveConfig(updated)
    }

    fun toggleNoiseGate(enabled: Boolean) {
        val updated = _configFlow.value.copy(noiseGateEnabled = enabled)
        saveConfig(updated)
    }

    fun toggleFloatingVuMeter(enabled: Boolean) {
        val updated = _configFlow.value.copy(showFloatingVuMeter = enabled)
        saveConfig(updated)
    }

    fun toggleVtuber(enabled: Boolean) {
        val updated = _configFlow.value.copy(showVtuber = enabled)
        saveConfig(updated)
    }

    fun updateVtuberPreset(preset: com.example.model.VtuberPreset) {
        val updated = _configFlow.value.copy(vtuberPreset = preset)
        saveConfig(updated)
    }

    fun updateVtuberSize(size: com.example.model.VtuberSize) {
        val updated = _configFlow.value.copy(vtuberSize = size)
        saveConfig(updated)
    }

    fun updateVtuberSensitivity(sensitivity: Float) {
        val updated = _configFlow.value.copy(vtuberSensitivity = sensitivity)
        saveConfig(updated)
    }

    fun toggleVtuberBounce(enabled: Boolean) {
        val updated = _configFlow.value.copy(vtuberBounceEnabled = enabled)
        saveConfig(updated)
    }

    fun updateVtuberIdleUri(uri: String?) {
        val updated = _configFlow.value.copy(
            vtuberIdleImageUri = uri,
            vtuberPreset = com.example.model.VtuberPreset.CUSTOM
        )
        saveConfig(updated)
    }

    fun updateVtuberTalkUri(uri: String?) {
        val updated = _configFlow.value.copy(
            vtuberTalkImageUri = uri,
            vtuberPreset = com.example.model.VtuberPreset.CUSTOM
        )
        saveConfig(updated)
    }

    fun updateVtuberBlinkUri(uri: String?) {
        val updated = _configFlow.value.copy(
            vtuberBlinkImageUri = uri,
            vtuberPreset = com.example.model.VtuberPreset.CUSTOM
        )
        saveConfig(updated)
    }

    fun updateVtuberBlinkTalkUri(uri: String?) {
        val updated = _configFlow.value.copy(
            vtuberBlinkTalkImageUri = uri,
            vtuberPreset = com.example.model.VtuberPreset.CUSTOM
        )
        saveConfig(updated)
    }

    fun updateVtuberCustomImages(
        idleUri: String?,
        talkUri: String?,
        blinkUri: String? = null,
        blinkTalkUri: String? = null
    ) {
        val updated = _configFlow.value.copy(
            vtuberIdleImageUri = idleUri,
            vtuberTalkImageUri = talkUri,
            vtuberBlinkImageUri = blinkUri,
            vtuberBlinkTalkImageUri = blinkTalkUri,
            vtuberPreset = com.example.model.VtuberPreset.CUSTOM
        )
        saveConfig(updated)
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
        val mbps = when (bitrate) {
            VideoBitrate.BITRATE_12M -> 12
            VideoBitrate.BITRATE_8M -> 8
            VideoBitrate.BITRATE_4M -> 4
        }
        val updated = _configFlow.value.copy(bitrate = bitrate, bitrateMbps = mbps)
        saveConfig(updated)
    }

    fun updateBitrateMbps(mbps: Int) {
        val clamped = mbps.coerceIn(1, 12)
        val legacy = when {
            clamped >= 12 -> VideoBitrate.BITRATE_12M
            clamped >= 8 -> VideoBitrate.BITRATE_8M
            else -> VideoBitrate.BITRATE_4M
        }
        val updated = _configFlow.value.copy(bitrateMbps = clamped, bitrate = legacy)
        saveConfig(updated)
    }

    fun updateFacecamFps(fps: com.example.model.FacecamFps) {
        val updated = _configFlow.value.copy(facecamFps = fps)
        saveConfig(updated)
    }

    fun updateAudioSampleRate(sampleRate: com.example.model.AudioSampleRate) {
        val updated = _configFlow.value.copy(audioSampleRate = sampleRate)
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
        private const val KEY_BITRATE_MBPS = "pref_bitrate_mbps"
        private const val KEY_FACECAM_FPS = "pref_facecam_fps"
        private const val KEY_AUDIO_SAMPLE_RATE = "pref_audio_sample_rate"
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
        private const val KEY_SHOW_VTUBER = "pref_show_vtuber"
        private const val KEY_VTUBER_PRESET = "pref_vtuber_preset"
        private const val KEY_VTUBER_SIZE = "pref_vtuber_size"
        private const val KEY_VTUBER_SENSITIVITY = "pref_vtuber_sensitivity"
        private const val KEY_VTUBER_BOUNCE = "pref_vtuber_bounce"
        private const val KEY_VTUBER_IDLE_URI = "pref_vtuber_idle_uri"
        private const val KEY_VTUBER_TALK_URI = "pref_vtuber_talk_uri"
        private const val KEY_VTUBER_BLINK_URI = "pref_vtuber_blink_uri"
        private const val KEY_VTUBER_BLINK_TALK_URI = "pref_vtuber_blink_talk_uri"
        private const val KEY_GAME_AUDIO_GAIN = "pref_game_audio_gain"
        private const val KEY_MIC_AUDIO_GAIN = "pref_mic_audio_gain"
        private const val KEY_AUDIO_DUCKING = "pref_audio_ducking"
        private const val KEY_NOISE_GATE = "pref_noise_gate"
        private const val KEY_SHOW_FLOATING_VU_METER = "pref_show_floating_vu_meter"
        private const val KEY_IMAGE_FORMAT = "pref_image_format"
        private const val KEY_IMAGE_QUALITY = "pref_image_quality"
        private const val KEY_IMAGE_WEBP_LOSSLESS = "pref_image_webp_lossless"
    }
}
