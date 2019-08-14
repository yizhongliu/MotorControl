//
// Created by llm on 19-8-13.
//
#include "motor_common.h"

#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdlib.h>
#include <linux/input.h>
#include <android/log.h>

#define LOG_TAG    "step_motor"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

int controlMotorDev(int fd, int gpio_num, int gpio_state) {
    struct motor_a3901 userdata_motor;
    userdata_motor.gpio_num   = gpio_num;
    userdata_motor.gpio_state = gpio_state;
    if((ioctl(fd, VS_SET_MOTOR_ENABLE, &userdata_motor)) < 0)
    {
        LOGE("%s ioctl error gpio_num:%d, gpio_state:%d", __FUNCTION__, gpio_num, gpio_state);
     	return -1;
    }

    return 0;
}