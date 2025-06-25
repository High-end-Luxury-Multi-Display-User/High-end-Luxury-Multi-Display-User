package android.vendor.gps;
@VintfStability
interface IGpsService {
    double getLatitude();
    double getLongitude();
    float getSpeed();
}

