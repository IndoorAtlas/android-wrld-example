package com.indooratlas.android.example.indoor.wrld.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.eegeo.indoors.IndoorMapView;
import com.eegeo.mapapi.EegeoApi;
import com.eegeo.mapapi.EegeoMap;
import com.eegeo.mapapi.MapView;
import com.eegeo.mapapi.geometry.LatLngAlt;
import com.eegeo.mapapi.map.OnMapReadyCallback;
import com.indooratlas.android.example.indoor.wrld.R;
import com.indooratlas.android.example.indoor.wrld.location.IndoorOutdoorLocationProducer;
import com.indooratlas.android.example.indoor.wrld.location.LocationProducer;
import com.indooratlas.android.example.indoor.wrld.model.VenueMetadataStorage;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


/**
 * Activity that displays indoor/outdoor locations on WRLD3D map.
 */
public class MapActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int RC_FINE_LOCATION = 100;

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.BLUETOOTH_ADMIN
    };

    @BindView(R.id.mapView)
    MapView mMapView;

    @BindView(R.id.button_toggle_tracking)
    Button mRecenterButton;

    @BindView(R.id.coordinator)
    CoordinatorLayout mCoordinatorLayout;

    @BindView(R.id.overlay)
    FrameLayout mOverlay;

    private EegeoMap mMap;
    private boolean mPermissionsIgnored;
    private NavigationController mNavigationController;
    private LocationProducer mLocationProducer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
        EegeoApi.init(this, getString(R.string.eegeo_api_key));

        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(EegeoMap map) {
                initMap(map);
            }
        });

        mLocationProducer = new IndoorOutdoorLocationProducer(this);

        // ... or to debug with pre-configured location events
        //    mLocationProducer = new PlaybackLocationProducer(this,
        //            PlaybackLocationProducer.DEFAULT_DELAY)
        //            .setData(R.raw.playback_kluuvi);

        mNavigationController = new NavigationController(this, mLocationProducer,
                VenueMetadataStorage.getInstance(this));
        mNavigationController.setCenterOnLocation(true);

        mRecenterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // enable automatic navigation when explicitly asked
                mNavigationController.setUiAutoNavigate(true);
            }
        });

        mOverlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // disable automatic navigation when user manually moves around the map
                mNavigationController.setUiAutoNavigate(false);
                return false;
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        initLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        mLocationProducer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mLocationProducer.destroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_share_trace_id:
                // IndoorAtlas SDK provides an identifier that can be used to pinpoint
                // positioning sessions. This service is available for paid accounts.
                String id = mLocationProducer.getDebugId();
                if (!TextUtils.isEmpty(id)) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, id);
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;

    }

    @AfterPermissionGranted(RC_FINE_LOCATION)
    private void initLocationUpdates() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            mLocationProducer.start();
        } else if (!mPermissionsIgnored) {
            EasyPermissions.requestPermissions(this,
                    getString(R.string.rationale_fine_location),
                    RC_FINE_LOCATION,
                    LOCATION_PERMISSIONS);
        }
    }


    private void initMap(EegeoMap map) {

        mMap = map;
        mNavigationController.setMap(map);
        new IndoorMapView(mMapView,
                (RelativeLayout) findViewById(R.id.mapViewContainer), mMap);
        mMap.addOnMapClickListener(new EegeoMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLngAlt point) {
                // N/A
            }
        });

    }


    @Override
    public void onBackPressed() {
        if (mMap != null && mMap.getActiveIndoorMap() != null) {
            mMap.exitIndoorMap();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        // Thanks!
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .build()
                    .show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE
                && resultCode == RESULT_CANCELED) {
            mPermissionsIgnored = true;
        }
    }
}
