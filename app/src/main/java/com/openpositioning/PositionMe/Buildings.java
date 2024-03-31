package com.openpositioning.PositionMe;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.HashMap;
import java.util.Map;

/**
 * A class used to store all static data associated for a building. Enums are chosen for readability and compile-time checking,
 * ensuring valid usage of buildings and constants. Enum simplicity and robustness are advantageous for static entities like buildings as opposed to creating an
 * object for each building.
 *
 * @author Thomas Deppe
 */
public enum Buildings {
    UNSPECIFIED("Outdoors", null, 0, null, 0),
    LIBRARY("Noreen and Kenneth Murray Library",
            new LatLngBounds(
                    new LatLng(55.92272222222222, -3.1751805555555555),
                    new LatLng(55.92303888888889, -3.1747666666666667)), 0,
            initializeLibraryFloorplan(), R.array.floors_library),
    NUCLEUS("Nucleus",
            new LatLngBounds(
                    new LatLng(55.922780555555555, -3.174836111111111),
                    //new LatLng(55.922781, -3.174836),
                    new LatLng(55.92335555555555, -3.1738527777777774)), 1.1811f,
            initializeNucleusFloorplan(), R.array.floors_nucleus),
    FLEMING_JENKINS("Fleming Jenkins",
            new LatLngBounds(
                    new LatLng(55.92219166666666, -3.173122222222222),
                    new LatLng(55.92262777777778, -3.171638888888889)), -122.5447f,
            initializeFlemingJenkinsFloorplan(), R.array.floors_fleming),
    CORRIDOR_NUCLEUS("Nucleus Corridor",
            new LatLngBounds(
                    new LatLng(55.92283144440415, -3.1747893497322983),
                    new LatLng(55.92290963514205, -3.174606739285856)), -122.5447f,
            null, 0);

    //Buildings data
    private String name;
    private LatLngBounds bounds;
    private float rotation;
    private Map<Floors, Integer> floorplanMap;
    private int floorsArray;

    /**
     * Constructor to initialise Building with all the relevant static data.
     *
     * @param name The name of the building, displayed in the recording settings.
     * @param bounds The bounds of the building used to check if the user enters the building and to display the floor plan. These are the SW and NE coordinates,
     *               used to create a boundary rectangle.
     * @param rotation The rotation of the floor plan image, when overlayed on the map. It is in degrees from North.
     * @param floorplanMap A hash map, mapping the Floors to the to the floor plan images.
     * @param floorsArray A Int resource ID, to a String array of the available floors used in by the spinner to set the available floor plans
     */
    Buildings (String name, LatLngBounds bounds, float rotation, Map<Floors, Integer> floorplanMap, int floorsArray){
        this.name = name;
        this.bounds = bounds;
        this.rotation = rotation;
        this.floorplanMap = floorplanMap;
        this.floorsArray = floorsArray;
    }

    /**
     * A get method used to retrieve the name of the building
     *
     * @return the string of the buildings name
     */
    public String getBuildingName() { return name; }

    /**
     * A get method used to get the boundary rectangle of the building.
     *
     * @return LatLngBounds of the building.
     */
    public LatLngBounds getBuildingBounds() {
        return bounds;
    }

    /**
     * A get method used to get the overlay rotation to accurately position it on the map.
     *
     * @return a float representing teh rotation in degrees.
     */
    public float getOverlayRotation() { return rotation; }

    /**
     * A get method used to access the hash map of the buildings, that maps the floors to the floorplan image.
     *
     * @return A hash map, with the key being the floor and the value the corresponding floorplan image.
     */
    public Map<Floors, Integer> getFloorMap() { return floorplanMap; }

    /**
     * A get method used to get the resource ID of the string array listing the avaible floors for the building.
     *
     * @return resource ID of the string array of the avaible floors.
     */
    public int getFloorsArray() { return floorsArray; }

    /**
     * A get method used to retrieve the floor plan image for a specific floor by using the key in the hashMap.
     *
     * @param floor The floor which floor plan image is to be retrieved.
     * @return An int resource ID of the floor plan image for the specified floor.
     */
    public int getFloorPlan(Floors floor){
        int floorPlanID = 0;
        if (this.floorplanMap != null) {
            floorPlanID = this.floorplanMap.getOrDefault(floor, 0);
        }
        return floorPlanID;
    }

    /**
     * Converts the integer value of the floor to the corresponding position in the String array of available floor plans.
     * Only needs to be modified for buildings with floors lower than ground. Ground is assumed to be 0, but lower ground would be -1.
     * In the string array lower ground would be 0 and so the offset of the floors below ground is added so that it accurately maps the floor to their position in the string array.
     *
     * @param floor The floor
     * @return The index of the floor in the string array
     */
    public int convertFloorToSpinnerIndex(int floor){
        int convertedFloor = floor;
        if (this == Buildings.NUCLEUS) {
            convertedFloor = convertedFloor + 1;
        }
        return convertedFloor;
    }

    /**
     * Converts the index in the String array of available floor plans to the corresponding integer of the floor.
     * Only needs to be modified for buildings with floors lower than ground. Ground is assumed to be 0, but lower ground would be -1.
     * In the string array lower ground would be 0 and so the offset of the floors below ground is subtracted so that it accurately maps the floor to their position in the string array.
     *
     * @param position The index of the string array, which will be returned by the spinner when the user selects a floor.
     * @return The integer representing the floor, for the index in the string array of available floors
     */
    public int convertSpinnerIndexToFloor(int position){
        int convertedFloor = position;
        if (this == Buildings.NUCLEUS) {
            convertedFloor = convertedFloor - 1;
        }
        return convertedFloor;
    }

    /**
     * A method that promotes type safety by mapping the integer values of floors to their corresponding floor. This ensures that the app never attempts to draw
     * a floorplan of a floor that is not available.
     * @param index The integer value of the floor
     * @return The Floors enum of the corresponding floor for the index.
     */
    public Floors convertFloorIndex(int index){
        Floors updatedFloor = Floors.Ground;
        switch (this){
            case NUCLEUS:
                switch (index){
                    case -1:
                        updatedFloor = Floors.Lower_Ground;
                        break;
                    case 0:
                        updatedFloor = Floors.Ground;
                        break;
                    case 1:
                        updatedFloor = Floors.First;
                        break;
                    case 2:
                        updatedFloor = Floors.Second;
                        break;
                    case 3:
                        updatedFloor = Floors.Third;
                        break;
                    default:
                        updatedFloor = Floors.Ground;
                        break;
                }
                break;
            case LIBRARY:
                switch (index){
                    case 0:
                        updatedFloor = Floors.Ground;
                        break;
                    case 1:
                        updatedFloor = Floors.First;
                        break;
                    case 2:
                        updatedFloor = Floors.Second;
                        break;
                    case 3:
                        updatedFloor = Floors.Third;
                        break;
                    default:
                        updatedFloor = Floors.Ground;
                        break;
                }
                break;
            case FLEMING_JENKINS:
                switch (index){
                    case 0:
                        updatedFloor = Floors.Ground;
                        break;
                    case 1:
                        updatedFloor = Floors.First;
                        break;
                    default:
                        updatedFloor = Floors.Ground;
                        break;
                }
                break;
        }

        return updatedFloor;
    }

    /**
     * Creates a hash map mapping the floors to their corresponding floorplan image for the Nucleus building.
     *
     * @return A hash map of the floors mapped to their floorplans.
     */
    private static Map<Floors, Integer> initializeNucleusFloorplan() {
        Map<Floors, Integer> floorplan = new HashMap<>();
        floorplan.put(Floors.Lower_Ground, R.drawable.nucleuslg);
        floorplan.put(Floors.Ground, R.drawable.nucleusground);
        floorplan.put(Floors.First, R.drawable.nucleus1);
        floorplan.put(Floors.Second, R.drawable.nucleus2);
        floorplan.put(Floors.Third, R.drawable.nucleus3);
        return floorplan;
    }

    /**
     * Creates a hash map mapping the floors to their corresponding floorplan image for the Library building.
     *
     * @return A hash map of the floors mapped to their floorplans.
     */
    private static Map<Floors, Integer> initializeLibraryFloorplan() {
        Map<Floors, Integer> floorplan = new HashMap<>();
        floorplan.put(Floors.Ground, R.drawable.libraryg);
        floorplan.put(Floors.First, R.drawable.library1);
        floorplan.put(Floors.Second, R.drawable.library2);
        floorplan.put(Floors.Third, R.drawable.library3);
        return floorplan;
    }

    /**
     * Creates a hash map mapping the floors to their corresponding floorplan image for the Fleming Jenkins building.
     *
     * @return A hash map of the floors mapped to their floorplans.
     */
    private static Map<Floors, Integer> initializeFlemingJenkinsFloorplan() {
        Map<Floors, Integer> floorplan = new HashMap<>();
        floorplan.put(Floors.Ground, R.drawable.flemingjenkinsg);
        floorplan.put(Floors.First, R.drawable.flemingjenkins1);
        return floorplan;
    }
}

