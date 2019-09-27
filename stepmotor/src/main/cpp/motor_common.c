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

 int motor_left_pi_state = 0;
 int motor_right_pi_state = 0;
 int motor_up_pi_state = 0;
 int motor_down_pi_state = 0;

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

int getPiState(int fd, int gpio_num, int gpio_state)
{
    struct motor_a3901 userdata_motor;
    userdata_motor.gpio_num   = gpio_num;
    userdata_motor.gpio_state = gpio_state;

    if((ioctl(fd, VS_GET_GPIO, &userdata_motor)) < 0)
    {
        LOGE("%s VIDIOC_DQBUF motor error!\n", __FUNCTION__);
        return -1;
    }
    return userdata_motor.gpio_state;
}

void motorDelay(int delay) {
    struct timeval start;
    struct timeval end;

    gettimeofday(&start, NULL); //gettimeofday(&start,&tz);结果一样
    while(1){
        gettimeofday(&end, NULL);
        if(((end.tv_sec-start.tv_sec)*1000000+(end.tv_usec-start.tv_usec)) >=  delay){
            break;
        }
    }
}

void controlMultipleMotors(int hSteps, int vSteps, int hDir, int vDir, int delay, bool bCheckLimitSwitch) {
    int counts = 0;
    int steps = 0;
    if (hSteps > vSteps) {
        counts = hSteps / vSteps;
        steps = hSteps;
    } else {
        counts = vSteps / hSteps;
        steps = vSteps;
    }

    if (counts != 1 && ( counts % 2 != 0 )) {
        LOGE("not support hSteps= %d, vStep=%d", hSteps, vSteps);
        return;
    }


    //init h motor
    int hMotorFd = open(MOTOR_DRV_LEFT_RIGHT, O_RDWR);
    if(hMotorFd == -1)
    {
        LOGE("%s open left_right_motor_device error..........!\n", __FUNCTION__);
        return;
    }


    controlMotorDev(hMotorFd, MOTO_ENABLE_LEFT_RIGHT, MOTOR_DISABLE);  //使能马达

    controlMotorDev(hMotorFd, MOTO_DIR_LEFT_RIGHT, hDir);  //设置马达转动方向

    int hGpioLevel = 0;
    controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, hGpioLevel);

    //init v motor
    int vMotorFd = open(MOTOR_DRV_UP_DOWN, O_RDWR);
    if(vMotorFd == -1)
    {
        LOGE("%s open left_right_motor_device error..........!\n", __FUNCTION__);
        return ;
    }


    controlMotorDev(vMotorFd, MOTO_ENABLE_UP_DOWN, MOTOR_DISABLE);  //使能马达

    controlMotorDev(vMotorFd, MOTO_DIR_UP_DOWN, vDir);  //设置马达转动方向

    int vGpioLevel = 0;
    controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, vGpioLevel);

    int count = 0;
    bVerticalMotorEnable = true;
    bHorizontalMotorEnable = true;

    for (int i = 0; i < steps; i++) {
        if (bCheckLimitSwitch) {
            if(getPiState(vMotorFd, MOTO_SENSOR_UP_DOWN_2, 0) == 1) {
                LOGE("reach up/down pi");
                if (vDir == MOTOR_DIRECTION_UP) {
                    if (motor_down_pi_state == 0) {
                        LOGE("reach up pi");
                        motor_up_pi_state = 1;
                        break;
                    }
                } else if (vDir == MOTOR_DIRECTION_DOWN) {
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

            if (getPiState(hMotorFd, MOTO_SENSOR_LEFT_RIGHT_1, 0) == 1) {
                if (hDir == MOTOR_DIRECTION_LEFT) {
                    if(motor_right_pi_state == 0){
                        motor_left_pi_state = 1;
                        LOGE("Reach left pi");
                        break;
                    }
                } else if (hDir == MOTOR_DIRECTION_RIGHT) {
                    if(motor_left_pi_state == 0){
                        motor_right_pi_state = 1;
                        LOGE("Reach right pi");
                        break;
                    }
                }
            } else {
                motor_right_pi_state = 0;
                motor_left_pi_state = 0;
            }
        }


        if (hSteps > vSteps) {
            hGpioLevel = !hGpioLevel;
            controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, hGpioLevel);

            if ((count % counts) == 0) {
                vGpioLevel = !vGpioLevel;
                controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, vGpioLevel);
            }

            count++;
        } else {
            vGpioLevel = !vGpioLevel;
            controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, vGpioLevel);

            if ((count % counts) == 0) {
                hGpioLevel = !hGpioLevel;
                controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, hGpioLevel);
            }

            count++;
        }

        motorDelay(delay);

        if (hSteps > vSteps) {
            hGpioLevel = !hGpioLevel;
            controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, hGpioLevel);

            if ((count % counts) == 0) {
                vGpioLevel = !vGpioLevel;
                controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, vGpioLevel);
            }

            count++;
        } else {
            vGpioLevel = !vGpioLevel;
            controlMotorDev(vMotorFd, MOTO_STEP_UP_DOWN, vGpioLevel);

            if ((count % counts) == 0) {
                hGpioLevel = !hGpioLevel;
                controlMotorDev(hMotorFd, MOTO_STEP_LEFT_RIGHT, hGpioLevel);
            }

            count++;
        }

        motorDelay(delay);

        if (bHorizontalMotorEnable == false && bVerticalMotorEnable == false) {
            break;
        }
    }

    controlMotorDev(vMotorFd, MOTO_ENABLE_UP_DOWN, MOTOR_ENABLE); //锁马达
    close(vMotorFd);
    bVerticalMotorEnable = false;

    controlMotorDev(hMotorFd, MOTO_ENABLE_LEFT_RIGHT, MOTOR_ENABLE); //锁马达
    bHorizontalMotorEnable = false;
    close(hMotorFd);

    return;
}

void stopMultipleMotors() {
    bVerticalMotorEnable = false;
    bHorizontalMotorEnable = false;
}