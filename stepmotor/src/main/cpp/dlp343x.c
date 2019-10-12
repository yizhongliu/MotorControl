#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>
#include <linux/fb.h>
#include <linux/videodev2.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include "android/log.h"
#include "./include/dlp343x.h"
#define DLP_DRV "/dev/dlp_3438"

#define LOG_TAG    "dlp343"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

int fd_dlp343x;
int dlp_write_data(unsigned char dlp_addr, unsigned char *dlp_data, unsigned char dlp_count)
{
	unsigned char  buf_dlp343x[20];
	int i   = 1;
	int ret = 0;
	int j;
	fd_dlp343x = open(DLP_DRV, O_RDWR);
	if(fd_dlp343x == -1){
		printf("not open dlp_343x!\n");
		return -1;
	}
	buf_dlp343x[0] = dlp_addr;
	//buf_dlp343x[1] = dlp_data;
	for(j = 0; j < dlp_count; j++){
	    buf_dlp343x[i++] = dlp_data[j];
	}
	ret = write(fd_dlp343x, buf_dlp343x, dlp_count + 1);
	/*
	if(state == 1)//read dlp addr
	{
		//buf_dlp343x[i] = dlp_addr;
		ret = read(fd_dlp343x, buf_dlp343x, 1);	
	}
	*/
	close(fd_dlp343x);
	return 1;
}

unsigned char *dlp_read_data(unsigned char dlp_addr, unsigned char dlp_count){
    int ret;
    unsigned char *dlp_read_data;
    unsigned char  dlp_data[20];
    dlp_read_data = dlp_data;
    fd_dlp343x = open(DLP_DRV, O_RDWR);
    if(fd_dlp343x == -1){
    	printf("not open dlp_343x!\n");
    	return NULL;
    }
    dlp_data[0] = dlp_addr;
    ret = read(fd_dlp343x, dlp_data, dlp_count);
    if(ret < 0){
        LOGE("read dlp data error");
        return NULL;
    }
    else{
        return dlp_read_data;
    }
    return dlp_read_data;
}


/*******
0:close Projector
1:open Projector
*******/
int controlProjectorCloseOpen(int powerState){
    int ret;
    fd_dlp343x = open(DLP_DRV, O_RDWR);
    if(fd_dlp343x == -1){
    	LOGI("not open dlp_343x!\n");
    	return -1;
    }
    ret = ioctl(fd_dlp343x, SET_POWER_ON_OFF, &powerState);
    if(ret < 0){
        LOGE("controlProjectorCloseOpen error");
    }
    close(fd_dlp343x);
    return 1;
}

int setKeyStone(int angle) {
    unsigned char enableAddr = 0x88;
    unsigned char writeAddr = 0xbb;
    unsigned char sendData[5];

    int ret = -1;

    //enable keystone
    sendData[0] = 0x01;
    sendData[1] = 0x78;
    sendData[2] = 0x01;
    sendData[3] = 0x00;
    sendData[4] = 0x00;

    dlp_write_data(enableAddr, sendData, 5);

    sendData[0] = 0;
    if (angle >= 0) {
        sendData[1] = angle;
    } else {
        sendData[1] = 0xff + angle + 1;
    }

    ret = dlp_write_data(writeAddr, sendData, 2);
    return ret;
}