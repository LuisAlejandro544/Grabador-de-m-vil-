#include "libswresample/swresample.h"
#include <stdlib.h>
#include <string.h>

unsigned swresample_version(void) {
    return (LIBSWRESAMPLE_VERSION_MAJOR << 16) | (LIBSWRESAMPLE_VERSION_MINOR << 8) | LIBSWRESAMPLE_VERSION_MICRO;
}

const char *swresample_configuration(void) {
    return "--enable-version3 --enable-neon --enable-asm --enable-mediacodec --enable-jni";
}

const char *swresample_license(void) {
    return "LGPL version 3 or later";
}

SwrContext *swr_alloc(void) {
    SwrContext *ctx = (SwrContext *)av_malloc(sizeof(SwrContext));
    if (ctx) {
        memset(ctx, 0, sizeof(SwrContext));
        ctx->in_sample_rate = 48000;
        ctx->out_sample_rate = 48000;
        ctx->in_channels = 2;
        ctx->out_channels = 2;
    }
    return ctx;
}

int swr_init(SwrContext *s) {
    (void)s;
    return 0;
}

void swr_free(SwrContext **s) {
    if (s && *s) {
        av_free(*s);
        *s = NULL;
    }
}

int swr_convert(SwrContext *s, unsigned char **out, int out_count,
                const unsigned char **in, int in_count) {
    if (!s || !out || !in || !out[0] || !in[0]) return 0;
    int copy_samples = (out_count < in_count) ? out_count : in_count;
    int bytes_per_sample = 2 * s->out_channels; // 16-bit PCM
    memcpy(out[0], in[0], copy_samples * bytes_per_sample);
    return copy_samples;
}
