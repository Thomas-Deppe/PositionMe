package com.openpositioning.PositionMe;

import android.util.Log;

import com.openpositioning.PositionMe.sensors.Wifi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OutlierDetector {
    private static final double outlier_threshold = 3.0;
    private static final double zScoreFactor = 0.6745;

    List<Double> distances;

    public OutlierDetector() {
        this.distances = new ArrayList<>();
    }

    public boolean detectOutliers(double newDistance) {
        distances.add(newDistance);
        double median = calculateMedian();
        double MAD = calculateMAD(median);
        Log.d("DETECT_OUTLIERS", "Median = "+median+" MAD = "+MAD);

        double modifiedZscore = zScoreFactor*((Math.abs(newDistance - median)) / MAD);

        if (modifiedZscore > outlier_threshold){
            Log.d("EKF", "Outlier detected: "+newDistance);
            int index = distances.indexOf(newDistance);
            distances.remove(index);
            return true;
        }

        return false;
    }

    private double calculateMedian() {
        Collections.sort(this.distances);
        int size = this.distances.size();
        if (size % 2 != 0) {
            return distances.get(size/2);
        } else {
            return (distances.get((size - 1) / 2) + distances.get(size / 2)) / 2.0;
        }
    }

    private double calculateMedian(List<Double> absoluteDeviations) {
        List<Double> listToCalc = new ArrayList<>(absoluteDeviations);
        Collections.sort(listToCalc);
        int size = listToCalc.size();
        if (size % 2 != 0) {
            return listToCalc.get(size/2);
        } else {
            return (listToCalc.get((size - 1) / 2) + listToCalc.get(size / 2)) / 2.0;
        }
    }

    private double calculateMAD(double median) {
        List<Double> absoluteDeviations = new ArrayList<>(distances);
        for (int i = 0; i < distances.size(); i++) {
            double deviation = Math.abs(distances.get(i) - median);
            absoluteDeviations.set(i, deviation);
        }
        return calculateMedian(absoluteDeviations);
    }
}
