// IGpsService.aidl
package com.example.driverlauncher;

// Declare any non-default types here with import statements

interface IGpsService {
       double getLatitude();
       double getLongitude();
       float getSpeed();
}