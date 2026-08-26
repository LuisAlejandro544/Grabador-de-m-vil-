package com.example.data.settings

import android.content.SharedPreferences
import com.example.model.FacecamShape
import com.example.model.FacecamSize
import com.example.model.RecordingConfig
import com.example.model.SceneOverlayType
import com.example.model.TouchColorOption
import com.example.model.VtuberPreset
import com.example.model.VtuberSize
import com.example.model.WatermarkSize
import com.example.model.WatermarkType

/**
 * Gestor de persistencia y serialización para superposiciones en pantalla:
 * Facecam, VTuber/PNGtuber, Watermark, Scene Overlay, Touch Visualizer, Vúmetro y Burbuja.
 */
class OverlaySettingsStore(private val prefs: SharedPreferences) {

    data class OverlayConfigSlice(
        val showFloatingBubble: Boolean,
        val showFacecam: Boolean,
        val facecamShape: FacecamShape,
        val facecamSize: FacecamSize,
        val isFrontCamera: Boolean,
        val beautyFilterEnabled: Boolean,
        val facecamRgbBorder: Boolean,
        val showTouchVisualizer: Boolean,
        val touchVisualizerColor: TouchColorOption,
        val showWatermark: Boolean,
        val watermarkType: WatermarkType,
        val watermarkText: String,
        val watermarkOpacity: Float,
        val watermarkSize: WatermarkSize,
        val watermarkColor: TouchColorOption,
        val watermarkCustomImageUri: String?,
        val showSceneOverlay: Boolean,
        val sceneOverlayType: SceneOverlayType,
        val sceneOverlayText: String,
        val sceneOverlayOpacity: Float,
        val sceneOverlayImageUri: String?,
        val showVtuber: Boolean,
        val vtuberPreset: VtuberPreset,
        val vtuberTrackingMode: com.example.model.VtuberTrackingMode,
        val vtuberSize: VtuberSize,
        val vtuberSensitivity: Float,
        val vtuberBounceEnabled: Boolean,
        val vtuberHeadTiltEnabled: Boolean,
        val vtuberEyeBlinkSensitivity: Float,
        val vtuberMouthSensitivity: Float,
        val vtuberIdleImageUri: String?,
        val vtuberTalkImageUri: String?,
        val vtuberBlinkImageUri: String?,
        val vtuberBlinkTalkImageUri: String?,
        val showFloatingVuMeter: Boolean,
        val showTouchAvatar: Boolean,
        val touchAvatarGenre: com.example.model.TouchAvatarGenre,
        val touchAvatarSize: com.example.model.TouchAvatarSize,
        val touchAvatarOpacity: Float,
        val touchAvatarVoiceSync: Boolean,
        val touchAvatarCustomImageUri: String?
    )

    fun load(): OverlayConfigSlice {
        val showFloatingBubble = prefs.getBoolean(KEY_FLOATING_BUBBLE, true)
        val showFacecam = prefs.getBoolean(KEY_SHOW_FACECAM, false)
        val shapeName = prefs.getString(KEY_FACECAM_SHAPE, FacecamShape.CIRCLE.name)
        val facecamShape = try {
            FacecamShape.valueOf(shapeName ?: FacecamShape.CIRCLE.name)
        } catch (_: Exception) {
            FacecamShape.CIRCLE
        }

        val sizeName = prefs.getString(KEY_FACECAM_SIZE, FacecamSize.MEDIUM.name)
        val facecamSize = try {
            FacecamSize.valueOf(sizeName ?: FacecamSize.MEDIUM.name)
        } catch (_: Exception) {
            FacecamSize.MEDIUM
        }

        val isFrontCamera = prefs.getBoolean(KEY_FACECAM_FRONT, true)
        val beautyFilter = prefs.getBoolean(KEY_BEAUTY_FILTER, false)
        val facecamRgbBorder = prefs.getBoolean(KEY_FACECAM_RGB, false)
        val showTouchVisualizer = prefs.getBoolean(KEY_SHOW_TOUCH_VISUALIZER, false)

        val touchColorName = prefs.getString(KEY_TOUCH_COLOR, TouchColorOption.CYAN.name)
        val touchColor = try {
            TouchColorOption.valueOf(touchColorName ?: TouchColorOption.CYAN.name)
        } catch (_: Exception) {
            TouchColorOption.CYAN
        }

        val showWatermark = prefs.getBoolean(KEY_SHOW_WATERMARK, false)
        val wmTypeName = prefs.getString(KEY_WATERMARK_TYPE, WatermarkType.TEXT.name)
        val watermarkType = try {
            WatermarkType.valueOf(wmTypeName ?: WatermarkType.TEXT.name)
        } catch (_: Exception) {
            WatermarkType.TEXT
        }
        val watermarkText = prefs.getString(KEY_WATERMARK_TEXT, "🌪️ Vortex Studio") ?: "🌪️ Vortex Studio"
        val watermarkOpacity = prefs.getFloat(KEY_WATERMARK_OPACITY, 0.85f)
        val wmSizeName = prefs.getString(KEY_WATERMARK_SIZE, WatermarkSize.MEDIUM.name)
        val watermarkSize = try {
            WatermarkSize.valueOf(wmSizeName ?: WatermarkSize.MEDIUM.name)
        } catch (_: Exception) {
            WatermarkSize.MEDIUM
        }
        val wmColorName = prefs.getString(KEY_WATERMARK_COLOR, TouchColorOption.CYAN.name)
        val watermarkColor = try {
            TouchColorOption.valueOf(wmColorName ?: TouchColorOption.CYAN.name)
        } catch (_: Exception) {
            TouchColorOption.CYAN
        }
        val watermarkCustomImageUri = prefs.getString(KEY_WATERMARK_IMAGE_URI, null)

        val showSceneOverlay = prefs.getBoolean(KEY_SHOW_SCENE_OVERLAY, false)
        val sceneTypeName = prefs.getString(KEY_SCENE_OVERLAY_TYPE, SceneOverlayType.GAMER_NEON_FRAME.name)
        val sceneOverlayType = try {
            SceneOverlayType.valueOf(sceneTypeName ?: SceneOverlayType.GAMER_NEON_FRAME.name)
        } catch (_: Exception) {
            SceneOverlayType.GAMER_NEON_FRAME
        }
        val sceneOverlayText = prefs.getString(KEY_SCENE_OVERLAY_TEXT, "🔴 EN VIVO | @TuCanal") ?: "🔴 EN VIVO | @TuCanal"
        val sceneOverlayOpacity = prefs.getFloat(KEY_SCENE_OVERLAY_OPACITY, 0.90f)
        val sceneOverlayImageUri = prefs.getString(KEY_SCENE_OVERLAY_IMAGE_URI, null)

        val showVtuber = prefs.getBoolean(KEY_SHOW_VTUBER, false)
        val vtuberPresetName = prefs.getString(KEY_VTUBER_PRESET, VtuberPreset.CYBER_CAT.name)
        val vtuberPreset = try {
            VtuberPreset.valueOf(vtuberPresetName ?: VtuberPreset.CYBER_CAT.name)
        } catch (_: Exception) {
            VtuberPreset.CYBER_CAT
        }
        val vtuberTrackingModeName = prefs.getString(KEY_VTUBER_TRACKING_MODE, com.example.model.VtuberTrackingMode.VOICE_ONLY.name)
        val vtuberTrackingMode = try {
            com.example.model.VtuberTrackingMode.valueOf(vtuberTrackingModeName ?: com.example.model.VtuberTrackingMode.VOICE_ONLY.name)
        } catch (_: Exception) {
            com.example.model.VtuberTrackingMode.VOICE_ONLY
        }
        val vtuberSizeName = prefs.getString(KEY_VTUBER_SIZE, VtuberSize.MEDIUM.name)
        val vtuberSize = try {
            VtuberSize.valueOf(vtuberSizeName ?: VtuberSize.MEDIUM.name)
        } catch (_: Exception) {
            VtuberSize.MEDIUM
        }
        val vtuberSensitivity = prefs.getFloat(KEY_VTUBER_SENSITIVITY, 0.18f)
        val vtuberBounceEnabled = prefs.getBoolean(KEY_VTUBER_BOUNCE, true)
        val vtuberHeadTiltEnabled = prefs.getBoolean(KEY_VTUBER_HEAD_TILT, true)
        val vtuberEyeBlinkSensitivity = prefs.getFloat(KEY_VTUBER_EYE_BLINK_SENSITIVITY, 0.35f)
        val vtuberMouthSensitivity = prefs.getFloat(KEY_VTUBER_MOUTH_SENSITIVITY, 0.40f)
        val vtuberIdleUri = prefs.getString(KEY_VTUBER_IDLE_URI, null)
        val vtuberTalkUri = prefs.getString(KEY_VTUBER_TALK_URI, null)
        val vtuberBlinkUri = prefs.getString(KEY_VTUBER_BLINK_URI, null)
        val vtuberBlinkTalkUri = prefs.getString(KEY_VTUBER_BLINK_TALK_URI, null)
        val showVuMeter = prefs.getBoolean(KEY_SHOW_FLOATING_VU_METER, false)

        val showTouchAvatar = prefs.getBoolean(KEY_SHOW_TOUCH_AVATAR, false)
        val genreName = prefs.getString(KEY_TOUCH_AVATAR_GENRE, com.example.model.TouchAvatarGenre.RHYTHM_4K.name)
        val touchAvatarGenre = try {
            com.example.model.TouchAvatarGenre.valueOf(genreName ?: com.example.model.TouchAvatarGenre.RHYTHM_4K.name)
        } catch (_: Exception) {
            com.example.model.TouchAvatarGenre.RHYTHM_4K
        }
        val sizeNameTouch = prefs.getString(KEY_TOUCH_AVATAR_SIZE, com.example.model.TouchAvatarSize.MEDIUM.name)
        val touchAvatarSize = try {
            com.example.model.TouchAvatarSize.valueOf(sizeNameTouch ?: com.example.model.TouchAvatarSize.MEDIUM.name)
        } catch (_: Exception) {
            com.example.model.TouchAvatarSize.MEDIUM
        }
        val touchAvatarOpacity = prefs.getFloat(KEY_TOUCH_AVATAR_OPACITY, 0.95f)
        val touchAvatarVoiceSync = prefs.getBoolean(KEY_TOUCH_AVATAR_VOICE_SYNC, true)
        val touchAvatarCustomUri = prefs.getString(KEY_TOUCH_AVATAR_CUSTOM_URI, null)

        return OverlayConfigSlice(
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
            vtuberTrackingMode = vtuberTrackingMode,
            vtuberSize = vtuberSize,
            vtuberSensitivity = vtuberSensitivity,
            vtuberBounceEnabled = vtuberBounceEnabled,
            vtuberHeadTiltEnabled = vtuberHeadTiltEnabled,
            vtuberEyeBlinkSensitivity = vtuberEyeBlinkSensitivity,
            vtuberMouthSensitivity = vtuberMouthSensitivity,
            vtuberIdleImageUri = vtuberIdleUri,
            vtuberTalkImageUri = vtuberTalkUri,
            vtuberBlinkImageUri = vtuberBlinkUri,
            vtuberBlinkTalkImageUri = vtuberBlinkTalkUri,
            showFloatingVuMeter = showVuMeter,
            showTouchAvatar = showTouchAvatar,
            touchAvatarGenre = touchAvatarGenre,
            touchAvatarSize = touchAvatarSize,
            touchAvatarOpacity = touchAvatarOpacity,
            touchAvatarVoiceSync = touchAvatarVoiceSync,
            touchAvatarCustomImageUri = touchAvatarCustomUri
        )
    }

    fun save(editor: SharedPreferences.Editor, config: RecordingConfig) {
        editor.putBoolean(KEY_FLOATING_BUBBLE, config.showFloatingBubble)
        editor.putBoolean(KEY_SHOW_FACECAM, config.showFacecam)
        editor.putString(KEY_FACECAM_SHAPE, config.facecamShape.name)
        editor.putString(KEY_FACECAM_SIZE, config.facecamSize.name)
        editor.putBoolean(KEY_FACECAM_FRONT, config.isFrontCamera)
        editor.putBoolean(KEY_BEAUTY_FILTER, config.beautyFilterEnabled)
        editor.putBoolean(KEY_FACECAM_RGB, config.facecamRgbBorder)
        editor.putBoolean(KEY_SHOW_TOUCH_VISUALIZER, config.showTouchVisualizer)
        editor.putString(KEY_TOUCH_COLOR, config.touchVisualizerColor.name)
        editor.putBoolean(KEY_SHOW_WATERMARK, config.showWatermark)
        editor.putString(KEY_WATERMARK_TYPE, config.watermarkType.name)
        editor.putString(KEY_WATERMARK_TEXT, config.watermarkText)
        editor.putFloat(KEY_WATERMARK_OPACITY, config.watermarkOpacity)
        editor.putString(KEY_WATERMARK_SIZE, config.watermarkSize.name)
        editor.putString(KEY_WATERMARK_COLOR, config.watermarkColor.name)
        editor.putString(KEY_WATERMARK_IMAGE_URI, config.watermarkCustomImageUri)
        editor.putBoolean(KEY_SHOW_SCENE_OVERLAY, config.showSceneOverlay)
        editor.putString(KEY_SCENE_OVERLAY_TYPE, config.sceneOverlayType.name)
        editor.putString(KEY_SCENE_OVERLAY_TEXT, config.sceneOverlayText)
        editor.putFloat(KEY_SCENE_OVERLAY_OPACITY, config.sceneOverlayOpacity)
        editor.putString(KEY_SCENE_OVERLAY_IMAGE_URI, config.sceneOverlayImageUri)
        editor.putBoolean(KEY_SHOW_VTUBER, config.showVtuber)
        editor.putString(KEY_VTUBER_PRESET, config.vtuberPreset.name)
        editor.putString(KEY_VTUBER_TRACKING_MODE, config.vtuberTrackingMode.name)
        editor.putString(KEY_VTUBER_SIZE, config.vtuberSize.name)
        editor.putFloat(KEY_VTUBER_SENSITIVITY, config.vtuberSensitivity)
        editor.putBoolean(KEY_VTUBER_BOUNCE, config.vtuberBounceEnabled)
        editor.putBoolean(KEY_VTUBER_HEAD_TILT, config.vtuberHeadTiltEnabled)
        editor.putFloat(KEY_VTUBER_EYE_BLINK_SENSITIVITY, config.vtuberEyeBlinkSensitivity)
        editor.putFloat(KEY_VTUBER_MOUTH_SENSITIVITY, config.vtuberMouthSensitivity)
        editor.putString(KEY_VTUBER_IDLE_URI, config.vtuberIdleImageUri)
        editor.putString(KEY_VTUBER_TALK_URI, config.vtuberTalkImageUri)
        editor.putString(KEY_VTUBER_BLINK_URI, config.vtuberBlinkImageUri)
        editor.putString(KEY_VTUBER_BLINK_TALK_URI, config.vtuberBlinkTalkImageUri)
        editor.putBoolean(KEY_SHOW_FLOATING_VU_METER, config.showFloatingVuMeter)
        editor.putBoolean(KEY_SHOW_TOUCH_AVATAR, config.showTouchAvatar)
        editor.putString(KEY_TOUCH_AVATAR_GENRE, config.touchAvatarGenre.name)
        editor.putString(KEY_TOUCH_AVATAR_SIZE, config.touchAvatarSize.name)
        editor.putFloat(KEY_TOUCH_AVATAR_OPACITY, config.touchAvatarOpacity)
        editor.putBoolean(KEY_TOUCH_AVATAR_VOICE_SYNC, config.touchAvatarVoiceSync)
        editor.putString(KEY_TOUCH_AVATAR_CUSTOM_URI, config.touchAvatarCustomImageUri)
    }

    companion object {
        const val KEY_FLOATING_BUBBLE = "pref_floating_bubble"
        const val KEY_SHOW_FACECAM = "pref_show_facecam"
        const val KEY_FACECAM_SHAPE = "pref_facecam_shape"
        const val KEY_FACECAM_SIZE = "pref_facecam_size"
        const val KEY_FACECAM_FRONT = "pref_facecam_front"
        const val KEY_BEAUTY_FILTER = "pref_beauty_filter"
        const val KEY_FACECAM_RGB = "pref_facecam_rgb"
        const val KEY_SHOW_TOUCH_VISUALIZER = "pref_show_touch_visualizer"
        const val KEY_TOUCH_COLOR = "pref_touch_color"
        const val KEY_SHOW_WATERMARK = "pref_show_watermark"
        const val KEY_WATERMARK_TYPE = "pref_watermark_type"
        const val KEY_WATERMARK_TEXT = "pref_watermark_text"
        const val KEY_WATERMARK_OPACITY = "pref_watermark_opacity"
        const val KEY_WATERMARK_SIZE = "pref_watermark_size"
        const val KEY_WATERMARK_COLOR = "pref_watermark_color"
        const val KEY_WATERMARK_IMAGE_URI = "pref_watermark_image_uri"
        const val KEY_SHOW_SCENE_OVERLAY = "pref_show_scene_overlay"
        const val KEY_SCENE_OVERLAY_TYPE = "pref_scene_overlay_type"
        const val KEY_SCENE_OVERLAY_TEXT = "pref_scene_overlay_text"
        const val KEY_SCENE_OVERLAY_OPACITY = "pref_scene_overlay_opacity"
        const val KEY_SCENE_OVERLAY_IMAGE_URI = "pref_scene_overlay_image_uri"
        const val KEY_SHOW_VTUBER = "pref_show_vtuber"
        const val KEY_VTUBER_PRESET = "pref_vtuber_preset"
        const val KEY_VTUBER_TRACKING_MODE = "pref_vtuber_tracking_mode"
        const val KEY_VTUBER_SIZE = "pref_vtuber_size"
        const val KEY_VTUBER_SENSITIVITY = "pref_vtuber_sensitivity"
        const val KEY_VTUBER_BOUNCE = "pref_vtuber_bounce"
        const val KEY_VTUBER_HEAD_TILT = "pref_vtuber_head_tilt"
        const val KEY_VTUBER_EYE_BLINK_SENSITIVITY = "pref_vtuber_eye_blink_sensitivity"
        const val KEY_VTUBER_MOUTH_SENSITIVITY = "pref_vtuber_mouth_sensitivity"
        const val KEY_VTUBER_IDLE_URI = "pref_vtuber_idle_uri"
        const val KEY_VTUBER_TALK_URI = "pref_vtuber_talk_uri"
        const val KEY_VTUBER_BLINK_URI = "pref_vtuber_blink_uri"
        const val KEY_VTUBER_BLINK_TALK_URI = "pref_vtuber_blink_talk_uri"
        const val KEY_SHOW_FLOATING_VU_METER = "pref_show_floating_vu_meter"
        const val KEY_SHOW_TOUCH_AVATAR = "pref_show_touch_avatar"
        const val KEY_TOUCH_AVATAR_GENRE = "pref_touch_avatar_genre"
        const val KEY_TOUCH_AVATAR_SIZE = "pref_touch_avatar_size"
        const val KEY_TOUCH_AVATAR_OPACITY = "pref_touch_avatar_opacity"
        const val KEY_TOUCH_AVATAR_VOICE_SYNC = "pref_touch_avatar_voice_sync"
        const val KEY_TOUCH_AVATAR_CUSTOM_URI = "pref_touch_avatar_custom_uri"
    }
}
