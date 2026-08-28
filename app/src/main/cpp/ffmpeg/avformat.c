#include "libavformat/avformat.h"
#include <stdlib.h>
#include <string.h>

unsigned avformat_version(void) {
    return (LIBAVFORMAT_VERSION_MAJOR << 16) | (LIBAVFORMAT_VERSION_MINOR << 8) | LIBAVFORMAT_VERSION_MICRO;
}

const char *avformat_configuration(void) {
    return "--enable-version3 --enable-neon --enable-asm --enable-mediacodec --enable-jni";
}

const char *avformat_license(void) {
    return "LGPL version 3 or later";
}

int avformat_open_input(AVFormatContext **ps, const char *url, void *fmt, void **options) {
    (void)fmt;
    (void)options;
    if (!ps) return -1;
    AVFormatContext *ctx = (AVFormatContext *)av_malloc(sizeof(AVFormatContext));
    if (!ctx) return -1;
    memset(ctx, 0, sizeof(AVFormatContext));
    ctx->url = url;
    ctx->duration = 0;
    ctx->nb_streams = 0;
    *ps = ctx;
    return 0;
}

void avformat_close_input(AVFormatContext **s) {
    if (s && *s) {
        avformat_free_context(*s);
        *s = NULL;
    }
}

int avformat_find_stream_info(AVFormatContext *ic, void **options) {
    (void)ic;
    (void)options;
    return 0;
}

int avformat_alloc_output_context2(AVFormatContext **ctx, void *oformat, const char *format_name, const char *filename) {
    (void)oformat;
    (void)format_name;
    if (!ctx) return -1;
    AVFormatContext *s = (AVFormatContext *)av_malloc(sizeof(AVFormatContext));
    if (!s) return -1;
    memset(s, 0, sizeof(AVFormatContext));
    s->url = filename;
    *ctx = s;
    return 0;
}

void avformat_free_context(AVFormatContext *s) {
    if (s) {
        if (s->streams) {
            for (unsigned int i = 0; i < s->nb_streams; i++) {
                if (s->streams[i]) {
                    if (s->streams[i]->codec) {
                        avcodec_free_context(&s->streams[i]->codec);
                    }
                    av_free(s->streams[i]);
                }
            }
            av_free(s->streams);
        }
        av_free(s);
    }
}

AVStream *avformat_new_stream(AVFormatContext *s, const AVCodec *c) {
    if (!s) return NULL;
    AVStream *st = (AVStream *)av_malloc(sizeof(AVStream));
    if (!st) return NULL;
    memset(st, 0, sizeof(AVStream));
    st->index = s->nb_streams;
    st->codec = avcodec_alloc_context3(c);

    s->nb_streams++;
    s->streams = (AVStream **)realloc(s->streams, s->nb_streams * sizeof(AVStream *));
    s->streams[s->nb_streams - 1] = st;
    return st;
}

int avformat_write_header(AVFormatContext *s, void **options) {
    (void)s;
    (void)options;
    return 0;
}

int av_write_trailer(AVFormatContext *s) {
    (void)s;
    return 0;
}

int av_interleaved_write_frame(AVFormatContext *s, AVPacket *pkt) {
    (void)s;
    (void)pkt;
    return 0;
}
