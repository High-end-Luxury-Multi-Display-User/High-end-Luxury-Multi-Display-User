#ifndef ADS_1115_H
#define ADS_1115_H
#define ADS1115_ADDRESS 0x48  
#define ADS1115_CONVERSION_REGISTER 0x00
#define ADS1115_CONFIG_REGISTER 0x01

#include <iostream>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>
#include <stdint.h>
#include <stdexcept>


int open_i2c(const char* device, int address);
void configure_ads1115(int file, int channel);
int16_t read_ads1115(int file);
int getI2c();
#endif
