//
// Created by llm on 19-8-13.
//
#include <jni.h>
#include <android/log.h>

extern "C"{
#include "motor_common.h"
}

#define LOG_TAG    "step_motor"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define H_MOTOR_ID 1
#define V_MOTOR_ID 2

extern "C" JNIEXPORT jint
Java_com_iview_stepmotor_MotorControl_nativeControlMotor(
        JNIEnv *env,
        jobject /* this */,jint motorId, jint steps, jint dir, jint delay) {
    LOGD("nativeControlMotor");
    if (motorId == H_MOTOR_ID) {
        return controlHorizontalMotor(steps, dir, delay);
    } else if (motorId == V_MOTOR_ID) {
        return controlVerticalMotor(steps, dir, delay);
    }
}

extern "C" JNIEXPORT jint
Java_com_iview_stepmotor_MotorControl_nativeSetMotorSpeed(
        JNIEnv *env,
        jobject /* this */,jint motorId, jint delay) {
    LOGD("nativeSetMotorSpeed");
    if (motorId == H_MOTOR_ID) {
        return setHorizontalMotorSpeed(delay);
    } else if (motorId == V_MOTOR_ID) {
        return setVerticalMotorSpeed(delay);
    }
}

extern "C" JNIEXPORT jint
Java_com_iview_stepmotor_MotorControl_nativeSetMotorDirection(
        JNIEnv *env,
        jobject /* this */,jint motorId, jint direction) {
    LOGD("nativeSetMotorDirection");
    if (motorId == H_MOTOR_ID) {
        return setHorizontalMotorDirection(direction);
    } else if (motorId == V_MOTOR_ID) {
        return setVerticalMotorDirection(direction);
    }
}

extern "C" JNIEXPORT jint
Java_com_iview_stepmotor_MotorControl_nativeStartMotorRunning(
        JNIEnv *env,
        jobject /* this */,jint motorId) {
    LOGD("nativeStartMotorRunning");
    if (motorId == H_MOTOR_ID) {
        return startHMotorRunning();
    } else if (motorId == V_MOTOR_ID) {
        return startVMotorRunning();
    }
}

extern "C" JNIEXPORT jint
Java_com_iview_stepmotor_MotorControl_nativeStopMotorRunning(
        JNIEnv *env,
        jobject /* this */,jint motorId) {
    LOGD("nativeStopMotorRunning");
    if (motorId == H_MOTOR_ID) {
        return stopHMotorRunning();
    } else if (motorId == V_MOTOR_ID) {
        return stopVMotorRunning();
    }
}

extern "C" JNIEXPORT jboolean
Java_com_iview_stepmotor_MotorControl_nativeGetMotorEnable(
        JNIEnv *env,
        jobject /* this */,jint motorId) {
    LOGD("nativeGetMotorEnable");
    if (motorId == H_MOTOR_ID) {
        return getHMotorEnable();
    } else if (motorId == V_MOTOR_ID) {
        return getVMotorEnable();
    }
}




