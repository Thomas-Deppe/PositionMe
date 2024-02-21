package com.openpositioning.PositionMe.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
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
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
import com.openpositioning.PositionMe.sensors.SensorTypes;

import java.util.List;

/**
 * A simple {@link Fragment} subclass. The recording fragment is displayed while the app is actively
 * saving data, with some UI elements indicating current PDR status.
 *
 * @see HomeFragment the previous fragment in the nav graph.
 * @see CorrectionFragment the next fragment in the nav graph.
 * @see SensorFusion the class containing sensors and recording.
 *
 * @author Thomas Deppe
 */
public class RecordingFragment extends Fragment implements SensorFusionUpdates{

    private GoogleMap recording_map;
    private Polyline user_trajectory;
    //Zoom of google maps
    private float zoom = 19f;
    //Button to end PDR recording
    private Button stopButton;
    private Button cancelButton;
    private ToggleButton showGNSS;
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
    private TextView accuracy;
    private TextView positioning_error;
    private Spinner mapTypeSpinner;
    private Spinner floorSpinner;

    //App settings
    private SharedPreferences settings;
    //Singleton class to collect all sensor data
    private SensorFusion sensorFusion;
    //Timer to end recording
    private CountDownTimer autoStop;
    //?
    private Handler refreshDataHandler;

    //variables to store data of the trajectory
    private float distance;
    private double previousPosX;
    private double previousPosY;

    private double[] startPosition = new double[3];
    private double[] ecefRefCoords = new double[3];

    private BuildingManager buildingManager;

    private LatLng currentPosition;

    private Marker GNSS_marker;
    private Marker user_marker;

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
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            recording_map = googleMap;
            recording_map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mapTypeSpinner.setSelection(0);
            recording_map.getUiSettings().setCompassEnabled(true);
            recording_map.getUiSettings().setTiltGesturesEnabled(true);
            recording_map.getUiSettings().setRotateGesturesEnabled(true);
            recording_map.getUiSettings().setScrollGesturesEnabled(true);
            recording_map.getUiSettings().setMyLocationButtonEnabled(true);

            // Add a marker in current GPS location and move the camera
            startPosition = sensorFusion.getGNSSLatLngAlt(true);
            ecefRefCoords = CoordinateTransform.geodeticToEcef(startPosition[0],startPosition[1], startPosition[2]);
            LatLng position = new LatLng(startPosition[0], startPosition[1]);
            //LatLng position = new LatLng(55.922431222785264, -3.1724382435880134);
            //PinConfig.Builder pinConfigBuilder = PinConfig.builder();
            //pinConfigBuilder.setGlyph(new PinConfig.Glyph(BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_directions_walk_24)));
            //PinConfig pinConfig = pinConfigBuilder.build();
            //user_marker = googleMap.addMarker(new AdvancedMarkerOptions()
            //)
            user_marker = recording_map.addMarker(new MarkerOptions()
                    .position(position)
                    .title("User Position"));
            recording_map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom ));
            user_trajectory = recording_map.addPolyline(new PolylineOptions().add(position));
            buildingManager = new BuildingManager(recording_map);
            currentPosition = position;
            checkBuildingBounds(currentPosition);
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
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set autoStop to null for repeat recordings
        this.autoStop = null;

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

        createRecordingSettingsDialog();

        this.recordingSettings = getView().findViewById(R.id.settingButton);
        this.recordingSettings.setOnClickListener(new View.OnClickListener() {
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
                    // Get new position
                    /*
                    float[] pdrValues = sensorFusion.getSensorValueMap().get(SensorTypes.PDR);
                    positionX.setText(getString(R.string.x, String.format("%.1f", pdrValues[0])));
                    positionY.setText(getString(R.string.y, String.format("%.1f", pdrValues[1])));
                    // Calculate distance travelled
                    distance += Math.sqrt(Math.pow(pdrValues[0] - previousPosX, 2) + Math.pow(pdrValues[1] - previousPosY, 2));
                    distanceTravelled.setText(getString(R.string.meter, String.format("%.2f", distance)));
                    previousPosX = pdrValues[0];
                    previousPosY = pdrValues[1];
                    // Display elevation and elevator icon when necessary
                    float elevationVal = sensorFusion.getElevation();
                    elevation.setText(getString(R.string.elevation, String.format("%.1f", elevationVal)));
                    if(sensorFusion.getElevator()) elevatorIcon.setVisibility(View.VISIBLE);
                    else elevatorIcon.setVisibility(View.GONE);

                    //Rotate compass image to heading angle
                    compassIcon.setRotation((float) -Math.toDegrees(sensorFusion.passOrientation()));

                     */
                    float elevationVal = sensorFusion.getElevation();
                    updateElevationUI(elevationVal, sensorFusion.getElevator());

                    checkBuildingBounds(currentPosition);

                    int calcFloor = sensorFusion.getCurrentFloor();
                    Log.d("CURRENT FLOOR", "current floor" + calcFloor);

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

    private void checkBuildingBounds (LatLng coordinate){
        if (buildingManager != null){
            if(buildingManager.checkBoundaries(coordinate)){
                hasEnteredBuilding();
            } else if (buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)){
                currentBuilding.setText(buildingManager.getCurrentBuilding().getBuildingName());
                buildingManager.removeGroundOverlay();
            }
        }
    }

    private void hasEnteredBuilding(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (buildingManager != null){
                    setupFloorSpinner();
                    currentBuilding.setText(buildingManager.getCurrentBuilding().getBuildingName());
                    if (buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)) {
                        buildingManager.removeGroundOverlay();
                    } else {
                        buildingManager.addGroundOverlay();
                    }
                }
            }
        });

    }

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

                Log.d("PDR Values", "x: "+pdrValues[0]+" y: "+pdrValues[1] + "up: "+ elevationVal);
                Log.d("REF_POINT", "lat: "+ startPosition[0] + " long: "+ startPosition[1] + "alt: "+startPosition[2]);
                if (recording_map != null) {
                    LatLng user_step = CoordinateTransform.enuToGeodetic(pdrValues[0], pdrValues[1], elevationVal, startPosition[0], startPosition[1], ecefRefCoords);
                    Log.d("UserLocation", "lat: "+user_step.latitude+" long: "+user_step.longitude);
                    //recording_map.addMarker(new MarkerOptions().position(user_step).title("Step"));
                    currentPosition = user_step;
                    updateUserTrajectory(user_step);
                    user_marker.setPosition(user_step);
                }

                // Calculate distance travelled
                distance += Math.sqrt(Math.pow(pdrValues[0] - previousPosX, 2) + Math.pow(pdrValues[1] - previousPosY, 2));
                distanceTravelled.setText(getString(R.string.distance_travelled, String.format("%.2f", distance)));
                previousPosX = pdrValues[0];
                previousPosY = pdrValues[1];
            }
        });

    }

    @Override
    public void onOrientationUpdate(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                compassIcon.setRotation((float) Math.toDegrees(sensorFusion.passOrientation()));
                //compassIcon.setRotation((float) -Math.toDegrees(sensorFusion.passOrientation()));
            }
        });
    }

    @Override
    public void onGNSSUpdate(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("GNSS POSITION UPDATE", "GNSS POSITION UPDATE CALLED");
                if (showGNSS!= null && showGNSS.isChecked()){
                    updateGNSSInfo();
                }
            }
        });
    }

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
        accuracy.setText(getString(R.string.meter, String.format("%.2f", GNNS_accuracy)));
        float[] distanceBetween = new float[1];
        Location.distanceBetween(GNSS_pos[0], GNSS_pos[1], currentPosition.latitude, currentPosition.longitude, distanceBetween);
        positioning_error.setText(getString(R.string.meter, String.format("%.2f", distanceBetween[0])));
    }

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

    public void updateUserTrajectory (LatLng point){
        if (user_trajectory != null) {
            List<LatLng> points = user_trajectory.getPoints();
            points.add(point);
            user_trajectory.setPoints(points);
        }
    }

    public void updateFloor(int calcFloor){
        if (currentFloor != calcFloor){
            currentFloor = calcFloor;
            if (buildingManager != null){
                if (!buildingManager.getCurrentBuilding().equals(Buildings.UNSPECIFIED)){
                    //sensorFusion.setCurrentFloor(calcFloor);
                    floorSpinner.setSelection(buildingManager.convertFloorToSpinnerIndex(currentFloor));
                    updateFloorPlan(currentFloor);
                }
            }
        }
    }

    public void updateFloorPlan(int floor){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buildingManager.updateFloor(floor);
            }
        });
    }

    /**
     * Runnable task used to refresh UI elements with live data.
     * Has to be run through a Handler object to be able to alter UI elements
     */
    private final Runnable refreshDataTask = new Runnable() {
        @Override
        public void run() {

            // Display elevation and elevator icon when necessary
            float elevationVal = sensorFusion.getElevation();
            updateElevationUI(elevationVal, sensorFusion.getElevator());

            checkBuildingBounds(currentPosition);

            int calcFloor = sensorFusion.getCurrentFloor();
            Log.d("CURRENT FLOOR", "current floor" + calcFloor);

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
     * Stops ongoing refresh task, but not the countdown timer which stops automatically
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
     */
    @Override
    public void onResume() {
        if(!this.settings.getBoolean("split_trajectory", false)) {
            refreshDataHandler.postDelayed(refreshDataTask, 500);
        }
        this.sensorFusion.registerForSensorUpdates(this);
        super.onResume();
    }

    @Override
    public void onDestroy() {
        this.sensorFusion.removeSensorUpdate(this);
        super.onDestroy();
    }

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
        /*
        switch (buildingManager.getCurrentBuilding()){
            case NUCLEUS:
                floorAdapter = ArrayAdapter.createFromResource(requireContext(),
                        R.array.floors_nucleus, android.R.layout.simple_spinner_item);
                floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                break;
            case LIBRARY:
                floorAdapter = ArrayAdapter.createFromResource(requireContext(),
                        R.array.floors_library, android.R.layout.simple_spinner_item);
                floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                break;
            case FLEMING_JENKINS:
                floorAdapter = ArrayAdapter.createFromResource(requireContext(),
                        R.array.floors_fleming, android.R.layout.simple_spinner_item);
                floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                break;
            case UNSPECIFIED:
                floorAdapter = null;
                break;
        }
         */

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

    private void mapTypeSpinnerInitialisation() {
        // Set up Spinner functionality as needed
        ArrayAdapter<CharSequence> mapTypeAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.map_type_options, android.R.layout.simple_spinner_item);
        mapTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mapTypeSpinner.setAdapter(mapTypeAdapter);

        // Set a listener to handle Spinner item selection
        this.mapTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Handle item selection
                String selectedMapType = parent.getItemAtPosition(position).toString();
                // Perform actions based on selected map type
                updateMapType(selectedMapType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

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

    private void createRecordingSettingsDialog(){
        recordingSettingsDialog = new Dialog(requireContext());
        recordingSettingsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        recordingSettingsDialog.setContentView(R.layout.recording_bottom_sheet);

        recordingSettingsDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        recordingSettingsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        recordingSettingsDialog.getWindow().getAttributes().windowAnimations = R.style.BottomSheetAnimation;
        recordingSettingsDialog.getWindow().setGravity(Gravity.BOTTOM);

        this.mapTypeSpinner = recordingSettingsDialog.findViewById(R.id.mapTypeSpinner);
        //this.mapTypeSpinner = getView().findViewById(R.id.mapTypeSpinner);
        mapTypeSpinnerInitialisation();

        this.floorSpinner = recordingSettingsDialog.findViewById(R.id.floorSpinner);
        //this.floorSpinner = getView().findViewById(R.id.floorSpinner);
        this.floorSpinner.setVisibility(View.GONE);

        this.floor_title = recordingSettingsDialog.findViewById(R.id.floor_title);
        this.currentBuilding = recordingSettingsDialog.findViewById(R.id.curBuilding);
        this.positioning_error = recordingSettingsDialog.findViewById(R.id.pos_error);
        this.accuracy = recordingSettingsDialog.findViewById(R.id.accuracy);
        this.showGNSS = recordingSettingsDialog.findViewById(R.id.toggleGNSS);
        this.showGNSS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (!isChecked){
                    if (GNSS_marker != null){
                        GNSS_marker.remove();
                    }
                    recordingSettingsDialog.findViewById(R.id.accuracy_title).setVisibility(View.GONE);
                    recordingSettingsDialog.findViewById(R.id.pos_error_title).setVisibility(View.GONE);
                    accuracy.setVisibility(View.GONE);
                    positioning_error.setVisibility(View.GONE);
                } else {
                    recordingSettingsDialog.findViewById(R.id.accuracy_title).setVisibility(View.VISIBLE);
                    recordingSettingsDialog.findViewById(R.id.pos_error_title).setVisibility(View.VISIBLE);
                    accuracy.setVisibility(View.VISIBLE);
                    positioning_error.setVisibility(View.VISIBLE);

                    updateGNSSInfo();
                }
            }
        });

    }
    private void showRecordingSettings() {

        recordingSettingsDialog.show();

    }
}