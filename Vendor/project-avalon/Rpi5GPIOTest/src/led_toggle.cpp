#include <iostream>
#include <unistd.h>
#include "gpiohalrpi5.hpp"

int main() {
    const int GPIO_PIN = 21;  
    GpioHal gpio;


    if (!gpio.setGpioDirOut(GPIO_PIN)) {
        std::cerr << "Failed to set GPIO " << GPIO_PIN << " direction to output" << std::endl;
        return 1;
    }


    for (int i = 0; i < 10; ++i) {
        int value = i % 2;
        
        if (!gpio.setGpioValue(GPIO_PIN, value)) {
            std::cerr << "Failed to set GPIO " << GPIO_PIN << " value" << std::endl;
            return 1;
        }

        std::cout << "Toggled GPIO " << GPIO_PIN 
                  << " to " << (value ? "HIGH" : "LOW")
                  << " (iteration " << i + 1 << ")" << std::endl;
        
        sleep(1);  
    }

    return 0;
}
