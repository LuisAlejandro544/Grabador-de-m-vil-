#include <jni.h>
#include <string>
#include <memory>
#include "obs_compositor.hpp"

static std::unique_ptr<obs::SceneCompositor> gCompositor = nullptr;

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
    std::string nameStr(nativeName);
    env->ReleaseStringUTFChars(name, nativeName);

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

JNIEXPORT jint JNICALL
Java_com_example_nativecore_NativeOBSBridge_nativeGetSourceCount(JNIEnv* /* env */, jobject /* this */) {
    if (!gCompositor) return 0;
    return gCompositor->getSourceCount();
}

} // extern "C"
