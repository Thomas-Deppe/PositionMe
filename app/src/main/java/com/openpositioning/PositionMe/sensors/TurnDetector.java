package com.openpositioning.PositionMe.sensors;

import android.util.Log;

public class TurnDetector {
    private static final float TURN_THRESHOLD = 1.6f;
    private static final float PSEUDO_TURN = 0.8f;
    private float orientationPrev;
    private boolean startMonitoring;
    private MovementType userMovement;

    public enum MovementType {
        STRAIGHT,
        PSEUDO_TURN,
        TURN;

        /**
         * Compares this type with another type to determine if it should be updated
         * based on specific rules. The rules are as follows:
         * 1. If the current type is TURN, it remains unchanged.
         * 2. If the current type is PSEUDO_TURN and the new type is TURN, update to TURN.
         * 3. STRAIGHT updates to any type.
         *
         * @param newType The new type to compare with the current one.
         * @return The updated type, or the current type if no update should be made according to the rules.
         */
        public MovementType compareAndUpdate(MovementType newType) {
            if (this == TURN) {
                // If the current type is TURN, it remains unchanged.
                return this;
            } else if (this == PSEUDO_TURN && newType == TURN) {
                // Allows updating from PSEUDO_TURN to TURN.
                return TURN;
            } else if (this == PSEUDO_TURN && newType == STRAIGHT){
                // If new type is STRAIGHT do not update PSEUDO_TURN.
                return this;
            }

            // STRAIGHT updates to any type.
            return newType;
        }
    }


    public TurnDetector() {
        this.orientationPrev = 0;
        this.userMovement = MovementType.STRAIGHT;
    }

    public void ProcessOrientationData(float orientationUpdate){
        if (!this.startMonitoring) return;

        float azimuthInDegrees = (float) (Math.toDegrees(orientationUpdate) + 360) % 360;
        float azimuthChange = Math.abs(azimuthInDegrees - orientationPrev);
        //double azimuthChange = wraptopi(Math.abs(orientationUpdate - orientationPrev));
        if (azimuthChange > 180) {
            azimuthChange = 360 - azimuthChange;
        }
        Log.d("TURN_DETECTOR", "Analysing azimuth change: " + azimuthChange);

        if (azimuthChange > TURN_THRESHOLD){
            Log.d("TURN_DETECTOR", "Motion: TURN " + azimuthChange);
            userMovement.compareAndUpdate(MovementType.TURN);
        } else if (azimuthChange > PSEUDO_TURN){
            Log.d("TURN_DETECTOR", "Motion: PSEUDO " + azimuthChange);
            userMovement.compareAndUpdate(MovementType.PSEUDO_TURN);
        } else {
            Log.d("TURN_DETECTOR", "Motion: STRAIGHT" + azimuthChange);
            userMovement.compareAndUpdate(MovementType.STRAIGHT);
        }

        orientationPrev = azimuthInDegrees;
    }

    public MovementType onStepDetected(float orientation){
        MovementType resultForStep = this.userMovement;
        this.orientationPrev = orientation;
        this.userMovement = MovementType.STRAIGHT;

        return resultForStep;
    }

    public void startMonitoring(){
        this.startMonitoring = true;
    }

    public void stopMonitoring(){
        this.startMonitoring = false;
        this.userMovement = MovementType.STRAIGHT;

    }
}