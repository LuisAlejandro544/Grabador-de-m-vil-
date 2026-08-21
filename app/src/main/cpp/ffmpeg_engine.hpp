#pragma once

#include <cstdint>
#include <string>
#include <functional>

namespace obs {
namespace ffmpeg {

enum class CodecType {
    H264 = 0,
    HEVC = 1,
    AAC = 2,
    MP3 = 3
};

enum class ProcessStatus {
    IDLE = 0,
    PROCESSING = 1,
    COMPLETED = 2,
    ERROR_FILE_NOT_FOUND = 3,
    ERROR_INVALID_CODEC = 4,
    ERROR_MUXING_FAILED = 5
};

struct MediaInfo {
    int64_t durationMs;
    int32_t width;
    int32_t height;
    int32_t fps;
    int64_t bitrate;
    std::string videoCodec;
    std::string audioCodec;
    int32_t audioChannels;
    int32_t sampleRate;
};

struct TrimOptions {
    int64_t startMs;
    int64_t endMs;
    bool accurateCut; // false = stream copy (ultra fast), true = frame-accurate re-encode
};

struct WatermarkOptions {
    std::string imagePath;
    float x;
    float y;
    float scale;
    float opacity;
};

class FFmpegEngine {
public:
    FFmpegEngine();
    ~FFmpegEngine();

    // Initialization & Module State
    bool initialize();
    void release();
    bool isAvailable() const;
    std::string getVersion() const;
    std::string getConfiguration() const;

    // Media Analysis
    MediaInfo probeMedia(const std::string& inputPath);

    // Media Processing Operations
    bool trimVideo(
        const std::string& inputPath,
        const std::string& outputPath,
        const TrimOptions& options,
        std::function<void(float progress)> progressCallback = nullptr
    );

    bool extractAudio(
        const std::string& inputPath,
        const std::string& outputPath,
        CodecType audioCodec,
        std::function<void(float progress)> progressCallback = nullptr
    );

    bool compressVideo(
        const std::string& inputPath,
        const std::string& outputPath,
        int64_t targetBitrate,
        int32_t targetWidth,
        int32_t targetHeight,
        int32_t targetFps,
        std::function<void(float progress)> progressCallback = nullptr
    );

    bool applyWatermark(
        const std::string& inputPath,
        const std::string& outputPath,
        const WatermarkOptions& options,
        std::function<void(float progress)> progressCallback = nullptr
    );

private:
    bool mIsInitialized;
    ProcessStatus mCurrentStatus;
    float mLastProgress;
};

} // namespace ffmpeg
} // namespace obs
