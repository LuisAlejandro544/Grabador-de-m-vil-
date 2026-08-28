#include "libavcodec/avcodec.h"
#include <stdlib.h>
#include <string.h>

static const AVCodec s_h264_codec = {
    .name = "h264_mediacodec",
    .long_name = "H.264 / AVC (Android MediaCodec Hardware Accelerated)",
    .id = AV_CODEC_ID_H264,
    .capabilities = 0
};

static const AVCodec s_hevc_codec = {
    .name = "hevc_mediacodec",
    .long_name = "H.265 / HEVC (Android MediaCodec Hardware Accelerated)",
    .id = AV_CODEC_ID_HEVC,
    .capabilities = 0
};

static const AVCodec s_aac_codec = {
    .name = "aac",
    .long_name = "AAC (Advanced Audio Coding)",
    .id = AV_CODEC_ID_AAC,
    .capabilities = 0
};

static const AVCodec s_mp3_codec = {
    .name = "mp3",
    .long_name = "MP3 (MPEG audio layer 3)",
    .id = AV_CODEC_ID_MP3,
    .capabilities = 0
};

unsigned avcodec_version(void) {
    return (LIBAVCODEC_VERSION_MAJOR << 16) | (LIBAVCODEC_VERSION_MINOR << 8) | LIBAVCODEC_VERSION_MICRO;
}

const char *avcodec_configuration(void) {
    return "--enable-version3 --enable-neon --enable-asm --enable-mediacodec --enable-jni";
}

const char *avcodec_license(void) {
    return "LGPL version 3 or later";
}

const AVCodec *avcodec_find_decoder(enum AVCodecID id) {
    switch (id) {
        case AV_CODEC_ID_H264: return &s_h264_codec;
        case AV_CODEC_ID_HEVC: return &s_hevc_codec;
        case AV_CODEC_ID_AAC:  return &s_aac_codec;
        case AV_CODEC_ID_MP3:  return &s_mp3_codec;
        default: return NULL;
    }
}

const AVCodec *avcodec_find_encoder(enum AVCodecID id) {
    return avcodec_find_decoder(id);
}

AVCodecContext *avcodec_alloc_context3(const AVCodec *codec) {
    AVCodecContext *ctx = (AVCodecContext *)av_malloc(sizeof(AVCodecContext));
    if (ctx) {
        memset(ctx, 0, sizeof(AVCodecContext));
        ctx->codec = codec;
        if (codec) {
            ctx->codec_id = codec->id;
        }
    }
    return ctx;
}

void avcodec_free_context(AVCodecContext **avctx) {
    if (avctx && *avctx) {
        av_free(*avctx);
        *avctx = NULL;
    }
}

int avcodec_open2(AVCodecContext *avctx, const AVCodec *codec, void **options) {
    (void)options;
    if (!avctx) return -1;
    if (codec) {
        avctx->codec = codec;
        avctx->codec_id = codec->id;
    }
    return 0;
}

AVPacket *av_packet_alloc(void) {
    AVPacket *pkt = (AVPacket *)av_malloc(sizeof(AVPacket));
    if (pkt) {
        memset(pkt, 0, sizeof(AVPacket));
    }
    return pkt;
}

void av_packet_free(AVPacket **pkt) {
    if (pkt && *pkt) {
        if ((*pkt)->data) {
            av_free((*pkt)->data);
        }
        av_free(*pkt);
        *pkt = NULL;
    }
}
