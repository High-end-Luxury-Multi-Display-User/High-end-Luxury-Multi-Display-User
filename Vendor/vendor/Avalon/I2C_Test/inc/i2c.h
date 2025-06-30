#ifndef ADS1115_H
#define ADS1115_H

#include <stdint.h>

// Constants for ADS1115
#define ADS1115_ADDRESS 0x48
#define ADS1115_CONVERSION_REGISTER 0x00
#define ADS1115_CONFIG_REGISTER 0x01

// Function to open I2C device and set slave address
int open_i2c(const char* device, int address);

// Function to configure the ADS1115 (set input channel)
void configure_ads1115(int file, int channel);

// Function to read the conversion result from the ADS1115
int16_t read_ads1115(int file);

#endif  // ADS1115_H

