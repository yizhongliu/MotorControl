//
// Created by llm on 19-9-9.
//
//
// Created by llm on 19-8-13.
//
#include <jni.h>
#include <android/log.h>

extern "C"{
#include "motor_common.h"
#include "dlp343x.h"
}

#define LOG_TAG    "projector_control"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define PROJECTOR_OFF 0
#define PROJECTOR_ON 1

extern "C" JNIEXPORT void
Java_com_iview_stepmotor_MotorControl_nativeSwitchProjector(
        JNIEnv *env,
        jobject /* this */,jint enable) {

    LOGD("nativeSwitchProjector");
    unsigned char sendData[0];

    if (enable == PROJECTOR_OFF) {
        sendData[0] = 0x0;
        dlp_write_data(0x52, sendData, 1);
    } else if (enable == PROJECTOR_ON) {
        sendData[0] = 0x07;
        dlp_write_data(0x52, sendData, 1);
    }

}

