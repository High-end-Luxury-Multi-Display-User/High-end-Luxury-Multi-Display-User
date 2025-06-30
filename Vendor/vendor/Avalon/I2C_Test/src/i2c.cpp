#include <iostream>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>
#include <stdexcept>
#include <android/log.h>
#include "i2c.h"

int open_i2c(const char* device, int address) {
    int file = open(device, O_RDWR);
    if (file < 0) {
        std::cerr << "Failed to open the bus" << std::endl;
        return -1;
    }

    if (ioctl(file, I2C_SLAVE, address) < 0) {
        std::cerr << "Failed to acquire bus access and/or talk to slave" << std::endl;
        close(file);
        return -1;
    }
    return file;
}

void configure_ads1115(int file, int channel) {
    uint16_t config = 0xC383;  // Default settings for 16-bit, 128 SPS, ±4.096V range

    switch (channel) {
        case 0: config |= 0x4000; break;  // AIN0
        case 1: config |= 0x5000; break;  // AIN1
        case 2: config |= 0x6000; break;  // AIN2
        case 3: config |= 0x7000; break;  // AIN3
        default: break;
    }

    uint8_t data[3] = {ADS1115_CONFIG_REGISTER, static_cast<uint8_t>(config >> 8), static_cast<uint8_t>(config & 0xFF)};
    if (write(file, data, 3) != 3) {
        std::cerr << "Failed to write to the configuration register" << std::endl;
        close(file);
        exit(1);
    }
}

int16_t read_ads1115(int file) {
    uint8_t reg = ADS1115_CONVERSION_REGISTER;
    if (write(file, &reg, 1) != 1) {
        std::cerr << "Failed to set conversion register" << std::endl;
        close(file);
        exit(1);
    }

    uint8_t buf[2];
    if (read(file, buf, 2) != 2) {
        std::cerr << "Failed to read conversion result" << std::endl;
        close(file);
        exit(1);
    }

    int16_t result = (buf[0] << 8) | buf[1];
    return result;
}

int main() {
    const char* device = "/dev/i2c-1";  // I2C bus
    int file = open_i2c(device, ADS1115_ADDRESS);
    if (file < 0) return 1;

    int channel = 1; 
    configure_ads1115(file, channel);

    usleep(10000);  

    int16_t value = read_ads1115(file);  // Read ADC value
    std::cout << "ADC Value for channel " << channel << ": " << value << std::endl;

    close(file);
    return 0;
}

