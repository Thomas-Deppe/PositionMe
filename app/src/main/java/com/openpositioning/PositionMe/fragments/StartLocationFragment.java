package com.openpositioning.PositionMe.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.openpositioning.PositionMe.Utils.JsonConverter;
import com.openpositioning.PositionMe.R;
import com.openpositioning.PositionMe.ServerCommunications;
import com.openpositioning.PositionMe.sensors.Observer;
import com.openpositioning.PositionMe.sensors.SensorFusion;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass. The startLocation fragment is displayed before the trajectory
 * recording starts. This fragment displays a map in which the user can adjust their location to
 * correct the PDR when it is complete
 *
 * @see HomeFragment the previous fragment in the nav graph.
 * @see RecordingFragment the next fragment in the nav graph.
 * @see SensorFusion the class containing sensors and recording.
 *
 * @author Virginia Cangelosi
 */
public class StartLocationFragment extends Fragment implements Observer{

    //Google maps parameters
    private GoogleMap start_map;
    private float zoom = 19f;
    //Button to go to next fragment and save the location
    private Button button;
    //Singleton SesnorFusion class which stores data from all sensors
    private SensorFusion sensorFusion = SensorFusion.getInstance();
    //Google maps LatLong object to pass location to the map
    private LatLng position;
    //Start position of the user to be stored
    private float[] startPosition = new float[2];
    private double[] startRef = new double[3];
    private Marker user_marker;
    boolean wifiFound = false;
    private Integer currentFloor = null;

    private ServerCommunications serverCommunications;

    /**
     * Public Constructor for the class.
     * Left empty as not required
     */
    public StartLocationFragment() {
        // Required empty public constructor
    }

    /**
     * {@inheritDoc}
     * The map is loaded and configured so that it displays a draggable marker for the start location
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
        View rootView = inflater.inflate(R.layout.fragment_startlocation, container, false);

        //Obtain the start position from the GPS data from the SensorFusion class
        startPosition = sensorFusion.getGNSSLatitude(false);
        startRef = sensorFusion.getGNSSLatLngAlt(false);

        // Attempt to get Wifi
        this.serverCommunications = ServerCommunications.getMainInstance();
        this.serverCommunications.registerObserver(this);
        if (sensorFusion.getWifiList() != null) {
            try {
                JSONObject jsonFingerprint = JsonConverter.toJson(sensorFusion.getWifiList());
                Log.d("Start Position:", "Sending fingerprint to server "+ jsonFingerprint.toString());
                serverCommunications.sendWifi(jsonFingerprint);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return rootView;
    }

    /**
     * {@inheritDoc}
     * Button onClick listener enabled to detect when to go to next fragment and start PDR recording.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //If not location found zoom the map out
        if(startPosition[0]==0 && startPosition[1]==0){
            zoom = 1f;
        }
        else {
            zoom = 19f;
        }
        // Initialize map fragment
        SupportMapFragment supportMapFragment=(SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.startMap);

        // Asynchronous map which can be configured
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            /**
             * {@inheritDoc}
             * Controls to allow scrolling, tilting, rotating and a compass view of the
             * map are enabled. A marker is added to the map with the start position and a marker
             * drag listener is generated to detect when the marker has moved to obtain the new
             * location.
             */
            @Override
            public void onMapReady(GoogleMap mMap) {
                start_map = mMap;
                start_map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                start_map.getUiSettings().setCompassEnabled(true);
                start_map.getUiSettings().setTiltGesturesEnabled(true);
                start_map.getUiSettings().setRotateGesturesEnabled(true);
                start_map.getUiSettings().setScrollGesturesEnabled(true);
                start_map.setBuildingsEnabled(false);

                // Add a marker in current GPS location and move the camera
                position = new LatLng(startRef[0], startRef[1]);
                Log.d("MARKER LOCATION", startRef[0] + " " + startRef[1]);
                user_marker = start_map.addMarker(new MarkerOptions()
                        .position(position)
                        .title("User Position"));
                start_map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));

                //Drag listener for the marker to execute when the markers location is changed
                start_map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener()
                {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void onMarkerDragStart(Marker marker){}

                    /**
                     * {@inheritDoc}
                     * Updates the start position of the user.
                     */
                    @Override
                    public void onMarkerDragEnd(Marker marker)
                    {
                        startPosition[0] = (float) marker.getPosition().latitude;
                        startPosition[1] = (float) marker.getPosition().longitude;
                        startRef[0] = marker.getPosition().latitude;
                        startRef[1] = marker.getPosition().longitude;

                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void onMarkerDrag(Marker marker){}
                });
            }
        });

        // Add button to begin PDR recording and go to recording fragment.
        this.button = (Button) getView().findViewById(R.id.startLocationDone);
        this.button.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * When button clicked the PDR recording can start and the start position is stored for
             * the {@link CorrectionFragment} to display. The {@link RecordingFragment} is loaded.
             */
            @Override
            public void onClick(View view) {
                // Starts recording data from the sensor fusion
                sensorFusion.startRecording();
                // Set the start location obtained
                sensorFusion.setStartGNSSLatitude(new float[] {(float) startRef[0], (float) startRef[1]});
                sensorFusion.setStartGNSSLatLngAlt(startRef);
                //sensorFusion.initialiseFusionAlgorithm(startRef[0], startRef[1], sensorFusion.getElevation());
                sensorFusion.initialiseFusionAlgorithm();
                if (currentFloor != null) {
                    sensorFusion.setCurrentFloor(currentFloor);
                }
                // Navigate to the RecordingFragment
                NavDirections action = StartLocationFragmentDirections.actionStartLocationFragmentToRecordingFragment();
                Navigation.findNavController(view).navigate(action);

            }
        });
    }

    private void updateMarker(LatLng new_position) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (start_map != null && user_marker != null) {
                    Log.d("MARKER WIFI LOCATION", new_position.latitude + " " + new_position);
                    LatLng new_position = new LatLng(startRef[0], startRef[1]);
                    user_marker.setPosition(new_position);
                    start_map.animateCamera(CameraUpdateFactory.newLatLngZoom(new_position, zoom));
                }
            }
        });
    }

    @Override
    public void updateServer(Object[] objList) {
        if (objList == null) return;

        JSONObject wifiResponse = (JSONObject) objList[0];

        try {
            double latitude = wifiResponse.getDouble("lat");
            double longitude = wifiResponse.getDouble("lon");
            currentFloor = wifiResponse.getInt("floor");
            startPosition[0] = (float) latitude;
            startRef[0] = latitude;
            startPosition[1] = (float) longitude;
            startRef[1] = longitude;
            startRef[2] = sensorFusion.getAbsoluteElevation();
            wifiFound = true;
            Log.d("MARKER WIFI RESPONSE", latitude + " " + longitude);
            updateMarker(new LatLng(latitude,longitude));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.serverCommunications.unRegisterObserver(this);
    }

    @Override
    public void updateWifi(Object[] objList) {
        return;
    }
}
