package com.openpositioning.PositionMe;

public interface SensorFusionUpdates {

     enum update_type {
        PDR_UPDATE,
        GNSS_UPDATE,
        ORIENTATION_UPDATE,
         //todo:NEW FUSED UPDATE
    }

    void onPDRUpdate();

    void onOrientationUpdate();

    void onGNSSUpdate();

}

