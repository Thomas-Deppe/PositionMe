package com.openpositioning.PositionMe;

import android.content.Context;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openpositioning.PositionMe.Utils.ConvertVectorToBitMap;
import java.util.ArrayList;
import java.util.List;


public class TrajectoryDisplay {

    private Polyline trajectory; // this shows the pdr trajectory
    private List<Marker> markersList;
    private int numberOfPointsToSmooth = 50;


    public TrajectoryDisplay(int color, GoogleMap recording_map, LatLng start_position) {

        // instantiating the polylines on the map
        trajectory = recording_map.addPolyline(new PolylineOptions()
                .add(start_position)
                .color(color)
                .width(5)
                .zIndex(1)
                );
        markersList = new ArrayList<>();
    }

    public void setVisibility(boolean visible){
        trajectory.setVisible(visible);
    }

    public void displayLastKDots(boolean display) {

        for (Marker marker : markersList) {
//            System.out.println("wifi here less than 3" + markers.size());
            marker.setVisible(display);
        }
    }

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

    public void adjustTrajectoryToFloor(boolean showLine) {

        // hide the polylines
        trajectory.setVisible(false);

        // reset the points for all polylines
        trajectory.setPoints(new ArrayList<LatLng>());

        // show the polylines again
        trajectory.setVisible(true);
    }

    public void resetTrajectoryPoints() {
        // reset the points for all polylines
        trajectory.setPoints(new ArrayList<LatLng>());
    }

    // Simple smoothing for trajectory, returns a list of interpolated points between two points
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
        if (markersList.size()-1 > 7){
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
