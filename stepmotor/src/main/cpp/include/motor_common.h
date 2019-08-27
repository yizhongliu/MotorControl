#ifndef _MOTOR_COMMON_H
#define _MOTOR_COMMON_H
#include <sys/ioctl.h>
#include <stdbool.h>

struct motor_a3901{
	 int gpio_num;
	 int gpio_state;
};
#define MOTOR_MAGIC 'x' 
#define VS_SET_GPIO   		 _IOWR(MOTOR_MAGIC, 0x0, struct motor_a3901)
#define VS_GET_GPIO   		 _IOWR(MOTOR_MAGIC, 0x1, struct motor_a3901)
#define VS_SET_MOTOR_ENABLE  _IOWR(MOTOR_MAGIC, 0x2, struct motor_a3901)

#define MOTO_ENABLE_AF    120
#define MOTO_STEP_AF 		123
#define MOTO_DIR_AF 		122
//#define MOTO_M0 84
//#define MOTO_M1 86
#define MOTO_SENSOR_AF_1 64
#define MOTO_SENSOR_AF_2 73


#define MOTO_ENABLE_UP_DOWN    66
#define MOTO_STEP_UP_DOWN 		65
#define MOTO_DIR_UP_DOWN 		67

//#define MOTO_ENABLE_UP_DOWN    127
//#define MOTO_STEP_UP_DOWN 		126
//#define MOTO_DIR_UP_DOWN 		125
//#define MOTO_M0 84
//#define MOTO_M1 86
#define MOTO_SENSOR_UP_DOWN_1 68
#define MOTO_SENSOR_UP_DOWN_2 74


#define MOTO_ENABLE_LEFT_RIGHT    69
#define MOTO_STEP_LEFT_RIGHT 		70
#define MOTO_DIR_LEFT_RIGHT 		71
//#define MOTO_M0 84
//#define MOTO_M1 86
#define MOTO_SENSOR_LEFT_RIGHT_1 76
#define MOTO_SENSOR_LEFT_RIGHT_2 72


#define MOTO_ENABLE_CAMERA    127
#define MOTO_STEP_CAMERA 		126
#define MOTO_DIR_CAMERA 		125
//#define MOTO_M0 84
//#define MOTO_M1 86
#define MOTO_SENSOR_CAMERA_1 157
#define MOTO_SENSOR_CAMERA_2 12

#define MOTOR_SPEED 2000
#define FORWARD 1
#define BACK    0
#define MOTOR_ENABLE 1
#define MOTOR_DISABLE 0


#define MOTOR_DIRECTION_LEFT 1
#define MOTOR_DIRECTION_RIGHT 0
#define MOTOR_DIRECTION_UP 1
#define MOTRO_DIRECTION_DOWN 0

int controlHorizontalMotor(int steps, int dir, int delay);
int setHorizontalMotorSpeed(int delay);
int setHorizontalMotorDirection(int direction);
int startHMotorRunning();
int stopHMotorRunning();
bool getHMotorEnable();

int controlVerticalMotor(int steps, int dir, int delay);
int setVerticalMotorSpeed(int delay);
int setVerticalMotorDirection(int direction);
int startVMotorRunning();
int stopVMotorRunning();
bool getVMotorEnable();

int controlMotorDev(int fd, int gpio_num, int gpio_state);
int getPiState(int fd, int gpio_num, int gpio_state);
void motorDelay(int delay);

#endif