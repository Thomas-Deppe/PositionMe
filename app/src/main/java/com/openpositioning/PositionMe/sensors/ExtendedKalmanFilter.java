package com.openpositioning.PositionMe.sensors;

import com.google.android.gms.maps.model.LatLng;
import com.openpositioning.PositionMe.CoordinateTransform;

import org.apache.commons.math3.linear.*;
import org.ejml.simple.SimpleMatrix;

public class ExtendedKalmanFilter {

    // Example standard deviations for process and measurement noise
    private double sigma_dx = 0.5; // Standard deviation for dx process noise
    private double sigma_dy = 0.5; // Standard deviation for dy process noise
    private double sigma_ds = 0.1; // Standard deviation for ds process noise
    private double sigma_dtheta = Math.toRadians(5); // Standard deviation for dθ process noise in radians

    private double sigma_x_meas = 1.0; // Standard deviation for x measurement noise
    private double sigma_y_meas = 1.0; // Standard deviation for y measurement noise

    private SimpleMatrix Fk; // State transition matrix
    private SimpleMatrix Qk; // Process noise covariance matrix
    private SimpleMatrix Hk; // Observation matrix
    private SimpleMatrix Rk; // Observation noise covariance matrix
    private SimpleMatrix Pk; // Estimate error covariance
    private SimpleMatrix Xk; // State estimate

    public ExtendedKalmanFilter() {

        // Initial state vector
        Xk = new SimpleMatrix(4, 1); // Assuming zero initial state

        // Initial covariance matrix
        this.Pk =  SimpleMatrix.diag(1, 1, 1, 1); // Initial error covariance // Initial uncertainty

        // Process noise covariance matrix Q
        this.Qk = SimpleMatrix.diag((sigma_dx*sigma_dx), (sigma_dy*sigma_dy), (sigma_ds*sigma_ds), (sigma_dtheta*sigma_dtheta));

        // Measurement noise covariance matrix R
        this.Rk = SimpleMatrix.diag((sigma_x_meas*sigma_x_meas), (sigma_y_meas*sigma_y_meas));

        // Hk based on the observation model (static in this case)
        this.Hk = new SimpleMatrix(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0}
        });
    }

    private void updateFk(double theta_k, double step_k){
        double cosTheta = Math.cos(theta_k);
        double sinTheta = Math.sin(theta_k);

        this.Fk = new SimpleMatrix(new double[][]{
                {1, 0 , cosTheta, -step_k * sinTheta},
                {0,1, sinTheta, step_k * cosTheta, 0},
                {0,0,1,0},
                {0,0,0,1}
        });
    }

    public void predict(double theta_k, double step_k) {
        // Update Fk based on the current state
        updateFk(theta_k, step_k);

        // Predict the state vector Xk
        this.Xk = Fk.mult(Xk);

        // Predict the covariance matrix Pk
        this.Pk = Fk.mult(Pk).mult(Fk.transpose()).plus(Qk);
        System.out.println("XK after predict: "+Xk.toString());
    }

    public void update(double[] observation_k){
        SimpleMatrix Zk = new SimpleMatrix(new double[][]{{observation_k[0]}, {observation_k[1]}});

        SimpleMatrix y_pred = Zk.minus(Hk.mult(Xk));
        SimpleMatrix Sk = Hk.mult(Pk).mult(Hk.transpose()).plus(Rk);
        SimpleMatrix KalmanGain = Pk.mult(Hk.transpose().mult(Sk.invert()));

        Xk = Xk.plus(KalmanGain.mult(y_pred));
        Pk = Pk.minus(KalmanGain.mult(Hk).mult(Pk));
    }

    public LatLng onObservationUpdate(double observe_x, double observe_y, double PDR_x, double PDR_y, double altitude){
        double[] observation = new double[] {(observe_x - PDR_x), (observe_y - PDR_y)};

        update(observation);

        System.out.println("XK after update: "+Xk.toString());
        double[] startPosition = SensorFusion.getInstance().getGNSSLatLngAlt(true);
        double[] ecefRefCoords = SensorFusion.getInstance().getEcefRefCoords();
        return CoordinateTransform.enuToGeodetic(Xk.get(0, 0), Xk.get(1,0), altitude, startPosition[0], startPosition[1], ecefRefCoords);
    }
}
