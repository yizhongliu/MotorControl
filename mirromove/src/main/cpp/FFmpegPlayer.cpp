//
// Created by llm on 19-8-29.
//
extern "C" {
#include <libavutil/opt.h>
};

#include "FFmpegPlayer.h"

const char *filter_descr = "rotate=0";

FFmpegPlayer::FFmpegPlayer(JavaCallHelper *javaCallHelper, char *dataSource, int rotate) {
    //参数robust放 jni 层判断
    this->javaCallHelper = javaCallHelper;

    this->dataSource = new char[strlen(dataSource) + 1];
    strcpy(this->dataSource, dataSource);

    this->rotateAngle = rotate;
}

FFmpegPlayer::~FFmpegPlayer() {
    DELETE(javaCallHelper);
    DELETE(dataSource);

    DELETE(clockTime);
}

void *task_prepare(void *args) {
    FFmpegPlayer *ffmpegPlayer = static_cast<FFmpegPlayer *>(args);
    ffmpegPlayer->_prepare();

    return 0;//一定一定一定要返回0！！！
}

void *task_stop(void *args) {
    FFmpegPlayer *ffmpegPlayer = static_cast<FFmpegPlayer *>(args);
    ffmpegPlayer->_stop();

    return 0;//一定一定一定要返回0！！！
}

int FFmpegPlayer::init_filters(const char *filters_descr, int video_index)
{
    char args[512];
    int ret = 0;
    const AVFilter *buffersrc  = avfilter_get_by_name("buffer");
    const AVFilter *buffersink = avfilter_get_by_name("buffersink");
    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs  = avfilter_inout_alloc();
    AVRational time_base = formatContext->streams[video_index]->time_base;
    enum AVPixelFormat pix_fmts[] = { AV_PIX_FMT_RGBA, AV_PIX_FMT_NONE };

    char   fstr[256];
    int ow;
    int oh;

    filter_graph = avfilter_graph_alloc();
    if (!outputs || !inputs || !filter_graph) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    /* buffer video source: the decoded frames from the decoder will be inserted here. */
    snprintf(args, sizeof(args),
             "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             codecContext->width, codecContext->height, codecContext->pix_fmt,
             time_base.num, time_base.den,
             codecContext->sample_aspect_ratio.num, codecContext->sample_aspect_ratio.den);

    ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in",
                                       args, NULL, filter_graph);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create buffer source\n");
        goto end;
    }

    /* buffer video sink: to terminate the filter chain. */
    ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
                                       NULL, NULL, filter_graph);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create buffer sink\n");
        goto end;
    }

//    ret = av_opt_set_int_list(buffersink_ctx, "pix_fmts", pix_fmts,
//                              AV_PIX_FMT_NONE, AV_OPT_SEARCH_CHILDREN);
//    if (ret < 0) {
//        av_log(NULL, AV_LOG_ERROR, "Cannot set output pixel format\n");
//        goto end;
//    }

    /*
 * Set the endpoints for the filter graph. The filter_graph will
 * be linked to the graph described by filters_descr.
 */

    /*
     * The buffer source output must be connected to the input pad of
     * the first filter described by filters_descr; since the first
     * filter input label is not specified, it is set to "in" by
     * default.
     */
    outputs->name       = av_strdup("in");
    outputs->filter_ctx = buffersrc_ctx;
    outputs->pad_idx    = 0;
    outputs->next       = NULL;

    /*
     * The buffer sink input must be connected to the output pad of
     * the last filter described by filters_descr; since the last
     * filter output label is not specified, it is set to "out" by
     * default.
     */
    inputs->name       = av_strdup("out");
    inputs->filter_ctx = buffersink_ctx;
    inputs->pad_idx    = 0;
    inputs->next       = NULL;


//    oVideoWidth = abs((int)(codecContext->width  * cos(angle * M_PI / 180)))
//             + abs((int)(codecContext->height * sin(angle * M_PI / 180)));
//    oVideoHeight = abs((int)(codecContext->width  * sin(angle * M_PI / 180)))
//             + abs((int)(codecContext->height * cos(angle * M_PI / 180)));
    oVideoWidth = codecContext->width;
    oVideoHeight = codecContext->height;
    LOGE("ow: %d, oh = %d", ow, oh);
//    sprintf(fstr, "rotate=%d*PI/180:%d:%d", angle, oVideoWidth, oVideoHeight);
    sprintf(fstr, "rotate=%d*PI/180", rotateAngle);


    if ((ret = avfilter_graph_parse_ptr(filter_graph, fstr,
                                        &inputs, &outputs, NULL)) < 0)
        goto end;

    if ((ret = avfilter_graph_config(filter_graph, NULL)) < 0)
        goto end;

    end:
    avfilter_inout_free(&inputs);
    avfilter_inout_free(&outputs);

    return ret;
}


void FFmpegPlayer::prepare() {
    LOGE("FFmpegPlayer::prepare()");
    pthread_create(&pid_prepare, 0, task_prepare, this);
}

void FFmpegPlayer::_prepare() {
    LOGE("FFmpegPlayer::_prepare()");
    LOGE("Open dataSource %s:", dataSource);

    formatContext = avformat_alloc_context();

    AVDictionary *dictionary = 0;
    av_dict_set(&dictionary, "timeout", "10000000", 0); //超时时间 10 秒

    int ret = avformat_open_input(&formatContext, dataSource, 0, &dictionary);

    av_dict_free(&dictionary);

    if (ret != 0) {
        LOGE("fail to open media :%s, %s",dataSource, av_err2str(ret));

        if (javaCallHelper) {
            javaCallHelper->onError(THREAD_CHILD, FFMPEG_CAN_NOT_OPEN_URL);
        }

        return;
    }

    ret = avformat_find_stream_info(formatContext, 0);
    if (ret < 0) {
        LOGE("avformat_find_stream_info error:%s", av_err2str(ret));

        if (javaCallHelper) {
            javaCallHelper->onError(THREAD_CHILD, FFMPEG_CAN_NOT_FIND_STREAMS);
        }
        return;
    }

    duration = formatContext->duration / AV_TIME_BASE;

    for (int i = 0; i < formatContext->nb_streams; i++) {
        AVStream *stream = formatContext->streams[i];

        AVCodecParameters *codecParameters = stream->codecpar;

        AVCodec *codec = avcodec_find_decoder(codecParameters->codec_id);
        if (!codec) {
            LOGE("avcodec_find_decoder error");
            if (javaCallHelper) {
                javaCallHelper->onError(THREAD_CHILD, FFMPEG_FIND_DECODER_FAIL);
            }

            return;
        }

        codecContext = avcodec_alloc_context3(codec);
        if (!codecContext) {
            LOGE("avcodec_alloc_context3");
            if (javaCallHelper) {
                javaCallHelper->onError(THREAD_CHILD, FFMPEG_ALLOC_CODEC_CONTEXT_FAIL);
            }
        }

        ret = avcodec_parameters_to_context(codecContext, codecParameters);
        if (ret < 0) {
            LOGE("avcodec_parameters_to_context error:%s", av_err2str(ret));
            if (javaCallHelper) {
                javaCallHelper->onError(THREAD_CHILD, FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL);
            }
            return;
        }

        ret = avcodec_open2(codecContext, codec, 0);
        if (ret != 0) {
            LOGE("avcodec_open2 error:%s", av_err2str(ret));
            if (javaCallHelper) {
                javaCallHelper->onError(THREAD_CHILD, FFMPEG_OPEN_DECODER_FAIL);
            }
        }

        AVRational time_base = stream->time_base;

        if (codecParameters->codec_type == AVMEDIA_TYPE_AUDIO) {
      //      audioChannel = new AudioChannel(i, codecContext, time_base, javaCallHelper);
        } else if (codecParameters->codec_type == AVMEDIA_TYPE_VIDEO) {
            AVRational frame_rate = stream->avg_frame_rate;
            int fps = av_q2d(frame_rate);

            oVideoWidth = codecContext->width;
            oVideoHeight = codecContext->height;

          //  init_filters(filter_descr, i);

            videoChannel = new VideoChannel(i, codecContext, fps, time_base, javaCallHelper, buffersink_ctx, buffersrc_ctx, oVideoWidth, oVideoHeight);
            videoChannel->setRenderCallback(renderCallback);
        }
    }

    if (!audioChannel && !videoChannel) {
        LOGE("没有音视频");
        if (javaCallHelper) {
            javaCallHelper->onError(THREAD_CHILD, FFMPEG_NOMEDIA);
        }
        return;
    }
    //准备好了，反射通知java
    if (javaCallHelper) {
        javaCallHelper->onPrepared(THREAD_CHILD);
    }
}

void *task_start(void *args) {
    FFmpegPlayer *ffmpegPlayer = static_cast<FFmpegPlayer *>(args);
    ffmpegPlayer->_start();

    return 0;//一定一定一定要返回0！！！
}

void FFmpegPlayer::start() {
    isPlaying = 1;
    if (videoChannel) {
        if (audioChannel) {
            videoChannel->setAudioChannel(audioChannel);
        }
        videoChannel->start();
    }

    if (audioChannel) {
        audioChannel->start();
    }

    pthread_create(&pid_start, 0, task_start, this);
}

void FFmpegPlayer::_start() {
    LOGE("FFmpegPlayer::_start()");
    int ret;
    while (isPlaying) {
        if (videoChannel && videoChannel->packets.size() > 100) {
            av_usleep(10 * 1000);
            continue;
        }

        AVPacket *packet = av_packet_alloc();

        ret = av_read_frame(formatContext, packet);
        if (ret == 0) {
            if (videoChannel && packet->stream_index == videoChannel->id) {
                videoChannel->packets.push(packet);
            } else if (audioChannel && packet->stream_index == audioChannel->id) {
                audioChannel->packets.push(packet);
            }
        } else if (ret == AVERROR_EOF) {
      //      LOGE("av_read_frame AVERROR_EOF");
            //FIXME:解码和显示的线程可能还没处理完，不能直接推出

            if (videoChannel && audioChannel) {
                if (videoChannel->packets.empty() && videoChannel->frames.empty()
                    && audioChannel->packets.empty() && audioChannel->frames.empty()) {
                    LOGE("play end both");
                    av_packet_free(&packet);
                    if (javaCallHelper) {
                        javaCallHelper->onCompletion(THREAD_CHILD);
                    }
                    break;
                }
            } else if (videoChannel) {
                if (videoChannel->packets.empty() && videoChannel->frames.empty()) {
                    LOGE("play end video");
                    av_packet_free(&packet);
                    if (javaCallHelper) {
                        javaCallHelper->onCompletion(THREAD_CHILD);
                    }
                    break;
                }
            } else if (audioChannel) {
                if (audioChannel->packets.empty() && audioChannel->frames.empty()) {
                    av_packet_free(&packet);
                    LOGE("play end audio");
                    if (javaCallHelper) {
                        javaCallHelper->onCompletion(THREAD_CHILD);
                    }
                    break;
                }
            }

            av_packet_free(&packet);
        } else {
            LOGE("av_read_frame error:%s", av_err2str(ret));
            av_packet_free(&packet);
            if (javaCallHelper) {
                javaCallHelper->onError(THREAD_CHILD, FFMPEG_READ_PACKETS_FAIL);
            }
            break;
        }
    }

    isPlaying = 0;

    if (videoChannel) {
        videoChannel->stop();
    }

    if (audioChannel) {
        audioChannel->stop();
    }

    LOGE("Leave _start");
}

void FFmpegPlayer::setRenderCallback(RenderCallback renderCallback) {
    this->renderCallback = renderCallback;
}

/**
 * 停止播放
 */
void FFmpegPlayer::stop() {
//    isPlaying = 0;
    javaCallHelper = 0;//prepare阻塞中停止了，还是会回调给java "准备好了"

    //既然在主线程会引发ANR，那么我们到子线程中去释放
//    pthread_create(&pid_stop, 0, task_stop, this);//创建stop子线程
    _stop();
}

void FFmpegPlayer::_stop() {

    isPlaying = 0;
    LOGE("FFmpegPlayer::_stop() ");
    pthread_join(pid_prepare, 0);//解决了：要保证_prepare方法（子线程中）执行完再释放（在主线程）的问题
    pthread_join(pid_start, 0);

    LOGE("FFmpegPlayer::_stop2() ");


    if (codecContext) {
        avcodec_free_context(&codecContext);
        codecContext = 0;
    }

    if (formatContext) {
        avformat_close_input(&formatContext);
        avformat_free_context(formatContext);
        formatContext = 0;
    }
    LOGE("FFmpegPlayer::_stop3()");
    DELETE(videoChannel);
    DELETE(audioChannel);

  //  DELETE(ffmpeg);
}

int FFmpegPlayer::getDuration() const {
    return duration;
}

int FFmpegPlayer::getVideoWidth() const {
    return oVideoWidth;
}

int FFmpegPlayer::getVideoHeight() const {
    return oVideoHeight;
}

void FFmpegPlayer::useClockTime(PlayClockTime *clockTime) {
    this->clockTime = clockTime;
    if (videoChannel) {
        videoChannel->setClockTime(clockTime);
    }

    if (audioChannel) {
        audioChannel->setClockTime(clockTime);
    }

}