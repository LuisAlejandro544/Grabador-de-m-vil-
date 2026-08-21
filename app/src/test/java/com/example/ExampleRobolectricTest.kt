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
  fun `ffmpeg pure bridge is available and returns version`() {
    val version = com.example.nativecore.NativeFFmpegBridge.getFFmpegVersion()
    assertTrue(version.isNotEmpty())
    assertTrue(com.example.nativecore.NativeFFmpegBridge.initFFmpeg())
  }
}

