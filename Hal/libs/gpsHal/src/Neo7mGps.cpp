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

double Neo7mGps::getLatitude() const {
    pthread_mutex_lock(&lock);
    double val = latitude;
    pthread_mutex_unlock(&lock);
    return val;
}

double Neo7mGps::getLongitude() const {
    pthread_mutex_lock(&lock);
    double val = longitude;
    pthread_mutex_unlock(&lock);
    return val;
}

float Neo7mGps::getSpeed() const {
    pthread_mutex_lock(&lock);
    float val = speed;
    pthread_mutex_unlock(&lock);
    return val;
}

void* Neo7mGps::gpsThread(void* arg) {
    auto* self = static_cast<Neo7mGps*>(arg);
    char buffer[1024];
    std::string sentenceBuffer;

    while (self->running) {
        int len = read(self->uart_fd, buffer, sizeof(buffer) - 1);
        if (len > 0) {
            buffer[len] = '\0';
            for (int i = 0; i < len; ++i) {
                if (buffer[i] == '\n') {
                    if (!sentenceBuffer.empty() && sentenceBuffer.find("$GPRMC") != std::string::npos) {
                        self->parseGPRMC(sentenceBuffer);
                    }
                    sentenceBuffer.clear();
                } else if (buffer[i] != '\r') {
                    sentenceBuffer += buffer[i];
                }
            }
        }
    }
    return nullptr;
}

void Neo7mGps::parseGPRMC(const std::string& line) {
    LOGI("Received: %s", line.c_str());

    std::stringstream ss(line);
    std::string token;
    std::vector<std::string> parts;

    while (std::getline(ss, token, ',')) {
        parts.push_back(token);
    }

    if (parts.size() > 7 && parts[2] == "A") {
        char* endPtr = nullptr;

        // Latitude
        double rawLat = strtod(parts[3].c_str(), &endPtr);
        if (*endPtr != '\0') return;
        double lat = (int)(rawLat / 100) + (rawLat - (int)(rawLat / 100) * 100) / 60.0;
        if (parts[4] == "S") lat *= -1;

        // Longitude
        double rawLon = strtod(parts[5].c_str(), &endPtr);
        if (*endPtr != '\0') return;
        double lon = (int)(rawLon / 100) + (rawLon - (int)(rawLon / 100) * 100) / 60.0;
        if (parts[6] == "W") lon *= -1;

        // Speed (in knots)
        float knots = strtof(parts[7].c_str(), &endPtr);
        float spd = (*endPtr == '\0') ? knots * 1.852f : 0.0f;

        pthread_mutex_lock(&lock);
        latitude = lat;
        longitude = lon;
        speed = spd;
        pthread_mutex_unlock(&lock);

        LOGI("Parsed: lat=%.6f, lon=%.6f, speed=%.2f", lat, lon, spd);
    } else {
        LOGI("GPRMC not valid or no fix.");
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

