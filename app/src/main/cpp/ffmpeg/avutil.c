#include "libavutil/avutil.h"
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <android/log.h>

#define LOG_TAG "FFMPEG_AVUTIL"

unsigned avutil_version(void) {
    return (LIBAVUTIL_VERSION_MAJOR << 16) | (LIBAVUTIL_VERSION_MINOR << 8) | LIBAVUTIL_VERSION_MICRO;
}

const char *avutil_configuration(void) {
    return "--enable-version3 --enable-neon --enable-asm --enable-mediacodec --enable-jni";
}

const char *avutil_license(void) {
    return "LGPL version 3 or later";
}

void av_log(void *avcl, int level, const char *fmt, ...) {
    (void)avcl;
    int android_level = ANDROID_LOG_INFO;
    if (level <= AV_LOG_FATAL) android_level = ANDROID_LOG_FATAL;
    else if (level <= AV_LOG_ERROR) android_level = ANDROID_LOG_ERROR;
    else if (level <= AV_LOG_WARNING) android_level = ANDROID_LOG_WARN;
    else if (level <= AV_LOG_INFO) android_level = ANDROID_LOG_INFO;
    else android_level = ANDROID_LOG_DEBUG;

    va_list args;
    va_start(args, fmt);
    __android_log_vprint(android_level, LOG_TAG, fmt, args);
    va_end(args);
}

void *av_malloc(unsigned long size) {
    if (size == 0) return NULL;
    return malloc(size);
}

void av_free(void *ptr) {
    if (ptr) {
        free(ptr);
    }
}
