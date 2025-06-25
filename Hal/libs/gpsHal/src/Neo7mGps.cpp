#include "Neo7mGps.hpp"

#include <android/log.h>
#include <fcntl.h>
#include <termios.h>
#include <unistd.h>
#include <sstream>
#include <vector>
#include <cstring>
#include <cstdlib>

#define LOG_TAG "Neo7mGpsLib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

Neo7mGps::Neo7mGps(const std::string& uartPath) {
    uart_fd = open(uartPath.c_str(), O_RDONLY | O_NOCTTY);
    if (uart_fd >= 0) {
        configureUart(uart_fd);
    } else {
        LOGE("Failed to open UART: %s", uartPath.c_str());
    }
}

Neo7mGps::~Neo7mGps() {
    stop();
    if (uart_fd >= 0) close(uart_fd);
}

bool Neo7mGps::start() {
    if (uart_fd < 0) return false;
    running = true;
    return pthread_create(&gps_thread, nullptr, gpsThread, this) == 0;
}

void Neo7mGps::stop() {
    running = false;
    pthread_join(gps_thread, nullptr);
}

double Neo7mGps::getLatitude() const { return latitude; }
double Neo7mGps::getLongitude() const { return longitude; }
float Neo7mGps::getSpeed() const { return speed; }

void* Neo7mGps::gpsThread(void* arg) {
    auto* self = static_cast<Neo7mGps*>(arg);
    char buffer[1024];
    while (self->running) {
        int len = read(self->uart_fd, buffer, sizeof(buffer) - 1);
        if (len > 0) {
            buffer[len] = '\0';
            if (strstr(buffer, "$GPRMC")) {
                self->parseGPRMC(buffer);
            }
        }
    }
    return nullptr;
}

void Neo7mGps::parseGPRMC(const char* line) {
    std::string sentence(line);
    std::stringstream ss(sentence);
    std::string token;
    std::vector<std::string> parts;

    while (std::getline(ss, token, ',')) {
        parts.push_back(token);
    }

    if (parts.size() > 7 && parts[2] == "A") {
        char* endPtr = nullptr;
        double rawLat = strtod(parts[3].c_str(), &endPtr);
        if (endPtr == parts[3].c_str()) return;
        latitude = (int)(rawLat / 100) + (rawLat - (int)(rawLat / 100) * 100) / 60.0;
        if (parts[4] == "S") latitude *= -1;

        double rawLon = strtod(parts[5].c_str(), &endPtr);
        if (endPtr == parts[5].c_str()) return;
        longitude = (int)(rawLon / 100) + (rawLon - (int)(rawLon / 100) * 100) / 60.0;
        if (parts[6] == "W") longitude *= -1;

        float knots = strtof(parts[7].c_str(), &endPtr);
        speed = (endPtr == parts[7].c_str()) ? 0.0f : knots * 1.852f;

        LOGI("Parsed: lat=%.6f, lon=%.6f, speed=%.2f", latitude, longitude, speed);
    }
}

void Neo7mGps::configureUart(int fd) {
    struct termios tty {};
    if (tcgetattr(fd, &tty) != 0) return;

    cfsetospeed(&tty, B9600);
    cfsetispeed(&tty, B9600);
    tty.c_cflag |= (CLOCAL | CREAD);
    tty.c_cflag &= ~CSIZE;
    tty.c_cflag |= CS8;
    tty.c_cflag &= ~PARENB;
    tty.c_cflag &= ~CSTOPB;
    tty.c_cflag &= ~CRTSCTS;
    tty.c_lflag = 0;
    tty.c_oflag = 0;
    tty.c_cc[VMIN] = 1;
    tty.c_cc[VTIME] = 0;

    tcflush(fd, TCIFLUSH);
    tcsetattr(fd, TCSANOW, &tty);
}

