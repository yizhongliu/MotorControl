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

int motor_up_pi_state = 0;
int motor_down_pi_state = 0;

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

        if(getPiState(vMotorFd, MOTO_SENSOR_UP_DOWN_2, 0) == 1) {
            LOGE("reach up/down pi");
            if (dir == MOTOR_DIRECTION_UP) {
                if (motor_down_pi_state == 0) {
                    LOGE("reach up pi");
                    motor_up_pi_state = 1;
                    break;
                }
            } else if (dir == MOTOR_DIRECTION_DOWN) {
                if (motor_up_pi_state == 0) {
                    LOGE("reach down pi");
                    motor_down_pi_state = 1;
                    break;
                }
            }
        } else {
            motor_up_pi_state = 0;
            motor_down_pi_state = 0;
        }

        gpioLevel = !gpioLevel;
    	controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);

    	motorDelay(delay);

    	gpioLevel = !gpioLevel;
    	controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);

    	motorDelay(delay);

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
         if(getPiState(vMotorFd, MOTO_SENSOR_UP_DOWN_2, 0) == 1) {
             if (dir == MOTOR_DIRECTION_UP) {
                 if (motor_down_pi_state == 0) {
                     motor_up_pi_state = 1;

                     if (bVerticalMotorEnable == false) {
                         break;
                     }

                     continue;
                 }
             } else if (dir == MOTOR_DIRECTION_DOWN) {
                 if (motor_up_pi_state == 0) {
                     motor_down_pi_state = 1;

                     if (bVerticalMotorEnable == false) {
                         break;
                     }

                     continue;
                 }
             }
         } else {
             motor_up_pi_state = 0;
             motor_down_pi_state = 0;
         }

         gpioLevel = !gpioLevel;
         controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);

         motorDelay(delay);

         gpioLevel = !gpioLevel;
         controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, gpioLevel);

         motorDelay(delay);

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

