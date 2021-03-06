/*------------------------------------------------------------------------------
 *   Authors: Slavik, George, Thao, Chelsea
 *   Copyright: (c) 2016 Team Magic
 *
 *   This file is part of GoodHikes.
 *
 *   GoodHikes is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   GoodHikes is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with GoodHikes.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.uwaterloo.magic.goodhikes;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.uwaterloo.magic.goodhikes.data.LocationPoint;
import ca.uwaterloo.magic.goodhikes.data.Milestone;
import ca.uwaterloo.magic.goodhikes.data.Route;
import ca.uwaterloo.magic.goodhikes.data.RoutesContract.RouteEntry;
import ca.uwaterloo.magic.goodhikes.data.RoutesDatabaseManager;
import ca.uwaterloo.magic.goodhikes.data.UserManager;

public class MapsActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnInfoWindowClickListener {

    protected GoogleMap mMap;
    private RoutesDatabaseManager database;
    private UserManager userManager;
    protected static final String LOG_TAG = "MapsActivity";
    private GPSUpdatesReceiver mGPSUpdatesReceiver;
    private IntentFilter mFilter;
    private ServiceConnection mConnection;
    private GPSLoggingService mLoggingService;
    private ImageButton mGPSTrackingButton, mMilestoneButton, mSettingsButton, mHistoryButton, mStatisticsButton, mShareButton;
    private Route selectedRoute;
    private Polyline visualRouteTrace, previousVisualRouteTrace;
    private Marker initRoutePointMarker, lastRoutePointMarker,
            previousRoutePointMarkerStart, previousRoutePointMarkerEnd;
    private boolean followingExistingRoute;
    private ArrayList<Marker> milestonePointMarkers;
    private ImageView previewImage;
    private Map<String, Bitmap> markerImageMap;

    //Identifiers for opening other activities and returning results to MapsActivity
    private final int PICK_ROUTE_REQUEST = 1;
    private final int RESULT_LOAD_IMG = 2;
    private final int RESULT_CAPTURE_IMG = 3;

    private final BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mLoggingService.stopLocationTracking();

            finish();
        }
    };

    /**
     * onCreate() is called only once for Activity, whereas onStart() - each time
     * appActivity is hidden from screen (user uses other apps), and then App is activated again.
     * <p/>
     * Using startService() overrides the default service lifetime that is managed by
     * bindService(Intent, ServiceConnection, int): it requires the service to remain running
     * until stopService(Intent) is called, regardless of whether any clients are connected to it.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        database = RoutesDatabaseManager.getInstance(this);
        userManager = new UserManager(getApplicationContext());
        milestonePointMarkers = new ArrayList<Marker>();
        markerImageMap = new HashMap<String, Bitmap>();

        //Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Create and attach listeners to buttons
        mSettingsButton = (ImageButton) findViewById(R.id.settings_button);
        mShareButton = (ImageButton) findViewById(R.id.share_button);
        mStatisticsButton = (ImageButton) findViewById(R.id.statistics_button);
        mHistoryButton = (ImageButton) findViewById(R.id.history_button);
        mGPSTrackingButton = (ImageButton) findViewById(R.id.gps_tracking_control_button);
        mMilestoneButton = (ImageButton) findViewById(R.id.milestone_button);
        attachUICallbacks();

        if (savedInstanceState != null) {
            long routeId = savedInstanceState.getLong(RouteEntry._ID);
            selectedRoute = database.getRoute(routeId);
            followingExistingRoute = savedInstanceState.getBoolean("followingExistingRoute");
        }
    }

    //updateMapType changes the map type when the setting is changed.

    public void updateMapType() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int map_type = Integer.parseInt(prefs.getString
                (getString(R.string.map_pref),
                        getString(R.string.map_type_default)));
        mMap.setMapType(map_type);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFilter = new IntentFilter(GPSLoggingService.locationUpdateCommand);
        mGPSUpdatesReceiver = new GPSUpdatesReceiver();
        mConnection = new GPSLoggingServiceConnection();
        if (selectedRoute == null)
            selectedRoute = database.getLatestRoute(userManager.getUser());
//        clearMap();
        LocalBroadcastManager.getInstance(this).registerReceiver(mGPSUpdatesReceiver, mFilter);
        registerReceiver(logoutReceiver, new IntentFilter("logout"));
        startService(new Intent(this, GPSLoggingService.class));
        Log.d(LOG_TAG, "Thread: " + Thread.currentThread().getId() + "; MapActivity started");
    }


    /**
     * Back key destroys the current Activity, home key doesn't. In the Activity lyfecycle,
     * pressing back calls all the way to current activity's onDestroy() method. On the other hand,
     * pressing home pauses the Activity, which stays alive in background.
     * http://stackoverflow.com/questions/6031052/difference-between-android-home-key-and-back-key-and-their-behaviour
     */
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(logoutReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGPSUpdatesReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = new Intent(this, GPSLoggingService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
        Log.d(LOG_TAG, "Thread: " + Thread.currentThread().getId() + "; Binding to service");
        if (mMap != null) {
            updateMapType();
        }
    }

    /*
     * Unbinding GPS service in onPause, so that there are always matching pairs of bind/unbind calls.
     * */
    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
        Log.d(LOG_TAG, "Thread: " + Thread.currentThread().getId() + "; Unbinding from GPS service");
    }

    /*
     * When display is rotated, the currently selected route needs to be saved.
     * */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (selectedRoute != null)
            outState.putLong(RouteEntry._ID, selectedRoute.getId());
        if (followingExistingRoute != false)
            outState.putBoolean("followingExistingRoute", followingExistingRoute);
        super.onSaveInstanceState(outState);
    }

    /*
    * When GPSLoggingService starts it has it's own lifecycle independent of the Activity that started it.
    * This means that GPSLoggingService will be running even if user switches to another app for a bit,
    * or even simply rotates the screen.
    * GPS tracking logging runs in a separate LoopingThread within GPSLoggingService,
    * and tracking should continue running in the background, even if the app is put in the background.
    * */

    public void stopGPSLoggingService() {
        stopService(new Intent(this, GPSLoggingService.class));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setInfoWindowAdapter(new CustomInfoWindow());
        mMap.setOnInfoWindowClickListener(this);
        UiSettings settings = mMap.getUiSettings();
        settings.setCompassEnabled(true);
        settings.setMapToolbarEnabled(true);
        settings.setScrollGesturesEnabled(true);
        updateMapType();
        enableMyLocation();
        if (followingExistingRoute) drawPreviousRouteTrace();
        Log.d(LOG_TAG, "Thread: " + Thread.currentThread().getId() + "; onMapReady()");
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        //Toast.makeText(this, "Info Window Clicked", Toast.LENGTH_SHORT).show();
    }

    public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            if (!marker.getTitle().equals("Milestone")) {
                return null;
            }

            View milestone = getLayoutInflater().inflate(R.layout.marker_info, null);
            TextView windowtitle = (TextView) milestone.findViewById(R.id.milestone_title);
            TextView windowdesc = (TextView) milestone.findViewById(R.id.milestone_desc);
            ImageView windowimage = (ImageView) milestone.findViewById(R.id.milestone_image);

            if (markerImageMap.get(marker.getId()) == null) {
                LinearLayout window = (LinearLayout) milestone.findViewById(R.id.marker_window);
                window.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }

            windowtitle.setText(marker.getTitle());
            windowdesc.setText(marker.getSnippet());
            windowimage.setImageBitmap(markerImageMap.get(marker.getId()));

            return milestone;
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    private void enableMyLocation() {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    private void updateLocation(Location location) {
        mLoggingService.updateGPSfrequency();
        LatLng coordinates = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(coordinates));
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
    }

    public class GPSUpdatesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location;
            Bundle extras = intent.getExtras();
            if (extras != null) {
                location = (Location) intent.getExtras().get(GPSLoggingService.locationUpdateCommand);
                updateLocation(location);
            }
            drawTrace(mLoggingService.currentRoute);
            Log.d(LOG_TAG, "Thread: " + Thread.currentThread().getId() + "; Location update received by UI");
        }

    }

    public class GPSLoggingServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mLoggingService = ((GPSLoggingService.LoggingBinder) binder).getService();
            setTrackingButtonIcon();
//            mLoggingService.broadcastLastKnownLocation();

            Log.d(LOG_TAG, "Thread: " + Thread.currentThread().getId() + "; GPS Logging service connected");
            if (!mLoggingService.isTrackingActive() && !mLoggingService.isTrackingOnPause())
                drawSelectedRoute();
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    }

    private void attachUICallbacks() {
        mGPSTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoggingService != null) {
                    if (!mLoggingService.isTrackingActive()) {
                        if (mLoggingService.isTrackingOnPause()) {
                            startTracking();
                        } else {
                            showStartTrackingDialog();
                        }
                    } else {
                        mLoggingService.stopLocationTracking();
                        setTrackingButtonIcon();
                        showStopTrackingDialog();
                    }
                }
            }
        });
        mMilestoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoggingService.isTrackingActive()) {
                    showAddMilestoneDialog();
                }
            }
        });
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }
        });
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CaptureMapScreen();
            }
        });
        mStatisticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String json = toJSON();
                if (json == null)
                    Toast.makeText(getApplicationContext(), "No current route!", Toast.LENGTH_SHORT).show();
                else {
                    Intent i = new Intent(getApplicationContext(), StatisticsActivity.class);
                    i.putExtra("current_route", json);
                    startActivity(i);
                }
            }
        });
        mHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getApplicationContext(), HistoryActivity.class),
                        PICK_ROUTE_REQUEST);
            }
        });
    }

    private void startTracking() {
        if (mLoggingService.isTrackingOnPause() == false) clearMap();
        mLoggingService.startLocationTracking();
        setTrackingButtonIcon();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_CANCELED) return;
        if (requestCode == PICK_ROUTE_REQUEST) {
            if (resultCode == RESULT_OK) {
                long routeId = data.getLongExtra(RouteEntry._ID, -1);
                selectedRoute = database.getRoute(routeId);
                if (!mLoggingService.isTrackingActive())
                    drawSelectedRoute();
            }
        }
    }


    private void drawSelectedRoute() {
        if (selectedRoute == null || selectedRoute.getId() == -1) return;
        clearMap();
        drawTrace(selectedRoute);
        moveMapCameraToRoute(selectedRoute);
    }

    private void setTrackingButtonIcon() {
        if (mLoggingService != null) {
            mGPSTrackingButton.setImageResource(mLoggingService.getTrackingButtonIcon());
            mMilestoneButton.setImageResource(mLoggingService.getMilestoneButtonIcon());
        }
    }

    private void initVisualTrace(boolean previousRoute) {
        PolylineOptions traceOptions = new PolylineOptions();
        int color = (!previousRoute) ? Color.BLUE : Color.rgb(255, 255, 153); //light green
        traceOptions.color(color);
        Polyline routeTrace = mMap.addPolyline(traceOptions);
        if (!previousRoute) visualRouteTrace = routeTrace;
        else previousVisualRouteTrace = routeTrace;
        routeTrace.setVisible(true);
    }

    private void drawTrace(Route route) {
        if (route == null || route.getId() == -1) return;
        if (visualRouteTrace == null) initVisualTrace(false);
        visualRouteTrace.setPoints(route.getPointsCoordinates());

        if (route.size() > 0 && initRoutePointMarker == null) {
            initRoutePointMarker = mMap.addMarker(
                    new MarkerOptions().position(route.getStartCoordinates()).title("Start"));
        }

        if (route.size() > 1) {
            if (lastRoutePointMarker != null) lastRoutePointMarker.remove();
            lastRoutePointMarker = mMap.addMarker(new MarkerOptions()
                    .position(route.getLastCoordinates())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("End"));
        }

        for (Marker marker : milestonePointMarkers) {
            marker.remove();
        }
        milestonePointMarkers.clear();
        markerImageMap.clear();
        for (Milestone milestone : route.getMilestones()) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(milestone.getLatLng())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    .title("Milestone")
                    .snippet(milestone.getNote()));
            markerImageMap.put(marker.getId(), milestone.getImage());
            milestonePointMarkers.add(marker);
        }
    }

    private void drawPreviousRouteTrace() {
        if (selectedRoute == null || selectedRoute.getId() == -1) return;
        followingExistingRoute = true;
        if (previousVisualRouteTrace == null) {
            initVisualTrace(true);
            previousVisualRouteTrace.setPoints(selectedRoute.getPointsCoordinates());
        }

        if (selectedRoute.size() > 0) {
            previousRoutePointMarkerStart = mMap.addMarker(new MarkerOptions()
                    .position(selectedRoute.getStartCoordinates())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title("Previous route start"));
        }

        if (selectedRoute.size() > 1) {
            previousRoutePointMarkerEnd = mMap.addMarker(new MarkerOptions()
                    .position(selectedRoute.getLastCoordinates())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                    .title("Previous route end"));
        }
    }

    private void clearRouteFollowed() {
        if (previousRoutePointMarkerStart != null) {
            previousRoutePointMarkerStart.remove();
            previousRoutePointMarkerStart = null;
        }
        if (previousRoutePointMarkerEnd != null) {
            previousRoutePointMarkerEnd.remove();
            previousRoutePointMarkerEnd = null;
        }
        if (previousVisualRouteTrace != null) {
            previousVisualRouteTrace.remove();
            previousVisualRouteTrace = null;
        }
    }

    private void clearMap() {
        if (initRoutePointMarker != null) {
            initRoutePointMarker.remove();
            initRoutePointMarker = null;
        }
        if (lastRoutePointMarker != null) {
            lastRoutePointMarker.remove();
            lastRoutePointMarker = null;
        }
        if (visualRouteTrace != null) {
            visualRouteTrace.remove();
            visualRouteTrace = null;
        }
        for (Marker marker : milestonePointMarkers) {
            marker.remove();
        }
        milestonePointMarkers.clear();
    }

    private void moveMapCameraToRoute(Route route) {
        int padding = 100; // offset from edges of the map in pixels
        int animationDuration = 2000;
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        if (route.getPointsCoordinates().size() > 0) {
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(route.getLatLngBounds(), width, height, padding);
            mMap.animateCamera(cu, animationDuration, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                }

                @Override
                public void onCancel() {
                }
            });
        }
    }

    private void showSaveRouteDialog() {
        FragmentManager fm = getSupportFragmentManager();
        SaveRouteDialogFragment saveRouteDialog = new SaveRouteDialogFragment();
        saveRouteDialog.show(fm, "saveRouteDialog");
    }

    private void showStopTrackingDialog() {
        FragmentManager fm = getSupportFragmentManager();
        StopTrackingDialogFragment dialog = new StopTrackingDialogFragment();
        dialog.show(fm, "stopTrackingDialog");
    }

    private void showAddMilestoneDialog() {
        FragmentManager fm = getSupportFragmentManager();
        AddMilestoneDialogFragment dialog = new AddMilestoneDialogFragment();
        dialog.show(fm, "addMilestoneDialog");
    }

    private void showStartTrackingDialog() {
        FragmentManager fm = getSupportFragmentManager();
        StartTrackingDialogFragment dialog = new StartTrackingDialogFragment();
        dialog.show(fm, "startTrackingDialog");
    }

    /*
    * http://stackoverflow.com/questions/15571010/fragment-inner-class-should-be-static
    * Non static inner classes do hold a reference to their parent classes.
    * The problem with making a Fragement innerclass non-static is that you always
    * hold a reference to the Activity. The GarbageCollector cannot collect your Activity.
    * So you can 'leak' the Activity if for example the orientation changes.
    * Because the Fragment might still live and gets inserted in a new Activity.
    *
    * Lint parser gives error This fragment inner class should be static [ValidFragment]
    * */
    @SuppressLint("ValidFragment")
    public class SaveRouteDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            builder.setView(inflater.inflate(R.layout.dialog_save_route, null))
                    .setTitle(R.string.saving_route)
                    .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            EditText descriptionField = (EditText) ((Dialog) dialog).findViewById(R.id.description);
                            String description = descriptionField.getText().toString();
                            mLoggingService.currentRoute.setDescription(description);

                            CheckBox privateCheckbox = (CheckBox) ((Dialog) dialog).findViewById(R.id.checkbox_private);
                            mLoggingService.currentRoute.setPrivate(privateCheckbox.isChecked());
                            mLoggingService.saveRoute();

                            if (followingExistingRoute) {
                                followingExistingRoute = false;
                                clearRouteFollowed();
                            }

                            Toast.makeText(getActivity(), "Route saved", Toast.LENGTH_SHORT).show();
                            Log.d(LOG_TAG, "Thread: " + Thread.currentThread().getId() + "; route saved");
                        }
                    })
                    .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            clearMap();
                            Toast.makeText(getActivity(), "Route discarded", Toast.LENGTH_SHORT).show();
                            drawSelectedRoute();
                            Log.d(LOG_TAG, "Thread: " + Thread.currentThread().getId() + "; route discarded");
                        }
                    });
            return builder.create();
        }
    }

    @SuppressLint("ValidFragment")
    public class StopTrackingDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            builder.setTitle(R.string.stop_tracking_dialog)
                    .setPositiveButton(R.string.stop_tracking, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            showSaveRouteDialog();
                        }
                    })
                    .setNegativeButton(R.string.pause_tracking, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mLoggingService.setTrackingOnPause(true);
                            Toast.makeText(getActivity(), "Route tracking is paused", Toast.LENGTH_SHORT).show();
                            Log.d(LOG_TAG, "Thread: " + Thread.currentThread().getId() + "; Tracking is paused");
                        }
                    });
            return builder.create();
        }
    }

    @SuppressLint("ValidFragment")
    public class StartTrackingDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            builder.setTitle(R.string.start_tracking)
                    .setPositiveButton(R.string.follow_the_white_rabbit, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            drawPreviousRouteTrace();
                            startTracking();
                        }
                    })
                    .setNegativeButton(R.string.new_route, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startTracking();
                        }
                    });
            return builder.create();
        }
    }

    public void imageSelected(Bitmap image, String note){
        mLoggingService.currentRoute.addMilestone(note, image);
    }

    /* send route info to statistics in JSON*/
    public String toJSON() {

        JSONObject jsonObj = new JSONObject();
        if (selectedRoute == null || selectedRoute.getId() == -1)
            return null;
        Route curr = selectedRoute;
        Log.d("maps:first_coord", String.valueOf(curr.getStartCoordinates()));
        float max_speed = 0, total_speed = 0;
        float distance = 0;
        ArrayList<LocationPoint> trace = curr.getTrace();
        for (int i = 0; i < curr.size(); i++) {
            Location location = trace.get(i);
            if (i > 0) {
                //Log.d(LOG_TAG, i-1 + " to " + i + ": " + location.distanceTo(trace.get(i-1)));
                distance += location.distanceTo(trace.get(i - 1));
            }
            if (location.getSpeed() > max_speed)
                max_speed = location.getSpeed();
            total_speed += location.getSpeed();
        }
        try {
            jsonObj.put("user_name", curr.getUser().getUsername());
            jsonObj.put("route_name", curr.getDescription());
            jsonObj.put("start_time", curr.getDateStartString() + ", " + curr.getTimeStart());
            jsonObj.put("end_time", curr.getDateEndString() + ", " + curr.getTimeEnd());
            jsonObj.put("duration", curr.getDurationString());
            jsonObj.put("distance", distance / 1000);
            jsonObj.put("aver_speed", total_speed / curr.size());
            jsonObj.put("max_speed", max_speed);
            jsonObj.put("num_waypoints", curr.size());
            jsonObj.put("private", curr.getPrivate() == true ? "True" : "False");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "JSON String: " + jsonObj.toString());
        return jsonObj.toString();
    }

    /* take screenshot of the map view and share to social media */
    public void CaptureMapScreen() {
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
            Bitmap bitmap;

            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                // TODO Auto-generated method stub
                bitmap = snapshot;
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "route_screenshot", null);
                Uri uri_image = Uri.parse(path);

                // share intent
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, uri_image);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "My Route");
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Hey! Check out my route on Goodhikes");
                sendIntent.setType("image/*");
                startActivity(Intent.createChooser(sendIntent, "Share to..."));

            }
        };

        mMap.snapshot(callback);

    }
}
