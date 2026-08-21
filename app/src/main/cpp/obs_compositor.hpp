#ifndef OBS_COMPOSITOR_HPP
#define OBS_COMPOSITOR_HPP

#include <cstdint>
#include <string>
#include <vector>

namespace obs {

enum class SourceType {
    SCREEN_CAPTURE = 0,
    CAMERA_FEED = 1,
    IMAGE_OVERLAY = 2,
    TEXT_LABEL = 3,
    BROWSER_WIDGET = 4
};

struct SceneSource {
    int32_t id;
    std::string name;
    SourceType type;
    float x;
    float y;
    float width;
    float height;
    float opacity;
    bool isVisible;
    int32_t zOrder;
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

    // Frame rendering pipeline step (OpenGL ES 3.0 texture blend)
    void renderFrame(int64_t timestampNs);

    int32_t getSourceCount() const;
    std::string getEngineVersion() const;

private:
    int32_t mCanvasWidth;
    int32_t mCanvasHeight;
    int32_t mTargetFps;
    bool mIsInitialized;
    std::vector<SceneSource> mSources;
    int32_t mNextSourceId;
};

} // namespace obs

#endif // OBS_COMPOSITOR_HPP
