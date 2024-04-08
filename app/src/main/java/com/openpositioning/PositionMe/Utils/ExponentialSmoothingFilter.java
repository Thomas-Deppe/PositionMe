package com.openpositioning.PositionMe.Utils;

public class ExponentialSmoothingFilter {

    private final double alpha; // Smoothing factor between 0 and 1
    private Double smoothedValue = null; // Stores the last smoothed value

    /**
     * Constructor for the exponential smoothing filter.
     *
     * @param alpha The smoothing factor used, between 0 (no smoothing) and 1 (ignore all but the most recent value).
     */
    public ExponentialSmoothingFilter(double alpha) {
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("Alpha must be between 0 and 1");
        }
        this.alpha = alpha;
    }

    /**
     * Applies exponential smoothing to a new value.
     *
     * @param newValue The new value to be smoothed.
     * @return The smoothed value.
     */
    public double applySmoothing(double newValue) {
        if (smoothedValue == null) {
            // First value, just use it as is for initialization
            smoothedValue = newValue;
        } else {
            // Apply exponential smoothing formula
            smoothedValue = alpha * newValue + (1 - alpha) * smoothedValue;
        }
        return smoothedValue;
    }

    /**
     * Resets the filter, clearing the last smoothed value.
     */
    public void reset() {
        smoothedValue = null;
    }
}
