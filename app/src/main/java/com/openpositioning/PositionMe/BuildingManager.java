package com.openpositioning.PositionMe;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.HashMap;
import java.util.Map;

public class BuildingManager {

    private GoogleMap recording_map;
    private GroundOverlay floorPlanDisp;
    private GroundOverlayOptions floorPlanOptions;
    private Buildings currentBuilding;
    private Floors currentFloor;

    public BuildingManager(GoogleMap recording_map){
        this.recording_map = recording_map;
        this.currentBuilding = Buildings.UNSPECIFIED;
        this.currentFloor = Floors.Ground;
    }

    public void addGroundOverlay(Floors floor){
        if (floorPlanDisp != null) {
            floorPlanDisp.remove();
        }

        floorPlanOptions = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(getCurrentBuildingFloorPlan(floor)))
                .positionFromBounds(currentBuilding.getBuildingBounds())
                .bearing(currentBuilding.getOverlayRotation()) // Optional: specify the bearing of the floorplan if needed
                .transparency(0.5f); // Optional: set transparency for the overlay

        floorPlanDisp = recording_map.addGroundOverlay(floorPlanOptions);

    }

    public void addGroundOverlay(){
        if (floorPlanDisp != null) {
            floorPlanDisp.remove();
        }

        floorPlanOptions = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(getCurrentBuildingFloorPlan(currentFloor)))
                .positionFromBounds(currentBuilding.getBuildingBounds())
                .bearing(currentBuilding.getOverlayRotation()); // Optional: specify the bearing of the floorplan if needed

        floorPlanDisp = recording_map.addGroundOverlay(floorPlanOptions);
    }

    public void updateGroundOverlay (Floors floor){

        if (this.floorPlanDisp != null){
            floorPlanDisp.setImage(BitmapDescriptorFactory.fromResource(getCurrentBuildingFloorPlan(floor)));

        } else {
            addGroundOverlay(floor);
        }
    }

    public void updateGroundOverlay (){
        if (this.floorPlanDisp != null){
            floorPlanDisp.setImage(BitmapDescriptorFactory.fromResource(getCurrentBuildingFloorPlan(currentFloor)));
        } else {
            addGroundOverlay(currentFloor);
        }
    }

    public void removeGroundOverlay (){
        if (this.floorPlanDisp !=null){
            floorPlanDisp.remove();
        }
    }

    public int getCurrentBuildingFloorPlan(Floors floor){
        return currentBuilding.getFloorPlan(floor);

        /*
        switch (currentBuilding) {
            case NUCLEUS:
                floorplanID = getNucleusFloorplan(floor);
                break;
            case LIBRARY:
                floorplanID = getLibraryFloorplan(floor);
                break;
            case FLEMING_JENKINS:
                floorplanID = getFlemingJenkinsFloorplan(floor);
                break;
        }

        return floorplanID;

         */
    }

    /*
    public int getNucleusFloorplan(Floors floor) {
        int floorplanID = 0;

        switch(floor) {
            case Lower_Ground:
                floorplanID = R.drawable.nucleuslg;
                break;
            case Ground:
                floorplanID = R.drawable.nucleusground;
                break;
            case First:
                floorplanID = R.drawable.nucleus1;
                break;
            case Second:
                floorplanID = R.drawable.nucleus2;
                break;
            case Third:
                floorplanID = R.drawable.nucleus3;
                break;
            default:
                floorplanID = R.drawable.nucleusground;
                break;
        }
        return floorplanID;
    }

    public int getLibraryFloorplan(Floors floor) {
        int floorplanID = 0;

        switch(floor) {
            case Ground:
                floorplanID = R.drawable.libraryg;
                break;
            case First:
                floorplanID = R.drawable.library1;
                break;
            case Second:
                floorplanID = R.drawable.library2;
                break;
            case Third:
                floorplanID = R.drawable.library3;
                break;
            default:
                floorplanID = R.drawable.libraryg;
                break;
        }
        return floorplanID;
    }

    public int getFlemingJenkinsFloorplan(Floors floor) {
        int floorplanID = 0;

        switch(floor) {
            case Ground:
                floorplanID = R.drawable.flemingjenkinsg;
                break;
            case First:
                floorplanID = R.drawable.flemingjenkins1;
                break;
            default:
                floorplanID = R.drawable.flemingjenkinsg;
                break;
        }
        return floorplanID;
    }

     */

    public boolean checkBoundaries(LatLng coordinate){
        boolean hasEnteredBuilding = false;
        Buildings newBuilding = Buildings.UNSPECIFIED;

        for (Buildings building : Buildings.values()){
            if (building.getBuildingBounds() != null && building.getBuildingBounds().contains(coordinate)){
                newBuilding = building;
                break;
            }
        }

        if (!currentBuilding.equals(newBuilding)){
            this.currentBuilding = newBuilding;
            hasEnteredBuilding = true;
        }

        /*
        if (Buildings.LIBRARY.getBuildingBounds().contains(coordinate)){
            if (!currentBuilding.equals(Buildings.LIBRARY)) {
                this.currentBuilding = Buildings.LIBRARY;
                hasEnteredBuilding = true;
            }
        } else if (Buildings.NUCLEUS.getBuildingBounds().contains(coordinate)){
            if (!currentBuilding.equals(Buildings.NUCLEUS)) {
                this.currentBuilding = Buildings.NUCLEUS;
                hasEnteredBuilding = true;
            }
        } else if (Buildings.FLEMING_JENKINS.getBuildingBounds().contains(coordinate)){
            if (!currentBuilding.equals(Buildings.FLEMING_JENKINS)) {
                this.currentBuilding = Buildings.FLEMING_JENKINS;
                hasEnteredBuilding = true;
            }
        } else {
            if (!currentBuilding.equals(Buildings.UNSPECIFIED)) {
                this.currentBuilding = Buildings.UNSPECIFIED;
                hasEnteredBuilding = true;
            }
        }
         */

        return hasEnteredBuilding;
    }

    public int convertFloorToSpinnerIndex(int floor){
        return currentBuilding.convertFloorToSpinnerIndex(floor);
        /*
        int convertedFloor = floor;
        switch (currentBuilding){
            case NUCLEUS:
                convertedFloor = convertedFloor+1;
        }
        return convertedFloor;
        */
    }

    public int convertSpinnerIndexToFloor(int position){
        return currentBuilding.convertSpinnerIndexToFloor(position);
        /*
        int convertedFloor = position;
        switch (currentBuilding){
            case NUCLEUS:
                convertedFloor = convertedFloor-1;
        }
        return convertedFloor;
         */
    }

    public void updateFloor(int floor){
        currentFloor = currentBuilding.convertFloorIndex(floor);
        if (currentFloor != null){
            updateGroundOverlay(currentFloor);
        }
        /*
        switch(currentBuilding){
            case NUCLEUS:
                switch (floor){
                    case -1:
                        currentFloor = Floors.Lower_Ground;
                        updateGroundOverlay(Floors.Lower_Ground);
                        break;
                    case 0:
                        currentFloor = Floors.Ground;
                        updateGroundOverlay(Floors.Ground);
                        break;
                    case 1:
                        currentFloor = Floors.First;
                        updateGroundOverlay(Floors.First);
                        break;
                    case 2:
                        currentFloor = Floors.Second;
                        updateGroundOverlay(Floors.Second);
                        break;
                    case 3:
                        currentFloor = Floors.Third;
                        updateGroundOverlay(Floors.Third);
                        break;
                    default:
                        currentFloor = Floors.Ground;
                        updateGroundOverlay(Floors.Ground);
                        break;
                }
                break;
            case LIBRARY:
                switch (floor){
                    case 0:
                        currentFloor = Floors.Ground;
                        updateGroundOverlay(Floors.Ground);
                        break;
                    case 1:
                        currentFloor = Floors.First;
                        updateGroundOverlay(Floors.First);
                        break;
                    case 2:
                        currentFloor = Floors.Second;
                        updateGroundOverlay(Floors.Second);
                        break;
                    case 3:
                        currentFloor = Floors.Third;
                        updateGroundOverlay(Floors.Third);
                        break;
                    default:
                        currentFloor = Floors.Ground;
                        updateGroundOverlay(Floors.Ground);
                        break;
                }
                break;
            case FLEMING_JENKINS:
                switch (floor){
                    case 0:
                        currentFloor = Floors.Ground;
                        updateGroundOverlay(Floors.Ground);
                        break;
                    case 1:
                        currentFloor = Floors.First;
                        updateGroundOverlay(Floors.First);
                        break;
                    default:
                        currentFloor = Floors.Ground;
                        updateGroundOverlay(Floors.Ground);
                        break;
                }
                break;
        }

         */
    }

    public void setCurrentBuilding(Buildings curBuilding){
        this.currentBuilding = curBuilding;
    }

    public Buildings getCurrentBuilding(){
        return currentBuilding;
    }

    public void setRecording_map(GoogleMap recording_map){
        this.recording_map = recording_map;
    }
}
