package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.RecordingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Grabador de Pantalla", appName)
  }

  @Test
  fun `floating bubble is enabled by default in configuration`() {
    val config = RecordingConfig()
    assertTrue(config.showFloatingBubble)
  }

  @Test
  fun `record storage helper prepares output file`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val file = com.example.service.RecordStorageHelper.prepareOutputFile(context)
    assertTrue(file.name.startsWith("OBS_REC_"))
    assertTrue(file.name.endsWith(".mp4"))
  }

  @Test
  fun `screenshot helper saves bitmap to gallery directory`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    var saved = false
    com.example.service.ScreenshotHelper.saveBitmapToGallery(
      context = context,
      bitmap = bitmap,
      onSuccess = { file ->
        saved = file.exists() && file.name.startsWith("OBS_SHOT_")
      },
      onError = {}
    )
    assertTrue(saved)
  }

  @Test
  fun `audio source defaults to internal and mic for dynamic switching`() {
    val config = RecordingConfig()
    assertEquals(com.example.model.AudioSourceType.INTERNAL_AND_MIC, config.audioSource)
  }

  @Test
  fun `ffmpeg pure bridge is available and returns version`() {
    val version = com.example.nativecore.NativeFFmpegBridge.getFFmpegVersion()
    assertTrue(version.isNotEmpty())
    assertTrue(com.example.nativecore.NativeFFmpegBridge.initFFmpeg())
  }

  @Test
  fun `audio dsp bridge initializes and handles mixing fallback gracefully`() {
    val sampleRate = 48000
    val channels = 2
    com.example.nativecore.NativeAudioDSPBridge.initAudioDsp(sampleRate, channels)
    com.example.nativecore.NativeAudioDSPBridge.configureAudioDsp(
      noiseGateThresholdDb = -40f,
      duckingAttenuation = 0.35f,
      micGain = 1.25f,
      gameGain = 1.0f,
      noiseGateEnabled = true,
      duckingEnabled = true,
      peakLimiterEnabled = true
    )
    val inputInternal = ByteArray(512)
    val inputMic = ByteArray(512)
    val outputMix = ByteArray(512)
    val processed = com.example.nativecore.NativeAudioDSPBridge.processAndMixAudio(
      internalAudio = inputInternal,
      micAudio = inputMic,
      outputMix = outputMix,
      byteCount = 512,
      isMicMuted = false
    )
    assertTrue(processed >= 0)
    com.example.nativecore.NativeAudioDSPBridge.releaseAudioDsp()
  }
}

