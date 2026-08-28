#include "libavfilter/avfilter.h"
#include <stdlib.h>
#include <string.h>

unsigned avfilter_version(void) {
    return (LIBAVFILTER_VERSION_MAJOR << 16) | (LIBAVFILTER_VERSION_MINOR << 8) | LIBAVFILTER_VERSION_MICRO;
}

const char *avfilter_configuration(void) {
    return "--enable-version3 --enable-neon --enable-asm --enable-mediacodec --enable-jni";
}

const char *avfilter_license(void) {
    return "LGPL version 3 or later";
}

AVFilterGraph *avfilter_graph_alloc(void) {
    AVFilterGraph *graph = (AVFilterGraph *)av_malloc(sizeof(AVFilterGraph));
    if (graph) {
        memset(graph, 0, sizeof(AVFilterGraph));
    }
    return graph;
}

void avfilter_graph_free(AVFilterGraph **graph) {
    if (graph && *graph) {
        av_free(*graph);
        *graph = NULL;
    }
}

int avfilter_graph_config(AVFilterGraph *graphctx, void *log_ctx) {
    (void)graphctx;
    (void)log_ctx;
    return 0;
}
