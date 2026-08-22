package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.RecordingConfig
import com.example.service.ScreenRecordService

/**
 * Lanzador modular de comandos e intents hacia [ScreenRecordService].
 * Garantiza inicio correcto en primer plano (Foreground Service) compatible con Android 8.0 - 15+.
 */
object RecordServiceLauncher {

    fun startService(
        context: Context,
        resultCode: Int,
        resultData: Intent,
        config: RecordingConfig
    ) {
        val (width, height) = config.resolution.getDimensions(isPortrait = true)

        val serviceIntent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, resultData)
            putExtra(ScreenRecordService.EXTRA_RES_WIDTH, width)
            putExtra(ScreenRecordService.EXTRA_RES_HEIGHT, height)
            putExtra(ScreenRecordService.EXTRA_FPS, config.fps.fps)
            putExtra(ScreenRecordService.EXTRA_BITRATE, config.getEffectiveBitrateBps())
            putExtra(ScreenRecordService.EXTRA_AUDIO_SOURCE, config.audioSource.name)
            putExtra(ScreenRecordService.EXTRA_SAMPLE_RATE, config.audioSampleRate.sampleRate)
            putExtra(ScreenRecordService.EXTRA_SHOW_FLOATING_BUBBLE, config.showFloatingBubble)
            putExtra(ScreenRecordService.EXTRA_SHOW_FACECAM, config.showFacecam)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun sendAction(context: Context, action: String) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }
}
