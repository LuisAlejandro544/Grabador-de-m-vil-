#include "obs_compositor.hpp"
#include <android/log.h>
#include <algorithm>
#include <cmath>

#define LOG_TAG "OBS_NATIVE_ENGINE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace obs {

// OpenGL ES 3.0 Vertex Shader for Layer transformation
static const char* VERTEX_SHADER_SRC = R"(#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main() {
    gl_Position = vec4(aPosition, 0.0, 1.0);
    vTexCoord = aTexCoord;
}
)";

// OpenGL ES 3.0 Fragment Shader with Facecam Circular Mask and GPU Chroma Key
static const char* FRAGMENT_SHADER_SRC = R"(#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uSampler;
uniform float uOpacity;
uniform int uShapeType; // 0 = Rectangular, 1 = Circular Facecam
uniform int uChromaKeyEnabled;
uniform vec3 uKeyColor;
uniform float uSimilarity;
uniform float uSmoothness;

void main() {
    vec4 texColor = texture(uSampler, vTexCoord);

    // 1. Circular Masking for Facecam
    if (uShapeType == 1) {
        vec2 center = vec2(0.5, 0.5);
        float dist = distance(vTexCoord, center);
        float radius = 0.5;
        float alphaMask = smoothstep(radius, radius - 0.02, dist);
        texColor.a *= alphaMask;
        if (texColor.a <= 0.0) {
            discard;
        }
    }

    // 2. Real-time GPU Chroma Key filter
    if (uChromaKeyEnabled == 1) {
        float diff = distance(texColor.rgb, uKeyColor);
        if (diff < uSimilarity) {
            float chromaAlpha = smoothstep(uSimilarity - uSmoothness, uSimilarity, diff);
            texColor.a *= chromaAlpha;
        }
        if (texColor.a <= 0.001) {
            discard;
        }
    }

    // 3. Layer Opacity Blending
    texColor.a *= uOpacity;
    fragColor = texColor;
}
)";

SceneCompositor::SceneCompositor()
    : mCanvasWidth(1920),
      mCanvasHeight(1080),
      mTargetFps(60),
      mIsInitialized(false),
      mNextSourceId(1),
      mEglDisplay(EGL_NO_DISPLAY),
      mEglContext(EGL_NO_CONTEXT),
      mEglSurface(EGL_NO_SURFACE),
      mEglConfig(nullptr),
      mShaderProgram(0),
      mPosLoc(-1),
      mTexCoordLoc(-1),
      mSamplerLoc(-1),
      mOpacityLoc(-1),
      mShapeTypeLoc(-1),
      mChromaKeyEnabledLoc(-1),
      mKeyColorLoc(-1),
      mSimilarityLoc(-1),
      mSmoothnessLoc(-1),
      mVBO(0),
      mVAO(0),
      mLastFrameTimestampNs(0),
      mFrameCount(0),
      mCurrentRenderFps(60.0f),
      mCurrentFrameTimeMs(16.6f) {}

SceneCompositor::~SceneCompositor() {
    release();
}

bool SceneCompositor::initEGL() {
    mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (mEglDisplay == EGL_NO_DISPLAY) {
        LOGE("EGL get display failed");
        return false;
    }

    EGLint major, minor;
    if (!eglInitialize(mEglDisplay, &major, &minor)) {
        LOGE("EGL initialize failed");
        return false;
    }

    const EGLint configAttribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_NONE
    };

    EGLint numConfigs;
    if (!eglChooseConfig(mEglDisplay, configAttribs, &mEglConfig, 1, &numConfigs) || numConfigs == 0) {
        LOGE("EGL choose config failed");
        return false;
    }

    const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };

    mEglContext = eglCreateContext(mEglDisplay, mEglConfig, EGL_NO_CONTEXT, contextAttribs);
    if (mEglContext == EGL_NO_CONTEXT) {
        LOGE("EGL create context failed");
        return false;
    }

    const EGLint pbufferAttribs[] = {
        EGL_WIDTH, mCanvasWidth,
        EGL_HEIGHT, mCanvasHeight,
        EGL_NONE
    };

    mEglSurface = eglCreatePbufferSurface(mEglDisplay, mEglConfig, pbufferAttribs);
    if (mEglSurface == EGL_NO_SURFACE) {
        LOGE("EGL create pbuffer surface failed");
        return false;
    }

    if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
        LOGE("EGL make current failed");
        return false;
    }

    LOGI("EGL & OpenGL ES 3.0 Context initialized successfully [%dx%d]", mCanvasWidth, mCanvasHeight);
    return true;
}

void SceneCompositor::releaseEGL() {
    if (mEglDisplay != EGL_NO_DISPLAY) {
        eglMakeCurrent(mEglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (mEglSurface != EGL_NO_SURFACE) {
            eglDestroySurface(mEglDisplay, mEglSurface);
            mEglSurface = EGL_NO_SURFACE;
        }
        if (mEglContext != EGL_NO_CONTEXT) {
            eglDestroyContext(mEglDisplay, mEglContext);
            mEglContext = EGL_NO_CONTEXT;
        }
        eglTerminate(mEglDisplay);
        mEglDisplay = EGL_NO_DISPLAY;
    }
}

GLuint SceneCompositor::compileShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    if (!shader) return 0;

    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 0) {
            std::vector<char> infoLog(infoLen);
            glGetShaderInfoLog(shader, infoLen, nullptr, infoLog.data());
            LOGE("Shader compilation error: %s", infoLog.data());
        }
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

bool SceneCompositor::initGLPipeline() {
    GLuint vertShader = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER_SRC);
    GLuint fragShader = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC);

    if (!vertShader || !fragShader) {
        LOGE("Failed to compile compositor shaders");
        return false;
    }

    mShaderProgram = glCreateProgram();
    glAttachShader(mShaderProgram, vertShader);
    glAttachShader(mShaderProgram, fragShader);
    glLinkProgram(mShaderProgram);

    GLint linked = 0;
    glGetProgramiv(mShaderProgram, GL_LINK_STATUS, &linked);
    if (!linked) {
        LOGE("Failed to link shader program");
        glDeleteProgram(mShaderProgram);
        mShaderProgram = 0;
        return false;
    }

    glDeleteShader(vertShader);
    glDeleteShader(fragShader);

    mPosLoc = glGetAttribLocation(mShaderProgram, "aPosition");
    mTexCoordLoc = glGetAttribLocation(mShaderProgram, "aTexCoord");
    mSamplerLoc = glGetUniformLocation(mShaderProgram, "uSampler");
    mOpacityLoc = glGetUniformLocation(mShaderProgram, "uOpacity");
    mShapeTypeLoc = glGetUniformLocation(mShaderProgram, "uShapeType");
    mChromaKeyEnabledLoc = glGetUniformLocation(mShaderProgram, "uChromaKeyEnabled");
    mKeyColorLoc = glGetUniformLocation(mShaderProgram, "uKeyColor");
    mSimilarityLoc = glGetUniformLocation(mShaderProgram, "uSimilarity");
    mSmoothnessLoc = glGetUniformLocation(mShaderProgram, "uSmoothness");

    // Configure Default Quad Buffers
    glGenVertexArrays(1, &mVAO);
    glGenBuffers(1, &mVBO);

    LOGI("OpenGL ES 3.0 Scene Compositor Pipeline ready");
    return true;
}

void SceneCompositor::releaseGLPipeline() {
    if (mVAO) {
        glDeleteVertexArrays(1, &mVAO);
        mVAO = 0;
    }
    if (mVBO) {
        glDeleteBuffers(1, &mVBO);
        mVBO = 0;
    }
    if (mShaderProgram) {
        glDeleteProgram(mShaderProgram);
        mShaderProgram = 0;
    }
}

bool SceneCompositor::initialize(int32_t canvasWidth, int32_t canvasHeight, int32_t targetFps) {
    mCanvasWidth = canvasWidth;
    mCanvasHeight = canvasHeight;
    mTargetFps = targetFps;
    mSources.clear();
    mNextSourceId = 1;
    mFrameCount = 0;
    mLastFrameTimestampNs = 0;

    initEGL();
    initGLPipeline();

    mIsInitialized = true;
    LOGI("OBS SceneCompositor C++ Engine Initialized [%dx%d @ %d FPS]", canvasWidth, canvasHeight, targetFps);
    return true;
}

void SceneCompositor::release() {
    releaseGLPipeline();
    releaseEGL();
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
    src.shape = (type == SourceType::CAMERA_FEED) ? LayerShape::CIRCULAR_FACECAM : LayerShape::RECTANGULAR;
    src.textureId = 0;

    mSources.push_back(src);
    LOGI("Added Source: %s (ID: %d, Type: %d, Shape: %d)", name.c_str(), newId, static_cast<int>(type), static_cast<int>(src.shape));
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
            s.opacity = std::clamp(opacity, 0.0f, 1.0f);
            s.isVisible = isVisible;
            return true;
        }
    }
    return false;
}

bool SceneCompositor::setSourceShape(int32_t sourceId, LayerShape shape) {
    for (auto& s : mSources) {
        if (s.id == sourceId) {
            s.shape = shape;
            LOGI("Updated Source ID %d Shape: %d", sourceId, static_cast<int>(shape));
            return true;
        }
    }
    return false;
}

bool SceneCompositor::setSourceChromaKey(int32_t sourceId, bool enabled, float r, float g, float b, float similarity, float smoothness) {
    for (auto& s : mSources) {
        if (s.id == sourceId) {
            s.chromaKey.enabled = enabled;
            s.chromaKey.keyColorR = r;
            s.chromaKey.keyColorG = g;
            s.chromaKey.keyColorB = b;
            s.chromaKey.similarity = similarity;
            s.chromaKey.smoothness = smoothness;
            LOGI("Updated Chroma Key on Source ID %d (Enabled: %d)", sourceId, enabled ? 1 : 0);
            return true;
        }
    }
    return false;
}

bool SceneCompositor::setSourceZOrder(int32_t sourceId, int32_t zOrder) {
    for (auto& s : mSources) {
        if (s.id == sourceId) {
            s.zOrder = zOrder;
            return true;
        }
    }
    return false;
}

void SceneCompositor::renderFrame(int64_t timestampNs) {
    if (!mIsInitialized) return;

    // Performance frame time calculation
    if (mLastFrameTimestampNs > 0) {
        int64_t deltaNs = timestampNs - mLastFrameTimestampNs;
        if (deltaNs > 0) {
            float frameMs = static_cast<float>(deltaNs) / 1000000.0f;
            mCurrentFrameTimeMs = (mCurrentFrameTimeMs * 0.9f) + (frameMs * 0.1f);
            if (frameMs > 0.0f) {
                float instantFps = 1000.0f / frameMs;
                mCurrentRenderFps = (mCurrentRenderFps * 0.9f) + (instantFps * 0.1f);
            }
        }
    }
    mLastFrameTimestampNs = timestampNs;
    mFrameCount++;

    // 1. Setup OpenGL ES 3.0 Viewport & Blending
    glViewport(0, 0, mCanvasWidth, mCanvasHeight);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    // 2. Sort visible sources by Z-Order
    std::vector<SceneSource> sortedSources = mSources;
    std::sort(sortedSources.begin(), sortedSources.end(), [](const SceneSource& a, const SceneSource& b) {
        return a.zOrder < b.zOrder;
    });

    glUseProgram(mShaderProgram);

    // 3. Render each layer quad
    for (const auto& src : sortedSources) {
        if (!src.isVisible || src.opacity <= 0.001f) continue;

        // Convert normalized [0, 1] to OpenGL NDC [-1, 1]
        float left = (src.x * 2.0f) - 1.0f;
        float right = ((src.x + src.width) * 2.0f) - 1.0f;
        float top = 1.0f - (src.y * 2.0f);
        float bottom = 1.0f - ((src.y + src.height) * 2.0f);

        // Quad Vertices: Position (x, y) and Texture Coordinates (u, v)
        float quadVertices[] = {
            left,  top,    0.0f, 0.0f,
            left,  bottom, 0.0f, 1.0f,
            right, bottom, 1.0f, 1.0f,

            left,  top,    0.0f, 0.0f,
            right, bottom, 1.0f, 1.0f,
            right, top,    1.0f, 0.0f
        };

        glBindVertexArray(mVAO);
        glBindBuffer(GL_ARRAY_BUFFER, mVBO);
        glBufferData(GL_ARRAY_BUFFER, sizeof(quadVertices), quadVertices, GL_DYNAMIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)0);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)(2 * sizeof(float)));

        // Pass Uniforms
        glUniform1f(mOpacityLoc, src.opacity);
        glUniform1i(mShapeTypeLoc, static_cast<int>(src.shape));
        glUniform1i(mChromaKeyEnabledLoc, src.chromaKey.enabled ? 1 : 0);
        if (src.chromaKey.enabled) {
            glUniform3f(mKeyColorLoc, src.chromaKey.keyColorR, src.chromaKey.keyColorG, src.chromaKey.keyColorB);
            glUniform1f(mSimilarityLoc, src.chromaKey.similarity);
            glUniform1f(mSmoothnessLoc, src.chromaKey.smoothness);
        }

        glDrawArrays(GL_TRIANGLES, 0, 6);
    }

    glDisable(GL_BLEND);
    glBindVertexArray(0);
    glUseProgram(0);
}

int32_t SceneCompositor::getSourceCount() const {
    return static_cast<int32_t>(mSources.size());
}

CompositorStats SceneCompositor::getPerformanceStats() const {
    CompositorStats stats;
    stats.renderFps = mCurrentRenderFps;
    stats.frameTimeMs = mCurrentFrameTimeMs;
    stats.totalFramesRendered = mFrameCount;
    return stats;
}

std::string SceneCompositor::getEngineVersion() const {
    return "OBS-NativeCore-v1.1.0 (C++20 / GLES3 Multilayer EGL)";
}

} // namespace obs
