#pragma once

#include "libavutil/avutil.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LIBSWRESAMPLE_VERSION_MAJOR 5
#define LIBSWRESAMPLE_VERSION_MINOR 1
#define LIBSWRESAMPLE_VERSION_MICRO 100

typedef struct SwrContext {
    int in_sample_rate;
    int out_sample_rate;
    int in_channels;
    int out_channels;
} SwrContext;

unsigned swresample_version(void);
const char *swresample_configuration(void);
const char *swresample_license(void);

SwrContext *swr_alloc(void);
int swr_init(SwrContext *s);
void swr_free(SwrContext **s);
int swr_convert(SwrContext *s, unsigned char **out, int out_count,
                const unsigned char **in, int in_count);

#ifdef __cplusplus
}
#endif
