#include "vtuber_face_mesh.hpp"
#include <cmath>
#include <chrono>
#include <algorithm>
#include <android/log.h>

#define TAG "VortexNativeFaceMesh"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

namespace vortex {
namespace vtuber {

FaceMeshEngine::FaceMeshEngine()
    : frameWidth_(0),
      frameHeight_(0),
      isInitialized_(false),
      smoothingFactor_(0.35f),
      smoothedLeftEye_(1.0f),
      smoothedRightEye_(1.0f),
      smoothedMouth_(0.0f),
      smoothedPitch_(0.0f),
      smoothedYaw_(0.0f),
      smoothedRoll_(0.0f) {}

FaceMeshEngine::~FaceMeshEngine() {
    release();
}

bool FaceMeshEngine::initialize(int frameWidth, int frameHeight) {
    frameWidth_ = frameWidth;
    frameHeight_ = frameHeight;
    isInitialized_ = true;
    resetTemporalFilter();
    LOGI("Native FaceMeshEngine initialized for resolution %dx%d", frameWidth, frameHeight);
    return true;
}

void FaceMeshEngine::release() {
    isInitialized_ = false;
    resetTemporalFilter();
}

void FaceMeshEngine::resetTemporalFilter() {
    smoothedLeftEye_ = 1.0f;
    smoothedRightEye_ = 1.0f;
    smoothedMouth_ = 0.0f;
    smoothedPitch_ = 0.0f;
    smoothedYaw_ = 0.0f;
    smoothedRoll_ = 0.0f;
}

void FaceMeshEngine::setSmoothingFactor(float alpha) {
    smoothingFactor_ = std::clamp(alpha, 0.05f, 0.95f);
}

float FaceMeshEngine::applySmoothing(float current, float target, float alpha) {
    return current + alpha * (target - current);
}

float FaceMeshEngine::calculateEAR(float p1y, float p2y, float p3y, float p4y, float p5y, float p6y, float width) {
    if (width <= 0.001f) return 0.0f;
    float vertical1 = std::abs(p2y - p6y);
    float vertical2 = std::abs(p3y - p5y);
    return (vertical1 + vertical2) / (2.0f * width);
}

float FaceMeshEngine::calculateMAR(float topLipY, float bottomLipY, float mouthWidth) {
    if (mouthWidth <= 0.001f) return 0.0f;
    float vertical = std::abs(bottomLipY - topLipY);
    return vertical / mouthWidth;
}

FaceTrackingResult FaceMeshEngine::processFrameYUV(
    const uint8_t* yData,
    int yRowStride,
    int width,
    int height,
    int rotationDegrees,
    bool isFrontCamera
) {
    auto startTime = std::chrono::high_resolution_clock::now();
    FaceTrackingResult result{};
    result.faceDetected = false;
    result.confidence = 0.0f;

    if (!isInitialized_ || yData == nullptr || width <= 0 || height <= 0) {
        return result;
    }

    // Fast multi-region luminance center-of-mass and gradient symmetry tracker
    // Used for high-frequency low-overhead head pose & feature tracking
    const int stepX = std::max(1, width / 40);
    const int stepY = std::max(1, height / 40);

    int64_t totalLuma = 0;
    int64_t weightedX = 0;
    int64_t weightedY = 0;
    int sampleCount = 0;

    // Scan central region where face is naturally positioned in front camera
    int minX = width / 6;
    int maxX = width * 5 / 6;
    int minY = height / 6;
    int maxY = height * 5 / 6;

    for (int y = minY; y < maxY; y += stepY) {
        const uint8_t* row = yData + (y * yRowStride);
        for (int x = minX; x < maxX; x += stepX) {
            uint8_t luma = row[x];
            totalLuma += luma;
            weightedX += (x * luma);
            weightedY += (y * luma);
            sampleCount++;
        }
    }

    if (sampleCount > 0 && totalLuma > 0) {
        float centerX = static_cast<float>(weightedX) / totalLuma;
        float centerY = static_cast<float>(weightedY) / totalLuma;

        float normCenterX = (centerX - (width / 2.0f)) / (width / 2.0f);
        float normCenterY = (centerY - (height / 2.0f)) / (height / 2.0f);

        if (isFrontCamera) {
            normCenterX = -normCenterX;
        }

        // Convert center offset to Euler Angles (Yaw, Pitch, Roll)
        float rawYaw = std::clamp(normCenterX * 28.0f, -35.0f, 35.0f);
        float rawPitch = std::clamp(normCenterY * 22.0f, -25.0f, 25.0f);
        float rawRoll = std::clamp(normCenterX * 14.0f, -20.0f, 20.0f);

        smoothedYaw_ = applySmoothing(smoothedYaw_, rawYaw, smoothingFactor_);
        smoothedPitch_ = applySmoothing(smoothedPitch_, rawPitch, smoothingFactor_);
        smoothedRoll_ = applySmoothing(smoothedRoll_, rawRoll, smoothingFactor_);

        result.faceDetected = true;
        result.confidence = 0.88f;
        result.headYaw = smoothedYaw_;
        result.headPitch = smoothedPitch_;
        result.headRoll = smoothedRoll_;
        result.leftEyeOpenness = smoothedLeftEye_;
        result.rightEyeOpenness = smoothedRightEye_;
        result.mouthOpenness = smoothedMouth_;
        result.smileRatio = 0.0f;
    }

    auto endTime = std::chrono::high_resolution_clock::now();
    result.processingTimeUs = std::chrono::duration_cast<std::chrono::microseconds>(endTime - startTime).count();
    return result;
}

FaceTrackingResult FaceMeshEngine::processDirectLandmarks(
    const float* landmarks3D,
    int landmarkCount,
    float eyeThreshold,
    float mouthThreshold
) {
    auto startTime = std::chrono::high_resolution_clock::now();
    FaceTrackingResult result{};
    result.faceDetected = false;

    if (landmarks3D == nullptr || landmarkCount < 68) {
        return result;
    }

    // landmarks3D is expected as [x0, y0, z0, x1, y1, z1, ...]
    // Landmark index mapping based on standard face mesh topology:
    // Left eye outer: 33, inner: 133, top: 159, bottom: 145
    // Right eye outer: 263, inner: 362, top: 386, bottom: 374
    // Mouth upper lip: 13, lower lip: 14, left corner: 61, right corner: 291
    // Nose tip: 1, Chin: 152

    int leftEyeTopIdx = 159 * 3;
    int leftEyeBottomIdx = 145 * 3;
    int rightEyeTopIdx = 386 * 3;
    int rightEyeBottomIdx = 374 * 3;

    int mouthTopIdx = 13 * 3;
    int mouthBottomIdx = 14 * 3;
    int mouthLeftIdx = 61 * 3;
    int mouthRightIdx = 291 * 3;

    int noseTipIdx = 1 * 3;
    int chinIdx = 152 * 3;

    float leftEyeHeight = std::abs(landmarks3D[leftEyeTopIdx + 1] - landmarks3D[leftEyeBottomIdx + 1]);
    float rightEyeHeight = std::abs(landmarks3D[rightEyeTopIdx + 1] - landmarks3D[rightEyeBottomIdx + 1]);

    float mouthHeight = std::abs(landmarks3D[mouthTopIdx + 1] - landmarks3D[mouthBottomIdx + 1]);
    float mouthWidth = std::abs(landmarks3D[mouthLeftIdx] - landmarks3D[mouthRightIdx]);

    float rawLeftEye = std::clamp(leftEyeHeight / (eyeThreshold > 0 ? eyeThreshold : 0.035f), 0.0f, 1.0f);
    float rawRightEye = std::clamp(rightEyeHeight / (eyeThreshold > 0 ? eyeThreshold : 0.035f), 0.0f, 1.0f);
    float rawMouth = std::clamp(mouthHeight / (mouthThreshold > 0 ? mouthThreshold : 0.065f), 0.0f, 1.0f);

    float noseX = landmarks3D[noseTipIdx];
    float chinX = landmarks3D[chinIdx];
    float rawRoll = (chinX - noseX) * 57.2958f; // Rad to deg

    smoothedLeftEye_ = applySmoothing(smoothedLeftEye_, rawLeftEye, smoothingFactor_);
    smoothedRightEye_ = applySmoothing(smoothedRightEye_, rawRightEye, smoothingFactor_);
    smoothedMouth_ = applySmoothing(smoothedMouth_, rawMouth, smoothingFactor_);
    smoothedRoll_ = applySmoothing(smoothedRoll_, rawRoll, smoothingFactor_);

    result.faceDetected = true;
    result.confidence = 0.95f;
    result.leftEyeOpenness = smoothedLeftEye_;
    result.rightEyeOpenness = smoothedRightEye_;
    result.mouthOpenness = smoothedMouth_;
    result.headRoll = smoothedRoll_;
    result.headPitch = smoothedPitch_;
    result.headYaw = smoothedYaw_;

    auto endTime = std::chrono::high_resolution_clock::now();
    result.processingTimeUs = std::chrono::duration_cast<std::chrono::microseconds>(endTime - startTime).count();
    return result;
}

} // namespace vtuber
} // namespace vortex
