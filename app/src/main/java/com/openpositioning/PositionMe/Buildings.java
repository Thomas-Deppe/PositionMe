package com.openpositioning.PositionMe;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.HashMap;
import java.util.Map;

public enum Buildings {
    UNSPECIFIED("Unspecified Building", null, 0, null, 0),
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
            initializeFlemingJenkinsFloorplan(), R.array.floors_fleming);

    private String name;
    private LatLngBounds bounds;
    private float rotation;
    private Map<Floors, Integer> floorplanMap;
    private int floorsArray;

    Buildings (String name, LatLngBounds bounds, float rotation, Map<Floors, Integer> floorplanMap, int floorsArray){
        this.name = name;
        this.bounds = bounds;
        this.rotation = rotation;
        this.floorplanMap = floorplanMap;
        this.floorsArray = floorsArray;
    }

    public String getBuildingName() { return name; }

    public LatLngBounds getBuildingBounds() {
        return bounds;
    }

    public float getOverlayRotation() {
        return rotation;
    }

    public Map<Floors, Integer> getFloorMap() { return floorplanMap; }

    public int getFloorsArray() { return floorsArray; }

    public int getFloorPlan(Floors floor){
        int floorPlanID = 0;
        if (this.floorplanMap != null) {
            floorPlanID = this.floorplanMap.getOrDefault(floor, 0);
        }
        return floorPlanID;
    }

    public int convertFloorToSpinnerIndex(int floor){
        int convertedFloor = floor;
        if (this == Buildings.NUCLEUS) {
            convertedFloor = convertedFloor + 1;
        }
        return convertedFloor;
    }

    public int convertSpinnerIndexToFloor(int position){
        int convertedFloor = position;
        if (this == Buildings.NUCLEUS) {
            convertedFloor = convertedFloor - 1;
        }
        return convertedFloor;
    }

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

    private static Map<Floors, Integer> initializeNucleusFloorplan() {
        Map<Floors, Integer> floorplan = new HashMap<>();
        floorplan.put(Floors.Lower_Ground, R.drawable.nucleuslg);
        floorplan.put(Floors.Ground, R.drawable.nucleusground);
        floorplan.put(Floors.First, R.drawable.nucleus1);
        floorplan.put(Floors.Second, R.drawable.nucleus2);
        floorplan.put(Floors.Third, R.drawable.nucleus3);
        return floorplan;
    }

    private static Map<Floors, Integer> initializeLibraryFloorplan() {
        Map<Floors, Integer> floorplan = new HashMap<>();
        floorplan.put(Floors.Ground, R.drawable.libraryg);
        floorplan.put(Floors.First, R.drawable.library1);
        floorplan.put(Floors.Second, R.drawable.library2);
        floorplan.put(Floors.Third, R.drawable.library3);
        return floorplan;
    }

    private static Map<Floors, Integer> initializeFlemingJenkinsFloorplan() {
        Map<Floors, Integer> floorplan = new HashMap<>();
        floorplan.put(Floors.Ground, R.drawable.flemingjenkinsg);
        floorplan.put(Floors.First, R.drawable.flemingjenkins1);
        return floorplan;
    }
}

