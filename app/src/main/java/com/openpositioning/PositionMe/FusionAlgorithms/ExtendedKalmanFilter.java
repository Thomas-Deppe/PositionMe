package com.openpositioning.PositionMe.FusionAlgorithms;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.openpositioning.PositionMe.Utils.CoordinateTransform;
import com.openpositioning.PositionMe.Utils.OutlierDetector;
import com.openpositioning.PositionMe.sensors.SensorFusion;
import com.openpositioning.PositionMe.sensors.TurnDetector;

import org.ejml.simple.SimpleMatrix;


public class ExtendedKalmanFilter{

    private final static long relevanceThreshold = 5000;
    private final static double stepPercentageError = 0.1;
    private final static double stepMisdirection = 0.05;
    private final static double defaultStepLength = 0.7;
    private final static long maxElapsedTimeForMaxPenalty = 3000; // Example: 3000 milliseconds for max penalty
    private static double sigma_dTheta = Math.toRadians(15); // Standard deviation for dθ process noise in radians
    private static double sigma_dPseudo = Math.toRadians(8);
    private static double sigma_dStraight = Math.toRadians(2);

    // Example standard deviations for process and measurement noise
    private double sigma_ds = 1; // variance for ds process noise

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
    private long lastOpUpdateTime;
    private long initialiseTime;
    private boolean usingWifi;
    private boolean stopEKF;
    private double prevStepLength;

    private HandlerThread ekfThread;
    private Handler ekfHandler;
    private OutlierDetector outlierDetector;

    public ExtendedKalmanFilter() {

        Log.d("EKF", "====== INITIALISING EKF ======");
        this.outlierDetector = new OutlierDetector();

        this.stopEKF = false;

        this.Xk = new SimpleMatrix(new double[][]{
                {0},
                {0},
                //{defaultStepLength},
                {0}
        });
        // Initial covariance matrix
        //this.Pk =  SimpleMatrix.diag(0, 0, 0, 0); // Initial error covariance // Initial uncertainty
        this.Pk =  SimpleMatrix.diag(0, 0, 0); // Initial error covariance // Initial uncertainty

        // Process noise covariance matrix Q
        //this.Qk = SimpleMatrix.diag((sigma_dN), (sigma_dE), (sigma_ds), (sigma_dtheta*sigma_dtheta));
        this.Qk = SimpleMatrix.diag((sigma_dTheta * sigma_dTheta), (sigma_ds));

        // Measurement noise covariance matrix R
        this.Rk = SimpleMatrix.diag((sigma_east_meas*sigma_east_meas), (sigma_north_meas*sigma_north_meas));

        // Hk based on the observation model (static in this case)
//        this.Hk = new SimpleMatrix(new double[][]{
//                {1, 0, 0, 0},
//                {0, 1, 0, 0}
//        });
        this.Hk = new SimpleMatrix(new double[][]{
                {0, 1, 0},
                {0, 0, 1}
        });

        Log.d("EKF","HK = "+Hk.toString());
        this.initialiseTime = android.os.SystemClock.uptimeMillis();
        this.lastOpUpdateTime = 0;
        this.prevStepLength = defaultStepLength;
        this.usingWifi = false;

        initialiseBackgroundHandler();
    }

    private void initialiseBackgroundHandler(){
        ekfThread = new HandlerThread("EKFProcessingThread");
        ekfThread.start();
        ekfHandler = new Handler(ekfThread.getLooper());
    }

    private void initialiseStateVector(double initialHeading){
        this.Xk = new SimpleMatrix(new double[][]{
                {initialHeading},
                {0},
                {0}
        });
    }

    private void updateFk(double theta_k, double step_k){
        // Change angle so zero rad is east
        Log.d("EKF:", "update theta "+theta_k);

        double cosTheta = Math.cos(theta_k);
        double sinTheta = Math.sin(theta_k);

//        this.Fk = new SimpleMatrix(new double[][]{
//                {1, 0 , cosTheta, -step_k * sinTheta},
//                {0, 1, sinTheta, step_k * cosTheta},
//                {0, 0, 1, 0},
//                {0, 0, 0, 1}
//        });
        this.Fk = new SimpleMatrix(new double[][]{
                {1, 0 , 0},
                {step_k * cosTheta, 1, 0},
                {-step_k * sinTheta, 0, 1}
        });
        Log.d("EKF:", "FK = "+Fk.toString());
    }

    private void updateQk(double averageStepLength, double theta_k, long refTime, double thetaStd){
        double penaltyFactor = calculateTimePenalty(refTime);
        Log.d("EKF:", "update Qk... average Step = "+averageStepLength+ " theta_k = "+theta_k + " Penalty = "+penaltyFactor);
        double step_error = (stepPercentageError * averageStepLength + stepMisdirection) * penaltyFactor;
        //float adaptedHeading = (float) (Math.PI/2 - theta_k);

        double bearing_error = calculateBearingPenalty(thetaStd, refTime);

        Log.d("EKF:", "update Qk... step_error = "+step_error+ " bearing_penalty = "+bearing_error +" Penalty = "+penaltyFactor+" bearing penalty = "+calculateBearingPenalty(thetaStd, (refTime-initialiseTime)));

        //this.Qk.set(0, 0, north_error*north_error);
        //this.Qk.set(1, 1, east_error*east_error);

        this.Qk.set(0, 0, bearing_error*bearing_error);
        this.Qk.set(1, 1, step_error*step_error);
    }

    private void updateRk(double penaltyFactor){
        if (this.usingWifi){
            Log.d("EKF:", "RK = "+(wifi_std*wifi_std)*penaltyFactor);
            this.Rk.set(0,0, (wifi_std*wifi_std)*penaltyFactor);
            this.Rk.set(1, 1, (wifi_std*wifi_std)*penaltyFactor);
        } else {
            this.Rk.set(0,0, (gnss_std*gnss_std)*penaltyFactor);
            this.Rk.set(1, 1, (gnss_std*gnss_std)*penaltyFactor);
        }
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

    public void predict(double theta_k, double step_k, double averageStepLength, long refTime, TurnDetector.MovementType userMovementInStep) {
        if (stopEKF) return;

        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                // Update Fk based on the current state
                Log.d("EKF", "======== PREDICT ========");
                Log.d("EKF", "Predicting... "+(Math.PI/2 - theta_k)+" "+step_k);
                double adaptedHeading = wraptopi((Math.PI/2 - theta_k));
                Log.d("EKF", "Adapted bearing "+ (Math.PI/2 - theta_k)+" wrapped bearing "+wraptopi((Math.PI/2 - theta_k)));
                //double adaptedHeading = (Math.PI/2 - theta_k);
                //double adaptedHeading = theta_k;
                //if (isFirstPrediction){
                //    initialiseStateVector(adaptedHeading);
                //}

                SimpleMatrix T_mat = new SimpleMatrix(new double[][]{
                        {1,0},
                        {0, Math.sin(theta_k)},
                        {0, Math.cos(theta_k)}
                });

                SimpleMatrix control_inputs = new SimpleMatrix(new double[][]{
                        {theta_k},
                        {prevStepLength}
                });

                SimpleMatrix add = T_mat.mult(control_inputs);
                double Xk_bearing = Xk.get(0,0);
                double Xk_x = Xk.get(1,0);
                double Xk_y = Xk.get(2, 0);
                Xk.set(0,0, (Xk_bearing+add.get(0, 0)));
                Xk.set(1,0, (Xk_x+add.get(1, 0)));
                Xk.set(2,0, (Xk_y+add.get(2, 0)));

                updateFk(adaptedHeading, prevStepLength);
                updateQk(averageStepLength, adaptedHeading, (refTime-initialiseTime), getThetaStd(userMovementInStep));

                SimpleMatrix L_k = new SimpleMatrix(new double[][]{
                        {1,0},
                        {0, Math.sin(theta_k)},
                        {0, Math.cos(theta_k)}
                });

                // Predict the state vector Xk
                //Xk = Fk.mult(Xk);

                // Predict the covariance matrix Pk
                Pk = Fk.mult(Pk).mult(Fk.transpose()).plus(L_k.mult(Qk).mult(L_k.transpose()));
                Log.d("EKF", "PK = "+Pk.toString());
                Log.d("EKF", "XK after predict: "+Xk.toString());
                Log.d("EKF", "Predicted: East = "+Xk.get(1, 0)+" North = "+Xk.get(2,0));
                Log.d("EKF", "Predicted Bearing = "+Xk.get(0,0));
                Log.d("EKF", "Step length = "+prevStepLength);

                prevStepLength = step_k;
            }
        });
    }

    public void update(double[] observation_k, double theta_k, double penaltyFactor){
        Log.d("EKF", "======== UPDATE ========");
        SimpleMatrix Mk = SimpleMatrix.identity(2);

        SimpleMatrix Zk = new SimpleMatrix(new double[][]{{observation_k[0]}, {observation_k[1]}});
        Log.d("EKF", "Observation matrix = "+Zk.toString());

        updateRk(penaltyFactor);
        //updateRk();
        Log.d("EKF", "RK = " +Rk.toString());

        SimpleMatrix y_pred = Zk.minus(Hk.mult(Xk));
        SimpleMatrix Sk = Hk.mult(Pk).mult(Hk.transpose()).plus(Mk.mult(Rk).mult(Mk.transpose()));
        SimpleMatrix KalmanGain = Pk.mult(Hk.transpose().mult(Sk.invert()));

        Xk = Xk.plus(KalmanGain.mult(y_pred));
        double adaptedHeading = wraptopi(Xk.get(0, 0));
        //double adaptedHeading = wraptopi((Math.PI/2 - theta_k));
        //double adaptedHeading = (Math.PI/2 - theta_k);
        //Log.d("EKF", "XK after update: "+Xk.toString());
        Xk.set(0, 0, adaptedHeading);
        Log.d("EKF:", "Wrapping calc angle: "+adaptedHeading);

        // Creating the identity matrix of the same dimension as Pk
        SimpleMatrix I = SimpleMatrix.identity(Pk.getNumRows());
        Pk = (I.minus(KalmanGain.mult(Hk))).mult(Pk);

        Log.d("EKF", "======== FINISHED UPDATE ========");
    }

    public void onOpportunisticUpdate(double[] observe, long refTime){
        if (stopEKF) return;
        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("EKF:", "Opportunistic update...");
                Log.d("EKF:", "East: "+observe[0]+ " North: "+ observe[1] + "Timestamp: " + (refTime-initialiseTime));
                //double predictedEast= Xk.get(1, 0);
                //double predictedNorth = Xk.get(0,0);
                //double distanceBetween = Math.sqrt(Math.pow(observe[0]-predictedEast, 2) + Math.pow(observe[1] - predictedNorth, 2));
                //Log.d("EKF:", "Distance between " + distanceBetween);

                lastOpportunisticUpdate = observe;
                lastOpUpdateTime = (refTime-initialiseTime);
            }
        });
    }

    public void onStepDetected(double pdrEast, double pdrNorth, double altitude, double theta_k, double stepLength, long refTime){
        if (stopEKF) return;
        Log.d("EKF", "======== ON STEP DETECTED ========");
        Log.d("EKF:", "Last update time: " + lastOpUpdateTime + " current time: " + (refTime-initialiseTime));

        if (lastOpportunisticUpdate != null && checkRelevance((refTime-initialiseTime))){
            Log.d("EKF:", "Using observation update");
            double distanceBetween = Math.sqrt(Math.pow(lastOpportunisticUpdate[0]-pdrEast, 2) + Math.pow(lastOpportunisticUpdate[1] - pdrNorth, 2));
            if (!outlierDetector.detectOutliers(distanceBetween)) {
                Log.d("EKF", "No outlier detected");

                onObservationUpdate(lastOpportunisticUpdate[0], lastOpportunisticUpdate[1], pdrEast, pdrNorth, theta_k, altitude, calculateTimePenalty((refTime-initialiseTime)));
                return;
            }
        }
        Log.d("EKF:", "Performing recursive correction");

        performRecursiveCorrection(pdrEast, pdrNorth, theta_k, altitude, calculateTimePenalty((refTime-initialiseTime)));
        Log.d("EKF", "======== FINISHED ON STEP DETECTED ========");
    }

    private boolean checkRelevance(long refTime){
        if (!usingWifi) return true;

        long timeDifference = Math.abs(refTime - lastOpUpdateTime);

        if ((timeDifference <= relevanceThreshold)) return true;

        this.lastOpportunisticUpdate = null;
        this.lastOpUpdateTime = 0;

        return false;
    }

    // Calculates the penalty factor based on the elapsed time since the last WiFi update
    private double calculateTimePenalty(long currentTime) {
        // Calculate elapsed time since last opportunistic (e.g., WiFi) update
        long elapsedTime = currentTime - lastOpUpdateTime;
        Log.d("EKF:", "Last update time: " + lastOpUpdateTime + " current time: " + currentTime + " elapsed time: " + elapsedTime);

        // Define the maximum elapsedTime that corresponds to the maximum penalty (4.0)
        // This is an example value and should be adjusted based on your requirements

        // Calculate the penalty factor with a range from 1 to 4
        // It linearly increases based on elapsedTime, capped at maxElapsedTimeForMaxPenalty
        double penaltyFactor = 1.0 + 3.0 * Math.min(elapsedTime, maxElapsedTimeForMaxPenalty) / maxElapsedTimeForMaxPenalty;

        Log.d("EKF:", "Penalty factor: " + penaltyFactor);

        // The penaltyFactor is already ensured to be between 1 and 4, no need for Math.max
        return penaltyFactor;
    }

    // Calculates the penalty factor based on the elapsed time since the last WiFi update
    private double calculateBearingPenalty(double thetaStd, long elapsedTime) {
        double elapsedTimeMinutes = elapsedTime / 60000.0;

        // Linear or exponential penalty factor based on elapsed time
        // Example: linear growth
        return thetaStd * elapsedTimeMinutes;// Adjust the divisor to control the rate of increase
    }

    public void onObservationUpdate(double observeEast, double observeNorth, double pdrEast, double pdrNorth,
                                    double theta_k, double altitude, double penaltyFactor){
        if (stopEKF) return;
        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("EKF", "======== ON OBSERVATION UPDATE ========");

                Log.d("EKF", "Observed... X = "+observeEast+" Y = "+observeNorth);
                Log.d("EKF", "PDR... X = "+pdrEast+" Y = "+pdrNorth);
                double[] observation = new double[] {(observeEast - pdrEast), (observeNorth - pdrNorth)};

                update(observation, theta_k, penaltyFactor);
                Log.d("EKF", "XK after update: "+Xk.toString());
                Log.d("EKF", "OUTPUT: East = "+Xk.get(1, 0) + " North = "+Xk.get(2,0));

                resetOpportunisticUpdate();
                double[] startPosition = SensorFusion.getInstance().getGNSSLatLngAlt(true);
                double[] ecefRefCoords = SensorFusion.getInstance().getEcefRefCoords();
                SensorFusion.getInstance().notifyFusedUpdate(
                        CoordinateTransform.enuToGeodetic(Xk.get(1, 0), Xk.get(2,0),
                                altitude,
                                startPosition[0], startPosition[1], ecefRefCoords)
                );
                Log.d("EKF", "======== FINISHED ON OBSERVATION UPDATE ========");
            }
        });
    }

    public void performRecursiveCorrection (double pdrEast, double pdrNorth,
                                            double theta_k, double altitude, double penaltyFactor){
        if (stopEKF) return;

        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("EKF", "======== RECURSIVE CORRECTION ========");

                Log.d("EKF:", "PDR ... East = "+pdrEast+" North = "+pdrNorth);
                double predictedEast= Xk.get(1, 0);
                double predictedNorth = Xk.get(2,0);
                Log.d("EKF:", "Predicted ... East = "+predictedEast+" North = "+predictedNorth);
                Log.d("EKF:", "Observation... East = "+(predictedEast - pdrEast)+" North = "+(predictedNorth - pdrNorth));
              
                double[] observation = new double[] {(predictedEast - pdrEast), (predictedNorth - pdrNorth)};
                update(observation, theta_k, penaltyFactor);
                Log.d("EKF:", "Recursive correction output... East = "+Xk.get(1, 0)+" North = "+Xk.get(2,0));

                double[] startPosition = SensorFusion.getInstance().getGNSSLatLngAlt(true);
                double[] ecefRefCoords = SensorFusion.getInstance().getEcefRefCoords();
                SensorFusion.getInstance().notifyFusedUpdate(
                        CoordinateTransform.enuToGeodetic(Xk.get(1, 0), Xk.get(2,0),
                                altitude,
                                startPosition[0], startPosition[1], ecefRefCoords)
                );
                Log.d("EKF", "======== FINISHED RECURSIVE CORRECTION ========");
            }
        });
    }

    private double getThetaStd(TurnDetector.MovementType userMovement){
        switch (userMovement){
            case TURN:
                Log.d("EKF", "User Turning");
                return sigma_dTheta;
            case PSEUDO_TURN:
                Log.d("EKF", "Pseudo turn");
                return sigma_dPseudo;
            case STRAIGHT:
                Log.d("EKF", "Straight");
                return sigma_dStraight;
            default:
                Log.d("EKF", "Default");
                return sigma_dTheta;
        }
    }

    private static double wraptopi(double x) {
        double bearing = x;
        bearing = bearing % (2 * Math.PI); // Normalize to range 0 to 2π
        if (bearing < -Math.PI) {
            bearing += 2 * Math.PI; // Adjust if bearing is less than -π
        } else if (bearing > Math.PI) {
            bearing -= 2 * Math.PI; // Adjust if bearing is more than π
        }
        return bearing;
    }

    private void resetOpportunisticUpdate(){
        Log.d("EKF", "Resetting opportunistic update");
        lastOpportunisticUpdate = null;
        lastOpUpdateTime = (android.os.SystemClock.uptimeMillis() - initialiseTime);
        Log.d("EKF:", "Last update time: " + lastOpUpdateTime);

    }
    public void setUsingWifi(boolean update) {
        if (this.stopEKF) return;
        ekfHandler.post(new Runnable() {
            @Override
            public void run() {
                usingWifi = update;
                Log.d("EKF", "UPDATING USING WIFI"+update);
            }
        });
    }

    public void stopFusion(){
        this.stopEKF = true;
        Log.d("EKF:", "Stopping EKF handler");
        ekfThread.quitSafely();
    }
}
