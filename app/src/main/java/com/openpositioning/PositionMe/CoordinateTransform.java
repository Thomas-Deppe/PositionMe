package com.openpositioning.PositionMe;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public final class CoordinateTransform {

    public static final double semimajor_axis = 6378137.0;
    public static final double semiminor_axis = 6356752.31424518;
    public static final double flattening = (semimajor_axis-semiminor_axis)/semimajor_axis;
    public static final double eccentricity_squared = flattening * (2-flattening);

    private CoordinateTransform(){
    }

    public static double[] geodeticToEcef(double latitude, double longitude, double altitude){
        double[] ecefCoords = new double[3];
        Log.d("geodeticToEcef", "lat: "+latitude+" long: "+longitude+" alt: "+altitude);
        double latRad = Math.toRadians(latitude);
        double lngRad = Math.toRadians(longitude);
        Log.d("geodeticToEcef", "Rad lat: "+latRad+" Rad long: "+lngRad);

        //Calculate Prime Vertical Radius of Curvature
        double N = Math.pow(semimajor_axis,2) /
                Math.hypot((semimajor_axis*Math.cos(latRad)), (semiminor_axis * Math.sin(latRad)));

        Log.d("geodeticToEcef", "N: "+N);

        ecefCoords[0] = (N + altitude) * Math.cos(latRad) * Math.cos(lngRad);
        ecefCoords[1] = (N + altitude) * Math.cos(latRad) * Math.sin(lngRad);
        ecefCoords[2] = (N * Math.pow((semiminor_axis / semimajor_axis),2) + altitude) * Math.sin(latRad);
        Log.d("geodeticToEcef", "x: "+ecefCoords[0]+" y: "+ecefCoords[1]+" z: "+ecefCoords[2]);

        return ecefCoords;
    }

    public static double[] enuToEcef(double east, double north, double up, double refLatitude, double refLongitude, double refAlt){
        double[] calCoords = new double[3];
        double[] ecefRefCoords = geodeticToEcef(refLatitude, refLongitude, refAlt);
        double latRad = Math.toRadians(refLatitude);
        double lngRad = Math.toRadians(refLongitude);

        calCoords[0] = (Math.cos(lngRad) * (Math.cos(latRad)*up - Math.sin(latRad)*north) - Math.sin(lngRad)*east) + ecefRefCoords[0];
        calCoords[1] = (Math.sin(lngRad)*(Math.cos(latRad)*up - Math.sin(latRad)*north) + Math.cos(lngRad)*east) + ecefRefCoords[1];
        calCoords[2] = (Math.sin(latRad)*up + Math.cos(latRad)*north) + ecefRefCoords[2];

        return calCoords;
    }

    public static double[] enuToEcef(double east, double north, double up, double refLatitude, double refLongitude, double[] ecefRefCoords){
        double[] calCoords = new double[3];
        double latRad = Math.toRadians(refLatitude);
        double lngRad = Math.toRadians(refLongitude);

        calCoords[0] = (Math.cos(lngRad) * (Math.cos(latRad)*up - Math.sin(latRad)*north) - Math.sin(lngRad)*east) + ecefRefCoords[0];
        calCoords[1] = (Math.sin(lngRad)*(Math.cos(latRad)*up - Math.sin(latRad)*north) + Math.cos(lngRad)*east) + ecefRefCoords[1];
        calCoords[2] = (Math.sin(latRad)*up + Math.cos(latRad)*north) + ecefRefCoords[2];

        return calCoords;
    }

    public static LatLng ecefToGeodetic(double[] ecefCoords) {

        double asq = Math.pow(semimajor_axis,2);
        double bsq = Math.pow(semiminor_axis,2);

        double ep = Math.sqrt((asq-bsq)/bsq);

        double p = Math.sqrt(Math.pow(ecefCoords[0],2) + Math.pow(ecefCoords[1],2));

        double th = Math.atan2(semimajor_axis *
                ecefCoords[2], semiminor_axis * p);

        double longitude = Math.atan2(ecefCoords[1],ecefCoords[0]);

        double latitude = Math.atan2((ecefCoords[2] + Math.pow(ep,2) *
                        semiminor_axis * Math.pow(Math.sin(th),3)),
                (p - eccentricity_squared*semimajor_axis*Math.pow(Math.cos(th),3)));

        double N = semimajor_axis/
                (Math.sqrt(1-eccentricity_squared*
                        Math.pow(Math.sin(latitude),2)));

        double altitude = p / Math.cos(latitude) - N;
        Log.d("UserLocation", "alt: "+altitude);

        longitude = longitude % (2*Math.PI);

        return new LatLng(toDegrees(latitude), toDegrees(longitude));
    }

    public static LatLng enuToGeodetic(double east, double north, double up, double refLatitude, double refLongitude, double[] ecefRefCoords) {
        double[] ecefCoords = enuToEcef(east, north, up, refLatitude, refLongitude, ecefRefCoords);
        Log.d("ECEFCOORDS", "x: "+ecefCoords[0]+" y: "+ecefCoords[1]+" z: "+ecefCoords[2]);

        return ecefToGeodetic(ecefCoords);
    }

    public static LatLng enuToGeodetic(double east, double north, double up, double refLatitude, double refLongitude, double refAlt) {
        double[] ecefCoords = enuToEcef(east, north, up, refLatitude, refLongitude, refAlt);
        Log.d("ECEFCOORDS", "x: "+ecefCoords[0]+" y: "+ecefCoords[1]+" z: "+ecefCoords[2]);

        return ecefToGeodetic(ecefCoords);
    }

    public static double toDegrees(double val) {
        return val * (180/Math.PI);
    }

}
