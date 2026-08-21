#include "audio_dsp_engine.hpp"
#include <android/log.h>
#include <cstring>

#define LOG_TAG "OBS_AudioDSP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace obs {
namespace dsp {

AudioDspEngine::AudioDspEngine()
    : mGateLinearThreshold(0.01f),
      mVoiceEnvelope(0.0f),
      mCurrentDuckingGain(1.0f),
      mGateAttackCoeff(0.0f),
      mGateReleaseCoeff(0.0f),
      mDuckingAttackCoeff(0.0f),
      mDuckingReleaseCoeff(0.0f) {
    initialize(48000, 2);
}

void AudioDspEngine::initialize(int sampleRate, int channels) {
    mConfig.sampleRate = sampleRate;
    mConfig.channels = channels;
    mVoiceEnvelope = 0.0f;
    mCurrentDuckingGain = 1.0f;
    updateCoefficients();
    LOGI("AudioDspEngine inicializado: %d Hz, %d canales", sampleRate, channels);
}

void AudioDspEngine::setConfig(const AudioDspConfig& config) {
    mConfig = config;
    updateCoefficients();
}

void AudioDspEngine::updateCoefficients() {
    // Convertir dB a amplitud lineal normalizada [-1.0, 1.0]
    // 0 dB = 1.0, -40 dB = 0.01
    mGateLinearThreshold = std::pow(10.0f, mConfig.noiseGateThresholdDb / 20.0f);

    // Tiempos de respuesta para filtros envelope IIR:
    // coeff = exp(-1.0 / (time_in_seconds * sample_rate))
    float dt = 1.0f / static_cast<float>(mConfig.sampleRate);
    
    // Gate: Attack 5ms, Release 70ms
    mGateAttackCoeff = std::exp(-dt / 0.005f);
    mGateReleaseCoeff = std::exp(-dt / 0.070f);

    // Ducking: Attack 15ms (bajada suave al hablar), Release 350ms (recuperación musical)
    mDuckingAttackCoeff = std::exp(-dt / 0.015f);
    mDuckingReleaseCoeff = std::exp(-dt / 0.350f);
}

// Curva de Soft Limiter / Tanh saturación suave para evitar distorsión dura (clipping digital)
inline float AudioDspEngine::softClip(float x) {
    if (x > 1.0f) {
        return (2.0f / 3.0f) + (x - 1.0f) / (1.0f + (x - 1.0f) * (x - 1.0f));
    } else if (x < -1.0f) {
        return -(2.0f / 3.0f) + (x + 1.0f) / (1.0f + (x + 1.0f) * (x + 1.0f));
    } else {
        return x - (x * x * x) / 3.0f;
    }
}

int AudioDspEngine::processAndMix(
    const int16_t* internalPcm,
    const int16_t* micPcm,
    int16_t* outMixedPcm,
    int sampleCount,
    bool isMicMuted
) {
    if (!outMixedPcm || sampleCount <= 0) {
        return 0;
    }

    const float kNorm16 = 1.0f / 32768.0f;
    const float kDenorm16 = 32767.0f;

    for (int i = 0; i < sampleCount; ++i) {
        // 1. Obtener muestra del juego normalizada
        float gameSample = 0.0f;
        if (internalPcm != nullptr) {
            gameSample = static_cast<float>(internalPcm[i]) * kNorm16 * mConfig.gameGain;
        }

        // 2. Procesamiento de Micrófono
        float micSample = 0.0f;
        if (micPcm != nullptr && !isMicMuted) {
            float rawMic = static_cast<float>(micPcm[i]) * kNorm16;
            float absMic = std::abs(rawMic);

            // Envelope Follower para medir energía de voz
            if (absMic > mVoiceEnvelope) {
                mVoiceEnvelope = mGateAttackCoeff * mVoiceEnvelope + (1.0f - mGateAttackCoeff) * absMic;
            } else {
                mVoiceEnvelope = mGateReleaseCoeff * mVoiceEnvelope + (1.0f - mGateReleaseCoeff) * absMic;
            }

            // A. Noise Gate (Puerta de ruido en GPU/C++ DSP)
            float gateGain = 1.0f;
            if (mConfig.noiseGateEnabled) {
                if (mVoiceEnvelope < mGateLinearThreshold) {
                    // Por debajo del umbral de voz -> atenuar ruido ambiente
                    gateGain = (mVoiceEnvelope / mGateLinearThreshold);
                    gateGain = gateGain * gateGain; // Curva cuadrática suave
                }
            }

            micSample = rawMic * gateGain * mConfig.micGain;
        } else {
            // Si el micrófono está silenciado, relajar el envelope de voz
            mVoiceEnvelope = mGateReleaseCoeff * mVoiceEnvelope;
        }

        // B. Audio Ducking Automático
        float targetDucking = 1.0f;
        if (mConfig.duckingEnabled && !isMicMuted && (mVoiceEnvelope > mGateLinearThreshold)) {
            // Se detectó voz: atenuar el audio del juego al nivel configurado
            targetDucking = mConfig.duckingAttenuation;
        }

        if (targetDucking < mCurrentDuckingGain) {
            mCurrentDuckingGain = mDuckingAttackCoeff * mCurrentDuckingGain + (1.0f - mDuckingAttackCoeff) * targetDucking;
        } else {
            mCurrentDuckingGain = mDuckingReleaseCoeff * mCurrentDuckingGain + (1.0f - mDuckingReleaseCoeff) * targetDucking;
        }

        // Aplicar ducking al juego
        float processedGame = gameSample * mCurrentDuckingGain;

        // 3. Mezcla y Soft Limiter (Suma de fuentes sin distorsión)
        float mixed = processedGame + micSample;

        if (mConfig.peakLimiterEnabled) {
            if (std::abs(mixed) > 0.85f) {
                mixed = softClip(mixed * 0.95f);
            }
        }

        // Clamp de seguridad a 16-bit PCM
        mixed = std::max(-1.0f, std::min(1.0f, mixed));
        outMixedPcm[i] = static_cast<int16_t>(mixed * kDenorm16);
    }

    return sampleCount * sizeof(int16_t);
}

} // namespace dsp
} // namespace obs
