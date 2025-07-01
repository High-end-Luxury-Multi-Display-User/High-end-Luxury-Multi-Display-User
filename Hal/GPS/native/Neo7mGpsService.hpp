
#pragma once

#include <aidl/com/example/driverlauncher/BnGpsService.h>
#include <Neo7mGps.hpp>

namespace aidl::com::example::driverlauncher {

class Neo7mGpsService : public BnGpsService {
public:
    Neo7mGpsService();
    ~Neo7mGpsService();

    ::ndk::ScopedAStatus getLatitude(double* _aidl_return) override;
    ::ndk::ScopedAStatus getLongitude(double* _aidl_return) override;
    ::ndk::ScopedAStatus getSpeed(float* _aidl_return) override;

private:
    Neo7mGps gps;
};

} // namespace aidl::android::vendor::gps

