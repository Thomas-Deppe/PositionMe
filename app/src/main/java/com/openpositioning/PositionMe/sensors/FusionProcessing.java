package com.openpositioning.PositionMe.sensors;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.openpositioning.PositionMe.Utils.CoordinateTransform;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class FusionProcessing {

    private static final double outlier_threshold = 3.0;
    private static final double zScoreFactor = 0.6745;

    private SensorFusion sensorFusion;
    //Variables to store the users starting position.
    private double[] startPosition = new double[3];
    private double[] ecefRefCoords = new double[3];

    private LatLng positionPDR, positionWifi, positionGNNS;

    //Todo: Implement Sensor Fusion Algorithm
    public void fusionEFG(){

        // todo: at the end call function in SensorFusion
        // sensorFusion.onFusionAlgComplete();
    }

    public FusionProcessing() {
        // todo - HELP- the following only once at the start
//        this.sensorFusion = SensorFusion.getInstance();
//        startPosition = sensorFusion.getGNSSLatLngAlt(true);
//        ecefRefCoords = CoordinateTransform.geodeticToEcef(startPosition[0],startPosition[1], startPosition[2]);
    }

    //Todo: Add Outlier Detection

    public void detectOutliers(List<Wifi> wifiList) {

        if (wifiList.isEmpty()){
            return;
        }

        int [] wifiData = wifiList.stream().mapToInt(Wifi::getLevel).toArray();
        double median = calculateMedian(wifiData);
        double MAD = calculateMAD(wifiData, median);
        Log.d("DETECT_OUTLIERS", "Median = "+median+" MAD = "+MAD);

        for (int i = 0; i <wifiData.length; i++){
            double modifiedZscore = zScoreFactor*((Math.abs(wifiData[i] - median)) / MAD);
            if (modifiedZscore > outlier_threshold) {
                Log.d("DETECT_OUTLIERS", "Outlier detected: " + wifiData[i] + " with modified z-score: " + modifiedZscore);
            }
        }

    }

    private static double calculateMedian(int[] wifiList) {
        int[] rssiValues = wifiList;
        Arrays.sort(rssiValues);
        int size = rssiValues.length;
        if (size % 2 != 0) {
            return rssiValues[size / 2];
        } else {
            return (rssiValues[(size - 1) / 2] + rssiValues[size / 2]) / 2.0;
        }
    }

    private static double calculateMedian(double[] wifiList) {
        double[] rssiValues = wifiList;
        Arrays.sort(rssiValues);
        int size = rssiValues.length;
        if (size % 2 != 0) {
            return rssiValues[size / 2];
        } else {
            return (rssiValues[(size - 1) / 2] + rssiValues[size / 2]) / 2.0;
        }
    }

    private static double calculateMAD(int[] deviations, double median) {
        double[] absoluteDeviations = new double[deviations.length];
        for (int i = 0; i < deviations.length; i++) {
            absoluteDeviations[i] = Math.abs(deviations[i] - median);
        }
        return calculateMedian(absoluteDeviations);
    }

    public JSONObject toJson(List<Wifi> wifiList) throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject fingerprint = new JSONObject();
        for (Wifi data : wifiList){
            String bssid = Long.toString(data.getBssid());
            System.out.println(bssid);
            fingerprint.put(bssid, data.getLevel());
        }

        json.put("wf", fingerprint);

        return json;
    }

    // block for structure
    public void updateFusionPDR(){

        // calculate new PDR, save as global variable
        double[] pdrValues = sensorFusion.getCurrentPDRCalc();
        float elevationVal = sensorFusion.getElevation();
        //Transform the ENU coordinates to WSG84 coordinates google maps uses
        sensorFusion.getGNSSLatitude(true);
        positionPDR = CoordinateTransform.enuToGeodetic(pdrValues[0], pdrValues[1], elevationVal, startPosition[0], startPosition[1], ecefRefCoords);

        // call fusion algorithm
        fusionEFG();
    }

    public void updateFusionWifi(JSONObject wifiresponse){

        // TODO: decode the new Wifi LatLng from JSON, save as global variable
        // positionWifi = 

        // call fusion algorithm
        fusionEFG();

    }
    public void updateFusionGNSS(double latitude,double longitude,double altitude,float GNSS_accuracy){

        // use the parameter values to get the LatLng
        positionGNNS = new LatLng(latitude, longitude);

        // call fusion algorithm
        fusionEFG();

    }

}
