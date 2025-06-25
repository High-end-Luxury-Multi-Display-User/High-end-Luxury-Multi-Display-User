#include "Neo7mGpsService.hpp"
#include <android/log.h>

#define LOG_TAG "GpsService"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace aidl::android::vendor::gps {

Neo7mGpsService::Neo7mGpsService() {
    if (gps.start()) {
        LOGI("GPS thread started successfully");
    } else {
        LOGE("Failed to start GPS thread");
    }
}

Neo7mGpsService::~Neo7mGpsService() {
    gps.stop();
}

::ndk::ScopedAStatus Neo7mGpsService::getLatitude(double* _aidl_return) {
    *_aidl_return = gps.getLatitude();
    return ::ndk::ScopedAStatus::ok();
}

::ndk::ScopedAStatus Neo7mGpsService::getLongitude(double* _aidl_return) {
    *_aidl_return = gps.getLongitude();
    return ::ndk::ScopedAStatus::ok();
}

::ndk::ScopedAStatus Neo7mGpsService::getSpeed(float* _aidl_return) {
    *_aidl_return = gps.getSpeed();
    return ::ndk::ScopedAStatus::ok();
}

} // namespace aidl::android::vendor::gps

