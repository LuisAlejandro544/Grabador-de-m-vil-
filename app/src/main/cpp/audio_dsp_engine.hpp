#ifndef OBS_AUDIO_DSP_ENGINE_HPP
#define OBS_AUDIO_DSP_ENGINE_HPP

#include <cstdint>
#include <vector>
#include <cmath>
#include <algorithm>
#include <string>

namespace obs {
namespace dsp {

struct AudioDspConfig {
    int sampleRate = 48000;
    int channels = 2;               // Stereo (Interleaved L, R)
    float noiseGateThresholdDb = -40.0f; // Umbral de apertura de puerta de ruido (-40 dB)
    float duckingAttenuation = 0.35f;    // Nivel de atenuación del juego cuando se detecta voz (35% volumen / -9dB)
    float micGain = 1.25f;          // Ganancia de voz para claridad
    float gameGain = 1.0f;          // Ganancia base del juego
    bool noiseGateEnabled = true;   // Filtro de ruido activo
    bool duckingEnabled = true;     // Ducking automático activo
    bool peakLimiterEnabled = true; // Soft limiter activo
};

class AudioDspEngine {
public:
    AudioDspEngine();
    ~AudioDspEngine() = default;

    void initialize(int sampleRate = 48000, int channels = 2);
    void setConfig(const AudioDspConfig& config);
    AudioDspConfig getConfig() const { return mConfig; }

    /**
     * Procesa y mezcla en tiempo real los buffers de audio del juego y del micrófono.
     * Aplica Noise Gate, Ducking inteligente y Soft Limiter contra saturación digital.
     *
     * @param internalPcm Buffer PCM 16-bit del juego (o nullptr)
     * @param micPcm Buffer PCM 16-bit del micrófono (o nullptr)
     * @param outMixedPcm Buffer de salida PCM 16-bit mezclado
     * @param sampleCount Número de muestras por canal
     * @param isMicMuted Si el micrófono está silenciado manualmente desde la UI
     * @return Número de bytes generados en outMixedPcm
     */
    int processAndMix(
        const int16_t* internalPcm,
        const int16_t* micPcm,
        int16_t* outMixedPcm,
        int sampleCount,
        bool isMicMuted
    );

    // Estado en vivo para telemetría
    float getVoiceActivityLevel() const { return mVoiceEnvelope; }
    float getDuckingLevel() const { return mCurrentDuckingGain; }
    bool isVoiceDetected() const { return mVoiceEnvelope > mGateLinearThreshold; }

private:
    AudioDspConfig mConfig;
    float mGateLinearThreshold;
    float mVoiceEnvelope;
    float mCurrentDuckingGain;
    
    // Constantes de filtro envelope follower (Attack / Release)
    float mGateAttackCoeff;
    float mGateReleaseCoeff;
    float mDuckingAttackCoeff;
    float mDuckingReleaseCoeff;

    void updateCoefficients();
    static inline float softClip(float x);
};

} // namespace dsp
} // namespace obs

#endif // OBS_AUDIO_DSP_ENGINE_HPP
