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

#define MOTOR_DRV_UP_DOWN    "/dev/motor_gpio_up_down"

#define  LOG_TAG    "vertical_motor"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static int vMotorFd = -1;

static struct timeval tv;
static struct timeval ts;

bool bVerticalMotorEnable = false;
int gVDelay = 2000;
int gVDirection = 0;

int controlVerticalMotor(int steps, int dir, int delay) {

    LOGD("controlHorizontalMotor step: %d, direction: %d, delay %d", steps, dir, delay);

    vMotorFd = open(MOTOR_DRV_UP_DOWN, O_RDWR);
    if(vMotorFd == -1)
    {
        LOGE("%s open left_right_motor_device error..........!\n", __FUNCTION__);
        return -1;
    }

    bVerticalMotorEnable = true;

    controlMotorDev(vMotorFd, MOTO_ENABLE_UP_DOWN, MOTOR_DISABLE);  //使能马达

    controlMotorDev(vMotorFd, MOTO_DIR_UP_DOWN, dir);  //设置马达转动方向

    int gpioLevel = 0;
    controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);
    while (steps--) {
        gpioLevel = !gpioLevel;
    	controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);
    	tv.tv_sec = 0;
    	tv.tv_usec = delay / 2;
    	select(0, NULL, NULL, NULL, &tv);
    	gpioLevel = !gpioLevel;
    	controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);
    	tv.tv_sec = 0;
    	tv.tv_usec = delay;
    	select(0, NULL, NULL, NULL, &tv);

        if (bVerticalMotorEnable == false) {
            break;
        }
    }

    controlMotorDev(vMotorFd, MOTO_ENABLE_UP_DOWN, MOTOR_ENABLE); //锁马达

    close(vMotorFd);
    vMotorFd = -1;

    bVerticalMotorEnable = false;

    return 0;
}


int setVerticalMotorSpeed(int delay) {
    gVDelay = delay;
}

int setVerticalMotorDirection(int direction) {
    gVDirection = direction;
}

int getVerticalMotorSpeed() {
    return gVDelay;
}

int getVerticalMotorDirection() {
    return gVDirection;
}

int startVMotorRunning() {
     if (vMotorFd == -1) {
        vMotorFd = open(MOTOR_DRV_UP_DOWN, O_RDWR);
        if(vMotorFd == -1)
        {
           LOGE("%s open left_right_motor_device error..........!\n", __FUNCTION__);
           return -1;
        }
     }

     bVerticalMotorEnable = true;

     int delay = getVerticalMotorSpeed();

     controlMotorDev(vMotorFd, MOTO_ENABLE_UP_DOWN, MOTOR_DISABLE);  //使能马达

     int dir = getVerticalMotorDirection();
     controlMotorDev(vMotorFd, MOTO_DIR_UP_DOWN, dir);  //设置马达转动方向

     int gpioLevel = 0;
     controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);

     while (true) {
         gpioLevel = !gpioLevel;
         controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);
         tv.tv_sec = 0;
         tv.tv_usec = delay / 2;
         select(0, NULL, NULL, NULL, &tv);
         gpioLevel = !gpioLevel;
         controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);
         tv.tv_sec = 0;
         tv.tv_usec = delay;
         select(0, NULL, NULL, NULL, &tv);

         if (dir != getVerticalMotorDirection()) {
             dir = getVerticalMotorDirection();
             controlMotorDev(vMotorFd, MOTO_DIR_UP_DOWN, dir);
         }

         delay = getVerticalMotorSpeed();

         if (bVerticalMotorEnable == false) {
             break;
         }
     }

     controlMotorDev(vMotorFd, MOTO_ENABLE_UP_DOWN, MOTOR_ENABLE); //锁马达

     bVerticalMotorEnable = false;

     close(vMotorFd);
     vMotorFd = -1;

     return 0;
}

int stopVMotorRunning() {
    bVerticalMotorEnable = false;
}

bool getVMotorEnable() {
    return bVerticalMotorEnable;
}