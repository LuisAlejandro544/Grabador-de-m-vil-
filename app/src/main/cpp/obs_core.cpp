#include <jni.h>
#include <string>
#include <memory>
#include "obs_compositor.hpp"
#include "ffmpeg_engine.hpp"
#include "audio_dsp_engine.hpp"

static std::unique_ptr<obs::SceneCompositor> gCompositor = nullptr;
static std::unique_ptr<obs::ffmpeg::FFmpegEngine> gFFmpegEngine = nullptr;
static std::unique_ptr<obs::dsp::AudioDspEngine> gAudioDspEngine = nullptr;

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeGetEngineVersion(JNIEnv* env, jobject /* this */) {
    if (!gCompositor) {
        gCompositor = std::make_unique<obs::SceneCompositor>();
    }
    return env->NewStringUTF(gCompositor->getEngineVersion().c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeInitCompositor(
    JNIEnv* /* env */,
    jobject /* this */,
    jint width,
    jint height,
    jint fps
) {
    if (!gCompositor) {
        gCompositor = std::make_unique<obs::SceneCompositor>();
    }
    return static_cast<jboolean>(gCompositor->initialize(width, height, fps));
}

JNIEXPORT void JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeReleaseCompositor(JNIEnv* /* env */, jobject /* this */) {
    if (gCompositor) {
        gCompositor->release();
    }
}

JNIEXPORT jint JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeAddSource(
    JNIEnv* env,
    jobject /* this */,
    jstring name,
    jint sourceType,
    jfloat x,
    jfloat y,
    jfloat width,
    jfloat height
) {
    if (!gCompositor) {
        gCompositor = std::make_unique<obs::SceneCompositor>();
    }
    const char* nativeName = env->GetStringUTFChars(name, nullptr);
    std::string nameStr(nativeName ? nativeName : "Source");
    if (nativeName) {
        env->ReleaseStringUTFChars(name, nativeName);
    }

    return gCompositor->addSource(
        nameStr,
        static_cast<obs::SourceType>(sourceType),
        x, y, width, height
    );
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeRemoveSource(
    JNIEnv* /* env */,
    jobject /* this */,
    jint sourceId
) {
    if (!gCompositor) return JNI_FALSE;
    return static_cast<jboolean>(gCompositor->removeSource(sourceId));
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeUpdateSourceTransform(
    JNIEnv* /* env */,
    jobject /* this */,
    jint sourceId,
    jfloat x,
    jfloat y,
    jfloat width,
    jfloat height,
    jfloat opacity,
    jboolean isVisible
) {
    if (!gCompositor) return JNI_FALSE;
    return static_cast<jboolean>(gCompositor->updateSourceTransform(sourceId, x, y, width, height, opacity, isVisible));
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeSetSourceShape(
    JNIEnv* /* env */,
    jobject /* this */,
    jint sourceId,
    jint shape
) {
    if (!gCompositor) return JNI_FALSE;
    return static_cast<jboolean>(gCompositor->setSourceShape(sourceId, static_cast<obs::LayerShape>(shape)));
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeSetSourceChromaKey(
    JNIEnv* /* env */,
    jobject /* this */,
    jint sourceId,
    jboolean enabled,
    jfloat r,
    jfloat g,
    jfloat b,
    jfloat similarity,
    jfloat smoothness
) {
    if (!gCompositor) return JNI_FALSE;
    return static_cast<jboolean>(gCompositor->setSourceChromaKey(sourceId, enabled, r, g, b, similarity, smoothness));
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeSetSourceZOrder(
    JNIEnv* /* env */,
    jobject /* this */,
    jint sourceId,
    jint zOrder
) {
    if (!gCompositor) return JNI_FALSE;
    return static_cast<jboolean>(gCompositor->setSourceZOrder(sourceId, zOrder));
}

JNIEXPORT void JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeRenderFrame(
    JNIEnv* /* env */,
    jobject /* this */,
    jlong timestampNs
) {
    if (gCompositor) {
        gCompositor->renderFrame(timestampNs);
    }
}

JNIEXPORT jfloat JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeGetRenderFps(JNIEnv* /* env */, jobject /* this */) {
    if (!gCompositor) return 60.0f;
    return gCompositor->getPerformanceStats().renderFps;
}

JNIEXPORT jfloat JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeGetFrameTimeMs(JNIEnv* /* env */, jobject /* this */) {
    if (!gCompositor) return 16.6f;
    return gCompositor->getPerformanceStats().frameTimeMs;
}

JNIEXPORT jint JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeGetSourceCount(JNIEnv* /* env */, jobject /* this */) {
    if (!gCompositor) return 0;
    return gCompositor->getSourceCount();
}

// -----------------------------------------------------------------------------
// Native FFmpeg Engine JNI Exports
// -----------------------------------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_example_nativecore_NativeFFmpegBridge_nativeGetFFmpegVersion(JNIEnv* env, jobject /* this */) {
    if (!gFFmpegEngine) {
        gFFmpegEngine = std::make_unique<obs::ffmpeg::FFmpegEngine>();
        gFFmpegEngine->initialize();
    }
    return env->NewStringUTF(gFFmpegEngine->getVersion().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_nativecore_NativeFFmpegBridge_nativeGetFFmpegConfig(JNIEnv* env, jobject /* this */) {
    if (!gFFmpegEngine) {
        gFFmpegEngine = std::make_unique<obs::ffmpeg::FFmpegEngine>();
        gFFmpegEngine->initialize();
    }
    return env->NewStringUTF(gFFmpegEngine->getConfiguration().c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeFFmpegBridge_nativeInitFFmpeg(JNIEnv* /* env */, jobject /* this */) {
    if (!gFFmpegEngine) {
        gFFmpegEngine = std::make_unique<obs::ffmpeg::FFmpegEngine>();
    }
    return static_cast<jboolean>(gFFmpegEngine->initialize());
}

JNIEXPORT void JNICALL
Java_com_example_nativecore_NativeFFmpegBridge_nativeReleaseFFmpeg(JNIEnv* /* env */, jobject /* this */) {
    if (gFFmpegEngine) {
        gFFmpegEngine->release();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeFFmpegBridge_nativeTrimVideo(
    JNIEnv* env,
    jobject /* this */,
    jstring inputPath,
    jstring outputPath,
    jlong startMs,
    jlong endMs,
    jboolean accurateCut
) {
    if (!gFFmpegEngine) {
        gFFmpegEngine = std::make_unique<obs::ffmpeg::FFmpegEngine>();
        gFFmpegEngine->initialize();
    }
    const char* inP = env->GetStringUTFChars(inputPath, nullptr);
    const char* outP = env->GetStringUTFChars(outputPath, nullptr);
    std::string inStr(inP ? inP : "");
    std::string outStr(outP ? outP : "");
    if (inP) env->ReleaseStringUTFChars(inputPath, inP);
    if (outP) env->ReleaseStringUTFChars(outputPath, outP);

    obs::ffmpeg::TrimOptions opts{};
    opts.startMs = startMs;
    opts.endMs = endMs;
    opts.accurateCut = accurateCut;

    return static_cast<jboolean>(gFFmpegEngine->trimVideo(inStr, outStr, opts, nullptr));
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeFFmpegBridge_nativeSplitVideo(
    JNIEnv* env,
    jobject /* this */,
    jstring inputPath,
    jstring outputPart1,
    jstring outputPart2,
    jlong splitMs
) {
    if (!gFFmpegEngine) {
        gFFmpegEngine = std::make_unique<obs::ffmpeg::FFmpegEngine>();
        gFFmpegEngine->initialize();
    }
    const char* inP = env->GetStringUTFChars(inputPath, nullptr);
    const char* outP1 = env->GetStringUTFChars(outputPart1, nullptr);
    const char* outP2 = env->GetStringUTFChars(outputPart2, nullptr);
    std::string inStr(inP ? inP : "");
    std::string outStr1(outP1 ? outP1 : "");
    std::string outStr2(outP2 ? outP2 : "");
    if (inP) env->ReleaseStringUTFChars(inputPath, inP);
    if (outP1) env->ReleaseStringUTFChars(outputPart1, outP1);
    if (outP2) env->ReleaseStringUTFChars(outputPart2, outP2);

    return static_cast<jboolean>(gFFmpegEngine->splitVideo(inStr, outStr1, outStr2, splitMs, nullptr));
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeFFmpegBridge_nativeConvertAspectRatio(
    JNIEnv* env,
    jobject /* this */,
    jstring inputPath,
    jstring outputPath,
    jint targetWidth,
    jint targetHeight,
    jint fitMode
) {
    if (!gFFmpegEngine) {
        gFFmpegEngine = std::make_unique<obs::ffmpeg::FFmpegEngine>();
        gFFmpegEngine->initialize();
    }
    const char* inP = env->GetStringUTFChars(inputPath, nullptr);
    const char* outP = env->GetStringUTFChars(outputPath, nullptr);
    std::string inStr(inP ? inP : "");
    std::string outStr(outP ? outP : "");
    if (inP) env->ReleaseStringUTFChars(inputPath, inP);
    if (outP) env->ReleaseStringUTFChars(outputPath, outP);

    return static_cast<jboolean>(gFFmpegEngine->convertAspectRatio(inStr, outStr, targetWidth, targetHeight, fitMode, nullptr));
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeFFmpegBridge_nativeExtractAudio(
    JNIEnv* env,
    jobject /* this */,
    jstring inputPath,
    jstring outputPath,
    jint codecType
) {
    if (!gFFmpegEngine) {
        gFFmpegEngine = std::make_unique<obs::ffmpeg::FFmpegEngine>();
        gFFmpegEngine->initialize();
    }
    const char* inP = env->GetStringUTFChars(inputPath, nullptr);
    const char* outP = env->GetStringUTFChars(outputPath, nullptr);
    std::string inStr(inP ? inP : "");
    std::string outStr(outP ? outP : "");
    if (inP) env->ReleaseStringUTFChars(inputPath, inP);
    if (outP) env->ReleaseStringUTFChars(outputPath, outP);

    return static_cast<jboolean>(gFFmpegEngine->extractAudio(inStr, outStr, static_cast<obs::ffmpeg::CodecType>(codecType), nullptr));
}

// -----------------------------------------------------------------------------
// Native Audio DSP Engine JNI Exports (Noise Gate, Ducking, Soft Limiter)
// -----------------------------------------------------------------------------

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeInitAudioDsp(
    JNIEnv* /* env */,
    jobject /* this */,
    jint sampleRate,
    jint channels
) {
    if (!gAudioDspEngine) {
        gAudioDspEngine = std::make_unique<obs::dsp::AudioDspEngine>();
    }
    gAudioDspEngine->initialize(sampleRate, channels);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeConfigureAudioDsp(
    JNIEnv* /* env */,
    jobject /* this */,
    jfloat noiseGateThresholdDb,
    jfloat duckingAttenuation,
    jfloat micGain,
    jfloat gameGain,
    jboolean noiseGateEnabled,
    jboolean duckingEnabled,
    jboolean peakLimiterEnabled
) {
    if (!gAudioDspEngine) {
        gAudioDspEngine = std::make_unique<obs::dsp::AudioDspEngine>();
    }
    obs::dsp::AudioDspConfig config = gAudioDspEngine->getConfig();
    config.noiseGateThresholdDb = noiseGateThresholdDb;
    config.duckingAttenuation = duckingAttenuation;
    config.micGain = micGain;
    config.gameGain = gameGain;
    config.noiseGateEnabled = noiseGateEnabled;
    config.duckingEnabled = duckingEnabled;
    config.peakLimiterEnabled = peakLimiterEnabled;
    gAudioDspEngine->setConfig(config);
}

JNIEXPORT void JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeSetGains(
    JNIEnv* /* env */,
    jobject /* this */,
    jfloat gameGain,
    jfloat micGain
) {
    if (gAudioDspEngine) {
        gAudioDspEngine->setGains(gameGain, micGain);
    }
}

JNIEXPORT void JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeSetFilters(
    JNIEnv* /* env */,
    jobject /* this */,
    jboolean noiseGate,
    jboolean ducking,
    jboolean limiter
) {
    if (gAudioDspEngine) {
        gAudioDspEngine->setFilters(noiseGate, ducking, limiter);
    }
}

JNIEXPORT void JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeGetAudioLevels(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray outArray
) {
    if (!gAudioDspEngine || !outArray) return;
    
    jsize len = env->GetArrayLength(outArray);
    if (len < 4) return;

    jfloat buf[4];
    buf[0] = gAudioDspEngine->getGameLevel();
    buf[1] = gAudioDspEngine->getMicLevel();
    buf[2] = gAudioDspEngine->getMasterLevel();
    buf[3] = gAudioDspEngine->getDuckingLevel();

    env->SetFloatArrayRegion(outArray, 0, 4, buf);
}

JNIEXPORT jint JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeProcessAndMixAudio(
    JNIEnv* env,
    jobject /* this */,
    jbyteArray internalAudio,
    jbyteArray micAudio,
    jbyteArray outputMix,
    jint byteCount,
    jboolean isMicMuted
) {
    if (!gAudioDspEngine || byteCount <= 0 || !outputMix) {
        return 0;
    }

    jbyte* pInternal = nullptr;
    if (internalAudio) {
        pInternal = env->GetByteArrayElements(internalAudio, nullptr);
    }

    jbyte* pMic = nullptr;
    if (micAudio) {
        pMic = env->GetByteArrayElements(micAudio, nullptr);
    }

    jbyte* pOut = env->GetByteArrayElements(outputMix, nullptr);

    int sampleCount = byteCount / sizeof(int16_t);
    int writtenBytes = gAudioDspEngine->processAndMix(
        reinterpret_cast<const int16_t*>(pInternal),
        reinterpret_cast<const int16_t*>(pMic),
        reinterpret_cast<int16_t*>(pOut),
        sampleCount,
        isMicMuted
    );

    if (pInternal) {
        env->ReleaseByteArrayElements(internalAudio, pInternal, JNI_ABORT);
    }
    if (pMic) {
        env->ReleaseByteArrayElements(micAudio, pMic, JNI_ABORT);
    }
    if (pOut) {
        env->ReleaseByteArrayElements(outputMix, pOut, 0);
    }

    return writtenBytes;
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeIsVoiceDetected(JNIEnv* /* env */, jobject /* this */) {
    if (!gAudioDspEngine) return JNI_FALSE;
    return static_cast<jboolean>(gAudioDspEngine->isVoiceDetected());
}

JNIEXPORT jfloat JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeGetVoiceEnvelope(JNIEnv* /* env */, jobject /* this */) {
    if (!gAudioDspEngine) return 0.0f;
    return gAudioDspEngine->getVoiceActivityLevel();
}

JNIEXPORT jfloat JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeGetDuckingGain(JNIEnv* /* env */, jobject /* this */) {
    if (!gAudioDspEngine) return 1.0f;
    return gAudioDspEngine->getDuckingLevel();
}

JNIEXPORT void JNICALL
Java_com_example_nativecore_NativeAudioDSPBridge_nativeReleaseAudioDsp(JNIEnv* /* env */, jobject /* this */) {
    gAudioDspEngine.reset();
}

} // extern "C"

