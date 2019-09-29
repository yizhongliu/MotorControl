#ifndef DLP343X_HHHH
#define DLP343X_HHHH

#include <sys/ioctl.h>

#define MOTOR_MAGIC 'x'
#define SET_POWER_ON_OFF   		 _IOWR(MOTOR_MAGIC, 0xa, int)

int dlp_write_data(unsigned char dlp_addr, unsigned char *dlp_data, unsigned char dlp_count);
unsigned char *dlp_read_data(unsigned char dlp_addr,  unsigned char dlp_count);
int controlProjectorCloseOpen(int powerState);
int setKeyStone(int angle);


#endif