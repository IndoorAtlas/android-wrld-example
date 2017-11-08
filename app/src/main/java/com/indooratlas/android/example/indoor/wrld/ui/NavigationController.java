package com.indooratlas.android.example.indoor.wrld.ui;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;

import com.eegeo.mapapi.EegeoMap;
import com.eegeo.mapapi.buildings.BuildingHighlight;
import com.eegeo.mapapi.buildings.BuildingHighlightOptions;
import com.eegeo.mapapi.camera.CameraPosition;
import com.eegeo.mapapi.camera.CameraUpdateFactory;
import com.eegeo.mapapi.geometry.LatLng;
import com.eegeo.mapapi.indoors.IndoorMap;
import com.eegeo.mapapi.indoors.OnIndoorEnteredListener;
import com.eegeo.mapapi.indoors.OnIndoorExitedListener;
import com.eegeo.mapapi.map.OnInitialStreamingCompleteListener;
import com.eegeo.mapapi.polygons.Polygon;
import com.eegeo.mapapi.polygons.PolygonOptions;
import com.google.maps.android.SphericalUtil;
import com.indooratlas.android.example.indoor.wrld.Global;
import com.indooratlas.android.example.indoor.wrld.R;
import com.indooratlas.android.example.indoor.wrld.Utils;
import com.indooratlas.android.example.indoor.wrld.location.LocationProducer;
import com.indooratlas.android.example.indoor.wrld.model.VenueMetadata;
import com.indooratlas.android.example.indoor.wrld.model.VenueMetadataStorage;

import timber.log.Timber;


/**
 *
 */
class NavigationController implements LocationProducer.Listener {

    private EegeoMap mMap;

    private BlueSphereHelper mBlueSphere;

    /**
     * The source of location events and indoor context information.
     */
    private LocationProducer mLocationProducer;

    /**
     * Metadata mapping between IndoorAtlas and WRLD
     */
    private VenueMetadataStorage mVenueStorage;

    private MapActivity mMapActivity;

    /**
     * Once true, it's OK to manipulate the map.
     */
    private boolean mInitialStreamingCompleted;

    /**
     * Should we keep the blue dot at the center of the screen.
     */
    private boolean mIsCenterOnLocation;

    /**
     * Should we rotate the camera according to location updates.
     */
    private boolean mIsAutoRotate = true;

    /**
     * Master switch for enabling or disabling UI navigation based on location&context updates.
     */
    private boolean mIsUiAutonavigate = true;

    private Snackbar mSnackbar;

    /**
     * Zoom level to apply when venue is detected.
     */
    private double mZoomLevelVenue = 17d;

    /**
     * Cached indoor map id in WRLD system.
     */
    private String mCurrentIndoorMapId;

    /**
     * Cached indoor floor id in WLRD system.
     */
    private int mCurrentIndoorFloorId;

    /**
     * Active building highlight if any.
     */
    private BuildingHighlight mBuildingHighlight;

    private Polygon mAccuracyCircle;


    NavigationController(MapActivity activity,
                         LocationProducer producer,
                         VenueMetadataStorage venueMetadataStorage) {
        mMapActivity = activity;
        mLocationProducer = producer;
        mVenueStorage = venueMetadataStorage;
        producer.setListener(this);
    }

    /**
     * If set to true, locations will move the camera so that location is displayed at center.
     */
    public NavigationController setCenterOnLocation(boolean value) {
        mIsCenterOnLocation = value;
        return this;
    }

    /**
     * Turns on/off automatic UI navigation. If this property is set to false, none of the other
     * UI features will also be turned off such as {@link #setCenterOnLocation(boolean)}.
     */
    public void setUiAutoNavigate(boolean enabled) {
        if (enabled == mIsUiAutonavigate) return;
        mIsUiAutonavigate = enabled;
        if (mIsUiAutonavigate) {
            if (mLocationProducer.isIndoors()) {
                handleEnterIndoors(mLocationProducer.getVenueId());
                handleFloorChange(mLocationProducer.getVenueId(),
                        mLocationProducer.getFloorId(),
                        mLocationProducer.getFloorLevel());
            } else if (mMap.getActiveIndoorMap() != null) {
                mMap.exitIndoorMap();
            }
            handleLocationUpdated(mLocationProducer.getLastLocation());
        } else {
            clearAccuracyCircle();
        }
    }

    /**
     * Returns true if automatic UI navigation based on user position in indoors/outdoors is
     * enabled.
     */
    public boolean isUiAutoNavigate() {
        return mIsUiAutonavigate;
    }


    /**
     * From LocationProducer
     */
    @Override
    public void onEnterIndoors(String venueId) {
        handleEnterIndoors(venueId);
    }


    /**
     * From LocationProducer. User has exited the venue to outdoors.
     */
    @Override
    public void onExitIndoors(String venueId) {

        if (mMap != null && mIsUiAutonavigate) {
            mMap.exitIndoorMap();
        } else {
            Timber.i("onExitIndoors map: %s, mIsAutonavigate: %b", mMap, mIsUiAutonavigate);
        }

        // todo: just guessing here, do we need to clear what
        mBlueSphere.setDisplayOnMap("", 0);

    }


    /**
     * From LocationProducer. User is in close proximity of a new venue.
     */
    @Override
    public void onEnterVenue(String venueId, @Nullable String venueName) {
        if (mMap == null) {
            return;
        }

        if (mIsUiAutonavigate) {
            CameraPosition position = new CameraPosition.Builder().zoom(mZoomLevelVenue).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
        enableBuildingHighLight();

    }


    /**
     * From LocationProducer. User has exited the close proximity of a venue.
     */
    @Override
    public void onExitVenue(String venueId) {
        Timber.d("onExitVenue");
        disableBuildingHighLight();
    }


    /**
     * From LocationProducer. Location update is either from indoor or outdoor provider.
     */
    @Override
    public void onLocationChanged(LocationProducer.LocationUpdate locationUpdate) {
        Timber.v("onLocationChanged: " + locationUpdate);
        if (mMap == null) {
            Timber.w("map not yet initialized, location update is ignored");
            return;
        }
        mBlueSphere.setLocation(locationUpdate.location);
    }


    /**
     * From LocationProducer. Called when floor change is detected.
     */
    @Override
    public void onFloorChanged(String venueId, String floorId, String floorLevel) {
        handleFloorChange(venueId, floorId, floorLevel);
    }

    /**
     * Called when EegeoMap is available.
     */
    void setMap(@NonNull final EegeoMap map) {

        mMap = map;

        mBlueSphere = new BlueSphereHelper(map.getBlueSphere()) {
            @Override
            protected void onUpdateLocation(double latitude, double longitude, float bearing,
                                            float accuracy) {
                // invoked on each animation frame, animate camera at same pace
                handleLocationUpdated(latitude, longitude, bearing, accuracy);
            }
        };

        mMap.addOnIndoorEnteredListener(new OnIndoorEnteredListener() {
            @Override
            public void onIndoorEntered() {
                Timber.d("onIndoorEntered, activeMap: " + Utils.toString(map.getActiveIndoorMap()));

                // replay the floor change detection if any
                handleFloorChange(mLocationProducer.getVenueId(),
                        mLocationProducer.getFloorId(),
                        mLocationProducer.getFloorLevel());

                disableBuildingHighLight();
            }
        });

        mMap.addOnIndoorExitedListener(new OnIndoorExitedListener() {
            @Override
            public void onIndoorExited() {
                enableBuildingHighLight();
            }
        });

        mMap.addInitialStreamingCompleteListener(new OnInitialStreamingCompleteListener() {
            @Override
            public void onInitialStreamingComplete() {

                Timber.d("initial streaming completed");
                mInitialStreamingCompleted = true;

                Location lastLocation = mLocationProducer.getLastLocation();
                if (lastLocation != null) {
                    LatLng latLng = new LatLng(lastLocation.getLatitude(),
                            lastLocation.getLongitude());

                    Timber.d("moving camera to last location: %s", lastLocation);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.addOnCameraMoveListener(new OneTimeCameraMoveListener() {

                        @Override
                        public void onCameraMoveOnce() {
                            Timber.d("initial camera move done");
                            // if venue is already detected before map is available
                            if (mLocationProducer.isIndoors()
                                    && mMap.getActiveIndoorMap() == null) {

                                Timber.d("streaming completed, camera moved, indoors detected, "
                                        + "requesting indoor map");
                                handleEnterIndoors(mLocationProducer.getVenueId());

                            }
                        }
                    });

                }


            }
        });

    }

    private void handleLocationUpdated(Location location) {
        if (location != null) {
            handleLocationUpdated(location.getLatitude(),
                    location.getLongitude(),
                    location.getBearing(),
                    location.getAccuracy());
        }
    }

    private void handleLocationUpdated(double latitude, double longitude, float bearing,
                                       float accuracy) {


        if (Global.isDebug) {
            clearAccuracyCircle();
            if (mMap.getActiveIndoorMap() != null) {
                int points = 50;
                int p = 360 / points;
                int d = 0;
                PolygonOptions polygonOptions = new PolygonOptions();
                polygonOptions.fillColor(ContextCompat.getColor(mMapActivity,
                        R.color.accuracyCircle));
                polygonOptions.elevation(0d);
                if (mMap.getActiveIndoorMap() != null) {
                    polygonOptions.indoor(mCurrentIndoorMapId, mCurrentIndoorFloorId);
                }
                LatLng center = new LatLng(latitude, longitude);
                for (int i = 0; i < points; ++i, d += p) {
                    polygonOptions.add(SphericalUtil.computeOffset(center, accuracy, d));
                }
                Timber.v("adding new accuracy circle");
                mAccuracyCircle = mMap.addPolygon(polygonOptions);
            }
        }

        if (mIsUiAutonavigate) {
            CameraPosition.Builder update = new CameraPosition.Builder();
            if (mIsCenterOnLocation) {
                update.target(latitude, longitude);
            }
            if (mIsAutoRotate) {
                update.bearing(bearing);
            }
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(update.build()));
        }
    }

    private void clearAccuracyCircle() {
        if (mMap != null && mAccuracyCircle != null) {
            Timber.v("removing old accuracy circle");
            mMap.removePolygon(mAccuracyCircle);
            mAccuracyCircle = null;
        }
    }


    /**
     * Logic for displaying WRLD indoor map if any.
     */
    private void handleEnterIndoors(@Nullable String venueId) {

        if (!mIsUiAutonavigate) {
            Timber.d("UI auto navigation disabled, not entering indoor view");
            return;
        }

        if (venueId == null) {
            Timber.w("will not handle enter indoors with null venue id");
            return;
        }
        if (!mInitialStreamingCompleted) {
            Timber.w("initial streaming not yet completed, not handling enter indoors, venueId: %s",
                    venueId);
            return;
        }

        VenueMetadata venue = mVenueStorage.findByIaId(venueId);
        if (venue != null) {
            Timber.d("opening indoor map for venue: %s", venue);
            mMap.enterIndoorMap(venue.wrldId);
        } else {
            Timber.w("no venue metadata found for id: %s", venueId);
        }
    }


    /**
     * Logic for changing a displayed floor if any.
     */
    private void handleFloorChange(@Nullable String venueId, @Nullable String floorId,
                                   @Nullable String floorLevel) {

        Timber.d("handleFloorChange, venueId: %s, floorId: %s", venueId, floorId);

        if (!mIsUiAutonavigate) {
            Timber.d("UI auto navigation disabled, not handling floor change");
            return;
        }

        if (!mInitialStreamingCompleted) {
            Timber.w("initial streaming not yet completed, not handling floor change");
            return;
        }

        if (venueId == null || floorId == null) {
            Timber.w("not handling floor change, missing ids, venue: %s, floor: %s", venueId,
                    floorId);
            return;
        }

        final IndoorMap activeIndoorMap = mMap.getActiveIndoorMap();
        if (activeIndoorMap == null) {
            Timber.w("floor changed but no indoor map is active");
            return;
        }

        // we may get floor change event though this is a venue we have detected
        final VenueMetadata venue = mVenueStorage.findByIaId(venueId);
        if (venue == null) {
            Timber.w("floor changed but we have no venue metadata for it, venue id: %s", venueId);
            return;
        }

        // floor was changed in the indoor map user is currently viewing
        VenueMetadata.FloorMetadata floor = venue.findFloorByIaId(floorId);
        if (floor == null) {
            Timber.w("floor changed but we have no floor metadata for it, floor id %s", floorId);
            // try to hide blue dot since we've moved to a floor for which we have no metadata
            mBlueSphere.setDisplayOnMap("", 0);
            return;
        }

        Timber.d("switching to floor with zOrder: %d", floor.zOrder);
        mMap.setIndoorFloor(floor.zOrder);

        Timber.d("displaying blue sphere in map: %s, floor: %d", venue.wrldId, floor.index);
        mCurrentIndoorMapId = venue.wrldId;
        mCurrentIndoorFloorId = floor.index;

        mBlueSphere.setDisplayOnMap(venue.wrldId, floor.index);

        FloorLevelView.show(mMapActivity, floorLevel, mMapActivity.getString(R.string.floor_level_subtitle));
    }

    /**
     *
     */
    private void disableBuildingHighLight() {
        if (mMap != null && mBuildingHighlight != null) {
            mMap.removeBuildingHighlight(mBuildingHighlight);
            mBuildingHighlight = null;
        }
    }

    /**
     *
     */
    private void enableBuildingHighLight() {
        if (mMap == null) {
            return;
        }
        String venueId = mLocationProducer.getVenueId();
        VenueMetadata venue = mVenueStorage.findByIaId(venueId);
        if (venue != null && venue.coordinates != null && venue.coordinates.length == 2) {
            LatLng latLng = new LatLng(venue.coordinates[0], venue.coordinates[1]);
            mBuildingHighlight = mMap.addBuildingHighlight(new BuildingHighlightOptions()
                    .highlightBuildingAtLocation(latLng)
                    .color(ContextCompat.getColor(mMapActivity, R.color.venue_highlight)));
        }

    }


}
