package com.openpositioning.PositionMe;

import com.google.android.gms.maps.model.LatLng;

public interface SensorFusionUpdates {

     enum update_type {
        PDR_UPDATE,
        GNSS_UPDATE,
        ORIENTATION_UPDATE,
        PARTICLE_UPDATE,
        KALMAN_UPDATE,
        WIFI_UPDATE
    }

    void onPDRUpdate();

    void onOrientationUpdate();

    void onGNSSUpdate();

    void onParticleUpdate(LatLng particle);

    void onKalmanUpdate(LatLng kalmna);

    void onWifiUpdate(LatLng wifi);
}

