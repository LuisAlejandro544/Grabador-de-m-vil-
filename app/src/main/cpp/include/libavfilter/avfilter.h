#pragma once

#include "libavutil/avutil.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LIBAVFILTER_VERSION_MAJOR 10
#define LIBAVFILTER_VERSION_MINOR 1
#define LIBAVFILTER_VERSION_MICRO 100

typedef struct AVFilterGraph {
    unsigned nb_filters;
    void *filters;
} AVFilterGraph;

unsigned avfilter_version(void);
const char *avfilter_configuration(void);
const char *avfilter_license(void);

AVFilterGraph *avfilter_graph_alloc(void);
void avfilter_graph_free(AVFilterGraph **graph);
int avfilter_graph_config(AVFilterGraph *graphctx, void *log_ctx);

#ifdef __cplusplus
}
#endif
