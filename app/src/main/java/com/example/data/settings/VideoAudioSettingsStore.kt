package com.example.data.settings

import android.content.SharedPreferences
import com.example.model.AudioSampleRate
import com.example.model.AudioSourceType
import com.example.model.FacecamFps
import com.example.model.RecordingConfig
import com.example.model.VideoBitrate
import com.example.model.VideoFps
import com.example.model.VideoResolution

/**
 * Gestor de persistencia y serialización para configuraciones de Video y Audio DSP.
 */
class VideoAudioSettingsStore(private val prefs: SharedPreferences) {

    data class VideoAudioSlice(
        val resolution: VideoResolution,
        val fps: VideoFps,
        val bitrate: VideoBitrate,
        val bitrateMbps: Int,
        val facecamFps: FacecamFps,
        val audioSampleRate: AudioSampleRate,
        val audioSource: AudioSourceType,
        val gameAudioGain: Float,
        val micAudioGain: Float,
        val audioDuckingEnabled: Boolean,
        val noiseGateEnabled: Boolean,
        val avSyncOffsetMs: Int
    )

    fun load(): VideoAudioSlice {
        val resName = prefs.getString(KEY_RESOLUTION, VideoResolution.RES_1080P.name)
        val resolution = try {
            VideoResolution.valueOf(resName ?: VideoResolution.RES_1080P.name)
        } catch (_: Exception) {
            VideoResolution.RES_1080P
        }

        val fpsName = prefs.getString(KEY_FPS, VideoFps.FPS_60.name)
        val fps = try {
            VideoFps.valueOf(fpsName ?: VideoFps.FPS_60.name)
        } catch (_: Exception) {
            VideoFps.FPS_60
        }

        val bitrateName = prefs.getString(KEY_BITRATE, VideoBitrate.BITRATE_8M.name)
        val bitrate = try {
            VideoBitrate.valueOf(bitrateName ?: VideoBitrate.BITRATE_8M.name)
        } catch (_: Exception) {
            VideoBitrate.BITRATE_8M
        }

        val bitrateMbps = prefs.getInt(KEY_BITRATE_MBPS, 8).coerceIn(1, 12)

        val facecamFpsName = prefs.getString(KEY_FACECAM_FPS, FacecamFps.FPS_30.name)
        val facecamFps = try {
            FacecamFps.valueOf(facecamFpsName ?: FacecamFps.FPS_30.name)
        } catch (_: Exception) {
            FacecamFps.FPS_30
        }

        val audioRateName = prefs.getString(KEY_AUDIO_SAMPLE_RATE, AudioSampleRate.RATE_48000.name)
        val audioSampleRate = try {
            AudioSampleRate.valueOf(audioRateName ?: AudioSampleRate.RATE_48000.name)
        } catch (_: Exception) {
            AudioSampleRate.RATE_48000
        }

        val audioName = prefs.getString(KEY_AUDIO_SOURCE, AudioSourceType.INTERNAL_AND_MIC.name)
        val audioSource = try {
            AudioSourceType.valueOf(audioName ?: AudioSourceType.INTERNAL_AND_MIC.name)
        } catch (_: Exception) {
            AudioSourceType.INTERNAL_AND_MIC
        }

        val gameGain = prefs.getFloat(KEY_GAME_AUDIO_GAIN, 1.0f)
        val micGain = prefs.getFloat(KEY_MIC_AUDIO_GAIN, 1.25f)
        val audioDucking = prefs.getBoolean(KEY_AUDIO_DUCKING, true)
        val noiseGate = prefs.getBoolean(KEY_NOISE_GATE, true)
        val avSyncOffset = prefs.getInt(KEY_AV_SYNC_OFFSET_MS, 0).coerceIn(-300, 300)

        return VideoAudioSlice(
            resolution = resolution,
            fps = fps,
            bitrate = bitrate,
            bitrateMbps = bitrateMbps,
            facecamFps = facecamFps,
            audioSampleRate = audioSampleRate,
            audioSource = audioSource,
            gameAudioGain = gameGain,
            micAudioGain = micGain,
            audioDuckingEnabled = audioDucking,
            noiseGateEnabled = noiseGate,
            avSyncOffsetMs = avSyncOffset
        )
    }

    fun save(editor: SharedPreferences.Editor, config: RecordingConfig) {
        editor.putString(KEY_RESOLUTION, config.resolution.name)
        editor.putString(KEY_FPS, config.fps.name)
        editor.putString(KEY_BITRATE, config.bitrate.name)
        editor.putInt(KEY_BITRATE_MBPS, config.bitrateMbps)
        editor.putString(KEY_FACECAM_FPS, config.facecamFps.name)
        editor.putString(KEY_AUDIO_SAMPLE_RATE, config.audioSampleRate.name)
        editor.putString(KEY_AUDIO_SOURCE, config.audioSource.name)
        editor.putFloat(KEY_GAME_AUDIO_GAIN, config.gameAudioGain)
        editor.putFloat(KEY_MIC_AUDIO_GAIN, config.micAudioGain)
        editor.putBoolean(KEY_AUDIO_DUCKING, config.audioDuckingEnabled)
        editor.putBoolean(KEY_NOISE_GATE, config.noiseGateEnabled)
        editor.putInt(KEY_AV_SYNC_OFFSET_MS, config.avSyncOffsetMs)
    }

    companion object {
        const val KEY_RESOLUTION = "pref_resolution"
        const val KEY_FPS = "pref_fps"
        const val KEY_BITRATE = "pref_bitrate"
        const val KEY_BITRATE_MBPS = "pref_bitrate_mbps"
        const val KEY_FACECAM_FPS = "pref_facecam_fps"
        const val KEY_AUDIO_SAMPLE_RATE = "pref_audio_sample_rate"
        const val KEY_AUDIO_SOURCE = "pref_audio_source"
        const val KEY_GAME_AUDIO_GAIN = "pref_game_audio_gain"
        const val KEY_MIC_AUDIO_GAIN = "pref_mic_audio_gain"
        const val KEY_AUDIO_DUCKING = "pref_audio_ducking"
        const val KEY_NOISE_GATE = "pref_noise_gate"
        const val KEY_AV_SYNC_OFFSET_MS = "pref_av_sync_offset_ms"
    }
}
