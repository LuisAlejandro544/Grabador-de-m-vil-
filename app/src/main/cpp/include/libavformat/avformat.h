#pragma once

#include "libavcodec/avcodec.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LIBAVFORMAT_VERSION_MAJOR 61
#define LIBAVFORMAT_VERSION_MINOR 1
#define LIBAVFORMAT_VERSION_MICRO 100

typedef struct AVStream {
    int index;
    int id;
    AVCodecContext *codec;
    long long duration;
    long long nb_frames;
} AVStream;

typedef struct AVFormatContext {
    const char *url;
    unsigned int nb_streams;
    AVStream **streams;
    long long duration;
    int bit_rate;
    void *priv_data;
} AVFormatContext;

unsigned avformat_version(void);
const char *avformat_configuration(void);
const char *avformat_license(void);

int avformat_open_input(AVFormatContext **ps, const char *url, void *fmt, void **options);
void avformat_close_input(AVFormatContext **s);
int avformat_find_stream_info(AVFormatContext *ic, void **options);

int avformat_alloc_output_context2(AVFormatContext **ctx, void *oformat, const char *format_name, const char *filename);
void avformat_free_context(AVFormatContext *s);

AVStream *avformat_new_stream(AVFormatContext *s, const AVCodec *c);
int avformat_write_header(AVFormatContext *s, void **options);
int av_write_trailer(AVFormatContext *s);
int av_interleaved_write_frame(AVFormatContext *s, AVPacket *pkt);

#ifdef __cplusplus
}
#endif
