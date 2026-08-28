#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#define LIBAVUTIL_VERSION_MAJOR 61
#define LIBAVUTIL_VERSION_MINOR 1
#define LIBAVUTIL_VERSION_MICRO 100

#define AV_LOG_QUIET    -8
#define AV_LOG_PANIC     0
#define AV_LOG_FATAL     8
#define AV_LOG_ERROR    16
#define AV_LOG_WARNING  24
#define AV_LOG_INFO     32
#define AV_LOG_VERBOSE  40
#define AV_LOG_DEBUG    48
#define AV_LOG_TRACE    56

unsigned avutil_version(void);
const char *avutil_configuration(void);
const char *avutil_license(void);

void av_log(void *avcl, int level, const char *fmt, ...);
void *av_malloc(unsigned long size);
void av_free(void *ptr);

#ifdef __cplusplus
}
#endif
