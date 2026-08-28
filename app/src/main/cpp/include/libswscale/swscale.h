#pragma once

#include "libavutil/avutil.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LIBSWSCALE_VERSION_MAJOR 8
#define LIBSWSCALE_VERSION_MINOR 1
#define LIBSWSCALE_VERSION_MICRO 100

typedef struct SwsContext {
    int src_w, src_h;
    int dst_w, dst_h;
    int flags;
} SwsContext;

unsigned swscale_version(void);
const char *swscale_configuration(void);
const char *swscale_license(void);

SwsContext *sws_getContext(int srcW, int srcH, int srcFormat,
                           int dstW, int dstH, int dstFormat,
                           int flags, void *srcFilter,
                           void *dstFilter, const double *param);

int sws_scale(SwsContext *c, const unsigned char *const srcSlice[],
              const int srcStride[], int srcSliceY, int srcSliceH,
              unsigned char *const dst[], const int dstStride[]);

void sws_freeContext(SwsContext *swsContext);

#ifdef __cplusplus
}
#endif
