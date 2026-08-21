#include "obs_compositor.hpp"
#include <android/log.h>
#include <algorithm>

#define LOG_TAG "OBS_NATIVE_ENGINE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace obs {

SceneCompositor::SceneCompositor()
    : mCanvasWidth(1920),
      mCanvasHeight(1080),
      mTargetFps(60),
      mIsInitialized(false),
      mNextSourceId(1) {}

SceneCompositor::~SceneCompositor() {
    release();
}

bool SceneCompositor::initialize(int32_t canvasWidth, int32_t canvasHeight, int32_t targetFps) {
    mCanvasWidth = canvasWidth;
    mCanvasHeight = canvasHeight;
    mTargetFps = targetFps;
    mSources.clear();
    mIsInitialized = true;
    LOGI("OBS SceneCompositor C++ Engine Initialized [%dx%d @ %d FPS]", canvasWidth, canvasHeight, targetFps);
    return true;
}

void SceneCompositor::release() {
    mSources.clear();
    mIsInitialized = false;
    LOGI("OBS SceneCompositor C++ Engine Released");
}

int32_t SceneCompositor::addSource(const std::string& name, SourceType type, float x, float y, float w, float h) {
    int32_t newId = mNextSourceId++;
    SceneSource src;
    src.id = newId;
    src.name = name;
    src.type = type;
    src.x = x;
    src.y = y;
    src.width = w;
    src.height = h;
    src.opacity = 1.0f;
    src.isVisible = true;
    src.zOrder = static_cast<int32_t>(mSources.size());

    mSources.push_back(src);
    LOGI("Added Source: %s (ID: %d, Type: %d)", name.c_str(), newId, static_cast<int>(type));
    return newId;
}

bool SceneCompositor::removeSource(int32_t sourceId) {
    auto it = std::remove_if(mSources.begin(), mSources.end(), [sourceId](const SceneSource& s) {
        return s.id == sourceId;
    });
    if (it != mSources.end()) {
        mSources.erase(it, mSources.end());
        LOGI("Removed Source ID: %d", sourceId);
        return true;
    }
    return false;
}

bool SceneCompositor::updateSourceTransform(int32_t sourceId, float x, float y, float w, float h, float opacity, bool isVisible) {
    for (auto& s : mSources) {
        if (s.id == sourceId) {
            s.x = x;
            s.y = y;
            s.width = w;
            s.height = h;
            s.opacity = opacity;
            s.isVisible = isVisible;
            return true;
        }
    }
    return false;
}

void SceneCompositor::renderFrame(int64_t timestampNs) {
    if (!mIsInitialized) return;
    // C++ OpenGL ES 3.0 texture blending pipeline hook
}

int32_t SceneCompositor::getSourceCount() const {
    return static_cast<int32_t>(mSources.size());
}

std::string SceneCompositor::getEngineVersion() const {
    return "OBS-NativeCore-v1.0.0 (C++20 / GLES3)";
}

} // namespace obs
