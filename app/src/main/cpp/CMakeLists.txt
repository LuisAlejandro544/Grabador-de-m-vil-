cmake_minimum_required(VERSION 3.22.1)

project("obs_core")

# Add native C++ OBS engine shared library
add_library(
    obs_core
    SHARED
    obs_core.cpp
    obs_compositor.cpp
    ffmpeg_engine.cpp
    audio_dsp_engine.cpp
)

# Search for system libraries in Android NDK
find_library(
    log-lib
    log
)

find_library(
    gles3-lib
    GLESv3
)

find_library(
    egl-lib
    EGL
)

find_library(
    android-lib
    android
)

# Link C++ engine with Android NDK native libraries (Log, GLESv3, EGL, Android Surface)
target_link_libraries(
    obs_core
    ${log-lib}
    ${gles3-lib}
    ${egl-lib}
    ${android-lib}
)
