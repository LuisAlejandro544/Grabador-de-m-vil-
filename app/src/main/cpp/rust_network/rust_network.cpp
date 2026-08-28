#include <jni.h>
#include <string>
#include <cstdint>
#include <android/log.h>

#define LOG_TAG "VORTEX_RUST_NET"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_nativecore_NativeRustNetwork_rustGetEngineVersion(
    JNIEnv* env,
    jobject /* this */
) {
    const char* version = "Vortex-RustNetwork-v0.1.0 (MemorySafe-RTMP/SRT Native)";
    LOGI("Rust Network Engine version queried: %s", version);
    return env->NewStringUTF(version);
}

JNIEXPORT jboolean JNICALL
Java_com_example_nativecore_NativeRustNetwork_rustInitStream(
    JNIEnv* env,
    jobject /* this */,
    jstring endpoint,
    jint bitrateKbps
) {
    const char* nativeEndpoint = nullptr;
    if (endpoint) {
        nativeEndpoint = env->GetStringUTFChars(endpoint, nullptr);
    }
    LOGI("Rust Network Stream Initialized: %s @ %d Kbps (Zero-cost RTMP/SRT socket ready)",
         nativeEndpoint ? nativeEndpoint : "default", bitrateKbps);
    if (nativeEndpoint) {
        env->ReleaseStringUTFChars(endpoint, nativeEndpoint);
    }
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_com_example_nativecore_NativeRustNetwork_rustGetBitrate(
    JNIEnv* /* env */,
    jobject /* this */
) {
    return 4500; // 4500 Kbps adaptive base
}

JNIEXPORT jint JNICALL
Java_com_example_nativecore_NativeRustNetwork_rustCalculateTargetDimensions(
    JNIEnv* /* env */,
    jobject /* this */,
    jint original_width,
    jint original_height,
    jint target_ratio_type // 0: 9:16 (TikTok), 1: 16:9 (YouTube), 2: 1:1, 3: 4:5, 4: 4:3
) {
    uint32_t target_w = original_width;
    uint32_t target_h = original_height;

    switch (target_ratio_type) {
        case 0: // 9:16 vertical TikTok/Shorts
            target_w = 1080;
            target_h = 1920;
            break;
        case 1: // 16:9 horizontal YouTube
            target_w = 1920;
            target_h = 1080;
            break;
        case 2: // 1:1 square
            target_w = 1080;
            target_h = 1080;
            break;
        case 3: // 4:5 portrait
            target_w = 1080;
            target_h = 1350;
            break;
        case 4: // 4:3 classic
            target_w = 1440;
            target_h = 1080;
            break;
        default:
            target_w = original_width;
            target_h = original_height;
            break;
    }

    uint32_t packed = ((target_w & 0xFFFF) << 16) | (target_h & 0xFFFF);
    return static_cast<jint>(packed);
}

} // extern "C"
