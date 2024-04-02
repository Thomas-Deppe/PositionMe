package com.openpositioning.PositionMe.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openpositioning.PositionMe.BuildingManager;
import com.openpositioning.PositionMe.Buildings;
import com.openpositioning.PositionMe.CoordinateTransform;
import com.openpositioning.PositionMe.Floors;
import com.openpositioning.PositionMe.R;
import com.openpositioning.PositionMe.SensorFusionUpdates;
import com.openpositioning.PositionMe.sensors.SensorFusion;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass. The recording fragment is displayed while the app is actively
 * saving data, with some UI elements indicating current PDR status. The users path is plotted on the map and a floor plan is displayed on this map if the user enters
 * a supported building.
 *
 * @see HomeFragment the previous fragment in the nav graph.
 * @see CorrectionFragment the next fragment in the nav graph.
 * @see SensorFusion the class containing sensors and recording.
 *
 * @author Thomas Deppe
 */
public class RecordingFragment extends Fragment implements SensorFusionUpdates{

    //Stores the map displayed
    private GoogleMap recording_map;
    //Stores the polyline of the users path so that it can be updated without the need to redraw the entire polyline
    private Polyline user_trajectory; // this shows the pdr trajectory
    private Polyline trajectory_wifi;
    private Polyline trajectory_particle;
    private Polyline trajectory_gnss;
    private Polyline trajectory_kalman;

    private List<Marker> pdrMarker = new ArrayList<>(),
            wifiMarker = new ArrayList<>(),
            gnssmarker = new ArrayList<>(),
            particleMarker = new ArrayList<>(),
            kalmanMarker = new ArrayList<>();

    //Zoom of google maps
    private float zoom = 19f;
    //Button to end PDR recording
    private Button stopButton;
    private Button cancelButton;
    private Button zoomInButton, zoomOutButton, mapChangeButton, recentreButton, posTagButton;

    private ToggleButton showGNSS;
    private Switch displayPRDToggle, displayWifiToggle, displayGNSSToggle, displayKalmanToggle, displayParticleToggle;
    private Button recordingSettings;
    private Dialog recordingSettingsDialog;
    //Recording icon to show user recording is in progress
    private ImageView recIcon;
    //Compass icon to show user direction of heading
    private ImageView compassIcon;
    // Elevator icon to show elevator usage
    private ImageView elevatorIcon;
    //Loading bar to show time remaining before recording automatically ends
    private ProgressBar timeRemaining;
    //Text views to display user position and elevation since beginning of recording
    private TextView positionX;
    private TextView positionY;
    private TextView elevation;
    private TextView distanceTravelled;
    private TextView currentBuilding;
    private TextView floor_title;
    private TextView errorWifi, errorPDR, errorGNSS;
    //Settings spinners that allow the user to change
//    private Spinner mapTypeSpinner;
    private Spinner floorSpinner;

    //App settings
    private SharedPreferences settings;
    //Singleton class to collect all sensor data
    private SensorFusion sensorFusion;
    //Timer to end recording
    private CountDownTimer autoStop;
    //A polling mechanism used to refresh the screen for updates
    private Handler refreshDataHandler;

    //variables to store data of the trajectory
    private float distance;
    private double previousPosX;
    private double previousPosY;

    //Variables to store the users starting position.
    private double[] startPosition = new double[3];
    private double[] ecefRefCoords = new double[3];

    private float[] poserrorPDR = new float[2];
    private float[] poserrorWifi = new float[2];
    private float[] poserrorGNSS = new float[2];

    //Used to manipulate the floor plans displayed and track which building the user is in.
    private BuildingManager buildingManager;
    //The coordinates of the users current position
    private LatLng currentPosition, wifiPosition, pdrPosition;

    //Stores the markers displaying the users location and GNSS position so they can be manipulated without removing and re-adding them
    private Marker GNSS_marker;
    private Marker user_marker; // set to fusion


    //The users current floor. The user is expected to start on the ground floor.
    private int currentFloor = 0;

    /**
     * Public Constructor for the class.
     * Left empty as not required
     */
    public RecordingFragment() {
        // Required empty public constructor
    }

    /**
     * {@inheritDoc}
     * Gets an instance of the {@link SensorFusion} class, and initialises the context and settings.
     * Creates a handler for periodically updating the displayed data.
     * It also registers this class as an observer to sensor fusion, so that it is asynchronously notified when new values are calculated
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.sensorFusion = SensorFusion.getInstance();
        this.sensorFusion.registerForSensorUpdates(this);
        Context context = getActivity();
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.refreshDataHandler = new Handler();
    }

    private OnMapReadyCallback rec_map_callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         * It initialises the building manager once the map is ready an plots the users initial location.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            recording_map = googleMap;
            recording_map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//            mapTypeSpinner.setSelection(0);
            recording_map.getUiSettings().setCompassEnabled(true);
            recording_map.getUiSettings().setTiltGesturesEnabled(true);
            recording_map.getUiSettings().setRotateGesturesEnabled(true);
            recording_map.getUiSettings().setScrollGesturesEnabled(true);
            recording_map.getUiSettings().setMyLocationButtonEnabled(true);
            recording_map.setBuildingsEnabled(false);

            // Add a marker in current GPS location and move the camera
            startPosition = sensorFusion.getGNSSLatLngAlt(true);
            ecefRefCoords = sensorFusion.getEcefRefCoords();
            //ecefRefCoords = CoordinateTransform.geodeticToEcef(startPosition[0],startPosition[1], startPosition[2]);
            LatLng position = new LatLng(startPosition[0], startPosition[1]);

            //LatLng position = new LatLng(55.922431222785264, -3.1724382435880134);
            user_marker = recording_map.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(convertVectorToBitmap(getContext(), Color.BLACK, R.drawable.ic_baseline_navigation_24))
                    .title("User Position"));
            recording_map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom ));

            // instantiating the polylines on the map
            user_trajectory = recording_map.addPolyline(new PolylineOptions().add(position));
            trajectory_gnss = recording_map.addPolyline(new PolylineOptions().add(position));
            trajectory_wifi = recording_map.addPolyline(new PolylineOptions().add(position));
            trajectory_particle = recording_map.addPolyline(new PolylineOptions().add(position));
            trajectory_kalman = recording_map.addPolyline(new PolylineOptions().add(position));

            // setting different colour of the polylines
            user_trajectory.setColor(Color.BLUE);
            trajectory_wifi.setColor(Color.GREEN);
            trajectory_gnss.setColor(Color.RED);
            trajectory_particle.setColor(Color.YELLOW);
            trajectory_kalman.setColor(Color.CYAN);
            int x= 5;
            user_trajectory.setWidth(x);
            trajectory_gnss.setWidth(x);
            trajectory_wifi.setWidth(x);
            trajectory_kalman.setWidth(x);
            trajectory_particle.setWidth(x);

            // display on the complete top
            user_trajectory.setZIndex(1);
            trajectory_gnss.setZIndex(1);
            trajectory_wifi.setZIndex(1);
            trajectory_kalman.setZIndex(1);
            trajectory_particle.setZIndex(1);

            // initialise filters
            //sensorFusion.initialiseFusionAlgorithm(startPosition[0], startPosition[1], sensorFusion.getElevation());
            System.out.println("starting location "+startPosition);

            // set to position from StartRecording Fragment
            buildingManager = new BuildingManager(recording_map);
            currentPosition = position;
            pdrPosition = position;
            checkBuildingBounds(currentPosition);
            //check if the user has changed floors
            int calcFloor = sensorFusion.getCurrentFloor();
            System.out.println("Current floor map ready  "+calcFloor);
            updateFloor(calcFloor);
        }
    };

    /**
     * {@inheritDoc}
     * Set title in action bar to "Recording"
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_recording, container, false);
        // Inflate the layout for this fragment
        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
        getActivity().setTitle("Recording...");
        return rootView;
    }

    /**
     * {@inheritDoc}
     * Text Views and Icons initialised to display the current PDR to the user. A Button onClick
     * listener is enabled to detect when to go to next fragment and allow the user to correct PDR.
     * A runnable thread is called to update the UI every 0.5 seconds.
     * A recording settings bottom sheet dialog is created and a button onClick listener is set to determine if this dialog should be displayed.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set autoStop to null for repeat recordings
        this.autoStop = null;

        //Sets up the map
        SupportMapFragment mapFragment =
                 (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.recordingMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(rec_map_callback);
        }

        //Initialise UI components
        this.positionX = getView().findViewById(R.id.currentXPos);
        this.positionY = getView().findViewById(R.id.currentYPos);
        this.elevation = getView().findViewById(R.id.currentElevation);
        this.distanceTravelled = getView().findViewById(R.id.currentDistanceTraveled);
        this.compassIcon = getView().findViewById(R.id.compass);
        this.elevatorIcon = getView().findViewById(R.id.elevatorImage);

        //Set default text of TextViews to 0
        this.positionX.setText(getString(R.string.x, "0"));
        this.positionY.setText(getString(R.string.y, "0"));
        this.elevation.setText(getString(R.string.elevation, "0"));
        this.distanceTravelled.setText(getString(R.string.distance_travelled, "0"));
        this.elevatorIcon.setVisibility(View.GONE);

        //Reset variables to 0
        this.distance = 0f;
        this.previousPosX = 0f;
        this.previousPosY = 0f;

        //creates the settings dialog
        createRecordingSettingsDialog();

        this.recordingSettings = getView().findViewById(R.id.settingButton);
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

        // Stop button to save trajectory and move to corrections
        this.stopButton = getView().findViewById(R.id.stopButton);
        this.stopButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to go to next fragment.
             * When button clicked the PDR recording is stopped and the {@link CorrectionFragment} is loaded.
             */
            @Override
            public void onClick(View view) {
                if(autoStop != null) autoStop.cancel();
                sensorFusion.stopRecording();
                NavDirections action = RecordingFragmentDirections.actionRecordingFragmentToCorrectionFragment();
                Navigation.findNavController(view).navigate(action);
            }
        });

        // Cancel button to discard trajectory and return to Home
        this.cancelButton = getView().findViewById(R.id.cancelButton);
        this.cancelButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to go to home fragment.
             * When button clicked the PDR recording is stopped and the {@link HomeFragment} is loaded.
             * The trajectory is not saved.
             */
            @Override
            public void onClick(View view) {
                sensorFusion.stopRecording();
                NavDirections action = RecordingFragmentDirections.actionRecordingFragmentToHomeFragment();
                Navigation.findNavController(view).navigate(action);
                if(autoStop != null) autoStop.cancel();
            }
        });

        // Button for Choosing the Map Type
        this.mapChangeButton = getView().findViewById(R.id.mapTypeButton);
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
                MapTypeAlertDialog();
            }
        });

        // Button for Zoom In
        this.zoomInButton = getView().findViewById(R.id.zoom_in_button);
        this.zoomInButton.setOnClickListener(new View.OnClickListener() {
            // Button for Choosing the floor in the building
            @Override
            public void onClick(View view) {
                setZoomInButton();
            }
        });

        // Button for Zoom Out
        this.zoomOutButton = getView().findViewById(R.id.zoom_out_button);
        this.zoomOutButton.setOnClickListener(new View.OnClickListener() {
            // Button for Choosing the floor in the building
            @Override
            public void onClick(View view) {
                setZoomOutButton();
            }
        });

        // Button for recentring to the current position
        this.recentreButton = getView().findViewById(R.id.recentre_button);
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


        // Button for choosing thhe current position as the reference point for the filters
        this.posTagButton = getView().findViewById(R.id.position_tag_button);
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
                    sensorFusion.addFusionTagTraj(currentPosition);
                }
            }
        });

        // Display the progress of the recording when a max record length is set
        this.timeRemaining = getView().findViewById(R.id.timeRemainingBar);

        // Display a blinking red dot to show recording is in progress
        blinkingRecording();

        // Check if there is manually set time limit:
        if(this.settings.getBoolean("split_trajectory", false)) {
            // If that time limit has been reached:
            long limit = this.settings.getInt("split_duration", 30) * 60000L;
            // Set progress bar
            this.timeRemaining.setMax((int) (limit/1000));
            this.timeRemaining.setScaleY(3f);

            // Create a CountDownTimer object to adhere to the time limit
            this.autoStop = new CountDownTimer(limit, 1000) {
                /**
                 * {@inheritDoc}
                 * Increment the progress bar to display progress and remaining time. Update the
                 * observed PDR values, and animate icons based on the data.
                 */
                @Override
                public void onTick(long l) {
                    // increment progress bar
                    timeRemaining.incrementProgressBy(1);
                    //Update the elevation UI elements
                    float elevationVal = sensorFusion.getElevation();
                    updateElevationUI(elevationVal, sensorFusion.getElevator());

                    //Check if the user has changed buildings
                    checkBuildingBounds(currentPosition);

                    //check if the user has changed floors
                    int calcFloor = sensorFusion.getCurrentFloor();

                    updateFloor(calcFloor);
                }

                /**
                 * {@inheritDoc}
                 * Finish recording and move to the correction fragment.
                 *
                 * @see CorrectionFragment
                 */
                @Override
                public void onFinish() {
                    // Timer done, move to next fragment automatically - will stop recording
                    sensorFusion.removeSensorUpdate(RecordingFragment.this);
                    sensorFusion.stopRecording();
                    NavDirections action = RecordingFragmentDirections.actionRecordingFragmentToCorrectionFragment();
                    Navigation.findNavController(view).navigate(action);
                }
            }.start();
        }
        else {
            // No time limit - use a repeating task to refresh UI.
            this.refreshDataHandler.post(refreshDataTask);
       }
    }

    /**
     * {@link BuildingManager}
     * A helper method to check if the user has entered or exited a building. If the user is not in any supported building then the ground overlay is removed and
     * no floor plan is dispalyed.
     *
     * @param coordinate The coordinate to check if it falls in the boundaries of any supported building.
     */
    private void checkBuildingBounds (LatLng coordinate){
        if (buildingManager != null){
            if(buildingManager.checkBoundaries(coordinate)){
                hasEnteredBuilding();
            } else if (buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)){
                currentBuilding.setText(buildingManager.getCurrentBuilding().getBuildingName());
                buildingManager.removeGroundOverlay();
                // no coverage
//                Toast.makeText(getActivity(), "This area has no wifi coverage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * A helper method called when the user changes buildings. It is a separate method to force the floor plan to be updated in the UI thread.
     * The floor spinner is then set up to display the floor plans available for the building the user is in.
     */
    private void hasEnteredBuilding(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (buildingManager != null){
                    setupFloorSpinner();
                    currentBuilding.setText(buildingManager.getCurrentBuilding().getBuildingName());
                    if (buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)) {
                        buildingManager.removeGroundOverlay();
                        System.out.println("toast - no wifi coverage" + buildingManager.getCurrentBuilding());
//                        Toast.makeText(getActivity(), "This area has no wifi coverage", Toast.LENGTH_SHORT).show();
                    }
                    else if (buildingManager.getCurrentBuilding().equals(Buildings.CORRIDOR_NUCLEUS)){
//                        Toast.makeText(getActivity(), "This area has no wifi coverage", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        buildingManager.addGroundOverlay();
                    }
                }
            }
        });

    }

    /**
     * An overridden{@link SensorFusionUpdates} method. This method is called by the sensor fusion class each time a new PDR is calculated.
     * Ensuring that the screen is updated in the UI thread as soon as this calculation is complete. This enables a move responsive UI that updates as soon as
     * the user takes a step.
     *
     * It retrieves all values from {@link SensorFusion}
     */
    @Override
    public void onPDRUpdate() {
        // Handle PDR update
        // Update map UI on the main UI thread
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Get new position
                double[] pdrValues = sensorFusion.getCurrentPDRCalc();
                float elevationVal = sensorFusion.getElevation();
                positionX.setText(getString(R.string.x, String.format("%.1f", pdrValues[0])));
                positionY.setText(getString(R.string.y, String.format("%.1f", pdrValues[1])));

                //Updates the users position and path on the map
                if (recording_map != null) {
                    //Transform the ENU coordinates to WSG84 coordinates google maps uses
                    LatLng user_step = CoordinateTransform.enuToGeodetic(pdrValues[0], pdrValues[1], elevationVal, startPosition[0], startPosition[1], ecefRefCoords);
                    pdrPosition = user_step;
                    updateUserTrajectory(user_step);
                    displayPolylineAsDots(user_trajectory.getPoints(), Color.BLUE, pdrMarker, displayPRDToggle.isChecked());
//                    user_marker.setPosition(user_step);
                }

                // Calculate distance travelled
                distance += Math.sqrt(Math.pow(pdrValues[0] - previousPosX, 2) + Math.pow(pdrValues[1] - previousPosY, 2));
                distanceTravelled.setText(getString(R.string.distance_travelled, String.format("%.2f", distance)));
                previousPosX = pdrValues[0];
                previousPosY = pdrValues[1];
            }
        });

    }

    /**
     * An overridden {@link SensorFusionUpdates} method. That is called as soon as the device receives a new orientation update. This is updated in the UI thread,
     * and updates the compass to show the users direction of movement in the map.
     *
     * {@link SensorFusion} is used to retrieve the orientation and by using this observer method the compass will more accurately mirror the users movement than polling.
     */
    @Override
    public void onOrientationUpdate(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                compassIcon.setRotation((float) Math.toDegrees(sensorFusion.passOrientation()));
                if (recording_map != null) {
                    user_marker.setRotation((float) Math.toDegrees(sensorFusion.passOrientation()));
                }
            }
        });
    }

    /**
     * An overridden {@link SensorFusionUpdates} that is called as soon as the device receives a new GNSS update. If the showGNSS button is toggled to on,
     * then GNSS data is displayed on the screen.
     *
     * It retrieves all values from {@link SensorFusion}
     */
    @Override
    public void onParticleUpdate(LatLng particleAlgPosition){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recording_map == null){return;}
                if (particleAlgPosition != null && trajectory_particle != null) {
                    updateParticleTrajectory(particleAlgPosition);
                    displayPolylineAsDots(trajectory_particle.getPoints(), Color.YELLOW, particleMarker, displayParticleToggle.isChecked());

                    // todo: set the marker to this trajectory as ground truth
                    if (!sensorFusion.getEnableFusionAlgorithms()){
                        currentPosition = particleAlgPosition;
                    }

                    if (showGNSS != null && showGNSS.isChecked()) {
                        // now update the positional errors of pdr, wifi, gnss
                        // setting the positional errors --> 1st groundtruth Kalman, 2nd ground truth Particle
                        float[] distanceBetween = new float[1];

                        // get current GNSS reading and compare it
                        float[] GNSS_pos = sensorFusion.getGNSSLatitude(false);
                        Location.distanceBetween(GNSS_pos[0], GNSS_pos[1], particleAlgPosition.latitude, particleAlgPosition.longitude, distanceBetween);
                        poserrorGNSS[0] = distanceBetween[0];

                        // get current PDR and compare it
                        Location.distanceBetween(pdrPosition.latitude, pdrPosition.longitude, particleAlgPosition.latitude, particleAlgPosition.longitude, distanceBetween);
                        poserrorPDR[0] = distanceBetween[0];

                        // get current wifi server position and compare it
                        if (wifiPosition == null) {
                            poserrorWifi[0] = 0;
                        } else {
                            Location.distanceBetween(wifiPosition.latitude, wifiPosition.longitude, particleAlgPosition.latitude, particleAlgPosition.longitude, distanceBetween);
                            poserrorWifi[0] = distanceBetween[0];
                        }
                        // update the UI
                        updatePositionError();
                    }
                }
            }
        });
    }


    /**
     * An overridden {@link SensorFusionUpdates} that is called as soon as the device receives a new GNSS update. If the showGNSS button is toggled to on,
     * then GNSS data is displayed on the screen.
     *
     * It retrieves all values from {@link SensorFusion}
     */
    @Override
    public void onKalmanUpdate(LatLng kalmanAlgPosition){

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // display the new position as a line and points
                if (recording_map == null){return;}

                updateKalmanTrajectory(kalmanAlgPosition);
                displayPolylineAsDots(trajectory_kalman.getPoints(), Color.CYAN, kalmanMarker, displayKalmanToggle.isChecked());


                // if kalman filter is selected to run
                if (sensorFusion.getEnableFusionAlgorithms()){
                    currentPosition = kalmanAlgPosition;
                    user_marker.setPosition(kalmanAlgPosition);
                }
                else{return;}

                if (showGNSS != null && showGNSS.isChecked()){
                    // now update the positional errors of pdr, wifi, gnss
                    // setting the positional errors --> 1st groundtruth Kalman, 2nd ground truth Particle
                    float[] distanceBetween = new float[1];

                    if (sensorFusion.getEnableFusionAlgorithms()) {
                        // get current GNSS reading and compare it
                        float[] GNSS_pos = sensorFusion.getGNSSLatitude(false);
                        Location.distanceBetween(GNSS_pos[0], GNSS_pos[1], kalmanAlgPosition.latitude, kalmanAlgPosition.longitude, distanceBetween);
                        poserrorGNSS[0] = distanceBetween[0];

                        // get current PDR and compare it
                        Location.distanceBetween(pdrPosition.latitude, pdrPosition.longitude, kalmanAlgPosition.latitude, kalmanAlgPosition.longitude, distanceBetween);
                        poserrorPDR[0] = distanceBetween[0];

                        // get current wifi server position and compare it
                        if (wifiPosition == null) {
                            poserrorWifi[1] = 0;
                        } else {
                            Location.distanceBetween(wifiPosition.latitude, wifiPosition.longitude, kalmanAlgPosition.latitude, kalmanAlgPosition.longitude, distanceBetween);
                            poserrorWifi[0] = distanceBetween[0];
                        }

                        // update the UI
                        updatePositionError();
                    }
                }
            }
        });
    }

    private void updatePositionError(){
        errorWifi.setText(getString(R.string.meter, String.format("%.2f", poserrorPDR[0])));
        errorGNSS.setText(getString(R.string.meter, String.format("%.2f", poserrorGNSS[0])));
        errorPDR.setText(getString(R.string.meter, String.format("%.2f",  poserrorPDR[0])));

    }


    /**
     * An overridden {@link SensorFusionUpdates} that is called as soon as the device receives a new GNSS update. If the showGNSS button is toggled to on,
     * then GNSS data is displayed on the screen.
     *
     * It retrieves all values from {@link SensorFusion}
     */
    @Override
    public void onGNSSUpdate(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recording_map == null){return;}
                if (showGNSS!= null && showGNSS.isChecked()) {
                    updateGNSSInfo();
                }
                float[] GNSS_pos = sensorFusion.getGNSSLatitude(false);
                updateGNSSTrajectory(new LatLng(GNSS_pos[0] , GNSS_pos[1]));
                displayPolylineAsDots(trajectory_gnss.getPoints(), Color.RED, gnssmarker, displayGNSSToggle.isChecked());
            }
        });
    }
    /**
     * If the show GNSS button is toggle to on then this method will retrieve the GNSS coordinates and update the map with a marker showing the new GNSS position.
     * The accuracy of the GNSS position is then displayed in the settings along with the positioning error of the GNSS location relative to the calculated PDR location.
     */
    public void updateGNSSInfo(){
        float[] GNSS_pos = sensorFusion.getGNSSLatitude(false);
        float GNNS_accuracy = sensorFusion.getGNSSAccuracy();
        if (GNSS_marker == null){
            GNSS_marker = recording_map.addMarker(new MarkerOptions()
                    .position(new LatLng(GNSS_pos[0],GNSS_pos[1]))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("Current GNSS Position"));
        } else {
            GNSS_marker.setPosition(new LatLng(GNSS_pos[0],GNSS_pos[1]));
        }
    }

    /**
     * A helper method to update the elevation UI components in the UI thread.
     * @param elevationVal The current elevation value, which will be displayed on the screen
     * @param elevator A boolean indicating whether the user is in an elevator.
     */
    public void updateElevationUI(float elevationVal, boolean elevator) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                elevation.setText(getString(R.string.elevation, String.format("%.1f", elevationVal)));
                if(elevator) elevatorIcon.setVisibility(View.VISIBLE);
                else elevatorIcon.setVisibility(View.GONE);
            }
        });
    }

    /**
     * An overridden {@link SensorFusionUpdates} that is called as soon as the device receives a new GNSS update. If the showGNSS button is toggled to on,
     * then GNSS data is displayed on the screen.
     *
     * It retrieves all values from {@link SensorFusion}
     */
    @Override
    public void onWifiUpdate(LatLng latlngFromWifiServer){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                wifiPosition = latlngFromWifiServer;
                if (recording_map == null){return;}
                updateWifiTrajectory(latlngFromWifiServer);
                displayPolylineAsDots(trajectory_wifi.getPoints(), Color.GREEN, wifiMarker, displayWifiToggle.isChecked());
            }
        });
    }


    /**
     * A helper function to update the path drawn on the screen with a new position, without the need to redraw teh entire path.
     *
     * @param point The new coordinate to add to the users path.
     */
    private void updateUserTrajectory (LatLng point){
        if (user_trajectory != null) {
            List<LatLng> points = user_trajectory.getPoints();
            points.add(point);
            user_trajectory.setPoints(points);
        }
    }

    private void updateWifiTrajectory (LatLng point){
        if (trajectory_wifi != null) {
            List<LatLng> points = trajectory_wifi.getPoints();
            points.add(point);
            trajectory_wifi.setPoints(points);
        }
    }

    private void updateGNSSTrajectory (LatLng point){
        if (trajectory_gnss != null) {
            List<LatLng> points = trajectory_gnss.getPoints();
            points.add(point);
            trajectory_gnss.setPoints(points);
        }
    }

    private void updateParticleTrajectory (LatLng point){
        if (trajectory_particle != null) {
            List<LatLng> points = trajectory_particle.getPoints();
            points.add(point);
            trajectory_particle.setPoints(points);
        }
    }

    private void updateKalmanTrajectory (LatLng point){
        if (trajectory_kalman != null) {
            List<LatLng> points = trajectory_kalman.getPoints();
            points.add(point);
            trajectory_kalman.setPoints(points);
        }
    }

    // Simple smoothing for trajectory, returns a list of interpolated points between two points
    private List<LatLng> interpolate_points(LatLng lastPoint, LatLng newPoint, int numPoints) {
        List<LatLng> interpolatedPoints = new ArrayList<>();

        double latDiff = newPoint.latitude - lastPoint.latitude;
        double lngDiff = newPoint.longitude - lastPoint.longitude;

        // Calculate step size for interpolation
        double stepLat = latDiff / (numPoints + 1);
        double stepLng = lngDiff / (numPoints + 1);

        // Start interpolation from the point following last_point
        double currentLat = lastPoint.latitude + stepLat;
        double currentLng = lastPoint.longitude + stepLng;

        // Generate intermediate points
        for (int i = 0; i < numPoints; i++) {
            interpolatedPoints.add(new LatLng(currentLat, currentLng));
            currentLat += stepLat;
            currentLng += stepLng;
        }

        return interpolatedPoints;
    }

    private void displayPolylineAsDots(List<LatLng> points, int dotColor, List<Marker> listOfMarkers, boolean enabledDisplay) {

        if (points == null){
            return;
        }

        // create a new marker and add it to the list array
        MarkerOptions markerOptions = new MarkerOptions()
                    .position(points.get(points.size() - 1))
                    .icon(convertVectorToBitmap(this.getContext(), dotColor, R.drawable.ic_market_dot));
        listOfMarkers.add(recording_map.addMarker(markerOptions.anchor(0.5f, 0.5f) .visible(false)));

        // if the list array is almost full, remove the oldest marker
        if (listOfMarkers.size()-1 > 5){
            listOfMarkers.get(0).setVisible(false);
            listOfMarkers.remove(0);
        }

        // if the display is enable, show them
        if (enabledDisplay){
            for (Marker marker : listOfMarkers){
                marker.setVisible(true);
            }
            System.out.println("display points " + dotColor + " " + " " + listOfMarkers.size());
        }

    }


    public void displayLastNDots(List<Marker> markers, boolean onOroff){
        for (Marker marker : markers) {
            System.out.println("wifi here less than 3" + markers.size());
            marker.setVisible(onOroff);
        }
    }


    /**
     * A helper function used to check if the user has changed floor, by comparing the new calculated floor from {@link com.openpositioning.PositionMe.PdrProcessing}
     * is the same as the current floor. If it is nothing happens. If it is different then the spinner that selects the floors is set to this poistion and the
     * floorplan is updated to the new floor the user is on.
     *
     * @param calcFloor The floor calculated by the {@link com.openpositioning.PositionMe.PdrProcessing}.
     */
    private void updateFloor(int calcFloor){
        System.out.println("Current floor in update "+calcFloor);
        if (currentFloor != calcFloor){
            System.out.println("Current floor pass if "+calcFloor+" "+ currentFloor);
            currentFloor = calcFloor;
            if (buildingManager != null){
                if (!buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)){
                    floorSpinner.setSelection(buildingManager.convertFloorToSpinnerIndex(currentFloor));
                    updateFloorPlan(currentFloor);
                    // remove the trjectory from the old floor
                    adjustPolylineToFloor();
                }
            }
        }
    }

    /**
     * A helper function that is used to update the floorplan to the floor the user is currently on. This is done in the UI thread.
     *
     * @param floor The new floor ths user is on.
     */
    private void updateFloorPlan(int floor){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buildingManager.updateFloor(floor);
            }
        });
    }

    /**
     * Runnable task used to refresh UI elements with live data.
     * Has to be run through a Handler object to be able to alter UI elements.
     * It is used as a polling mechnasim to refresh data that has frequent updates, such as the elevation. This is done in order to promote better
     * resource utilisation and power consumption, as updating the screen everytime these updates occur is unnecessary.
     */
    private final Runnable refreshDataTask = new Runnable() {
        @Override
        public void run() {
            // Update the elevation UI elements
            float elevationVal = sensorFusion.getElevation();
            updateElevationUI(elevationVal, sensorFusion.getElevator());

            //Check if the user has changed buildings
            checkBuildingBounds(currentPosition);

            //Check if the user has changed floors
            int calcFloor = sensorFusion.getCurrentFloor();

            updateFloor(calcFloor);

            // Loop the task again to keep refreshing the data
            refreshDataHandler.postDelayed(refreshDataTask, 500);
        }
    };

    /**
     * Displays a blinking red dot to signify an ongoing recording.
     *
     * @see Animation for makin the red dot blink.
     */
    private void blinkingRecording() {
        //Initialise Image View
        this.recIcon = getView().findViewById(R.id.redDot);
        //Configure blinking animation
        Animation blinking_rec = new AlphaAnimation(1, 0);
        blinking_rec.setDuration(800);
        blinking_rec.setInterpolator(new LinearInterpolator());
        blinking_rec.setRepeatCount(Animation.INFINITE);
        blinking_rec.setRepeatMode(Animation.REVERSE);
        recIcon.startAnimation(blinking_rec);
    }

    /**
     * {@inheritDoc}
     * Stops ongoing refresh task, but not the countdown timer which stops automatically.
     * Stops this class from receiving sensor fusion updates.
     */
    @Override
    public void onPause() {
        refreshDataHandler.removeCallbacks(refreshDataTask);
        this.sensorFusion.removeSensorUpdate(this);
        super.onPause();
    }

    /**
     * {@inheritDoc}
     * Restarts UI refreshing task when no countdown task is in progress
     * Sets this class to receive sensor fusion updates
     */
    @Override
    public void onResume() {
        if(!this.settings.getBoolean("split_trajectory", false)) {
            refreshDataHandler.postDelayed(refreshDataTask, 500);
        }
        this.sensorFusion.registerForSensorUpdates(this);
        super.onResume();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        this.sensorFusion.removeSensorUpdate(this);
        super.onDestroy();
    }

    /**
     * A helper method to set up the floor spinner that allows the user to select floors. This method is called when the user changes building
     * to ensure that this spinner reflects teh available floor plans of the building. It sets an OnItemSelectedListener to monitor if the user selects a new floor.
     */
    private void setupFloorSpinner() {
        ArrayAdapter<CharSequence> floorAdapter = null;
        if (buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)){
            floorAdapter = null;
        } else {
            floorAdapter = ArrayAdapter.createFromResource(requireContext(),
                    buildingManager.getCurrentBuilding().getFloorsArray(),
                    android.R.layout.simple_spinner_item);
            floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        if (floorAdapter != null){
            this.floorSpinner.setAdapter(floorAdapter);
            this.floorSpinner.setSelection(buildingManager.convertFloorToSpinnerIndex(currentFloor));
            this.floor_title.setVisibility(View.VISIBLE);
            this.floorSpinner.setVisibility(View.VISIBLE);
        } else {
            this.floor_title.setVisibility(View.GONE);
            this.floorSpinner.setVisibility(View.GONE);
        }


        // Set a listener to handle Spinner item selection
        this.floorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Handle item selection
                String selectedFloor = parent.getItemAtPosition(position).toString();

                //Update the displayed floor plan
                if (buildingManager != null) {
                    buildingManager.updateGroundOverlay(Floors.valueOf(selectedFloor));
                    sensorFusion.setCurrentFloor(buildingManager.convertSpinnerIndexToFloor(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    /**
     * A helper method to set up the map type spinner that allows the user to choose between different map types. It sets an OnItemSelectedListener to monitor if the user
     * selects a new map type and updates the displayed map accordingly.
     */
//    private void mapTypeSpinnerInitialisation() {
//        // Set up Spinner functionality as needed
//        ArrayAdapter<CharSequence> mapTypeAdapter = ArrayAdapter.createFromResource(requireContext(),
//                R.array.map_type_options, android.R.layout.simple_spinner_item);
//        mapTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        this.mapTypeSpinner.setAdapter(mapTypeAdapter);
//
//        // Set a listener to handle Spinner item selection
//        this.mapTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                // Handle item selection
//                String selectedMapType = parent.getItemAtPosition(position).toString();
//                // Perform actions based on selected map type
//                updateMapType(selectedMapType);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Do nothing
//            }
//        });
//    }

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
     * A helper method, used to create a recording bottom sheet dialog. This will be displayed at the bottom of the screen when the user presses the settings button.
     * It initialises both the floor and map type spinners. Furthermore, it implements an OnCheckedChangeListener to determine if the user has toggled the showGNSS functionality.
     */
    private void createRecordingSettingsDialog(){
        recordingSettingsDialog = new Dialog(requireContext());
        recordingSettingsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        recordingSettingsDialog.setContentView(R.layout.recording_bottom_sheet);

        recordingSettingsDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        recordingSettingsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        recordingSettingsDialog.getWindow().getAttributes().windowAnimations = R.style.BottomSheetAnimation;
        recordingSettingsDialog.getWindow().setGravity(Gravity.BOTTOM);

//        this.mapTypeSpinner = recordingSettingsDialog.findViewById(R.id.mapTypeSpinner);
        //this.mapTypeSpinner = getView().findViewById(R.id.mapTypeSpinner);
//        mapTypeSpinnerInitialisation();

        // switches for displaying the trajectories
        displayPRDToggle = recordingSettingsDialog.findViewById(R.id.displayPDR);
        displayPRDToggle.setChecked(true);
        displayPRDToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    if (user_trajectory != null){
                        user_trajectory.setVisible(true);
                        displayLastNDots(pdrMarker, true);
                    }
                }
                else{
                    if (user_trajectory != null){
                        user_trajectory.setVisible(false);
                        displayLastNDots(pdrMarker, false);
                    }
                }
            }
        });

        displayWifiToggle = recordingSettingsDialog.findViewById(R.id.displayWifi);
        displayWifiToggle.setChecked(true);
        displayWifiToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    if (trajectory_wifi != null){
                        trajectory_wifi.setVisible(true);
                        displayLastNDots(wifiMarker, true);
                    }
                }
                else{
                    if (trajectory_wifi != null){
                        trajectory_wifi.setVisible(false);
                        displayLastNDots(wifiMarker, false);
                    }
                }
            }
        });

        displayGNSSToggle = recordingSettingsDialog.findViewById(R.id.displayGNSS);
        displayGNSSToggle.setChecked(true);
        displayGNSSToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    if (trajectory_gnss != null){
                        trajectory_gnss.setVisible(true);
                        displayLastNDots(gnssmarker, true);
                    }
                }
                else{
                    if (trajectory_gnss != null){
                        trajectory_gnss.setVisible(false);
                        displayLastNDots(gnssmarker, false);
                    }
                }
            }
        });

        displayKalmanToggle = recordingSettingsDialog.findViewById(R.id.displayKalman);
        displayKalmanToggle.setChecked(true);
        displayKalmanToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    if (trajectory_kalman != null){
                        trajectory_kalman.setVisible(true);
                        displayLastNDots(kalmanMarker, true);
                    }
                }
                else{
                    if (trajectory_kalman != null){
                        trajectory_kalman.setVisible(false);
                        displayLastNDots(kalmanMarker, false);
                    }
                }
            }
        });

        displayParticleToggle = recordingSettingsDialog.findViewById(R.id.displayParticle);
        displayParticleToggle.setChecked(true);
        displayParticleToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    if (trajectory_particle != null){
                        trajectory_particle.setVisible(true);
                        displayLastNDots(particleMarker, true);
                    }
                }
                else{
                    if (trajectory_particle != null){
                        trajectory_particle.setVisible(false);
                        displayLastNDots(particleMarker, false);
                    }
                }
            }
        });

        if (sensorFusion.getEnableFusionAlgorithms()){
            displayParticleToggle.setVisibility(View.GONE);
        }
        else {
            displayKalmanToggle.setVisibility(View.GONE);
        }

        
        this.floorSpinner = recordingSettingsDialog.findViewById(R.id.floorSpinner);
        this.floorSpinner.setVisibility(View.GONE);

        this.floor_title = recordingSettingsDialog.findViewById(R.id.floor_title);
        this.currentBuilding = recordingSettingsDialog.findViewById(R.id.curBuilding);

        // setting the position error TextView
        this.errorPDR = recordingSettingsDialog.findViewById(R.id.errorParticlepdr);
        this.errorWifi = recordingSettingsDialog.findViewById(R.id.errorParticlewifi);
        this.errorGNSS = recordingSettingsDialog.findViewById(R.id.errParticlegnss);

        // turn on button to display the POSITION ERRORS
        this.showGNSS = recordingSettingsDialog.findViewById(R.id.toggleGNSS);
        this.showGNSS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (!isChecked){
                    if (GNSS_marker != null){
                        GNSS_marker.remove();
                    }
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

                    updateGNSSInfo();
                }
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
     * increases the zoom and updates the Animate Camera to zoom in to the map
     * @var positionCurr, zoom
     * **/
    public void setZoomInButton() {
        zoom++;
        if (!(recording_map == null)) {
            recording_map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom));
        }
    }

    /**
     * decreases the zoom and updates the Animate Camera to zoom out of the map
     * @var positionCurr, zoom
     * **/
    public void setZoomOutButton(){
        zoom--;
        if (!(recording_map == null)) {
            recording_map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom));
        }
    }


    /**
     * Updates the Animate Camera to the current location on the map
     * @var positionCurr, zoom
     * **/
    public void recenterMap(){
        recording_map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom ));
        return;
    }

    /**
     * Defines an Alert Dialog Object for change of the Map Type
     * Redirects to changeMapType() method to update the Map Object
     * @var currentMapType - int
     */
    public void MapTypeAlertDialog(){

        if (recording_map == null){return;}

        // AlertDialog builder instance
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("Choose a Map Type:");

        // list of items to be displayed
        final String[] listItems = new String[]{"Terrain", "Hybrid", "Satellite", "Normal"};
        final int[] checkedItem = {-1};

        alertDialog.setSingleChoiceItems(listItems, checkedItem[0], (dialog, which) -> {
            // set the chosen map type to the global variable
            checkedItem[0] = which;
            System.out.println("new map" + listItems[checkedItem[0]]);

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

    public BitmapDescriptor convertVectorToBitmap(Context context, int Color, int drawable_id){

        Drawable vectorDrawable = ContextCompat.getDrawable(context, drawable_id);
        if (Color != -1) {
            vectorDrawable.setColorFilter(Color, PorterDuff.Mode.SRC_IN);
        }
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void adjustPolylineToFloor(){
        if (recording_map == null){return;}

        // hide all polylines
        user_trajectory.setVisible(false);
        trajectory_wifi.setVisible(false);
        trajectory_gnss.setVisible(false);
        trajectory_kalman.setVisible(false);
        trajectory_particle.setVisible(false);

        // reset the points for all polylines
        user_trajectory.setPoints(new ArrayList<LatLng>());
        trajectory_wifi.setPoints(new ArrayList<LatLng>());
        trajectory_wifi.setPoints(new ArrayList<LatLng>());
        trajectory_kalman.setPoints(new ArrayList<LatLng>());
        trajectory_particle.setPoints(new ArrayList<LatLng>());

        // show all polylines again
        user_trajectory.setVisible(true);
        trajectory_wifi.setVisible(true);
        trajectory_gnss.setVisible(true);
        trajectory_kalman.setVisible(true);
        trajectory_particle.setVisible(false);
    }

}

