package com.openpositioning.PositionMe;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;

/**
 * A class used to manage and track the current building. It groups all the functionality to update the floor plan ground overlays. It is closely related to the
 * {@link Buildings} class, but provides all teh methods to access the data from that class as it is not intended to be manipulated directly, as it stores static building
 * information.
 *
 * @author Thomas Deppe
 */

public class BuildingManager {

    //The map to add teh ground overlays on
    private GoogleMap recording_map;
    //Stores the ground overlay options and objects to manipulate without needing to redraw the overlay
    private GroundOverlay floorPlanDisp;
    private GroundOverlayOptions floorPlanOptions;
    //variables to track the users movement within a building
    private Buildings currentBuilding;
    private Floors currentFloor;

    /**
     * A public constructor which initialises the class to its default values. The user is assumed to start outside of a building and on the ground floor.
     *
     * @param recording_map The map on which to add the ground overlays.
     */
    public BuildingManager(GoogleMap recording_map){
        this.recording_map = recording_map;
        this.currentBuilding = Buildings.UNSPECIFIED;
        this.currentFloor = Floors.Ground;
    }

    /**
     * A helper function to add a ground overlay of a specfic floor to the map. The floor added will always correspond to the current building the user is in.
     * If a ground overlay is already added this method will remove it and add a new one. Called as teh user changes buildings.
     *
     * @param floor The floor of which to display the floor plan on the map.
     */
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

    /**
     * An overloaded method similar to the previous, but instead of displaying the floor plan of a specific floor.
     * The floor plan of the current floor is automatically displayed.
     */
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

    /**
     * Updates the ground overlay with the specified floor. It is intended to be called on changes between floors in the same building without the need to remove and readd
     * the overlay. If no ground overlay exists, addGroundOverlay is called.
     *
     * @param floor The floor which corresponding floorplan should be displayed for the current building.
     */
    public void updateGroundOverlay (Floors floor){

        if (this.floorPlanDisp != null){
            floorPlanDisp.setImage(BitmapDescriptorFactory.fromResource(getCurrentBuildingFloorPlan(floor)));

        } else {
            addGroundOverlay(floor);
        }
    }

    /**
     * An overloaded method similar to the previous, but instead of displaying the floor plan of a specific floor.
     * The floor plan of the current floor is automatically updated.
     */
    public void updateGroundOverlay (){
        if (this.floorPlanDisp != null){
            floorPlanDisp.setImage(BitmapDescriptorFactory.fromResource(getCurrentBuildingFloorPlan(currentFloor)));
        } else {
            addGroundOverlay(currentFloor);
        }
    }

    /**
     * A helper method to remove the ground overlay from the map
     */
    public void removeGroundOverlay (){
        if (this.floorPlanDisp !=null){
            floorPlanDisp.remove();
        }
    }

    /**
     * A get method used to retrieve the floor plan image of the  specified floor. {@link Buildings}.
     *
     * @param floor The floor which floor plan image is to be retrieved
     * @return The resource ID of the floor plan for the specified floor
     */
    public int getCurrentBuildingFloorPlan(Floors floor){
        return currentBuilding.getFloorPlan(floor);
    }

    /**
     * Used to check if the user has entered the boundaries of any building. Loops through all the buildings declared in {@link Buildings}.
     * If the user is in the same building as teh current building, then false is returned. It only returns true if the user enters teh bounds of a different
     * building than their current building. If the user enters a different building teh current building is updated to this building
     *
     * @param coordinate The LatLng coordinate to check
     * @return True if the user has entered a building, false otherwise
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

        return hasEnteredBuilding;
    }

    /**
     * {@link Buildings}
     * Converts the integer value of the floor to the corresponding position in the String array of available floor plans.
     * Only needs to be modified for buildings with floors lower than ground. Ground is assumed to be 0, but lower ground would be -1.
     * In the string array lower ground would be 0 and so the offset of the floors below ground is added so that it accurately maps the floor to their position in the string array.
     *
     * @param floor The floor
     * @return The index of the floor in the string array
     */
    public int convertFloorToSpinnerIndex(int floor){
        return currentBuilding.convertFloorToSpinnerIndex(floor);
    }

    /**
     * {@link Buildings}
     * Converts the index in the String array of available floor plans to the corresponding integer of the floor.
     * Only needs to be modified for buildings with floors lower than ground. Ground is assumed to be 0, but lower ground would be -1.
     * In the string array lower ground would be 0 and so the offset of the floors below ground is subtracted so that it accurately maps the floor to their position in the string array.
     *
     * @param position The index of the string array, which will be returned by the spinner when the user selects a floor.
     * @return The integer representing the floor, for the index in the string array of available floors
     */
    public int convertSpinnerIndexToFloor(int position){
        return currentBuilding.convertSpinnerIndexToFloor(position);
    }

    /**
     * A helper method to update the users floor. {@link Buildings} is used to safely convert the floor int returned by {@link PdrProcessing} to a Floors.
     * The floorplan will then be updated to this floor.
     *
     * @param floor The integer of the floor that the user is now on.
     */
    public void updateFloor(int floor){
        currentFloor = currentBuilding.convertFloorIndex(floor);
        System.out.println("Current floor "+floor+" "+currentFloor.toString());
        if (currentFloor != null){
            updateGroundOverlay(currentFloor);
        }
    }

    /**
     * A set method used to set the users current building, in case this needs to be manually done. However, should rely of checkBoundaries to do this.
     * Doing this manually could result in errors.
     *
     * @param curBuilding The new building the user is in
     */
    public void setCurrentBuilding(Buildings curBuilding){
        this.currentBuilding = curBuilding;
    }

    /**
     * A get method used to retrive the current building the user is in.
     *
     * @return The current building the user is in.
     */
    public Buildings getCurrentBuilding(){
        return currentBuilding;
    }

    /**
     * A set method that can be used incase the map is not available when initialising the building manager.
     * @param recording_map The map to add ground overlays on
     */
    public void setRecording_map(GoogleMap recording_map){
        this.recording_map = recording_map;
    }
}
