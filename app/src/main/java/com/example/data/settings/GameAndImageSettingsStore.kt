package com.example.data.settings

import android.content.SharedPreferences
import com.example.model.ImageFormatOption
import com.example.model.RecordingConfig

/**
 * Gestor de persistencia para modo de juego, cuenta atrás y formato de capturas de pantalla.
 */
class GameAndImageSettingsStore(private val prefs: SharedPreferences) {

    data class GameAndImageSlice(
        val countdownSeconds: Int,
        val isGameMode: Boolean,
        val imageFormat: ImageFormatOption,
        val imageQuality: Int,
        val imageWebpLossless: Boolean
    )

    fun load(): GameAndImageSlice {
        val countdown = prefs.getInt(KEY_COUNTDOWN, 3)
        val isGameMode = prefs.getBoolean(KEY_GAME_MODE, true)

        val imgFormatName = prefs.getString(KEY_IMAGE_FORMAT, ImageFormatOption.PNG.name)
        val imageFormat = try {
            ImageFormatOption.valueOf(imgFormatName ?: ImageFormatOption.PNG.name)
        } catch (_: Exception) {
            ImageFormatOption.PNG
        }
        val imageQuality = prefs.getInt(KEY_IMAGE_QUALITY, 80).coerceIn(10, 100)
        val imageWebpLossless = prefs.getBoolean(KEY_IMAGE_WEBP_LOSSLESS, false)

        return GameAndImageSlice(
            countdownSeconds = countdown,
            isGameMode = isGameMode,
            imageFormat = imageFormat,
            imageQuality = imageQuality,
            imageWebpLossless = imageWebpLossless
        )
    }

    fun save(editor: SharedPreferences.Editor, config: RecordingConfig) {
        editor.putInt(KEY_COUNTDOWN, config.countdownSeconds)
        editor.putBoolean(KEY_GAME_MODE, config.isGameMode)
        editor.putString(KEY_IMAGE_FORMAT, config.imageFormat.name)
        editor.putInt(KEY_IMAGE_QUALITY, config.imageQuality)
        editor.putBoolean(KEY_IMAGE_WEBP_LOSSLESS, config.imageWebpLossless)
    }

    companion object {
        const val KEY_COUNTDOWN = "pref_countdown"
        const val KEY_GAME_MODE = "pref_game_mode"
        const val KEY_IMAGE_FORMAT = "pref_image_format"
        const val KEY_IMAGE_QUALITY = "pref_image_quality"
        const val KEY_IMAGE_WEBP_LOSSLESS = "pref_image_webp_lossless"
    }
}
