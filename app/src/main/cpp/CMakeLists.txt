cmake_minimum_required(VERSION 3.22.1)

project("vortex_native_suite")

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Search for system libraries in Android NDK
find_library(log-lib log)
find_library(gles3-lib GLESv3)
find_library(egl-lib EGL)
find_library(android-lib android)

# -----------------------------------------------------------------------------
# 1. FFmpeg Native Shared Libraries (libavutil, libavcodec, libavformat, etc.)
# -----------------------------------------------------------------------------

add_library(
    avutil
    SHARED
    ffmpeg/avutil.c
)
target_link_libraries(avutil ${log-lib})

add_library(
    swresample
    SHARED
    ffmpeg/swresample.c
)
target_link_libraries(swresample avutil ${log-lib})

add_library(
    swscale
    SHARED
    ffmpeg/swscale.c
)
target_link_libraries(swscale avutil ${log-lib})

add_library(
    avcodec
    SHARED
    ffmpeg/avcodec.c
)
target_link_libraries(avcodec avutil swresample ${log-lib})

add_library(
    avformat
    SHARED
    ffmpeg/avformat.c
)
target_link_libraries(avformat avcodec avutil ${log-lib})

add_library(
    avfilter
    SHARED
    ffmpeg/avfilter.c
)
target_link_libraries(avfilter avformat avcodec avutil swscale ${log-lib})

# -----------------------------------------------------------------------------
# 2. Rust Network Engine (libvortex_rust_network.so)
# -----------------------------------------------------------------------------

add_library(
    vortex_rust_network
    SHARED
    rust_network/rust_network.cpp
)
target_link_libraries(vortex_rust_network ${log-lib})

# -----------------------------------------------------------------------------
# 3. Main OBS Compositor Engine (libobs_core.so)
# -----------------------------------------------------------------------------

add_library(
    obs_core
    SHARED
    obs_core.cpp
    obs_compositor.cpp
    ffmpeg_engine.cpp
    audio_dsp_engine.cpp
    vtuber_face_mesh.cpp
)

target_link_libraries(
    obs_core
    avformat
    avcodec
    avfilter
    avutil
    swscale
    swresample
    ${log-lib}
    ${gles3-lib}
    ${egl-lib}
    ${android-lib}
)
