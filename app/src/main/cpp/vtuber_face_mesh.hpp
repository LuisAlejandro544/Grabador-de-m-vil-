#ifndef VTUBER_FACE_MESH_HPP
#define VTUBER_FACE_MESH_HPP

#include <cstdint>
#include <vector>
#include <string>

/**
 * Native C++ High-Performance Face Tracking & Mesh Landmark Solver for VTuber Avatars.
 * Processes camera frames, computes Eye Aspect Ratio (EAR), Mouth Aspect Ratio (MAR),
 * and Head Pose Euler angles (Yaw, Pitch, Roll) with temporal smoothing filter at 60 FPS.
 * 100% Offline, Zero Cloud Dependencies, Ultra-low Memory Footprint.
 */
namespace vortex {
namespace vtuber {

struct FaceTrackingResult {
    bool faceDetected;
    float leftEyeOpenness;   // 0.0 (closed) to 1.0 (fully open)
    float rightEyeOpenness;  // 0.0 (closed) to 1.0 (fully open)
    float mouthOpenness;     // 0.0 (closed) to 1.0 (fully open)
    float smileRatio;        // 0.0 (neutral) to 1.0 (smiling)
    float headPitch;         // Up / Down tilt (-30 to +30 deg)
    float headYaw;           // Left / Right rotation (-45 to +45 deg)
    float headRoll;          // Lateral tilt (-45 to +45 deg)
    float confidence;        // 0.0 to 1.0
    int64_t processingTimeUs;
};

class FaceMeshEngine {
public:
    FaceMeshEngine();
    ~FaceMeshEngine();

    bool initialize(int frameWidth, int frameHeight);
    void release();

    FaceTrackingResult processFrameYUV(
        const uint8_t* yData,
        int yRowStride,
        int width,
        int height,
        int rotationDegrees,
        bool isFrontCamera
    );

    FaceTrackingResult processDirectLandmarks(
        const float* landmarks3D,
        int landmarkCount,
        float eyeThreshold,
        float mouthThreshold
    );

    void setSmoothingFactor(float alpha);
    void resetTemporalFilter();

private:
    int frameWidth_;
    int frameHeight_;
    bool isInitialized_;
    float smoothingFactor_;

    // Filtered state for anti-jitter
    float smoothedLeftEye_;
    float smoothedRightEye_;
    float smoothedMouth_;
    float smoothedPitch_;
    float smoothedYaw_;
    float smoothedRoll_;

    float applySmoothing(float current, float target, float alpha);
    float calculateEAR(float p1y, float p2y, float p3y, float p4y, float p5y, float p6y, float width);
    float calculateMAR(float topLipY, float bottomLipY, float mouthWidth);
};

} // namespace vtuber
} // namespace vortex

#endif // VTUBER_FACE_MESH_HPP
