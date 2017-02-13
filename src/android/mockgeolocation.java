package me.rahul.plugins.mockgeolocation;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class detects the fake geolocation coordinates provided by the system.
 */
public class mockgeolocation extends CordovaPlugin implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "MockGeolocationPlugin";
    private static CordovaInterface _cordova;
    private static CallbackContext _callbackContext;
    private static boolean mockLocationsEnabled;
    private static int numTimesPermissionDeclined;
    private static Location lastLocation;
    private static int locationUpdateRequestCont;
    private Activity _cordovaActivity;
    private GoogleApiClient googleApiClient;
    private boolean permissionGranted;
    private LocationRequest mLocationRequest;
    private int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    private long updateInterval = 1000;
    private int maxNumberUpdates = 2;

    /**
     * Callback handler for this Class
     *
     * @param status  Message status
     * @param message Any message
     */
    private void sendCallback(PluginResult.Status status, int code, String message) {
        JSONObject response = new JSONObject();
        this.stop();
        try {
            response.put("message", message);
            response.put("code", code);
            response.put("mockLocationEnabled", mockLocationsEnabled);
            if (lastLocation != null) {
                response.put("latitude", lastLocation.getLatitude());
                response.put("longitude", lastLocation.getLongitude());
                response.put("accuracy", lastLocation.getAccuracy());
                response.put("altitude", lastLocation.getAltitude());
                response.put("provider", lastLocation.getProvider());
                response.put("bearing", lastLocation.getBearing());
                response.put("time", lastLocation.getTime());
            }
            //result.setKeepCallback(true);
            _callbackContext.sendPluginResult(new PluginResult(status, response));
        } catch (Exception e) {
            e.printStackTrace();
            _callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
        }

    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        _cordova = this.cordova;
        _cordovaActivity = this.cordova.getActivity();
        Log.d(TAG, "Initialized");
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        _callbackContext = callbackContext;
        if (action.equals("check")) {
            if (googleApiClient == null) {
                lastLocation = null;
                googleApiClient = new GoogleApiClient.Builder(_cordovaActivity.getApplicationContext())
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
            }
            updateDefaultArguments(args);

            this.check();
            return true;
        }
        return false;
    }

    private void updateDefaultArguments(JSONArray args) {
        try {
            JSONObject jsonObject = args.getJSONObject(0);
            /* Get Accuracy Variable from args. Default is balanced */
            String accuracy = jsonObject.optString("priority", "balanced");
            if (accuracy.equalsIgnoreCase("high"))
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
            else if (accuracy.equalsIgnoreCase("low"))
                priority = LocationRequest.PRIORITY_LOW_POWER;
            else if (accuracy.equalsIgnoreCase("no"))
                priority = LocationRequest.PRIORITY_NO_POWER;
            else if (accuracy.equalsIgnoreCase("balanced"))
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
            /* Get update interval. Default is 1000 */
            updateInterval = jsonObject.optInt("updateInterval", 1000);
            /* Get Max Number Location Update Tries before plugin call error function. Default is 2 */
            maxNumberUpdates = jsonObject.optInt("maxNumberUpdates", 2);
            /* Get Max Number of times to ask for permission. Default is 2 */
            numTimesPermissionDeclined = jsonObject.optInt("numTimesPermissionDeclined", 2);
        } catch (JSONException e) {
            Log.d(TAG, "No Arguments Found");
        }
    }

    protected void stop() {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
        permissionGranted = false;
    }

    private void checkLocationPermission() {
        permissionGranted = Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(_cordovaActivity.getApplicationContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.i(TAG, "CheckLocationpermission = " + permissionGranted);
        if (!permissionGranted) {
            try {
                numTimesPermissionDeclined++;
                java.lang.reflect.Method method = cordova.getClass().getMethod("requestPermissions", org.apache.cordova.CordovaPlugin.class, int.class, java.lang.String[].class);
                method.invoke(cordova, this, -1, new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "requestPermissions() method not found in CordovaInterface implementation of Cordova v" + CordovaWebView.CORDOVA_VERSION);
            }
        }
    }

    private void check() {
        checkMockPermissions();
        googleApiClient.connect();
    }

    /* If the build version is less than 18 then directly check the mocked location. Otherwise check permissions only*/
    private void checkMockPermissions() {
        if (Build.VERSION.SDK_INT < 18) {
            if (!android.provider.Settings.Secure.getString(_cordovaActivity.getApplicationContext().getContentResolver(), Secure.ALLOW_MOCK_LOCATION).equals("0")) {
                mockLocationsEnabled = true;
                sendCallback(PluginResult.Status.OK, 200, "Mocked Geolocation Detected");
            } else {
                mockLocationsEnabled = false;
                sendCallback(PluginResult.Status.OK, 200, "Mocked Geolocation Not Detected");
            }

        } else {
            if (!permissionGranted) checkLocationPermission();
            if (!permissionGranted) {
                if (numTimesPermissionDeclined >= 2) {
                    mockLocationsEnabled = false;
                    sendCallback(PluginResult.Status.ERROR, 403, "GPS Permission Denied even after asking twice.");
                    return;
                } else {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkMockPermissions();
                        }
                    }, 4000);
                }
            }

        }
    }

    /* Only needed when build version is greater than 18 */
    private void isLocationMocked(Location location) {
        boolean isMock = mockLocationsEnabled || (Build.VERSION.SDK_INT >= 18 && location.isFromMockProvider());
        if (isMock) {
            mockLocationsEnabled = true;
            sendCallback(PluginResult.Status.OK, 200, "Mocked Geolocation Detected");
        } else {
            mockLocationsEnabled = false;
            sendCallback(PluginResult.Status.OK, 200, "Mocked Geolocation Not Detected");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location Connection Successful");
        try {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            Log.i(TAG, "location onconnected" + lastLocation);
            startLocationUpdates();
        } catch (SecurityException e) {
            e.printStackTrace();
            lastLocation = null;
            mockLocationsEnabled = false;
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        Log.i(TAG, "Start Location Updates");
        mLocationRequest = LocationRequest.create()
                .setPriority(priority)
                .setInterval(updateInterval)
                .setFastestInterval(updateInterval);
//                .setNumUpdates(1);
        // Request location updates
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                    mLocationRequest, this);
        } catch (SecurityException e) {
            e.printStackTrace();
            lastLocation = null;
            mockLocationsEnabled = false;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Location Connection Failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location Changed" + location);
        lastLocation = location;
        locationUpdateRequestCont++;
        if (locationUpdateRequestCont > maxNumberUpdates) {
            isLocationMocked(location);
        }
    }
}