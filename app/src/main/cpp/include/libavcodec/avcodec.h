#pragma once

#include "libavutil/avutil.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LIBAVCODEC_VERSION_MAJOR 61
#define LIBAVCODEC_VERSION_MINOR 3
#define LIBAVCODEC_VERSION_MICRO 100

enum AVCodecID {
    AV_CODEC_ID_NONE = 0,
    AV_CODEC_ID_H264 = 27,
    AV_CODEC_ID_HEVC = 173,
    AV_CODEC_ID_AAC  = 86018,
    AV_CODEC_ID_MP3  = 86017,
};

typedef struct AVCodec {
    const char *name;
    const char *long_name;
    enum AVCodecID id;
    int capabilities;
} AVCodec;

typedef struct AVCodecContext {
    const AVCodec *codec;
    enum AVCodecID codec_id;
    int width;
    int height;
    int bit_rate;
    int sample_rate;
    int channels;
    void *priv_data;
} AVCodecContext;

typedef struct AVPacket {
    unsigned char *data;
    int size;
    long long pts;
    long long dts;
    int flags;
} AVPacket;

unsigned avcodec_version(void);
const char *avcodec_configuration(void);
const char *avcodec_license(void);

const AVCodec *avcodec_find_decoder(enum AVCodecID id);
const AVCodec *avcodec_find_encoder(enum AVCodecID id);
AVCodecContext *avcodec_alloc_context3(const AVCodec *codec);
void avcodec_free_context(AVCodecContext **avctx);
int avcodec_open2(AVCodecContext *avctx, const AVCodec *codec, void **options);

AVPacket *av_packet_alloc(void);
void av_packet_free(AVPacket **pkt);

#ifdef __cplusplus
}
#endif
