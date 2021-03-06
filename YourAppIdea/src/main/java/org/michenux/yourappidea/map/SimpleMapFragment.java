package org.michenux.yourappidea.map;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.tbruyelle.rxpermissions.RxPermissions;

import org.michenux.drodrolib.ui.snackbar.SnackbarHelper;
import org.michenux.yourappidea.BuildConfig;
import org.michenux.yourappidea.R;
import org.michenux.yourappidea.YourApplication;

import rx.Subscription;

public class SimpleMapFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback {

    private static final int LOCATIONSERVICES_RESOLUTION_RESULCODE = 765;

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mRequest;

    private boolean mPermissionGranted = false;

    private Subscription mSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        if (BuildConfig.DEBUG) {
            Log.d(YourApplication.LOG_TAG, "simpleMapFragment.onCreate");
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this.getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mRequest = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(16)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(YourApplication.LOG_TAG, "simpleMapFragment.onCreateView");
        }
        return inflater.inflate(R.layout.simplemap_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = getMapFragment();
        if (BuildConfig.DEBUG) {
            Log.d(YourApplication.LOG_TAG, "simpleMapFragment.onViewCreated - mapFragment = " + mapFragment);
        }
        if (mapFragment == null) {
            mapFragment = new SupportMapFragment();
            mapFragment.setRetainInstance(true);
            getChildFragmentManager().beginTransaction().replace(R.id.simplemap, mapFragment, null).commit();
        }
        mapFragment.getMapAsync(this);
    }

    private SupportMapFragment getMapFragment() {
        return (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.simplemap);
    }

    @Override
    public void onResume() {
        super.onResume();

        mSubscription = RxPermissions.getInstance(this.getActivity())
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(granted -> {
                    mPermissionGranted = granted;
                    if (granted) {
                        mGoogleApiClient.connect();
                    } else {
                        SnackbarHelper.showInfoLongMessage(this.getView(), R.string.error_permissionfinelocationrequired);
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        mGoogleApiClient.disconnect();
        mSubscription.unsubscribe();
    }

    @Override
    public void onConnected(Bundle bundle) {
        enableLocationOnMap();
        registerLocationUpdates();
    }

    @SuppressWarnings({"MissingPermission"})
    private void registerLocationUpdates() {

        PendingResult<Status> result = LocationServices.FusedLocationApi
                .requestLocationUpdates(
                        mGoogleApiClient,
                        mRequest,
                        this);
        result.setResultCallback(status -> {
            if (status.isSuccess()) {
                // Successfully registered
            } else if (status.hasResolution()) {
                // Google provides a way to fix the issue
                try {
                    status.startResolutionForResult(
                            SimpleMapFragment.this.getActivity(),     // your current activity used to receive the result
                            LOCATIONSERVICES_RESOLUTION_RESULCODE); // the result code you'll look for in your onActivityResult method to retry registering
                } catch (IntentSender.SendIntentException e) {
                    Log.e(YourApplication.LOG_TAG, "SimpleMapFragment start resolution failure: ", e);
                }
            } else {
                // No recovery. Weep softly or inform the user.
                Log.e(YourApplication.LOG_TAG, "SimpleMapFragment registering failed: " + status.getStatusMessage());
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( requestCode == LOCATIONSERVICES_RESOLUTION_RESULCODE && resultCode == Activity.RESULT_OK ) {
            registerLocationUpdates();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (BuildConfig.DEBUG) {
            Log.d(YourApplication.LOG_TAG, "simpleMapFragment.onLocationChanged");
        }
        if ( mMap != null ) {
            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(loc, 15);
            mMap.animateCamera(cameraUpdate);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationOnMap() {
        if ( mPermissionGranted && mMap != null && !mMap.isMyLocationEnabled()) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.simplemap_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.simplemap_menu_info:
                new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.simplemap_info_title)
                        .items(R.array.simplemap_info_details)
                        .positiveText(R.string.close)
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        SnackbarHelper.showInfoLongMessage(this.getView(), R.string.error_connectionfailed);
    }

    @Override
    public void onConnectionSuspended(int i) {

        SnackbarHelper.showInfoLongMessage(this.getView(), R.string.error_connectionsuspended);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (BuildConfig.DEBUG) {
            Log.d(YourApplication.LOG_TAG, "simpleMapFragment.onMapCreated - map: " + googleMap);
        }
        if ( googleMap != null ) {
            mMap = googleMap;
            this.enableLocationOnMap();
        }
    }
}
