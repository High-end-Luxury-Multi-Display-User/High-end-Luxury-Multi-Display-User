#pragma once

#include <string>
#include <pthread.h>

class Neo7mGps {
public:
    Neo7mGps(const std::string& uartPath = "/dev/ttyAMA0");
    ~Neo7mGps();

    bool start();
    void stop();

    double getLatitude() const;
    double getLongitude() const;
    float getSpeed() const;

private:
    int uart_fd = -1;
    pthread_t gps_thread{};
    bool running = false;

    mutable pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;

    double latitude = 0.0;
    double longitude = 0.0;
    float speed = 0.0f;

    void configureUart(int fd);
    static void* gpsThread(void* arg);
    void parseGPRMC(const std::string& line);
};

