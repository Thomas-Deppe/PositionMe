package com.openpositioning.PositionMe.sensors;

import org.apache.commons.math3.linear.*;

public class ExtendedKalmanFilter {

    private static final double q_trans = 0.01; //translation velocity variance
    private static final double q_rot = 0.01; //rotational velocity variance
    private static final double q_dist = 0.01; //rotational velocity variance

    // Error covariance of the current state
    private RealMatrix P;
    private RealVector X;
    //State transition matrix
    private RealMatrix F;

    private RealMatrix H;
    // Process error
    private final RealMatrix Q;
    // Measurement error
    private final RealMatrix R;


    public ExtendedKalmanFilter() {

        // Initial state vector
        this.X = new ArrayRealVector(new double[]{0.0, 0.0, 0.0, 0.0});

        // Initial covariance matrix
        this.P = MatrixUtils.createRealIdentityMatrix(4).scalarMultiply(0.001); // Initial uncertainty

        // Process noise covariance matrix Q
        this.Q = MatrixUtils.createRealDiagonalMatrix(new double[]{q_trans, q_rot, q_dist});

        // Measurement noise covariance matrix R
        this.R = MatrixUtils.createRealDiagonalMatrix(new double[]{1.0, 1.0});

        // Hk based on the observation model (static in this case)
        this.H = MatrixUtils.createRealMatrix(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0}
        });
    }

    public void predict(double theta, double ds) {
        // Update Fk based on the current state
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);
        F = MatrixUtils.createRealMatrix(new double[][]{
                {1, 0, cosTheta - ds * sinTheta, 0},
                {0, 1, sinTheta + ds * cosTheta, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        });

        // Predict the state vector Xk
        X = F.operate(X);

        // Predict the covariance matrix Pk
        P = F.multiply(P).multiply(F.transpose()).add(Q);
    }

    public void update(double[] latLong_data, double relX, double relY, double bearing){

    }

    private double wrapToPi (double angle){
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle <= -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
}
