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
    assertEquals("Vortex Studio", appName)
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

  @Test
  fun `settings repository persists and updates recording configuration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.SettingsRepository(context)

    // Test updating resolution and fps
    repository.updateResolution(com.example.model.VideoResolution.RES_720P)
    repository.updateFps(com.example.model.VideoFps.FPS_30)
    repository.updateBitrate(com.example.model.VideoBitrate.BITRATE_4M)
    repository.updateAudioSource(com.example.model.AudioSourceType.INTERNAL_GAME)
    repository.updateCountdown(5)
    repository.toggleFloatingBubble(false)

    val saved = repository.loadConfig()
    assertEquals(com.example.model.VideoResolution.RES_720P, saved.resolution)
    assertEquals(com.example.model.VideoFps.FPS_30, saved.fps)
    assertEquals(com.example.model.VideoBitrate.BITRATE_4M, saved.bitrate)
    assertEquals(com.example.model.AudioSourceType.INTERNAL_GAME, saved.audioSource)
    assertEquals(5, saved.countdownSeconds)
    assertEquals(false, saved.showFloatingBubble)
  }

  @Test
  fun `settings repository persists facecam shapes and size selection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.SettingsRepository(context)

    repository.toggleFacecam(true)
    repository.updateFacecamShape(com.example.model.FacecamShape.RECTANGLE)
    repository.updateFacecamSize(com.example.model.FacecamSize.LARGE)
    repository.setFacecamCamera(false)

    val saved = repository.loadConfig()
    assertTrue(saved.showFacecam)
    assertEquals(com.example.model.FacecamShape.RECTANGLE, saved.facecamShape)
    assertEquals(com.example.model.FacecamSize.LARGE, saved.facecamSize)
    assertEquals(false, saved.isFrontCamera)

    // Test cycle shape
    repository.updateFacecamShape(com.example.model.FacecamShape.ROUNDED_SQUARE)
    assertEquals(com.example.model.FacecamShape.ROUNDED_SQUARE, repository.loadConfig().facecamShape)
  }

  @Test
  fun `audio pipeline state and mic toggle functioning correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = com.example.service.ScreenCaptureEngine(context)
    // Initially not recording, mic toggle behaves safely
    val toggled = engine.toggleMicrophoneMuted()
    assertEquals(false, toggled)
  }

  @Test
  fun `settings repository persists av sync offset and audio filters`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.SettingsRepository(context)

    repository.updateAvSyncOffset(100)
    repository.toggleAudioDucking(true)
    repository.toggleNoiseGate(true)

    val saved = repository.loadConfig()
    assertEquals(100, saved.avSyncOffsetMs)
    assertTrue(saved.audioDuckingEnabled)
    assertTrue(saved.noiseGateEnabled)
  }
}


