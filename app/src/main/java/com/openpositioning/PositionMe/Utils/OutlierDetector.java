package com.openpositioning.PositionMe.Utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for detecting outliers in a list of distances.
 */
public class OutlierDetector {
    // Threshold for outlier detection
    private static final double OUTLIER_THRESHOLD = 2.8;
    // Factor for computing modified Z-score
    private static final double Z_SCORE_FACTOR = 0.6745;
    private static final double max_distance_threshold = 10;
    // List to store distances
    private final List<Double> distances;

    /**
     * Constructor to initialize the outlier detector.
     */
    public OutlierDetector() {
        this.distances = new ArrayList<>();
    }

    /**
     * Detects outliers in the provided distance.
     *
     * @param newDistance The new distance to check for outliers.
     * @return True if an outlier is detected, false otherwise.
     */
    public boolean detectOutliers(double newDistance) {
        // Add the new distance to the list
        distances.add(newDistance);

        if (newDistance > max_distance_threshold) {
            Log.d("EKF", "Outlier detected: "+newDistance);
            return true;
        }

        // Calculate the median of distances
        double median = calculateMedian();

        // Calculate the Median Absolute Deviation (MAD)
        double mad = calculateMAD(median);
        Log.d("DETECT_OUTLIERS", "Median = " + median + " MAD = " + mad);

        // Calculate the modified Z-score
        double modifiedZScore = Z_SCORE_FACTOR * ((Math.abs(newDistance - median)) / mad);

        // Check if the modified Z-score exceeds the outlier threshold
        if (modifiedZScore > OUTLIER_THRESHOLD) {
            Log.d("EKF", "Outlier detected: " + newDistance);
            // Remove the outlier from the list
            int index = distances.indexOf(newDistance);
            distances.remove(index);
            return true;
        }

        return false;
    }

    /**
     * Calculates the median of distances.
     *
     * @return The median value.
     */
    private double calculateMedian() {
        // Sort distances in order to determine median
        Collections.sort(this.distances);

        // calculate median based on length of list
        int size = this.distances.size();
        if (size % 2 != 0) {
            return distances.get(size / 2);
        } else {
            return (distances.get((size - 1) / 2) + distances.get(size / 2)) / 2.0;
        }
    }

    /**
     * Calculates the Median Absolute Deviation (MAD) of distances.
     *
     * @param median The median value of distances.
     * @return The MAD value.
     */
    private double calculateMAD(double median) {
        List<Double> absoluteDeviations = new ArrayList<>();
        for (double distance : distances) {
            double deviation = Math.abs(distance - median);
            absoluteDeviations.add(deviation);
        }
        return calculateMedian(absoluteDeviations);
    }

    /**
     * Calculates the median of a list of absolute deviations.
     *
     * @param absoluteDeviations The list of absolute deviations.
     * @return The median of absolute deviations.
     */
    private double calculateMedian(List<Double> absoluteDeviations) {
        Collections.sort(absoluteDeviations);
        int size = absoluteDeviations.size();
        if (size % 2 != 0) {
            return absoluteDeviations.get(size / 2);
        } else {
            return (absoluteDeviations.get((size - 1) / 2) + absoluteDeviations.get(size / 2)) / 2.0;
        }
    }
}
