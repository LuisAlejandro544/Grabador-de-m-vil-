package com.example.nativecore

import android.util.Log

/**
 * Native C++ Audio DSP Bridge for OBS Mobile.
 * Provides real-time Noise Gate, Intelligent Audio Ducking, and Soft Limiter.
 */
object NativeAudioDSPBridge {
    private const val TAG = "NativeAudioDSPBridge"
    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("obs_core")
            isLibraryLoaded = true
            Log.i(TAG, "C++ Audio DSP Engine library 'obs_core' loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            isLibraryLoaded = false
            Log.w(TAG, "Native C++ Audio DSP Engine not loaded, fallback to Kotlin mixing: ${e.message}")
        }
    }

    fun isNativeReady(): Boolean = isLibraryLoaded

    fun initAudioDsp(sampleRate: Int = 48000, channels: Int = 2): Boolean {
        return if (isLibraryLoaded) {
            try {
                nativeInitAudioDsp(sampleRate, channels)
            } catch (e: Throwable) {
                Log.w(TAG, "Error initializing native Audio DSP: ${e.message}")
                false
            }
        } else {
            false
        }
    }

    fun configureAudioDsp(
        noiseGateThresholdDb: Float = -40.0f,
        duckingAttenuation: Float = 0.35f,
        micGain: Float = 1.25f,
        gameGain: Float = 1.0f,
        noiseGateEnabled: Boolean = true,
        duckingEnabled: Boolean = true,
        peakLimiterEnabled: Boolean = true
    ) {
        if (isLibraryLoaded) {
            try {
                nativeConfigureAudioDsp(
                    noiseGateThresholdDb,
                    duckingAttenuation,
                    micGain,
                    gameGain,
                    noiseGateEnabled,
                    duckingEnabled,
                    peakLimiterEnabled
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Error configuring native Audio DSP: ${e.message}")
            }
        }
    }

    fun setGains(gameGain: Float, micGain: Float) {
        if (isLibraryLoaded) {
            try {
                nativeSetGains(gameGain, micGain)
            } catch (e: Throwable) {
                Log.w(TAG, "Error setting native gains: ${e.message}")
            }
        }
    }

    fun setFilters(noiseGate: Boolean, ducking: Boolean, limiter: Boolean) {
        if (isLibraryLoaded) {
            try {
                nativeSetFilters(noiseGate, ducking, limiter)
            } catch (e: Throwable) {
                Log.w(TAG, "Error setting native filters: ${e.message}")
            }
        }
    }

    /**
     * Devuelve los niveles en tiempo real para el Vúmetro:
     * [0] = gameLevel (0.0f - 1.0f)
     * [1] = micLevel (0.0f - 1.0f)
     * [2] = masterLevel (0.0f - 1.0f)
     * [3] = duckingGain (0.0f - 1.0f)
     */
    fun getAudioLevels(): FloatArray {
        val levels = FloatArray(4)
        if (isLibraryLoaded) {
            try {
                nativeGetAudioLevels(levels)
            } catch (e: Throwable) {
                // Fallback
            }
        }
        return levels
    }

    fun processAndMixAudio(
        internalAudio: ByteArray?,
        micAudio: ByteArray?,
        outputMix: ByteArray,
        byteCount: Int,
        isMicMuted: Boolean
    ): Int {
        if (isLibraryLoaded) {
            try {
                return nativeProcessAndMixAudio(internalAudio, micAudio, outputMix, byteCount, isMicMuted)
            } catch (e: Throwable) {
                Log.w(TAG, "Error in native processAndMixAudio, falling back: ${e.message}")
            }
        }
        return 0
    }

    fun isVoiceDetected(): Boolean {
        return if (isLibraryLoaded) {
            try {
                nativeIsVoiceDetected()
            } catch (e: Throwable) {
                false
            }
        } else {
            false
        }
    }

    fun getVoiceEnvelope(): Float {
        return if (isLibraryLoaded) {
            try {
                nativeGetVoiceEnvelope()
            } catch (e: Throwable) {
                0.0f
            }
        } else {
            0.0f
        }
    }

    fun getDuckingGain(): Float {
        return if (isLibraryLoaded) {
            try {
                nativeGetDuckingGain()
            } catch (e: Throwable) {
                1.0f
            }
        } else {
            1.0f
        }
    }

    fun releaseAudioDsp() {
        if (isLibraryLoaded) {
            try {
                nativeReleaseAudioDsp()
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    // JNI External Functions
    private external fun nativeInitAudioDsp(sampleRate: Int, channels: Int): Boolean
    private external fun nativeConfigureAudioDsp(
        noiseGateThresholdDb: Float,
        duckingAttenuation: Float,
        micGain: Float,
        gameGain: Float,
        noiseGateEnabled: Boolean,
        duckingEnabled: Boolean,
        peakLimiterEnabled: Boolean
    )
    private external fun nativeSetGains(gameGain: Float, micGain: Float)
    private external fun nativeSetFilters(noiseGate: Boolean, ducking: Boolean, limiter: Boolean)
    private external fun nativeGetAudioLevels(outArray: FloatArray)
    private external fun nativeProcessAndMixAudio(
        internalAudio: ByteArray?,
        micAudio: ByteArray?,
        outputMix: ByteArray,
        byteCount: Int,
        isMicMuted: Boolean
    ): Int
    private external fun nativeIsVoiceDetected(): Boolean
    private external fun nativeGetVoiceEnvelope(): Float
    private external fun nativeGetDuckingGain(): Float
    private external fun nativeReleaseAudioDsp()
}
