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
  fun `ffmpeg pure bridge is available and returns version`() {
    val version = com.example.nativecore.NativeFFmpegBridge.getFFmpegVersion()
    assertTrue(version.isNotEmpty())
    assertTrue(com.example.nativecore.NativeFFmpegBridge.initFFmpeg())
  }
}

