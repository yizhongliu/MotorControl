//
// Created by llm on 19-8-29.
//

#ifndef NATIVEPLAYER_FFMPEGPLAYER_H
#define NATIVEPLAYER_FFMPEGPLAYER_H


#include "JavaCallHelper.h"
#include "AudioChannel.h"
#include "VideoChannel.h"
#include "macro.h"
#include <pthread.h>

#include "net/PlayClockTime.h"

extern "C" {
#include <libavformat/avformat.h>
#include <libavfilter/avfilter.h>
};

class FFmpegPlayer {
public:
    FFmpegPlayer(JavaCallHelper *javaCallHelper, char *dataSource, int rotate);
    ~FFmpegPlayer();

    void prepare();

    void _prepare();

    void start();

    void _start();

    void stop();

    void _stop();

    void setRenderCallback(RenderCallback renderCallback);

    int getDuration() const;

    int getVideoWidth() const;

    int getVideoHeight() const;

    void useClockTime(PlayClockTime *clockTime);

private:
    JavaCallHelper *javaCallHelper = 0;  // 回调java

    AudioChannel *audioChannel = 0;
    VideoChannel *videoChannel = 0;

    char *dataSource; //媒体文件路径

    pthread_t pid_prepare; //prepare线程id

    pthread_t pid_start; //start线程id

    pthread_t pid_stop;

    AVFormatContext *formatContext = 0;

    int isPlaying = 0;

    RenderCallback renderCallback;

    int duration;

    int videoWidth = 0;

    int videoHeight = 0;

    int oVideoWidth = 0;

    int oVideoHeight = 0;

    int rotateAngle = 0;

    PlayClockTime *clockTime = 0;

    AVCodecContext *codecContext = 0; //FIXME:未释放

    AVFilterContext *buffersink_ctx = 0;
    AVFilterContext *buffersrc_ctx = 0;
    AVFilterGraph *filter_graph = 0;

    int init_filters(const char *filters_descr, int video_index);
};

#endif //NATIVEPLAYER_FFMPEGPLAYER_H
