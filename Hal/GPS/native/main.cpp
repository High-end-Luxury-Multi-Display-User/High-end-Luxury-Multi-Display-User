#include "Neo7mGpsService.hpp"

#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <android-base/logging.h>

#define LOG_TAG "GpsMain"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

int main() {
    ABinderProcess_setThreadPoolMaxThreadCount(0);

    std::shared_ptr<aidl::android::vendor::gps::Neo7mGpsService> service = ndk::SharedRefBase::make<aidl::android::vendor::gps::Neo7mGpsService>();

    const std::string instance = std::string() + aidl::android::vendor::gps::Neo7mGpsService::descriptor + "/default";
   binder_status_t status = AServiceManager_addService(service->asBinder().get(), instance.c_str());
    LOG(INFO) << "Registering service with name: " << instance;

    CHECK(status == STATUS_OK);
    if (status == STATUS_OK) {
        LOG(INFO) << "Service registered successfully.";
    } else {
        LOG(ERROR) << "Failed to register service. Status: " << status;
        return EXIT_FAILURE;
    }
    ABinderProcess_joinThreadPool();
    return EXIT_FAILURE;
}

