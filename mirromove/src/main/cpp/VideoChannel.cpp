//
// Created by Administrator on 2019/8/9.
//
#include "VideoChannel.h"

extern "C" {
#include <libavutil/opt.h>
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>
}

/**
 * 丢包（AVPacket）
 * @param q
 */
void dropAVPacket(queue<AVPacket *> &q) {
    while (!q.empty()) {
        AVPacket *avPacket = q.front();
        // I 帧、B 帧、 P 帧
        //不能丢 I 帧,AV_PKT_FLAG_KEY: I帧（关键帧）
        if (avPacket->flags != AV_PKT_FLAG_KEY) {
            //丢弃非 I 帧
            BaseChannel::releaseAVPacket(&avPacket);
            q.pop();
        } else {
            break;
        }
    }
}

/**
 * 丢包（AVFrame）
 * @param q
 */
void dropAVFrame(queue<AVFrame *> &q) {
    if (!q.empty()) {
        AVFrame *avFrame = q.front();
        BaseChannel::releaseAVFrame(&avFrame);
        q.pop();
    }
}

VideoChannel::VideoChannel(int id, AVCodecContext *codecContext, int fps, AVRational time_base, int rotate,
                           JavaCallHelper *javaCallHelper)
        : BaseChannel(id, codecContext, time_base, javaCallHelper) {
    this->fps = fps;
    packets.setSyncHandle(dropAVPacket);
    frames.setSyncHandle(dropAVFrame);

    this->rotate = rotate;
}

VideoChannel::~VideoChannel() {
    LOGE("VideoChannel::~VideoChannel");
}

void *task_video_decode(void *args) {
    VideoChannel *videoChannel = static_cast<VideoChannel *>(args);
    //2 dataSource
    videoChannel->video_decode();
    return 0;//一定一定一定要返回0！！！
}

void *task_video_play(void *args) {
    VideoChannel *videoChannel = static_cast<VideoChannel *>(args);
    //2 dataSource
    videoChannel->video_play();
    return 0;//一定一定一定要返回0！！！
}

void VideoChannel::start() {
    isPlaying = 1;
    //设置队列状态为工作状态
    packets.setWork(1);
    frames.setWork(1);

    char args[256];
    snprintf(args, sizeof(args), "rotate=%d*PI/180", rotate);
    init_filter(args);
    //可以进行解码播放？
    //解码
    pthread_create(&pid_video_decode, 0, task_video_decode, this);
    //播放
    pthread_create(&pid_video_play, 0, task_video_play, this);
}


/**
 * 真正视频解码
 */
void VideoChannel::video_decode() {
    LOGE("enter : %s", __FUNCTION__);
    AVPacket *packet = 0;
    int ret;
    while (isPlaying) {

        if (frames.size() > 100) {
            continue;
        }

        ret = packets.pop(packet);
        if (!isPlaying) {
            //如果停止播放了，跳出循环 释放packet
            break;
        }
        if (!ret) {
            //取数据包失败
            continue;
        }
        //拿到了视频数据包（编码压缩了的），需要把数据包给解码器进行解码
        ret = avcodec_send_packet(codecContext, packet);
//        releaseAVPacket(&packet);//?
        if (ret) {
            //往解码器发送数据包失败，跳出循环
            ;
        }
        releaseAVPacket(&packet);//释放packet，后面不需要了
        AVFrame *frame = av_frame_alloc();


        ret = avcodec_receive_frame(codecContext, frame);
        if (ret == AVERROR(EAGAIN)) {
            releaseAVFrame(&frame);
            //重来
            continue;
        } else if (ret != 0) {
            releaseAVFrame(&frame);
            break;
        }

        AVFrame *filter_frame = av_frame_alloc();
        /* push the decoded frame into the filtergraph */
        if (av_buffersrc_add_frame_flags(buffersrc_ctx, frame, AV_BUFFERSRC_FLAG_KEEP_REF) < 0) {
            LOGE("Error while feeding the filtergraph");
            releaseAVFrame(&frame);
            releaseAVFrame(&filter_frame);
            break;
        }

        ret = av_buffersink_get_frame(buffersink_ctx, filter_frame);
        if (ret < 0) {
            LOGE("Error while get frame frame the filtergraph");
            releaseAVFrame(&frame);
            releaseAVFrame(&filter_frame);
            break;
        }


        //ret == 0 数据收发正常,成功获取到了解码后的视频原始数据包 AVFrame ，格式是 yuv
        //对frame进行处理（渲染播放）直接写？
        /**
         * 内存泄漏点2
         * 控制 frames 队列
         */
//        while (isPlaying && frames.size() > 100) {
//            av_usleep(10 * 1000);
//            continue;
//        }

//            frames.push(frame);

        frames.push(filter_frame);
        releaseAVFrame(&frame);
    }//end while
    releaseAVPacket(&packet);

    LOGE("leave : %s", __FUNCTION__);
}

void VideoChannel::video_play() {
    LOGE("enter : %s", __FUNCTION__);
    AVFrame *frame = 0;
    //要注意对原始数据进行格式转换：yuv > rgba
    // yuv: 400x800 > rgba: 400x800
    uint8_t *dst_data[4];
    int dst_linesize[4];
    SwsContext *sws_ctx = sws_getContext(codecContext->width, codecContext->height,
                                         codecContext->pix_fmt,
                                         codecContext->width, codecContext->height, AV_PIX_FMT_RGBA,
                                         SWS_BILINEAR, NULL, NULL, NULL);
    //给 dst_data dst_linesize 申请内存
    av_image_alloc(dst_data, dst_linesize,
                   codecContext->width, codecContext->height, AV_PIX_FMT_RGBA, 1);
    //根据fps（传入的流的平均帧率来控制每一帧的延时时间）
    //sleep : fps > 时间

    //单位是 : 秒
    double delay_time_per_frame = 1.0 / fps;
    int ret;

    double extra_delay;
    double real_delay;
    double video_time;

    while (isPlaying) {
        ret = frames.pop(frame);
        if (!isPlaying) {
            //如果停止播放了，跳出循环 释放packet
            break;
        }
        if (!ret) {
            //取数据包失败
            continue;
        }
        //取到了yuv原始数据，下面要进行格式转换
        sws_scale(sws_ctx, frame->data,
                  frame->linesize, 0, codecContext->height, dst_data, dst_linesize);
        //进行休眠
        //每一帧还有自己的额外延时时间

        //extra_delay = repeat_pict / (2*fps)
        extra_delay = frame->repeat_pict / (2 * fps);
        real_delay = delay_time_per_frame + extra_delay;
        //单位是：微秒
//        av_usleep(real_delay * 1000000);//直接以视频播放规则来播放

        //不能了，需要根据音频的播放时间来判断
        //获取视频的播放时间
        video_time = frame->best_effort_timestamp * av_q2d(time_base);

 //       av_usleep((real_delay * 3) * 1000000);

        //音视频同步：永远都是你追我赶的状态
        if (!audioChannel) {
            //没有音频（类似GIF）
            av_usleep(real_delay * 1000000);
            if (javaCallHelper) {
                javaCallHelper->onProgress(THREAD_CHILD, video_time);
            }
        } else {
            double audioTime = audioChannel->audio_time;
            //获取音视频播放的时间差
            double time_diff = video_time - audioTime;
            if (time_diff > 0) {
      //          LOGE("视频比音频快：%lf", time_diff);
                //视频比音频快：等音频（sleep）
                //自然播放状态下，time_diff的值不会很大
                //但是，seek后time_diff的值可能会很大，导致视频休眠太久

                //av_usleep((real_delay + time_diff) * 1000000);//TODO seek后测试

                if (time_diff > 1) {
                    //等音频慢慢赶上来
                    av_usleep((real_delay * 2) * 1000000);
                } else {
                    av_usleep((real_delay + time_diff) * 1000000);
                }
            } else if (time_diff < 0) {
       //         LOGE("音频比视频快: %lf", fabs(time_diff));
                //音频比视频快：追音频（尝试丢视频包）
                //视频包：packets 和 frames
                if (fabs(time_diff) >= 0.05) {
                    //时间差如果大于0.05，有明显的延迟感
                    //丢包：要操作队列中数据！一定要小心！
//                    packets.sync();
                //    frames.sync();
                    releaseAVFrame(&frame);
                    continue;
                }
            } else {
                LOGE("音视频完美同步！");
            }
        }



        //dst_data: AV_PIX_FMT_RGBA格式的数据
        //渲染，回调出去> native-lib里
        // 渲染一副图像 需要什么信息？
        // 宽高！> 图像的尺寸！
        // 图像的内容！(数据)>图像怎么画
        //需要：1，data;2，linesize；3，width; 4， height
        renderCallback(dst_data[0], dst_linesize[0], codecContext->width, codecContext->height);
        releaseAVFrame(&frame);
    }
    releaseAVFrame(&frame);
    isPlaying = 0;
    av_freep(&dst_data[0]);
    sws_freeContext(sws_ctx);
    //MediaCodec

    LOGE("leave : %s", __FUNCTION__);
}

void VideoChannel::setRenderCallback(RenderCallback callback) {
    this->renderCallback = callback;
}

void VideoChannel::setAudioChannel(AudioChannel *audioChannel) {
    this->audioChannel = audioChannel;
}

void VideoChannel::stop() {
    isPlaying = 0;
    javaCallHelper = 0;
    packets.setWork(0);
    frames.setWork(0);
    pthread_join(pid_video_decode, 0);
    pthread_join(pid_video_play, 0);

    avfilter_graph_free(&filter_graph);
    filter_graph = 0;
}

int VideoChannel::init_filter(const char *filters_descr) {
    char args[512];
    int ret = 0;
    const AVFilter *buffersrc = avfilter_get_by_name("buffer");
    const AVFilter *buffersink = avfilter_get_by_name("buffersink");
    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs = avfilter_inout_alloc();
    AVRational time_base = BaseChannel::time_base;
    enum AVPixelFormat pix_fmts[] = {codecContext->pix_fmt, AV_PIX_FMT_NONE};

    if (filters_descr != 0) {
        LOGE("filters_descr : %s", filters_descr);
    }

    filter_graph = avfilter_graph_alloc();
    if (!outputs || ! inputs || !filter_graph) {
        ret = AVERROR(ENOMEM);
    }

    /* buffer video source: the decoded frames from the decoder will be inserted here. */
    snprintf(args, sizeof(args),
             "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             codecContext->width, codecContext->height, codecContext->pix_fmt,
             time_base.num, time_base.den,
             codecContext->sample_aspect_ratio.num, codecContext->sample_aspect_ratio.den);

    ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in", args, NULL, filter_graph);
    if (ret < 0) {
        LOGE("Cannot create buffer src!!!");
        goto end;
    }

    /* buffer video sink: to terminate the filter chain. */
    ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
                                       NULL, NULL, filter_graph);
    if (ret < 0) {
        LOGE("Cannot create buffer sink");
        goto end;
    }

    ret = av_opt_set_int_list(buffersink_ctx, "pix_fmts", pix_fmts,
                              AV_PIX_FMT_NONE, AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        LOGE( "Cannot set output pixel format");
        goto end;
    }

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

    if ((ret = avfilter_graph_parse_ptr(filter_graph, filters_descr,
                                        &inputs, &outputs, NULL)) < 0)
        goto end;

    if ((ret = avfilter_graph_config(filter_graph, NULL)) < 0)
        goto end;



end:
    avfilter_inout_free(&inputs);
    avfilter_inout_free(&outputs);

    return ret;
}