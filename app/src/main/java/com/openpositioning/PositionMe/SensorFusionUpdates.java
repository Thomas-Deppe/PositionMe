package com.openpositioning.PositionMe;



public interface SensorFusionUpdates {

     enum update_type {
        PDR_UPDATE,
        GNSS_UPDATE,
        ORIENTATION_UPDATE,
    }

    void onPDRUpdate();

    void onOrientationUpdate();

    void onGNSSUpdate();

}

