package com.openpositioning.PositionMe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openpositioning.PositionMe.fragments.CorrectionFragment;
import com.openpositioning.PositionMe.fragments.HomeFragment;
import com.openpositioning.PositionMe.fragments.RecordingFragmentDirections;
import com.openpositioning.PositionMe.sensors.SensorFusion;
import com.openpositioning.PositionMe.sensors.SensorTypes;

public class MapRecordingFragment extends Fragment {

    private SensorFusion sensorFusion;
    private float[] startPosition = new float[3];
    //Zoom of google maps
    private float zoom = 19f;

    private GoogleMap recording_map;
    private PolylineOptions user_trajectory;

    //Button to end PDR recording
    private Button stopButton;
    private Button cancelButton;
    //Recording icon to show user recording is in progress
    private ImageView recIcon;
    //Loading bar to show time remaining before recording automatically ends
    private ProgressBar timeRemaining;
    //App settings
    private SharedPreferences settings;
    //Timer to end recording
    private CountDownTimer autoStop;

    private Handler refreshDataHandler;

    //Text views to display user position and elevation since beginning of recording
    private TextView positionX;
    private TextView positionY;
    private TextView elevation;
    private TextView distanceTravelled;

    //variables to store data of the trajectory
    private float distance;
    private float previousPosX;
    private float previousPosY;

    /**
     * Public Constructor for the class.
     * Left empty as not required
     */
    public MapRecordingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.sensorFusion = SensorFusion.getInstance();
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
            user_trajectory = new PolylineOptions();
            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setTiltGesturesEnabled(true);
            googleMap.getUiSettings().setRotateGesturesEnabled(true);
            googleMap.getUiSettings().setScrollGesturesEnabled(true);

            // Add a marker in current GPS location and move the camera
            startPosition = sensorFusion.getGNSSLatLongAlt(true);
            LatLng position = new LatLng(startPosition[0], startPosition[1]);
            googleMap.addMarker(new MarkerOptions().position(position).title("Start Position"));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom ));
            user_trajectory.add(position);
            googleMap.addPolyline(user_trajectory);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_map_recording, container, false);
        // Inflate the layout for this fragment
        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
        getActivity().setTitle("Recording...");
        return rootView;
    }

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

        //Set default text of TextViews to 0
        this.positionX.setText(getString(R.string.x, "0"));
        this.positionY.setText(getString(R.string.y, "0"));
        this.positionY.setText(getString(R.string.elevation, "0"));
        this.distanceTravelled.setText(getString(R.string.meter, "0"));

        //Reset variables to 0
        this.distance = 0f;
        this.previousPosX = 0f;
        this.previousPosY = 0f;

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
                NavDirections action = MapRecordingFragmentDirections.actionMapRecordingFragmentToCorrectionFragment();
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
                NavDirections action = MapRecordingFragmentDirections.actionMapRecordingFragmentToHomeFragment();
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
     * Runnable task used to refresh UI elements with live data.
     * Has to be run through a Handler object to be able to alter UI elements
     */
    private final Runnable refreshDataTask = new Runnable() {
        @Override
        public void run() {
            // Get new position
            float[] pdrValues = sensorFusion.getSensorValueMap().get(SensorTypes.PDR);
            positionX.setText(getString(R.string.x, String.format("%.1f", pdrValues[0])));
            positionY.setText(getString(R.string.y, String.format("%.1f", pdrValues[1])));

            //CoordinateTransform.geodeticToEcef(new LatLng(34.0522, -118.2437), 0);
            //LatLng test = CoordinateTransform.enuToGeodetic(1000, 2000, 500, new LatLng(34.0522, -118.2437), 0);
            //Log.d("UserLocation", "lat: "+test.latitude+" long: "+test.longitude);

            Log.d("PDR Values", "x: "+pdrValues[0]+" y: "+pdrValues[1] + "up: "+ sensorFusion.getElevation());
            /*
            if (recording_map != null) {
                recording_map.clear();
                LatLng user_step = CoordinateTransform.enuToGeodetic(pdrValues[0], pdrValues[1], sensorFusion.getElevation(), new LatLng(startPosition[0], startPosition[1]), startPosition[2]);
                Log.d("UserLocation", "lat: "+user_step.latitude+" long: "+user_step.longitude);
                recording_map.addMarker(new MarkerOptions().position(user_step).title("Step"));
                user_trajectory.add(user_step);
                recording_map.addPolyline(user_trajectory);
            }
            */

            // Calculate distance travelled
            distance += Math.sqrt(Math.pow(pdrValues[0] - previousPosX, 2) + Math.pow(pdrValues[1] - previousPosY, 2));
            distanceTravelled.setText(getString(R.string.meter, String.format("%.2f", distance)));
            previousPosX = pdrValues[0];
            previousPosY = pdrValues[1];
            // Display elevation and elevator icon when necessary
            float elevationVal = sensorFusion.getElevation();
            elevation.setText(getString(R.string.elevation, String.format("%.1f", elevationVal)));

            // Loop the task again to keep refreshing the data
            refreshDataHandler.postDelayed(refreshDataTask, 100);
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
        super.onPause();
    }

    /**
     * {@inheritDoc}
     * Restarts UI refreshing task when no countdown task is in progress
     */
    @Override
    public void onResume() {
        if(!this.settings.getBoolean("split_trajectory", false)) {
            refreshDataHandler.postDelayed(refreshDataTask, 100);
        }
        super.onResume();
    }
}