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
#include <stdbool.h>
#include <android/log.h>

#define MOTOR_DRV_LEFT_RIGHT     "/dev/motor_gpio_left_right"

#define  LOG_TAG    "horizontal_motor"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static int hMotorFd = -1;

static struct timeval tv;
static struct timeval ts;

bool bHorizontalMotorEnable = false;

int gHDelay = 2000;
int gHDirection = 0;

int controlHorizontalMotor(int steps, int dir, int delay) {

    LOGD("controlHorizontalMotor step: %d, direction: %d, delay %d", steps, dir, delay);

    hMotorFd = open(MOTOR_DRV_LEFT_RIGHT, O_RDWR);
    if(hMotorFd == -1)
    {
        LOGE("%s open left_right_motor_device error..........!\n", __FUNCTION__);
        return -1;
    }

    bHorizontalMotorEnable = true;

    controlMotorDev(hMotorFd, MOTO_ENABLE_LEFT_RIGHT, MOTOR_DISABLE);  //使能马达

    controlMotorDev(hMotorFd, MOTO_DIR_LEFT_RIGHT, dir);  //设置马达转动方向

    int gpioLevel = 0;
    controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);
    while (steps--) {
        gpioLevel = !gpioLevel;
    	controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);
    	tv.tv_sec = 0;
    	tv.tv_usec = delay / 2;
    	select(0, NULL, NULL, NULL, &tv);
    	gpioLevel = !gpioLevel;
    	controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);
    	tv.tv_sec = 0;
    	tv.tv_usec = delay;
    	select(0, NULL, NULL, NULL, &tv);

        if (bHorizontalMotorEnable == false) {
            break;
        }
    }

    controlMotorDev(hMotorFd, MOTO_ENABLE_LEFT_RIGHT, MOTOR_ENABLE); //锁马达

    close(hMotorFd);
    hMotorFd = -1;

    bHorizontalMotorEnable = false;

    return 0;
}

int setHorizontalMotorSpeed(int delay) {
    gHDelay = delay;
}

int setHorizontalMotorDirection(int direction) {
    gHDirection = direction;
}

int getHorizontalMotorSpeed() {
    return gHDelay;
}

int getHorizontalMotorDirection() {
    return gHDirection;
}

int startHMotorRunning() {

    hMotorFd = open(MOTOR_DRV_LEFT_RIGHT, O_RDWR);
    if(hMotorFd == -1)
    {
       LOGE("%s open left_right_motor_device error..........!\n", __FUNCTION__);
       return -1;
    }


    bHorizontalMotorEnable = true;

    int delay = getHorizontalMotorSpeed();

    controlMotorDev(hMotorFd, MOTO_ENABLE_LEFT_RIGHT, MOTOR_DISABLE);  //使能马达

    int dir = getHorizontalMotorDirection();
    controlMotorDev(hMotorFd, MOTO_DIR_LEFT_RIGHT, dir);  //设置马达转动方向

    int gpioLevel = 0;
    controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);

     while (true) {
         gpioLevel = !gpioLevel;
         controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);
         tv.tv_sec = 0;
         tv.tv_usec = delay / 2;
         select(0, NULL, NULL, NULL, &tv);
         gpioLevel = !gpioLevel;
         controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);
         tv.tv_sec = 0;
         tv.tv_usec = delay;
         select(0, NULL, NULL, NULL, &tv);

         if (dir != getHorizontalMotorDirection()) {
             dir = getHorizontalMotorDirection();
             controlMotorDev(hMotorFd, MOTO_DIR_LEFT_RIGHT, dir);
         }

         delay = getHorizontalMotorSpeed();

         if (bHorizontalMotorEnable == false) {
             break;
         }
     }

     controlMotorDev(hMotorFd, MOTO_ENABLE_LEFT_RIGHT, MOTOR_ENABLE); //锁马达

     bHorizontalMotorEnable = false;

     close(hMotorFd);
     hMotorFd = -1;

     return 0;
}

int stopHMotorRunning() {
    bHorizontalMotorEnable = false;
}

bool getHMotorEnable() {
    return bHorizontalMotorEnable;
}

