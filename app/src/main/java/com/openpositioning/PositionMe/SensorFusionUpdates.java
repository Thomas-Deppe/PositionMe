package com.openpositioning.PositionMe;

import com.google.android.gms.maps.model.LatLng;

public interface SensorFusionUpdates {

     enum update_type {
        PDR_UPDATE,
        GNSS_UPDATE,
        ORIENTATION_UPDATE,
        FUSION_UPDATE,
        WIFI_UPDATE
    }

    void onPDRUpdate();

    void onOrientationUpdate();

    void onGNSSUpdate();

    void onFusionUpdate(LatLng fusion);

    void onWifiUpdate(LatLng wifi);
}

