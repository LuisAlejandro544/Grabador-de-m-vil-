package com.example.service.capture

import com.example.nativecore.NativeAudioDSPBridge

/**
 * Mezclador digital de audio y procesador DSP (Noise Gate, Ducking automático, Peak Limiter).
 * Desacopla la lógica de mezcla nativa en C++ y el fallback de mezcla PCM con soft-clipping.
 */
class AudioDspMixer(
    private val sampleRate: Int = 48000,
    private val channelCount: Int = 2
) {
    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192
    }

    private var mixBuf = ByteArray(DEFAULT_BUFFER_SIZE)

    private fun ensureMixBufSize(requiredSize: Int) {
        if (mixBuf.size < requiredSize) {
            mixBuf = ByteArray(maxOf(mixBuf.size * 2, requiredSize * 2))
        }
    }

    fun initializeDsp(
        gameGain: Float = 1.0f,
        micGain: Float = 1.25f,
        noiseGateEnabled: Boolean = true,
        duckingEnabled: Boolean = true,
        peakLimiterEnabled: Boolean = true
    ) {
        NativeAudioDSPBridge.initAudioDsp(sampleRate, channelCount)
        NativeAudioDSPBridge.configureAudioDsp(
            noiseGateThresholdDb = -38.0f,
            duckingAttenuation = 0.35f,
            micGain = micGain,
            gameGain = gameGain,
            noiseGateEnabled = noiseGateEnabled,
            duckingEnabled = duckingEnabled,
            peakLimiterEnabled = peakLimiterEnabled
        )
    }

    fun setGains(gameGain: Float, micGain: Float) {
        NativeAudioDSPBridge.setGains(gameGain, micGain)
    }

    fun setFilters(noiseGate: Boolean, ducking: Boolean, limiter: Boolean) {
        NativeAudioDSPBridge.setFilters(noiseGate, ducking, limiter)
    }

    fun getAudioLevels(): FloatArray {
        return NativeAudioDSPBridge.getAudioLevels()
    }

    fun mixDualAudio(internalData: ByteArray, micData: ByteArray): Pair<ByteArray, Int> {
        val pcmCount = minOf(internalData.size, micData.size)
        val finalSize = maxOf(internalData.size, micData.size)
        ensureMixBufSize(finalSize)

        val processedBytes = if (NativeAudioDSPBridge.isNativeReady()) {
            NativeAudioDSPBridge.processAndMixAudio(
                internalAudio = internalData,
                micAudio = micData,
                outputMix = mixBuf,
                byteCount = pcmCount,
                isMicMuted = false
            )
        } else 0

        return if (processedBytes > 0) {
            if (internalData.size > processedBytes) {
                System.arraycopy(
                    internalData,
                    processedBytes,
                    mixBuf,
                    processedBytes,
                    internalData.size - processedBytes
                )
            }
            Pair(mixBuf, finalSize)
        } else {
            // Mezclador PCM 16-bit estéreo con soft clipping
            val sampleCount = pcmCount / 2
            for (i in 0 until sampleCount) {
                val idx = i * 2
                val sInternal = (internalData[idx].toInt() and 0xFF) or (internalData[idx + 1].toInt() shl 8)
                val sMic = (micData[idx].toInt() and 0xFF) or (micData[idx + 1].toInt() shl 8)
                val mixed = (sInternal.toShort() + sMic.toShort()).coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                )
                mixBuf[idx] = (mixed and 0xFF).toByte()
                mixBuf[idx + 1] = ((mixed shr 8) and 0xFF).toByte()
            }
            if (internalData.size > micData.size) {
                System.arraycopy(internalData, pcmCount, mixBuf, pcmCount, internalData.size - pcmCount)
            } else if (micData.size > internalData.size) {
                System.arraycopy(micData, pcmCount, mixBuf, pcmCount, micData.size - pcmCount)
            }
            Pair(mixBuf, finalSize)
        }
    }

    fun processSingleMicAudio(micData: ByteArray): Pair<ByteArray, Int> {
        val finalSize = micData.size
        ensureMixBufSize(finalSize)
        val processed = if (NativeAudioDSPBridge.isNativeReady()) {
            NativeAudioDSPBridge.processAndMixAudio(
                internalAudio = null,
                micAudio = micData,
                outputMix = mixBuf,
                byteCount = finalSize,
                isMicMuted = false
            )
        } else 0
        val finalBytes = if (processed > 0) mixBuf else micData
        return Pair(finalBytes, finalSize)
    }

    fun release() {
        NativeAudioDSPBridge.releaseAudioDsp()
    }
}
