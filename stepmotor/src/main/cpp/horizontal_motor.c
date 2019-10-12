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



#define  LOG_TAG    "horizontal_motor"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static int hMotorFd = -1;

static struct timeval tv;
static struct timeval ts;


int gHDelay = 2000;
int gHDirection = 0;

extern int motor_left_pi_state;
extern int motor_right_pi_state;

static int getLeftRightPiState(int fd, int gpio_num, int gpio_state)
{
    return getPiState(fd, gpio_num, gpio_state);
}

int controlHorizontalMotor(int steps, int dir, int delay, bool bCheckLimitSwitch) {

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
        if (bCheckLimitSwitch == true) {
            if (dir == MOTOR_DIRECTION_LEFT) {
                if (getPiState(hMotorFd, MOTO_SENSOR_LEFT_RIGHT_1, 0) == 1) {
                    if(motor_right_pi_state == 0){
                        motor_left_pi_state = 1;
                        LOGE("Reach left pi");
                        break;
                    }
                }
                else{
                    motor_right_pi_state = 0;
                    motor_left_pi_state = 0;
                }
            } else if (dir == MOTOR_DIRECTION_RIGHT) {
                if(getPiState(hMotorFd, MOTO_SENSOR_LEFT_RIGHT_1, 0) == 1){
                    if(motor_left_pi_state == 0){
                        motor_right_pi_state = 1;
                        LOGE("Reach right pi");
                        break;
                    }
                }
                else{
                    motor_right_pi_state = 0;
                    motor_left_pi_state = 0;
                }
            }
        }


        gpioLevel = !gpioLevel;
    	controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);

    	motorDelay(delay);

    	gpioLevel = !gpioLevel;
    	controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);

    	motorDelay(delay);

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

int startHMotorRunning(bool bCheckLimitSwitch) {

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
         if (bCheckLimitSwitch) {
             if (dir == MOTOR_DIRECTION_LEFT) {
                 if (getPiState(hMotorFd, MOTO_SENSOR_LEFT_RIGHT_1, 0) == 1) {
                     if(motor_right_pi_state == 0){
                         motor_left_pi_state = 1;
                         LOGE("Reach left pi");

                         if (bHorizontalMotorEnable == false) {
                             break;
                         }

                         continue;
                     }
                 }
                 else{
                     motor_right_pi_state = 0;
                     motor_left_pi_state = 0;
                 }
             } else if (dir == MOTOR_DIRECTION_RIGHT) {
                 if(getPiState(hMotorFd, MOTO_SENSOR_LEFT_RIGHT_1, 0) == 1){
                     if(motor_left_pi_state == 0){
                         motor_right_pi_state = 1;
                         LOGE("Reach right pi");

                         if (bHorizontalMotorEnable == false) {
                             break;
                         }

                         continue;
                     }
                 }
                 else{
                     motor_right_pi_state = 0;
                     motor_left_pi_state = 0;
                 }
             }
         }

         gpioLevel = !gpioLevel;
         controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);

         motorDelay(delay);

         gpioLevel = !gpioLevel;
         controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, gpioLevel);

         motorDelay(delay);

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

