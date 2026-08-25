package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.data.SettingsRepository
import com.example.model.AudioSourceType
import com.example.model.RecordingConfig

/**
 * Datos estructurados y validados para inicializar una sesión de grabación en [ScreenRecordService].
 */
data class ServiceRecordingParams(
    val resultCode: Int,
    val resultData: Intent,
    val width: Int,
    val height: Int,
    val densityDpi: Int,
    val fps: Int,
    val bitrate: Int,
    val audioSource: String,
    val sampleRate: Int,
    val avSyncOffsetMs: Int,
    val countdownSeconds: Int,
    val showFloatingBubble: Boolean,
    val showFacecam: Boolean,
    val savedConfig: RecordingConfig
)

/**
 * Extractor y validador desacoplado de parámetros para el servicio de grabación.
 */
object ServiceParamsExtractor {

    fun extractParams(context: Context, intent: Intent): ServiceRecordingParams? {
        val resultCode = intent.getIntExtra(ScreenRecordService.EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(ScreenRecordService.EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(ScreenRecordService.EXTRA_RESULT_DATA)
        }

        if (resultCode == 0 || resultData == null) {
            return null
        }

        val savedConfig = SettingsRepository(context).getConfig()
        val defaultDims = savedConfig.resolution.getDimensions(isPortrait = true)

        val width = intent.getIntExtra(ScreenRecordService.EXTRA_RES_WIDTH, defaultDims.first)
        val height = intent.getIntExtra(ScreenRecordService.EXTRA_RES_HEIGHT, defaultDims.second)
        val fps = intent.getIntExtra(ScreenRecordService.EXTRA_FPS, savedConfig.fps.fps)
        val bitrate = intent.getIntExtra(ScreenRecordService.EXTRA_BITRATE, savedConfig.getEffectiveBitrateBps())
        val audioSource = intent.getStringExtra(ScreenRecordService.EXTRA_AUDIO_SOURCE) ?: savedConfig.audioSource.name
        val sampleRate = intent.getIntExtra(ScreenRecordService.EXTRA_SAMPLE_RATE, savedConfig.audioSampleRate.sampleRate)
        val countdownSeconds = intent.getIntExtra(ScreenRecordService.EXTRA_COUNTDOWN_SECONDS, savedConfig.countdownSeconds)
        val showFloatingBubble = intent.getBooleanExtra(ScreenRecordService.EXTRA_SHOW_FLOATING_BUBBLE, savedConfig.showFloatingBubble)
        val showFacecam = intent.getBooleanExtra(ScreenRecordService.EXTRA_SHOW_FACECAM, savedConfig.showFacecam)

        val metrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val isPortrait = metrics.heightPixels >= metrics.widthPixels
        val recWidth = if (isPortrait) minOf(width, height) else maxOf(width, height)
        val recHeight = if (isPortrait) maxOf(width, height) else minOf(width, height)

        return ServiceRecordingParams(
            resultCode = resultCode,
            resultData = resultData,
            width = recWidth,
            height = recHeight,
            densityDpi = metrics.densityDpi,
            fps = fps,
            bitrate = bitrate,
            audioSource = audioSource,
            sampleRate = sampleRate,
            avSyncOffsetMs = savedConfig.avSyncOffsetMs,
            countdownSeconds = countdownSeconds,
            showFloatingBubble = showFloatingBubble,
            showFacecam = showFacecam,
            savedConfig = savedConfig
        )
    }
}
