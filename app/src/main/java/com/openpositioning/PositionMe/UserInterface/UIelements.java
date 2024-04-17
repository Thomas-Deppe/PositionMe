package com.openpositioning.PositionMe.UserInterface;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.openpositioning.PositionMe.Buildings.Buildings;
import com.openpositioning.PositionMe.R;
import com.openpositioning.PositionMe.Utils.ConvertVectorToBitMap;
import com.openpositioning.PositionMe.fragments.HomeFragment;
import com.openpositioning.PositionMe.fragments.RecordingFragment;
import com.openpositioning.PositionMe.sensors.SensorFusion;

/**
 * The UIelements class instantiates and modifies all the UI elements shown for the {@link RecordingFragment}
 *
 * The class instantiates objects for UI elements such as  buttons, ToggleButtons, Text Views, Image Views and Markers.
 * The class provides a number of button and togglebutton listeners and display methods that are called on
 * the instantiated UIelements object from the {@link RecordingFragment} class
 *
 * @author Alexandra Geciova
 * @author Thomas Deppe
 * @author Christopher Khoo
 */
public class UIelements {

    private GoogleMap recording_map;
    private TrajectoryDisplay wifiTrajectory, pdrTrajectory, gnssTrajectory, fusedTrajectory;

    //Zoom of google maps
    private float zoom = 19f;

    //Buttons
    private Button zoomInButton, zoomOutButton, mapChangeButton, recentreButton, posTagButton;
    private ToggleButton showPosError;
    private Switch displayPRDToggle, displayWifiToggle, displayGNSSToggle, displayFusedToggle;
    private Button recordingSettings;
    private Dialog recordingSettingsDialog;

    //Recording icon to show user recording is in progress
    private ImageView recIcon;

    //Compass icon to show user direction of heading
    private ImageView compassIcon;
    private ImageView noCoverageIcon;

    // Elevator icon to show elevator usage
    private ImageView elevatorIcon;

    //Text views to display user position and elevation since beginning of recording
    private TextView positionX;
    private TextView positionY;
    private TextView elevation;
    private TextView distanceTravelled;
    private TextView currentBuilding;
    private TextView floor_title;
    private TextView errorWifi, errorPDR, errorGNSS;

    //Settings spinners that allow the user to change
    private Spinner floorSpinner;

    //Stores the markers displaying the users location - based on the fusion trajectory
    private Marker user_marker;

    // Stores LatLng of current position
    private LatLng currentPosition;

    // Recording fragment context
    private Context contextRecordFrag;


    /**
     * Initializes a new UIelements instance with the given context.
     *
     * @param context The context used to initialize the UIelements instance.
     */
    public UIelements(Context context) {
        this.contextRecordFrag = context;
    }

    /**
     * Initializes the UI elements when the map is ready.
     *
     * @param recording_map   The GoogleMap object.
     * @param startPosition   The initial position.
     */
    public void initialiseMapReady(GoogleMap recording_map, LatLng startPosition){

        this.recording_map = recording_map;
        this.currentPosition = startPosition;


        // instantiating the polylines on the map
        pdrTrajectory = new TrajectoryDisplay(Color.BLUE, recording_map, startPosition);
        wifiTrajectory = new TrajectoryDisplay(Color.GREEN, recording_map, startPosition);
        gnssTrajectory = new TrajectoryDisplay(Color.RED, recording_map, startPosition);
        fusedTrajectory = new TrajectoryDisplay(Color.CYAN, recording_map, startPosition);

        // do not display line for wifi and gnss
        wifiTrajectory.setVisibility(false);
        gnssTrajectory.setVisibility(false);

        // user Marker current
        user_marker = recording_map.addMarker(new MarkerOptions()
                .position(startPosition)
                .icon(ConvertVectorToBitMap.convert(contextRecordFrag, Color.BLACK, R.drawable.ic_baseline_navigation_24))
                .title("User Position"));
        recording_map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom));
    }

    /**
     * Initializes the UI elements when the view is created.
     *
     * @param view     The view associated with the UI elements.
     * @param activity The activity context.
     */
    public void initialiseViewCreated(@NonNull View view, Activity activity){

        //Initialise UI components
        this.positionX = view.findViewById(R.id.currentXPos);
        this.positionY = view.findViewById(R.id.currentYPos);
        this.elevation = view.findViewById(R.id.currentElevation);
        this.distanceTravelled = view.findViewById(R.id.currentDistanceTraveled);
        this.compassIcon = view.findViewById(R.id.compass);
        this.elevatorIcon = view.findViewById(R.id.elevatorImage);
        this.noCoverageIcon = view.findViewById(R.id.noCoverageIndicator);

        //Set default text of TextViews to 0
        this.positionX.setText(contextRecordFrag.getString(R.string.x, "0"));
        this.positionY.setText(contextRecordFrag.getString(R.string.y, "0"));
        this.elevation.setText(contextRecordFrag.getString(R.string.elevation, "0"));
        this.distanceTravelled.setText(contextRecordFrag.getString(R.string.distance_travelled, "0"));
        this.elevatorIcon.setVisibility(View.GONE);

        // Button for Choosing the Map Type
        this.mapChangeButton = view.findViewById(R.id.mapTypeButton);
        this.mapChangeButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to go to home fragment.
             * When button clicked the PDR recording is stopped and the {@link HomeFragment} is loaded.
             * The trajectory is not saved.
             */
            @Override
            public void onClick(View view) {
                // Alert Dialog to choose from the Map Type Options
                MapTypeAlertDialog(activity);
            }
        });


        //creates the settings dialog
        createRecordingSettingsDialog();

        this.recordingSettings = view.findViewById(R.id.settingButton);
        this.recordingSettings.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for whether to display the recording settings dialog.
             */
            @Override
            public void onClick(View view) {
                showRecordingSettings();
            }
        });


        // Button for choosing thhe current position as the reference point for the filters
        this.posTagButton = view.findViewById(R.id.position_tag_button);
        this.posTagButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to go to home fragment.
             * When button clicked the PDR recording is stopped and the {@link HomeFragment} is loaded.
             * The trajectory is not saved.
             */
            @Override
            public void onClick(View view) {
                if (currentPosition != null) {
                    SensorFusion.getInstance().addTagFusionTrajectory(currentPosition);
                }
            }
        });

        // Button for Zoom In
        this.zoomInButton = view.findViewById(R.id.zoom_in_button);
        // Button for Choosing the floor in the building
        this.zoomInButton.setOnClickListener(view1 -> setZoomInButton());

        // Button for Zoom Out
        this.zoomOutButton = view.findViewById(R.id.zoom_out_button);
        // Button for Choosing the floor in the building
        this.zoomOutButton.setOnClickListener(view12 -> setZoomOutButton());

        // Button for recentring to the current position
        this.recentreButton = view.findViewById(R.id.recentre_button);
        this.recentreButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to go to home fragment.
             * When button clicked the PDR recording is stopped and the {@link HomeFragment} is loaded.
             * The trajectory is not saved.
             */
            @Override
            public void onClick(View view) {
                // call the method that centers the view to the current geo location
                recenterMap();
            }
        });

        // Display a blinking red dot to show recording is in progress
        blinkingRecording(view);
    }


    /**
     * Sets up the floor spinner widget for selecting floors based on the current building.
     * This method initializes the spinner and configures its adapter based on the floors available in the specified building.
     * It also handles visibility changes of the spinner and its title based on the building specified.
     *
     * @param currentBuilding The building whose floors are to be displayed in the spinner.
     * @param currentFloor The currently selected floor, used to set the default selection in the spinner.
     * @param recordingFragment The fragment that contains callbacks related to spinner selection changes.
     */
    public void setupFloorSpinner(Buildings currentBuilding, int currentFloor, RecordingFragment recordingFragment) {

        // Initialize the ArrayAdapter that will hold the data for the spinner
        ArrayAdapter<CharSequence> floorAdapter;

        // Check if the current building is specified or not
        if (currentBuilding.equals(Buildings.UNSPECIFIED)){
            // If no building is specified, set the adapter to null (no floors to display)
            floorAdapter = null;
        } else {
            // If a building is specified, create an ArrayAdapter for the spinner
            floorAdapter = ArrayAdapter.createFromResource(contextRecordFrag,
                    currentBuilding.getFloorsArray(),
                    android.R.layout.simple_spinner_item);
            // Specify the layout for dropdown items
            floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        // Check if the floor adapter is not null
        if (floorAdapter != null){
            // Set the adapter for the spinner
            this.floorSpinner.setAdapter(floorAdapter);
            // Set the current floor as the selected item in the spinner
            this.floorSpinner.setSelection(currentBuilding.convertFloorToSpinnerIndex(currentFloor));
            // Make the floor title and spinner visible
            this.floor_title.setVisibility(View.VISIBLE);
            this.floorSpinner.setVisibility(View.VISIBLE);
        } else {
            // If the adapter is null, hide the floor title and spinner
            this.floor_title.setVisibility(View.GONE);
            this.floorSpinner.setVisibility(View.GONE);
        }

        // Set a new listener for the spinner item selections
        this.floorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // This method is called when a new item is selected in the spinner

                // Retrieve the selected floor as a string
                String selectedFloor = parent.getItemAtPosition(position).toString();

                // Call the method in the recordingFragment to update the floor plan based on the selected floor and its position
                recordingFragment.spinnerUpdateFloor(selectedFloor, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // This method is called when the selection disappears from this view
                // Do nothing as there's no action required here
            }
        });
    }


    /**
     * A helper method, used to create a recording bottom sheet dialog. This will be displayed at the bottom of the screen when the user presses the settings button.
     * It initialises both the floor and map type spinners. Furthermore, it implements an OnCheckedChangeListener to determine if the user has toggled the showGNSS functionality.
     */
    private void createRecordingSettingsDialog(){
        recordingSettingsDialog = new Dialog(contextRecordFrag);
        recordingSettingsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        recordingSettingsDialog.setContentView(R.layout.recording_bottom_sheet);

        recordingSettingsDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        recordingSettingsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        recordingSettingsDialog.getWindow().getAttributes().windowAnimations = R.style.BottomSheetAnimation;
        recordingSettingsDialog.getWindow().setGravity(Gravity.BOTTOM);

        // switches for displaying the trajectories
        displayPRDToggle = recordingSettingsDialog.findViewById(R.id.displayPDR);
        displayPRDToggle.setChecked(true);
        displayPRDToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (pdrTrajectory != null){
                pdrTrajectory.setVisibility(isChecked);
                pdrTrajectory.displayLastKDots(isChecked);
            }
        });

        displayWifiToggle = recordingSettingsDialog.findViewById(R.id.displayWifi);
        displayWifiToggle.setChecked(true);
        displayWifiToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (wifiTrajectory != null){
                wifiTrajectory.setVisibility(false);
                wifiTrajectory.displayLastKDots(isChecked);
            }
        });

        displayGNSSToggle = recordingSettingsDialog.findViewById(R.id.displayGNSS);
        displayGNSSToggle.setChecked(true);
        displayGNSSToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (gnssTrajectory != null){
                gnssTrajectory.setVisibility(false);
                gnssTrajectory.displayLastKDots(isChecked);
            }
        });

        displayFusedToggle = recordingSettingsDialog.findViewById(R.id.displayFused);
        displayFusedToggle.setChecked(true);
        displayFusedToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (fusedTrajectory != null){
                fusedTrajectory.setVisibility(isChecked);
                fusedTrajectory.displayLastKDots(isChecked);
            }
        });


        this.floorSpinner = recordingSettingsDialog.findViewById(R.id.floorSpinner);
        this.floorSpinner.setVisibility(View.GONE);

        this.floor_title = recordingSettingsDialog.findViewById(R.id.floor_title);
        this.currentBuilding = recordingSettingsDialog.findViewById(R.id.curBuilding);

        // setting the position error TextView
        this.errorPDR = recordingSettingsDialog.findViewById(R.id.errorParticlepdr);
        this.errorWifi = recordingSettingsDialog.findViewById(R.id.errorParticlewifi);
        this.errorGNSS = recordingSettingsDialog.findViewById(R.id.errParticlegnss);

        // turn on button to display the POSITION ERRORS
        this.showPosError = recordingSettingsDialog.findViewById(R.id.togglePosError);
        this.showPosError.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (!isChecked){
                // hide the titles
                recordingSettingsDialog.findViewById(R.id.error_title).setVisibility(View.GONE);
                recordingSettingsDialog.findViewById(R.id.error_pdr).setVisibility(View.GONE);
                recordingSettingsDialog.findViewById(R.id.error_gnss).setVisibility(View.GONE);
                recordingSettingsDialog.findViewById(R.id.error_wifi).setVisibility(View.GONE);

                // hide the printed data
                errorPDR.setVisibility(View.GONE);
                errorWifi.setVisibility(View.GONE);
                errorGNSS.setVisibility(View.GONE);

            } else {
                // show the titles
                recordingSettingsDialog.findViewById(R.id.error_title).setVisibility(View.VISIBLE);
                recordingSettingsDialog.findViewById(R.id.error_wifi).setVisibility(View.VISIBLE);
                recordingSettingsDialog.findViewById(R.id.error_gnss).setVisibility(View.VISIBLE);
                recordingSettingsDialog.findViewById(R.id.error_pdr).setVisibility(View.VISIBLE);

                // show the displayed data
                errorPDR.setVisibility(View.VISIBLE);
                errorWifi.setVisibility(View.VISIBLE);
                errorGNSS.setVisibility(View.VISIBLE);
            }
        });

    }

    /**
     * A helper method used to display the recording settings bottom sheet when a button is clicked.
     */
    private void showRecordingSettings() {
        recordingSettingsDialog.show();
    }

    /**
    * Displays a blinking red dot to signify an ongoing recording.
    *
    * @see Animation for makin the red dot blink.
    */
    private void blinkingRecording(View view) {
        //Initialise Image View
        this.recIcon = view.findViewById(R.id.redDot);
        //Configure blinking animation
        Animation blinking_rec = new AlphaAnimation(1, 0);
        blinking_rec.setDuration(800);
        blinking_rec.setInterpolator(new LinearInterpolator());
        blinking_rec.setRepeatCount(Animation.INFINITE);
        blinking_rec.setRepeatMode(Animation.REVERSE);
        recIcon.startAnimation(blinking_rec);
    }

    /**
     * Updates the display of position errors for different localization systems in the UI.
     * This method updates the text views dedicated to displaying the position error for PDR, Wi-Fi, and GNSS.
     *
     * @param positionError An array of floats representing the position errors for PDR, Wi-Fi, and GNSS respectively.
     */
    public void updatePositionError(float[] positionError){

        // Check if the contextRecordFrag is null, and return immediately if it is.
        // This check is essential to avoid NullPointerException if the context is not properly set.
        if (contextRecordFrag == null){
            return;
        }

        // Set the text of the PDR error TextView. Format the error to two decimal places and append "m" to denote meters.
        // PDR (Pedestrian Dead Reckoning) error is updated from the first element of the positionError array.
        errorPDR.setText(contextRecordFrag.getString(R.string.meter, String.format("%.2f", positionError[0])));

        // Set the text of the Wi-Fi error TextView. Similarly, format the error to two decimal places and append "m".
        // Wi-Fi error is updated from the second element of the positionError array.
        errorWifi.setText(contextRecordFrag.getString(R.string.meter, String.format("%.2f", positionError[1])));

        // Set the text of the GNSS error TextView. Format the error as done for the others and append "m".
        // GNSS (Global Navigation Satellite System) error is updated from the third element of the positionError array.
        errorGNSS.setText(contextRecordFrag.getString(R.string.meter, String.format("%.2f", positionError[2])));
    }

    /**
     * A helper method used to change the text value of the spinner into the map types google supports.
     *
     * @param selectedMapType A string indicated the map type selected by the spinner.
     */
    private void updateMapType(String selectedMapType) {
        if (recording_map != null) {
            int mapType = GoogleMap.MAP_TYPE_NORMAL;
            switch(selectedMapType){
                case "Normal":
                    mapType = GoogleMap.MAP_TYPE_NORMAL;
                    break;
                case "Satellite":
                    mapType = GoogleMap.MAP_TYPE_SATELLITE;
                    break;
                case "Terrain":
                    mapType = GoogleMap.MAP_TYPE_TERRAIN;
                    break;
                case "Hybrid":
                    mapType = GoogleMap.MAP_TYPE_HYBRID;
                    break;
            }
            recording_map.setMapType(mapType);
        }
    }

    /**
     * Defines an Alert Dialog Object for change of the Map Type
     * Redirects to changeMapType() method to update the Map Object
     *
     * @param activity The activity to attach the alert to
     */
    public void MapTypeAlertDialog(Activity activity){

        if (recording_map == null){return;}

        // AlertDialog builder instance
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        alertDialog.setTitle("Choose a Map Type:");

        // list of items to be displayed
        final String[] listItems = new String[]{"Terrain", "Hybrid", "Satellite", "Normal"};
        final int[] checkedItem = {-1};

        alertDialog.setSingleChoiceItems(listItems, checkedItem[0], (dialog, which) -> {
            // set the chosen map type to the global variable
            checkedItem[0] = which;

            // call method to update the Map Object
            updateMapType(listItems[checkedItem[0]]);

            // close the dialog alert
            dialog.dismiss();
        });

        // negative button
        alertDialog.setNegativeButton("Cancel", (dialog, which) -> {
        });

        AlertDialog customAlertDialog = alertDialog.create();
        customAlertDialog.show();
    }

    /**
     * increases the zoom and updates the Animate Camera to zoom in to the map
     * **/
    public void setZoomInButton() {
        zoom++;
        if (!(recording_map == null)) {
            recording_map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom));
        }
    }

    /**
     * decreases the zoom and updates the Animate Camera to zoom out of the map
     **/
    public void setZoomOutButton(){
        zoom--;
        if (!(recording_map == null)) {
            recording_map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom));
        }
    }

    /**
     * Updates the Animate Camera to the current location on the map
     * **/
    public void recenterMap(){
        recording_map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom ));
    }


    /**
     * Adjusts the UI elements when the floor changes. Clearing and resetting the trajectories.
     */
    public void adjustUItoFloorChange(){
        // Remove the trajectory from the old floor
        pdrTrajectory.adjustTrajectoryToFloor(true);
        wifiTrajectory.adjustTrajectoryToFloor(false);
        gnssTrajectory.adjustTrajectoryToFloor(false);
        fusedTrajectory.adjustTrajectoryToFloor(true);
    }

    /**
     * Displays the distance traveled and position coordinates.
     *
     * @param distance   The distance traveled.
     * @param pdrValues  The PDR values containing position coordinates.
     */
    public void displayXYDistance(float distance, double[] pdrValues){
        distanceTravelled.setText(contextRecordFrag.getString(R.string.distance_travelled, String.format("%.2f", distance)));
        positionX.setText(contextRecordFrag.getString(R.string.x, String.format("%.1f", pdrValues[0])));
        positionY.setText(contextRecordFrag.getString(R.string.y, String.format("%.1f", pdrValues[1])));
    }

    /**
     * Sets the rotation of the compass icon.
     *
     * @param rotation   The rotation angle.
     */
    public void setCompassIconRotation(float rotation){
        compassIcon.setRotation((float) Math.toDegrees(-rotation));
    }

    /**
     * Sets the rotation of the user marker.
     *
     * @param rotation   The rotation angle.
     */
    public void setUserMarkerRotation(float rotation){
        user_marker.setRotation((float) Math.toDegrees(rotation));
    }

    /**
     * Sets the selected floor number in the floor spinner.
     *
     * @param newFloor   The new floor number.
     */
    public void setFloorSpinnerFloorNumber(int newFloor){
        floorSpinner.setSelection(newFloor);
    }

    /**
     * Updates the current position of the user marker.
     *
     * @param newPosition   The new position coordinates.
     */
    public void updateCurrentPosition(LatLng newPosition){
        currentPosition = newPosition;
        user_marker.setPosition(newPosition);
    }

    /**
     * Sets the name of the current building.
     *
     * @param newBuildingName   The name of the new building.
     */
    public void setCurrentBuilding(String newBuildingName) {
        currentBuilding.setText(newBuildingName);
    }

    /**
     * Displays the elevator icon and elevation value.
     *
     * @param elevator       True if elevator is being used, false otherwise.
     * @param elevationVal   The elevation value.
     */
    public void displayElevatorIcon(boolean elevator, float elevationVal){
        elevation.setText(contextRecordFrag.getString(R.string.elevation, String.format("%.1f", elevationVal)));
        if(elevator) elevatorIcon.setVisibility(View.VISIBLE);
        else elevatorIcon.setVisibility(View.GONE);
    }

    /**
     * Gets the showPosError ToggleButton.
     *
     * @return The showPosError ToggleButton.
     */
    public ToggleButton getShowPosError(){
        return showPosError;
    }

    /**
     * Gets the noCoverageIcon ImageView.
     *
     * @return The noCoverageIcon ImageView.
     */
    public ImageView getNoCoverageIcon(){
        return noCoverageIcon;
    }

    /**
     * Displays the WiFi trajectory.
     *
     * @param latlngFromWifiServer   The WiFi server position coordinates.
     * @param context                The context.
     */
    public void showWifiTrajectory(LatLng latlngFromWifiServer, Context context){
        wifiTrajectory.updateTrajectory(latlngFromWifiServer, false, false);
        wifiTrajectory.displayTrajectoryDots(recording_map, context, Color.GREEN, displayWifiToggle.isChecked());
    }

    /**
     * Displays the PDR trajectory.
     *
     * @param pdrPosition   The PDR position coordinates.
     * @param context       The context.
     */
    public void showPDRTrajectory(LatLng pdrPosition, Context context){
        pdrTrajectory.updateTrajectory(pdrPosition, displayPRDToggle.isChecked(), false);
        pdrTrajectory.displayTrajectoryDots(recording_map, context, Color.BLUE, displayPRDToggle.isChecked());
    }

    /**
     * Displays the GNSS trajectory.
     *
     * @param GNSS_pos   The GNSS position coordinates.
     * @param context    The context.
     */
    public void showGNSSTrajectory(float[] GNSS_pos, Context context){
        gnssTrajectory.updateTrajectory(new LatLng(GNSS_pos[0] , GNSS_pos[1]), false, false);
        gnssTrajectory.displayTrajectoryDots(recording_map, context, Color.RED, displayGNSSToggle.isChecked());
    }

    /**
     * Displays the fused trajectory.
     *
     * @param newCoordinate   The new fused position coordinates.
     * @param context         The context.
     */
    public void showFusedTrajectory(LatLng newCoordinate, Context context){
        fusedTrajectory.updateTrajectory(newCoordinate, displayFusedToggle.isChecked(), true);
        fusedTrajectory.displayTrajectoryDots(recording_map, context, Color.CYAN, displayFusedToggle.isChecked());
    }

}