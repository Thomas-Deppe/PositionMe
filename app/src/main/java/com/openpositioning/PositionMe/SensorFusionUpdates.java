package com.openpositioning.PositionMe;

import com.google.android.gms.maps.model.LatLng;

public interface SensorFusionUpdates {

     enum update_type {
        PDR_UPDATE,
        GNSS_UPDATE,
        ORIENTATION_UPDATE,
        //PARTICLE_UPDATE,
         FUSED_UPDATE,
        WIFI_UPDATE
    }

    void onPDRUpdate();

    void onOrientationUpdate();

    void onGNSSUpdate();

    //void onParticleUpdate(LatLng particle);

    void onFusedUpdate(LatLng coordinate);

    void onWifiUpdate(LatLng wifi);
}

