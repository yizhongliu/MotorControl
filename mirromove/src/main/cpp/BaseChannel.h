//
// Created by Administrator on 2019/8/9.
//

#ifndef NE_PLAYER_1_BASECHANNEL_H
#define NE_PLAYER_1_BASECHANNEL_H

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/frame.h>
#include <libavutil/time.h>
};

#include "safe_queue.h"
#include "JavaCallHelper.h"

/**
 * VideoChannel和AudioChannel的父类
 */
class BaseChannel {
public:
    BaseChannel(int id, AVCodecContext *codecContext, AVRational time_base,
                JavaCallHelper *javaCallHelper) : id(id), codecContext(
            codecContext), time_base(time_base), javaCallHelper(javaCallHelper) {
        packets.setReleaseCallback(releaseAVPacket);
        frames.setReleaseCallback(releaseAVFrame);
    }

    virtual ~BaseChannel() {
        LOGE("enter: %s  framesize: %d", __FUNCTION__, frames.size());
        packets.clear();
        frames.clear();
        if (codecContext) {
            avcodec_close(codecContext);
            avcodec_free_context(&codecContext);
            codecContext = 0;
        }
    }

    /**
     * 释放 AVPacket
     * @param packet
     */
    static void releaseAVPacket(AVPacket **packet) {
        if (packet) {
            av_packet_free(packet);
            *packet = 0;
        }
    }

    /**
     * 释放 AVFrame
     * @param frame
     */
    static void releaseAVFrame(AVFrame **frame) {
   //     LOGE("releaveAvframe");
        if (frame) {

            av_frame_free(frame);
            *frame = 0;
        }
    }


    //纯虚函数（抽象方法）
    virtual void start() = 0;

    virtual void stop() = 0;


    SafeQueue<AVPacket *> packets;
    SafeQueue<AVFrame *> frames;
    int id;
    bool isPlaying = 0;
    //解码器上下文
    AVCodecContext *codecContext;
    AVRational time_base;
    double audio_time;
    JavaCallHelper *javaCallHelper = 0;
};


#endif //NE_PLAYER_1_BASECHANNEL_H
