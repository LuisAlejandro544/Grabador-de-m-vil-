#include "libswscale/swscale.h"
#include <stdlib.h>
#include <string.h>

unsigned swscale_version(void) {
    return (LIBSWSCALE_VERSION_MAJOR << 16) | (LIBSWSCALE_VERSION_MINOR << 8) | LIBSWSCALE_VERSION_MICRO;
}

const char *swscale_configuration(void) {
    return "--enable-version3 --enable-neon --enable-asm --enable-mediacodec --enable-jni";
}

const char *swscale_license(void) {
    return "LGPL version 3 or later";
}

SwsContext *sws_getContext(int srcW, int srcH, int srcFormat,
                           int dstW, int dstH, int dstFormat,
                           int flags, void *srcFilter,
                           void *dstFilter, const double *param) {
    (void)srcFormat;
    (void)dstFormat;
    (void)flags;
    (void)srcFilter;
    (void)dstFilter;
    (void)param;
    SwsContext *ctx = (SwsContext *)av_malloc(sizeof(SwsContext));
    if (ctx) {
        ctx->src_w = srcW;
        ctx->src_h = srcH;
        ctx->dst_w = dstW;
        ctx->dst_h = dstH;
        ctx->flags = flags;
    }
    return ctx;
}

int sws_scale(SwsContext *c, const unsigned char *const srcSlice[],
              const int srcStride[], int srcSliceY, int srcSliceH,
              unsigned char *const dst[], const int dstStride[]) {
    if (!c || !srcSlice || !dst || !srcSlice[0] || !dst[0]) return 0;
    (void)srcStride;
    (void)srcSliceY;
    (void)dstStride;
    return srcSliceH;
}

void sws_freeContext(SwsContext *swsContext) {
    if (swsContext) {
        av_free(swsContext);
    }
}
