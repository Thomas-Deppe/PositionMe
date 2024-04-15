package com.openpositioning.PositionMe.UserIterface;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openpositioning.PositionMe.R;
import com.openpositioning.PositionMe.Utils.ConvertVectorToBitMap;

import java.util.ArrayList;
import java.util.List;

/**
 * The TrajectoryDisplay class defines objects that represent a trajecotry plotted on the map.
 * It stores it formatting settings, data points and sets the desired display.
 *
 * The class is instantited in {@link UIelements} for each of the trajectories plotted: PDR, WIFI, GNSS, Fusion
 * As Attributes, the class stores
 * - the Polyline object with all the position points
 * - an Array of last K marker (objects)
 *
 * The class provides a number of methods that are called on the instantiated objects from a different
 * class to either show or hide the polyline or add points.
 * The class includes a smoothing function that is used for plotting the polyline.
 *
 * @author Alexandra Geciova
 * @author Tom
 * @author Chris
 */
public class TrajectoryDisplay {

    private Polyline trajectory; // this shows the pdr trajectory
    private List<Marker> markersList;
    private int numberOfPointsToSmooth = 50;
    private int numberOfMarkersDisplayed = 7;


    /**
     * Represents a Trajectory Display on a Google Map.
     * It displays a polyline trajectory and markers along the trajectory.
     *
     * @param color          The color of the polyline.
     * @param recording_map  The Google Map instance to display the trajectory on.
     * @param start_position The starting position of the trajectory.
     */
    public TrajectoryDisplay(int color, GoogleMap recording_map, LatLng start_position) {

        if (recording_map == null){return;}

        // instantiating the polylines on the map
        trajectory = recording_map.addPolyline(new PolylineOptions()
                .add(start_position)
                .color(color)
                .width(5)
                .zIndex(1)
                );
        markersList = new ArrayList<>();
    }

    /**
     * Sets the visibility of the polyline trajectory.
     *
     * @param visible True to make the trajectory visible, false otherwise.
     */
    public void setVisibility(boolean visible){
        trajectory.setVisible(visible);
    }

    /**
     * Sets the visibility of the markers along the trajectory.
     *
     * @param display True to display the markers, false to hide them.
     */
    public void displayLastKDots(boolean display) {

        for (Marker marker : markersList) {
            marker.setVisible(display);
        }
    }

    /**
     * Updates the trajectory with a new point and adjusts its visibility.
     *
     * @param point      The new point to add to the trajectory.
     * @param showLine   True to show the trajectory line, false to hide it.
     * @param smoothing  True to apply smoothing to the trajectory, false otherwise.
     */

    public void updateTrajectory (LatLng point, boolean showLine, boolean smoothing){

        if (trajectory == null) {return;}

        List<LatLng> points = trajectory.getPoints();

        // if smoothing is true
        if (!points.isEmpty() && smoothing) {
            // Get the last point in the polyline
            LatLng lastPoint = points.get(points.size() - 1);

            // Generate interpolated points
            List<LatLng> interpolatedPoints = interpolate_points(lastPoint, point);

            // Add interpolated points to the polyline
            points.addAll(interpolatedPoints);

        }

        // Add the new point and show on the map
        points.add(point);
        trajectory.setPoints(points);

        // show line (pdr, fused) or no line (wifi, gnss)
        trajectory.setVisible(showLine);
    }

    /**
     * Adjusts the trajectory to match the floor level and updates its visibility.
     *
     * @param showLine True to show the trajectory line, false to hide it.
     */
    public void adjustTrajectoryToFloor(boolean showLine) {

        // hide the polylines
        trajectory.setVisible(false);

        // reset the points for all polylines
        trajectory.setPoints(new ArrayList<LatLng>());

        // show the polylines again
        trajectory.setVisible(true);
    }

    /**
     * Interpolates points between two given points to smooth the trajectory.
     *
     * @param lastPoint The last point of the trajectory.
     * @param newPoint  The new point to be added to the trajectory.
     * @return A list of interpolated points between the last point and the new point.
     */
    private List<LatLng> interpolate_points(LatLng lastPoint, LatLng newPoint) {
        List<LatLng> interpolatedPoints = new ArrayList<>();

        double latDiff = newPoint.latitude - lastPoint.latitude;
        double lngDiff = newPoint.longitude - lastPoint.longitude;

        // Calculate step size for interpolation
        double stepLat = latDiff / (numberOfPointsToSmooth + 1);
        double stepLng = lngDiff / (numberOfPointsToSmooth + 1);

        // Start interpolation from the point following last_point
        double currentLat = lastPoint.latitude + stepLat;
        double currentLng = lastPoint.longitude + stepLng;

        // Generate intermediate points
        for (int i = 0; i < numberOfPointsToSmooth; i++) {
            interpolatedPoints.add(new LatLng(currentLat, currentLng));
            currentLat += stepLat;
            currentLng += stepLng;
        }

        return interpolatedPoints;
    }

    /**
     * Displays trajectory dots (markers) on the map.
     *
     * @param recording_map   The Google Map instance to display the markers on.
     * @param context         The application context.
     * @param dotColor        The color of the dots.
     * @param enabledDisplay  True to display the markers, false to hide them.
     */
    public void displayTrajectoryDots(GoogleMap recording_map, Context context, int dotColor, boolean enabledDisplay) {

        List<LatLng> points = trajectory.getPoints();

        if (points == null){
            return;
        }

        // create a new marker and add it to the list array
        MarkerOptions markerOptions = new MarkerOptions()
                .position(points.get(points.size() - 1))
                .icon(ConvertVectorToBitMap.convert(context, dotColor, R.drawable.ic_market_dot));

        markersList.add(recording_map.addMarker(markerOptions.anchor(0.5f, 0.5f) .visible(false)));

        // if the list array is full, remove the oldest marker
        if (markersList.size()-1 > numberOfMarkersDisplayed){
            markersList.get(0).setVisible(false);
            markersList.remove(0);
        }

        // if the display is enable, show them
        if (enabledDisplay){
            for (Marker marker : markersList){
                marker.setVisible(true);
            }
        }

    }

}
