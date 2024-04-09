//package com.openpositioning.PositionMe.fragments;
//
//import static android.provider.Settings.System.getString;
//
//import android.app.Dialog;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.location.Location;
//import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.os.Handler;
//import android.provider.Contacts;
//import android.view.Gravity;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.Window;
//import android.view.animation.AlphaAnimation;
//import android.view.animation.Animation;
//import android.view.animation.LinearInterpolator;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.Spinner;
//import android.widget.Switch;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.widget.ToggleButton;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.fragment.app.Fragment;
//import androidx.navigation.NavDirections;
//import androidx.navigation.Navigation;
//import androidx.preference.PreferenceManager;
//
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.BitmapDescriptorFactory;
//import com.google.android.gms.maps.model.JointType;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;
//import com.openpositioning.PositionMe.Buildings.BuildingManager;
//import com.openpositioning.PositionMe.Buildings.Buildings;
//import com.openpositioning.PositionMe.TrajectoryDisplay;
//import com.openpositioning.PositionMe.UIelements;
//import com.openpositioning.PositionMe.Utils.ConvertVectorToBitMap;
//import com.openpositioning.PositionMe.Utils.CoordinateTransform;
//import com.openpositioning.PositionMe.Buildings.Floors;
//import com.openpositioning.PositionMe.R;
//import com.openpositioning.PositionMe.SensorFusionUpdates;
//import com.openpositioning.PositionMe.sensors.SensorFusion;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * A simple {@link Fragment} subclass. The recording fragment is displayed while the app is actively
// * saving data, with some UI elements indicating current PDR status. The users path is plotted on the map and a floor plan is displayed on this map if the user enters
// * a supported building.
// *
// * @see HomeFragment the previous fragment in the nav graph.
// * @see CorrectionFragment the next fragment in the nav graph.
// * @see SensorFusion the class containing sensors and recording.
// *
// * @author Thomas Deppe
// */
//public class cordingFragment extends Fragment implements SensorFusionUpdates{
//
//    private static final int numberOfPointsToSmooth = 50;
//    //Stores the map displayed
//    private GoogleMap recording_map;
//
//    // UIelements objects
//    private UIelements uiElements;
//
//    //Button to end PDR recording
//    private Button stopButton;
//    private Button cancelButton;
//    //Loading bar to show time remaining before recording automatically ends
//    private ProgressBar timeRemaining;
//
//    private Button recordingSettings;
//
//    //App settings
//    private SharedPreferences settings;
//    //Singleton class to collect all sensor data
//    private SensorFusion sensorFusion;
//    //Timer to end recording
//    private CountDownTimer autoStop;
//    //A polling mechanism used to refresh the screen for updates
//    private Handler refreshDataHandler;
//
//    //variables to store data of the trajectory
//    private float distance;
//    private double previousPosX;
//    private double previousPosY;
//
//    //Variables to store the users starting position.
//    private double[] startPosition = new double[3];
//    private double[] ecefRefCoords = new double[3];
//
//    private float[] positionError = new float[3];
//
//    //Used to manipulate the floor plans displayed and track which building the user is in.
//    private BuildingManager buildingManager;
//    //The coordinates of the users current position
//    private LatLng currentPosition, wifiPosition, pdrPosition;
//
//    //The users current floor. The user is expected to start on the ground floor.
//    private int currentFloor = 0;
//
//    /**
//     * Public Constructor for the class.
//     * Left empty as not required
//     */
//    public cordingFragment() {
//        // Required empty public constructor
//    }
//
//    /**
//     * {@inheritDoc}
//     * Gets an instance of the {@link SensorFusion} class, and initialises the context and settings.
//     * Creates a handler for periodically updating the displayed data.
//     * It also registers this class as an observer to sensor fusion, so that it is asynchronously notified when new values are calculated
//     *
//     */
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        this.sensorFusion = SensorFusion.getInstance();
//        this.sensorFusion.registerForSensorUpdates(this);
//        Context context = getActivity();
//        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
//        this.refreshDataHandler = new Handler();
//    }
//
//    private OnMapReadyCallback rec_map_callback = new OnMapReadyCallback() {
//
//        /**
//         * Manipulates the map once available.
//         * This callback is triggered when the map is ready to be used.
//         * This is where we can add markers or lines, add listeners or move the camera.
//         * In this case, we just add a marker near Sydney, Australia.
//         * If Google Play services is not installed on the device, the user will be prompted to
//         * install it inside the SupportMapFragment. This method will only be triggered once the
//         * user has installed Google Play services and returned to the app.
//         * It initialises the building manager once the map is ready an plots the users initial location.
//         */
//        @Override
//        public void onMapReady(GoogleMap googleMap) {
//            recording_map = googleMap;
//            recording_map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//            recording_map.getUiSettings().setCompassEnabled(true);
//            recording_map.getUiSettings().setTiltGesturesEnabled(true);
//            recording_map.getUiSettings().setRotateGesturesEnabled(true);
//            recording_map.getUiSettings().setScrollGesturesEnabled(true);
//            recording_map.getUiSettings().setMyLocationButtonEnabled(true);
//            recording_map.setBuildingsEnabled(false);
//
//            // Add a marker in current GPS location and move the camera
//            startPosition = sensorFusion.getGNSSLatLngAlt(true);
//            ecefRefCoords = sensorFusion.getEcefRefCoords();
//
//            // write to data collection in sensor fusion
//            sensorFusion.startLocationWriteTextFile(startPosition);
//
//            //ecefRefCoords = CoordinateTransform.geodeticToEcef(startPosition[0],startPosition[1], startPosition[2]);
//            LatLng position = new LatLng(startPosition[0], startPosition[1]);
//            currentPosition = position;
//            pdrPosition = position;
//
//            // set to position from StartRecording Fragment
//            buildingManager = new BuildingManager(recording_map);
//
//
//            //check if the user has changed floors
//            checkBuildingBounds(currentPosition);
//
//            int calcFloor = sensorFusion.getCurrentFloor();
//            System.out.println("Current floor map ready  "+calcFloor);
//            updateFloor(calcFloor);
//        }
//    };
//
//    /**
//     * {@inheritDoc}
//     * Set title in action bar to "Recording"
//     */
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        View rootView = inflater.inflate(R.layout.fragment_recording, container, false);
//        // Inflate the layout for this fragment
//        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
//        getActivity().setTitle("Recording...");
//        return rootView;
//    }
//
//    /**
//     * {@inheritDoc}
//     * Text Views and Icons initialised to display the current PDR to the user. A Button onClick
//     * listener is enabled to detect when to go to next fragment and allow the user to correct PDR.
//     * A runnable thread is called to update the UI every 0.5 seconds.
//     * A recording settings bottom sheet dialog is created and a button onClick listener is set to determine if this dialog should be displayed.
//     */
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        // Set autoStop to null for repeat recordings
//        this.autoStop = null;
//
//        //Sets up the map
//        SupportMapFragment mapFragment =
//                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.recordingMap);
//        if (mapFragment != null) {
//            mapFragment.getMapAsync(rec_map_callback);
//        }
//
//        // constructor + instantiation of Map Ready
//        uiElements = new UIelements(recording_map, currentPosition, getContext());
//        uiElements.initialiseMapReady(currentPosition, getContext());
//
//        //Reset variables to 0
//        this.distance = 0f;
//        this.previousPosX = 0f;
//        this.previousPosY = 0f;
//
//        uiElements.initialiseViewCreated(view);
//
//        // Stop button to save trajectory and move to corrections
//        this.stopButton = getView().findViewById(R.id.stopButton);
//        this.stopButton.setOnClickListener(new View.OnClickListener() {
//            /**
//             * {@inheritDoc}
//             * OnClick listener for button to go to next fragment.
//             * When button clicked the PDR recording is stopped and the {@link CorrectionFragment} is loaded.
//             */
//            @Override
//            public void onClick(View view) {
//                if(autoStop != null) autoStop.cancel();
//                sensorFusion.stopRecording();
//                NavDirections action = RecordingFragmentDirections.actionRecordingFragmentToCorrectionFragment();
//                Navigation.findNavController(view).navigate(action);
//            }
//        });
//
//        // Cancel button to discard trajectory and return to Home
//        this.cancelButton = getView().findViewById(R.id.cancelButton);
//        this.cancelButton.setOnClickListener(new View.OnClickListener() {
//            /**
//             * {@inheritDoc}
//             * OnClick listener for button to go to home fragment.
//             * When button clicked the PDR recording is stopped and the {@link HomeFragment} is loaded.
//             * The trajectory is not saved.
//             */
//            @Override
//            public void onClick(View view) {
//                sensorFusion.stopRecording();
//                NavDirections action = RecordingFragmentDirections.actionRecordingFragmentToHomeFragment();
//                Navigation.findNavController(view).navigate(action);
//                if(autoStop != null) autoStop.cancel();
//            }
//        });
//
//        // Check if there is manually set time limit:
//        if(this.settings.getBoolean("split_trajectory", false)) {
//            // If that time limit has been reached:
//            long limit = this.settings.getInt("split_duration", 30) * 60000L;
//            // Set progress bar
//            this.timeRemaining.setMax((int) (limit/1000));
//            this.timeRemaining.setScaleY(3f);
//
//            // Create a CountDownTimer object to adhere to the time limit
//            this.autoStop = new CountDownTimer(limit, 1000) {
//                /**
//                 * {@inheritDoc}
//                 * Increment the progress bar to display progress and remaining time. Update the
//                 * observed PDR values, and animate icons based on the data.
//                 */
//                @Override
//                public void onTick(long l) {
//                    // increment progress bar
//                    timeRemaining.incrementProgressBy(1);
//                    //Update the elevation UI elements
//                    float elevationVal = sensorFusion.getElevation();
//                    updateElevationUI(elevationVal, sensorFusion.getElevator());
//
//                    //Check if the user has changed buildings
//                    checkBuildingBounds(currentPosition);
//
//                    //check if the user has changed floors
//                    int calcFloor = sensorFusion.getCurrentFloor();
//
//                    updateFloor(calcFloor);
//                }
//
//                /**
//                 * {@inheritDoc}
//                 * Finish recording and move to the correction fragment.
//                 *
//                 * @see CorrectionFragment
//                 */
//                @Override
//                public void onFinish() {
//                    // Timer done, move to next fragment automatically - will stop recording
//                    sensorFusion.removeSensorUpdate(cordingFragment.this);
//                    sensorFusion.stopRecording();
//                    NavDirections action = RecordingFragmentDirections.actionRecordingFragmentToCorrectionFragment();
//                    Navigation.findNavController(view).navigate(action);
//                }
//            }.start();
//        }
//        else {
//            // No time limit - use a repeating task to refresh UI.
//            this.refreshDataHandler.post(refreshDataTask);
//        }
//        // Display the progress of the recording when a max record length is set
//        this.timeRemaining = view.findViewById(R.id.timeRemainingBar);
//    }
//
//
//    /**
//     * A helper method to update the elevation UI components in the UI thread.
//     * @param elevationVal The current elevation value, which will be displayed on the screen
//     * @param elevator A boolean indicating whether the user is in an elevator.
//     */
//    public void updateElevationUI(float elevationVal, boolean elevator) {
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                uiElements.displayElevatorIcon(elevator, elevationVal);
//            }
//        });
//    }
//
//    /**
//     * {@link BuildingManager}
//     * A helper method to check if the user has entered or exited a building. If the user is not in any supported building then the ground overlay is removed and
//     * no floor plan is dispalyed.
//     *
//     * @param coordinate The coordinate to check if it falls in the boundaries of any supported building.
//     */
//    private void checkBuildingBounds (LatLng coordinate){
//        if (buildingManager != null){
//            if(buildingManager.checkBoundaries(coordinate)){
//                hasEnteredBuilding();
//            } else if (buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)){
//                uiElements.setCurrentBuilding(buildingManager.getCurrentBuilding().getBuildingName());
//                buildingManager.removeGroundOverlay();
//                //sensorFusion.setNoCoverage(true);
//                // no coverage
////                Toast.makeText(getActivity(), "This area has no wifi coverage", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    /**
//     * A helper method called when the user changes buildings. It is a separate method to force the floor plan to be updated in the UI thread.
//     * The floor spinner is then set up to display the floor plans available for the building the user is in.
//     */
//    private void hasEnteredBuilding(){
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (buildingManager != null){
//
//                    // set the name and the floor plan
//                    uiElements.setupFloorSpinner(buildingManager.getCurrentBuilding(), currentFloor);
//
//
//                    uiElements.setCurrentBuilding(buildingManager.getCurrentBuilding().getBuildingName());
//
//                    if (buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)) {
//                        buildingManager.removeGroundOverlay();
//                        uiElements.getNoCoverageIcon().setVisibility(View.VISIBLE);
//                        sensorFusion.setNoCoverage(true);
//                        //System.out.println("toast - no wifi coverage" + buildingManager.getCurrentBuilding());
////                        Toast.makeText(getActivity(), "This area has no wifi coverage", Toast.LENGTH_SHORT).show();
//                    }
//                    else if (buildingManager.getCurrentBuilding().equals(Buildings.CORRIDOR_NUCLEUS)){
//                        uiElements.getNoCoverageIcon().setVisibility(View.VISIBLE);
//                        //sensorFusion.setNoCoverage(true);
////                        Toast.makeText(getActivity(), "This area has no wifi coverage", Toast.LENGTH_SHORT).show();
//                    }
//                    else {
//                        uiElements.getNoCoverageIcon().setVisibility(View.GONE);
//                        sensorFusion.setNoCoverage(false);
//                        buildingManager.addGroundOverlay();
//                    }
//                }
//            }
//        });
//
//    }
//
//    /**
//     * An overridden{@link SensorFusionUpdates} method. This method is called by the sensor fusion class each time a new PDR is calculated.
//     * Ensuring that the screen is updated in the UI thread as soon as this calculation is complete. This enables a move responsive UI that updates as soon as
//     * the user takes a step.
//     *
//     * It retrieves all values from {@link SensorFusion}
//     */
//    @Override
//    public void onPDRUpdate() {
//        // Handle PDR update
//        // Update map UI on the main UI thread
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                // Get new position
//                double[] pdrValues = sensorFusion.getCurrentPDRCalc();
//                float elevationVal = sensorFusion.getElevation();
//
//                // Calculate distance travelled
//                distance += Math.sqrt(Math.pow(pdrValues[0] - previousPosX, 2) + Math.pow(pdrValues[1] - previousPosY, 2));
//
//                // display all new x,y displament and distance
//                uiElements.displayXYDistance(distance, pdrValues);
//                previousPosX = pdrValues[0];
//                previousPosY = pdrValues[1];
//
//
//                //Updates the users position and path on the map
//                if (recording_map != null) {
//                    //Transform the ENU coordinates to WSG84 coordinates google maps uses
//                    LatLng user_step = CoordinateTransform.enuToGeodetic(pdrValues[0], pdrValues[1], elevationVal, startPosition[0], startPosition[1], ecefRefCoords);
//                    pdrPosition = user_step;
////                    updateUserTrajectory(user_step);
//
//                    // display the trajectory and the points
//                    uiElements.showPDRTrajectory(pdrPosition, getContext());
//                }
//            }
//        });
//
//    }
//
//    /**
//     * An overridden {@link SensorFusionUpdates} method. That is called as soon as the device receives a new orientation update. This is updated in the UI thread,
//     * and updates the compass to show the users direction of movement in the map.
//     *
//     * {@link SensorFusion} is used to retrieve the orientation and by using this observer method the compass will more accurately mirror the users movement than polling.
//     */
//    @Override
//    public void onOrientationUpdate(){
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
////                compassIcon.setRotation((float) Math.toDegrees(sensorFusion.passOrientation()));
//                uiElements.setCompassIconRotation(sensorFusion.passOrientation());
//
//                if (recording_map != null) {
//                    uiElements.setUserMarkerRotation(sensorFusion.passOrientation());
//                }
//            }
//        });
//    }
//
//    /**
//     * An overridden {@link SensorFusionUpdates} that is called as soon as the device receives a new GNSS update. If the showGNSS button is toggled to on,
//     * then GNSS data is displayed on the screen.
//     *
//     * It retrieves all values from {@link SensorFusion}
//     */
//    @Override
//    public void onFusedUpdate(LatLng newCoordinate){
//
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                // display the new position as a line and points
//                if (recording_map == null){return;}
//
//                // display the trajectory and markers with the new point
////                updateFusedTrajectory(coordinate);
////                displayPolylineAsDots(trajectory_fused.getPoints(), Color.CYAN, fusedMarker, displayFusedToggle.isChecked());
////                fusedTrajectory.updateTrajectory(newCoordinate, displayFusedToggle.isChecked(), true);
////                fusedTrajectory.displayTrajectoryDots(recording_map, getContext(), Color.CYAN, displayFusedToggle.isChecked());
//                uiElements.showFusedTrajectory(newCoordinate, getContext());
//
//                currentPosition = newCoordinate;
//                // updates the marker
//                uiElements.updateCurrentPosition(newCoordinate);
//
//                //
//                if (uiElements.getShowPosError() != null && uiElements.getShowPosError().isChecked()){
//                    // calculate the position error
//                    calculatePosError(newCoordinate);
//                    // update the UI
//                    uiElements.updatePositionError(positionError);
//                }
//            }
//        });
//    }
//
//    private void calculatePosError(LatLng coordinate){
//        // now update the positional errors of pdr, wifi, gnss
//        // setting the positional errors --> 1st groundtruth Kalman, 2nd ground truth Particle
//        float[] distanceBetween = new float[1];
//
//        // get current GNSS reading and compare it
//        float[] GNSS_pos = sensorFusion.getGNSSLatitude(false);
//        Location.distanceBetween(GNSS_pos[0], GNSS_pos[1], coordinate.latitude, coordinate.longitude, distanceBetween);
//        positionError[2] = distanceBetween[0];
//
//        // get current PDR and compare it
//        Location.distanceBetween(pdrPosition.latitude, pdrPosition.longitude, coordinate.latitude, coordinate.longitude, distanceBetween);
//        positionError[0] = distanceBetween[0];
//
//        // get current wifi server position and compare it
//        if (wifiPosition == null) {
//            positionError[1] = 0;
//        } else {
//            Location.distanceBetween(wifiPosition.latitude, wifiPosition.longitude, coordinate.latitude, coordinate.longitude, distanceBetween);
//            positionError[1] = distanceBetween[0];
//        }
//    }
//
//    /**
//     * An overridden {@link SensorFusionUpdates} that is called as soon as the device receives a new GNSS update. If the showGNSS button is toggled to on,
//     * then GNSS data is displayed on the screen.
//     *
//     * It retrieves all values from {@link SensorFusion}
//     */
//    @Override
//    public void onGNSSUpdate(){
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (recording_map == null){return;}
//
//                float[] GNSS_pos = sensorFusion.getGNSSLatitude(false);
//                // display new point in trajectory and poitnt
////                updateGNSSTrajectory(new LatLng(GNSS_pos[0] , GNSS_pos[1]));
////                displayPolylineAsDots(trajectory_gnss.getPoints(), Color.RED, gnssmarker, displayGNSSToggle.isChecked());
////                gnssTrajectory.updateTrajectory(new LatLng(GNSS_pos[0] , GNSS_pos[1]), false, false);
////                gnssTrajectory.displayTrajectoryDots(recording_map, getContext(), Color.RED, displayGNSSToggle.isChecked());
//                uiElements.showGNSSTrajectory(GNSS_pos, getContext());
//            }
//        });
//    }
//
//    /**
//     * An overridden {@link SensorFusionUpdates} that is called as soon as the device receives a new GNSS update. If the showGNSS button is toggled to on,
//     * then GNSS data is displayed on the screen.
//     *
//     * It retrieves all values from {@link SensorFusion}
//     */
//    @Override
//    public void onWifiUpdate(LatLng latlngFromWifiServer){
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//                // display the no coverage icon
//                if (latlngFromWifiServer == null){
//                    uiElements.getNoCoverageIcon().setVisibility(View.VISIBLE);
//                    return;
//                }
//
//                wifiPosition = latlngFromWifiServer;
//                if (recording_map == null){return;}
//
//                // otherwise display the new wifi point
////                updateWifiTrajectory(latlngFromWifiServer);
////                displayPolylineAsDots(trajectory_wifi.getPoints(), Color.GREEN, wifiMarker, displayWifiToggle.isChecked());
////                wifiTrajectory.updateTrajectory(latlngFromWifiServer, false, false);
////                wifiTrajectory.displayTrajectoryDots(recording_map, getContext(), Color.GREEN, displayWifiToggle.isChecked());
//                uiElements.showWifiTrajectory(latlngFromWifiServer, getContext());
//            }
//        });
//    }
//
//
//    /**
//     * A helper function used to check if the user has changed floor, by comparing the new calculated floor from {@link com.openpositioning.PositionMe.PdrProcessing}
//     * is the same as the current floor. If it is nothing happens. If it is different then the spinner that selects the floors is set to this poistion and the
//     * floorplan is updated to the new floor the user is on.
//     *
//     * @param calcFloor The floor calculated by the {@link com.openpositioning.PositionMe.PdrProcessing}.
//     */
//    private void updateFloor(int calcFloor){
//        System.out.println("Current floor in update "+calcFloor);
//        if (currentFloor != calcFloor){
//            System.out.println("Current floor pass if "+calcFloor+" "+ currentFloor);
//            currentFloor = calcFloor;
//            if (buildingManager != null){
//                if (!buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)){
//
//                    // set new floor in the spinner
//                    uiElements.setFloorSpinnerFloorNumber(buildingManager.convertFloorToSpinnerIndex(currentFloor));
//
//                    updateFloorPlan(currentFloor);
//
//                    // remove the trajectory from the old floor
//                    uiElements.adjustUItoFloorChange();
//                }
//            }
//        }
//    }
//
//    /**
//     * A helper function that is used to update the floorplan to the floor the user is currently on. This is done in the UI thread.
//     *
//     * @param floor The new floor ths user is on.
//     */
//    private void updateFloorPlan(int floor){
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                buildingManager.updateFloor(floor);
//            }
//        });
//    }
//
//    /**
//     * Runnable task used to refresh UI elements with live data.
//     * Has to be run through a Handler object to be able to alter UI elements.
//     * It is used as a polling mechanism to refresh data that has frequent updates, such as the elevation. This is done in order to promote better
//     * resource utilisation and power consumption, as updating the screen everytime these updates occur is unnecessary.
//     */
//    private final Runnable refreshDataTask = new Runnable() {
//        @Override
//        public void run() {
//            // Update the elevation UI elements
//            float elevationVal = sensorFusion.getElevation();
//            updateElevationUI(elevationVal, sensorFusion.getElevator());
//
//            //Check if the user has changed buildings
//            checkBuildingBounds(currentPosition);
//
//            //Check if the user has changed floors
//            int calcFloor = sensorFusion.getCurrentFloor();
//
//            updateFloor(calcFloor);
//
//            // Loop the task again to keep refreshing the data
//            refreshDataHandler.postDelayed(refreshDataTask, 500);
//        }
//    };
//
//    /**
//     * {@inheritDoc}
//     * Stops ongoing refresh task, but not the countdown timer which stops automatically.
//     * Stops this class from receiving sensor fusion updates.
//     */
//    @Override
//    public void onPause() {
//        refreshDataHandler.removeCallbacks(refreshDataTask);
//        this.sensorFusion.removeSensorUpdate(this);
//        super.onPause();
//    }
//
//    /**
//     * {@inheritDoc}
//     * Restarts UI refreshing task when no countdown task is in progress
//     * Sets this class to receive sensor fusion updates
//     */
//    @Override
//    public void onResume() {
//        if(!this.settings.getBoolean("split_trajectory", false)) {
//            refreshDataHandler.postDelayed(refreshDataTask, 500);
//        }
//        this.sensorFusion.registerForSensorUpdates(this);
//        super.onResume();
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void onDestroy() {
//        this.sensorFusion.removeSensorUpdate(this);
//        super.onDestroy();
//    }
//
//    public void spinnerUpdateFloor (String selectedFloor, int position){
//
//        //Update the displayed floor plan
//        if (buildingManager != null) {
//            buildingManager.updateGroundOverlay(Floors.valueOf(selectedFloor));
//            sensorFusion.setCurrentFloor(buildingManager.convertSpinnerIndexToFloor(position));
//        }
//    }
//
//}
//
