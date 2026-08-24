package com.example

import com.example.model.AudioSampleRate
import com.example.model.AudioSourceType
import com.example.model.RecordingConfig
import com.example.model.ReleaseChannel
import com.example.model.AppUpdateInfo
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias locales para validación de configuración de captura y sincronización A/V.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun recordingConfig_avSyncOffsetDefaultAndCustom() {
        val defaultConfig = RecordingConfig()
        assertEquals(0, defaultConfig.avSyncOffsetMs)

        val customConfig = defaultConfig.copy(avSyncOffsetMs = -50)
        assertEquals(-50, customConfig.avSyncOffsetMs)
    }

    @Test
    fun recordingConfig_effectiveBitrate() {
        val config = RecordingConfig(bitrateMbps = 15)
        assertEquals(15 * 1_000_000, config.getEffectiveBitrateBps())
    }

    @Test
    fun releaseChannel_matchingLogic() {
        // Canal Beta
        assertTrue(ReleaseChannel.BETA.matchesRelease("v1.5.0-beta.1", "Beta Build", isPrerelease = true))
        assertTrue(ReleaseChannel.BETA.matchesRelease("beta-v1.0.4", "v1.0.4", isPrerelease = true))
        assertFalse(ReleaseChannel.BETA.matchesRelease("v1.5.0-canary.1", "Canary Build", isPrerelease = true))
        assertFalse(ReleaseChannel.BETA.matchesRelease("v1.5.0-dev.1", "Dev Build", isPrerelease = true))

        // Canal Canary
        assertTrue(ReleaseChannel.CANARY.matchesRelease("v1.5.0-canary.2", "Canary", isPrerelease = true))
        assertFalse(ReleaseChannel.CANARY.matchesRelease("v1.5.0-beta.1", "Beta", isPrerelease = true))

        // Canal Dev
        assertTrue(ReleaseChannel.DEV.matchesRelease("v1.5.0-dev.4", "Dev", isPrerelease = true))
        assertFalse(ReleaseChannel.DEV.matchesRelease("v1.5.0-beta.1", "Beta", isPrerelease = true))

        // Canal Stable
        assertTrue(ReleaseChannel.STABLE.matchesRelease("v1.5.0", "Release 1.5.0", isPrerelease = false))
        assertFalse(ReleaseChannel.STABLE.matchesRelease("v1.5.0-beta.1", "Beta", isPrerelease = true))
    }

    @Test
    fun appUpdateInfo_defaultValues() {
        val info = AppUpdateInfo()
        assertFalse(info.isUpdateAvailable)
        assertFalse(info.isChecking)
        assertEquals("https://github.com/LuisAlejandro544/Vortex/releases", AppUpdateInfo.GITHUB_RELEASES_URL)
    }
}


