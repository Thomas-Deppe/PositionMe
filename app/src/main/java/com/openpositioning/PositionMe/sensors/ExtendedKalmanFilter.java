package com.openpositioning.PositionMe.sensors;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.openpositioning.PositionMe.CoordinateTransform;
import com.openpositioning.PositionMe.OutlierDetector;

import org.ejml.simple.SimpleMatrix;

public class ExtendedKalmanFilter {

    private static long relevanceThreshold = 5000;
    private static double stepPercentageError = 0.1;
    private static double stepMisdirection = 0.05;
    private static double defaultStepLength = 0.7;

    // Example standard deviations for process and measurement noise
    private double sigma_dN = 2; // Variance for dx process noise
    private double sigma_dE = 2; // Variance for dy process noise
    private double sigma_ds = 1; // variance for ds process noise
    private double sigma_dtheta = Math.toRadians(15); // Standard deviation for dθ process noise in radians

    private double sigma_north_meas = 10; // Standard deviation for x measurement noise PDR
    private double sigma_east_meas = 10; // Standard deviation for y measurement noise PDR
    private double wifi_std = 10;
    private double gnss_std = 5;

    private SimpleMatrix Fk; // State transition matrix
    private SimpleMatrix Qk; // Process noise covariance matrix
    private SimpleMatrix Hk; // Observation matrix
    private SimpleMatrix Rk; // Observation noise covariance matrix
    private SimpleMatrix Pk; // Estimate error covariance
    private SimpleMatrix Xk; // State estimate

    private double[] lastOpportunisticUpdate;
    private long lastOpUpdateTime = Long.MAX_VALUE;
    private boolean usingWifi = true;
    private boolean isFirstPrediction;

    private HandlerThread ekfThread;
    private Handler ekfHandler;
    private OutlierDetector outlierDetector;

    public ExtendedKalmanFilter() {

        this.outlierDetector = new OutlierDetector();

        this.isFirstPrediction = true;
        // Initial covariance matrix
        this.Pk =  SimpleMatrix.diag(0.01, 0.01, 0.01, 0.01); // Initial error covariance // Initial uncertainty

        // Process noise covariance matrix Q
        this.Qk = SimpleMatrix.diag((sigma_dN), (sigma_dE), (sigma_ds), (sigma_dtheta*sigma_dtheta));

        // Measurement noise covariance matrix R
        this.Rk = SimpleMatrix.diag((sigma_north_meas*sigma_north_meas), (sigma_east_meas*sigma_east_meas));

        // Hk based on the observation model (static in this case)
        this.Hk = new SimpleMatrix(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0}
        });

        initialiseBackgroundHandler();
    }

    private void initialiseBackgroundHandler(){
        ekfThread = new HandlerThread("EKFProcessingThread");
        ekfThread.start();
        ekfHandler = new Handler(ekfThread.getLooper());
    }

    private void initialiseStateVector(double initialHeading){
        this.Xk = new SimpleMatrix(new double[][]{
                {0},
                {0},
                {defaultStepLength},
                {initialHeading}
        });
        this.isFirstPrediction = false;
    }

    private void updateFk(double theta_k, double step_k){
        // Change angle so zero rad is east
        Log.d("EKF:", "update theta "+theta_k);

        double cosTheta = Math.cos(theta_k);
        double sinTheta = Math.sin(theta_k);

        this.Fk = new SimpleMatrix(new double[][]{
                {1, 0 , cosTheta, -step_k * sinTheta},
                {0, 1, sinTheta, step_k * cosTheta},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        });
    }


    private void updateQk (double averageStepLength, double theta_k, long refTime){
        double penaltyFactor = calculateTimePenalty(refTime);
        Log.d("EKF:", "update Qk... average Step = "+averageStepLength+ " theta_k = "+theta_k + " Penalty = "+penaltyFactor);
        double step_error = (stepPercentageError * averageStepLength + stepMisdirection) * penaltyFactor;
        //float adaptedHeading = (float) (Math.PI/2 - theta_k);
        double adaptedHeading = theta_k;
        double north_error = step_error*Math.cos(adaptedHeading);
        double east_error = step_error*Math.sin(adaptedHeading);
        double bearing_error = calculateBearingPenalty(refTime);
        Log.d("EKF:", "new error variances: step_error = "+step_error+ " north_error = "+north_error+" east_error = "+east_error);

        this.Qk.set(0, 0, north_error*north_error);
        this.Qk.set(1, 1, east_error*east_error);
        this.Qk.set(2, 2, step_error*step_error);
        this.Qk.set(3, 3, bearing_error*bearing_error);
    }

    private void updateRk (){
        if (this.usingWifi){
            this.Rk.set(0,0, wifi_std*wifi_std);
            this.Rk.set(1, 1, wifi_std*wifi_std);
        } else {
            this.Rk.set(0,0, gnss_std*gnss_std);
            this.Rk.set(1, 1, gnss_std*gnss_std);
        }
    }

    public void predict(double theta_k, double step_k, double averageStepLength) {
        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                // Update Fk based on the current state
                Log.d("EKF:", "Predicting... "+theta_k+" "+step_k);
                //double adaptedHeading = wraptopi((Math.PI/2 - theta_k));
                //double adaptedHeading = (Math.PI/2 - theta_k);
                double adaptedHeading = theta_k;
                if (isFirstPrediction){
                    initialiseStateVector(adaptedHeading);
                }
                updateFk(adaptedHeading, step_k);
                //updateQk(averageStepLength, adaptedHeading, refTime);

                // Predict the state vector Xk
                Xk = Fk.mult(Xk);

                // Predict the covariance matrix Pk
                Pk = Fk.mult(Pk).mult(Fk.transpose()).plus(Qk);
                Log.d("EKF:", "XK after predict: "+Xk.toString());
                Log.d("EKF:", "Predicted: East = "+Xk.get(1, 0)+" North = "+Xk.get(0,0));
            }
        });
    }

    public void update(double[] observation_k){
        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                SimpleMatrix Zk = new SimpleMatrix(new double[][]{{observation_k[0]}, {observation_k[1]}});
                //updateRk(penaltyFactor);
                updateRk();

                SimpleMatrix y_pred = Zk.minus(Hk.mult(Xk));
                SimpleMatrix Sk = Hk.mult(Pk).mult(Hk.transpose()).plus(Rk);
                SimpleMatrix KalmanGain = Pk.mult(Hk.transpose().mult(Sk.invert()));

                Xk = Xk.plus(KalmanGain.mult(y_pred));
                //double updatedAngle = Xk.get(3,0);
                //Xk.set(3, 0, wraptopi(updatedAngle));
                //Log.d("EKF:", "Wrapping calc angle: "+updatedAngle+" to "+wraptopi(updatedAngle));
                Pk = Pk.minus(KalmanGain.mult(Hk).mult(Pk));
            }
        });
    }

    public void onOpportunisticUpdate(double[] observe, long refTime){
        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("EKF:", "Opportunistic update...");
                Log.d("EKF:", "East: "+observe[0]+ " North: "+ observe[1] + "Timestamp: " + refTime);
                double predictedEast= Xk.get(1, 0);
                double predictedNorth = Xk.get(0,0);
                double distanceBetween = Math.sqrt(Math.pow(predictedEast - observe[0], 2) + Math.pow(predictedNorth - observe[1], 2));

                if (!outlierDetector.detectOutliers(distanceBetween)){
                    Log.d("EKF", "No outlier detected");
                    lastOpportunisticUpdate = observe;
                    lastOpUpdateTime = refTime;
                }
            }
        });
    }

    public void onStepDetected(double pdrEast, double pdrNorth, double altitude, double theta_k, double stepLength, long refTime){
        if (lastOpportunisticUpdate != null && checkRelevance(refTime)){
            Log.d("EKF:", "Using observation update");
            onObservationUpdate(lastOpportunisticUpdate[0], lastOpportunisticUpdate[1], pdrEast, pdrNorth, altitude, calculateTimePenalty(refTime));
            return;
        }

        if (checkRelevance(refTime)){
            onObservationUpdate(lastOpportunisticUpdate[0], lastOpportunisticUpdate[1], pdrEast, pdrNorth, altitude);
            return;
        }

        performRecursiveCorrection(pdrEast, pdrNorth, altitude);
    }


    private boolean checkRelevance(long refTime){
        long timeDifference = Math.abs(refTime - lastOpUpdateTime);

        if (timeDifference <= relevanceThreshold) return true;

        this.lastOpportunisticUpdate = null;
        this.lastOpUpdateTime = Long.MAX_VALUE;

        return false;
    }

    // Calculates the penalty factor based on the elapsed time since the last WiFi update
    private double calculateTimePenalty(long currentTime) {
        // Calculate elapsed time since last opportunistic (e.g., WiFi) update
        long elapsedTime = currentTime - lastOpUpdateTime;
        Log.d("EKF:", "Last update time: "+lastOpUpdateTime+" current time: "+currentTime + " elapsed time: "+elapsedTime);
        // Define how the penalty increases over time. This is an example and can be adjusted.
        double penaltyFactor = 1.0 + (elapsedTime / 1000.0); // Simple linear penalty
        Log.d("EKF:", "Penalty factor: "+penaltyFactor);
        return Math.max(penaltyFactor, 1); // Ensure the penalty factor never reduces measurement noise
    }

    // Calculates the penalty factor based on the elapsed time since the last WiFi update
    private double calculateBearingPenalty(long elapsedTime) {
        double elapsedTimeMinutes = elapsedTime / 60000.0;

        // Linear or exponential penalty factor based on elapsed time
        // Example: linear growth
        return sigma_dtheta*elapsedTimeMinutes;// Adjust the divisor to control the rate of increase
    }

    public void onObservationUpdate(double observeEast, double observeNorth, double pdrEast, double pdrNorth, double altitude, double penaltyFactor){
        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("EKF:", "Observed... X = "+observeEast+" Y = "+observeNorth);
                Log.d("EKF:", "PDR... X = "+pdrEast+" Y = "+pdrNorth);
                double[] observation = new double[] {(observeNorth - pdrNorth), (observeEast - pdrEast)};

                update(observation);
                Log.d("EKF:", "XK after update: "+Xk.toString());
                Log.d("EKF:", "East = "+Xk.get(1, 0) + " North = "+Xk.get(0,0));
                double[] startPosition = SensorFusion.getInstance().getGNSSLatLngAlt(true);
                double[] ecefRefCoords = SensorFusion.getInstance().getEcefRefCoords();
                SensorFusion.getInstance().notifyKalmanFilterUpdate(
                        CoordinateTransform.enuToGeodetic(Xk.get(1, 0), Xk.get(0,0),
                                altitude,
                                startPosition[0], startPosition[1], ecefRefCoords)
                );
            }
        });
    }

    public void performRecursiveCorrection (double pdrEast, double pdrNorth, double altitude){
        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("EKF:", "PDR ... East = "+pdrEast+" North = "+pdrNorth);
                double predictedEast= Xk.get(1, 0);
                double predictedNorth = Xk.get(0,0);
                Log.d("EKF:", "Predicted ... East = "+predictedEast+" North = "+predictedNorth);
                Log.d("EKF:", "Observation... East = "+(predictedEast - pdrEast)+" North = "+(predictedNorth - pdrNorth));
                double[] observation = new double[] {(predictedNorth - pdrNorth), (predictedEast - pdrEast)};
                update(observation);
                Log.d("EKF:", "Recursive correction output... East = "+Xk.get(1, 0)+" North = "+Xk.get(0,0));

                double[] startPosition = SensorFusion.getInstance().getGNSSLatLngAlt(true);
                double[] ecefRefCoords = SensorFusion.getInstance().getEcefRefCoords();
                SensorFusion.getInstance().notifyKalmanFilterUpdate(
                        CoordinateTransform.enuToGeodetic(Xk.get(1, 0), Xk.get(0,0),
                                altitude,
                                startPosition[0], startPosition[1], ecefRefCoords)
                );
            }
        });
    }

    private static double wraptopi(double x) {
        if (x > Math.PI) {
            x = x - (Math.floor(x / (2 * Math.PI)) + 1) * 2 * Math.PI;
        } else if (x < -Math.PI) {
            x = x + (Math.floor(x / (-2 * Math.PI)) + 1) * 2 * Math.PI;
        }
        return x;
    }


    public void stopFusion(){
        Log.d("EKF:", "Stopping EKF handler");
        ekfThread.quitSafely();
    }
}
