#ifndef OBS_COMPOSITOR_HPP
#define OBS_COMPOSITOR_HPP

#include <cstdint>
#include <string>
#include <vector>
#include <EGL/egl.h>
#include <GLES3/gl3.h>

namespace obs {

enum class SourceType {
    SCREEN_CAPTURE = 0,
    CAMERA_FEED = 1,
    IMAGE_OVERLAY = 2,
    TEXT_LABEL = 3,
    BROWSER_WIDGET = 4
};

enum class LayerShape {
    RECTANGULAR = 0,
    CIRCULAR_FACECAM = 1
};

struct ChromaKeySettings {
    bool enabled = false;
    float keyColorR = 0.0f; // 0.0f - 1.0f (default green 0.0f, 1.0f, 0.0f)
    float keyColorG = 1.0f;
    float keyColorB = 0.0f;
    float similarity = 0.40f; // Tolerance threshold
    float smoothness = 0.10f; // Edge fade
};

struct SceneSource {
    int32_t id;
    std::string name;
    SourceType type;
    float x;           // Normalized screen coordinates [0.0 - 1.0]
    float y;           // Normalized screen coordinates [0.0 - 1.0]
    float width;       // Normalized width [0.0 - 1.0]
    float height;      // Normalized height [0.0 - 1.0]
    float opacity;     // Alpha factor [0.0 - 1.0]
    bool isVisible;
    int32_t zOrder;
    LayerShape shape;
    ChromaKeySettings chromaKey;
    GLuint textureId;
};

struct CompositorStats {
    float renderFps;
    float frameTimeMs;
    int64_t totalFramesRendered;
};

class SceneCompositor {
public:
    SceneCompositor();
    ~SceneCompositor();

    bool initialize(int32_t canvasWidth, int32_t canvasHeight, int32_t targetFps);
    void release();

    int32_t addSource(const std::string& name, SourceType type, float x, float y, float w, float h);
    bool removeSource(int32_t sourceId);
    bool updateSourceTransform(int32_t sourceId, float x, float y, float w, float h, float opacity, bool isVisible);
    bool setSourceShape(int32_t sourceId, LayerShape shape);
    bool setSourceChromaKey(int32_t sourceId, bool enabled, float r, float g, float b, float similarity, float smoothness);
    bool setSourceZOrder(int32_t sourceId, int32_t zOrder);

    // OpenGL ES 3.0 Multilayer Rendering Pass
    void renderFrame(int64_t timestampNs);

    int32_t getSourceCount() const;
    CompositorStats getPerformanceStats() const;
    std::string getEngineVersion() const;

private:
    bool initEGL();
    void releaseEGL();
    bool initGLPipeline();
    void releaseGLPipeline();
    GLuint compileShader(GLenum type, const char* source);

    int32_t mCanvasWidth;
    int32_t mCanvasHeight;
    int32_t mTargetFps;
    bool mIsInitialized;
    std::vector<SceneSource> mSources;
    int32_t mNextSourceId;

    // EGL State
    EGLDisplay mEglDisplay;
    EGLContext mEglContext;
    EGLSurface mEglSurface;
    EGLConfig mEglConfig;

    // OpenGL ES 3.0 Shader Program State
    GLuint mShaderProgram;
    GLint mPosLoc;
    GLint mTexCoordLoc;
    GLint mSamplerLoc;
    GLint mOpacityLoc;
    GLint mShapeTypeLoc;
    GLint mChromaKeyEnabledLoc;
    GLint mKeyColorLoc;
    GLint mSimilarityLoc;
    GLint mSmoothnessLoc;

    GLuint mVBO;
    GLuint mVAO;

    // Performance tracking
    int64_t mLastFrameTimestampNs;
    int64_t mFrameCount;
    float mCurrentRenderFps;
    float mCurrentFrameTimeMs;
};

} // namespace obs

#endif // OBS_COMPOSITOR_HPP
