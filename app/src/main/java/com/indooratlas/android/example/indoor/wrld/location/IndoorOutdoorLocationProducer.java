package com.indooratlas.android.example.indoor.wrld.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.example.indoor.wrld.Global;

import timber.log.Timber;

/**
 * Implementation of {@link LocationProducer} which fuses together locations from indoors
 * (IndoorAtlas) and outdoors (platform location). Note that this class is not thread safe and not
 * intended for production use.
 */
public class IndoorOutdoorLocationProducer implements
        LocationProducer,
        IALocationListener,
        LocationListener,
        IARegion.Listener {

    private static final String TAG = IndoorOutdoorLocationProducer.class.getSimpleName();

    /** Delay after last exit-floor event when we'll fire exit venue locally. */
    private static final long EXIT_VENUE_DELAY = 1500;

    /** IndoorAtlas location manager for indoor positions. */
    private IALocationManager mIndoorManager;

    /** Platform location manager for outdoor locations (GPS). */
    private LocationManager mOutdoorManager;

    /** Listener for the location related events we produce. */
    private LocationProducer.Listener mListener = new ListenerSupport();

    /** True when it's detected that we've entered indoor space - according to IndoorAtlas IPS */
    private volatile boolean mIsIndoors;

    private volatile String mCurrentVenueId;
    private volatile String mCurrentFloorPlanId;
    private volatile Location mLastLocation;
    private volatile int mCurrentFloorLevel;
    private boolean mPendingFloorChange;

    private final Handler mHandler = new Handler();

    public IndoorOutdoorLocationProducer(Context context) {
        mIndoorManager = IALocationManager.create(context);
        mOutdoorManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Global.indoorAtlasSdkVersion = mIndoorManager.getExtraInfo().version;
    }

    /**
     * Indoor location is updated.
     */
    @Override
    public void onLocationChanged(IALocation location) {

        if (mPendingFloorChange) {
            mPendingFloorChange = false;
            mCurrentFloorLevel = location.getFloorLevel();
            mListener.onFloorChanged(mCurrentVenueId, mCurrentFloorPlanId,
                    String.valueOf(mCurrentFloorLevel));
        }

        Location tmp = location.toLocation();
        if (mIsIndoors) {
            // only updating last location from IndoorAtlas if it's already detected that we are
            // indoors
            mLastLocation = tmp;
            mListener.onLocationChanged(LocationUpdate.indoor(tmp));
        }

    }

    /**
     * Outdoor location is updated.
     */
    @Override
    public void onLocationChanged(@Nullable Location location) {
        if (location == null) {
            // last known location can be null
            return;
        }
        if (!mIsIndoors) {
            // update current location but don't override with lower quality locations
            mLastLocation = location;
            mListener.onLocationChanged(LocationUpdate.outdoor(mLastLocation));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle bundle) {
        Timber.d("onStatusChanged: " + provider + ", status");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Timber.d("onProviderEnabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Timber.d("onProviderDisabled: " + provider);

    }

    @Override
    public void onEnterRegion(IARegion region) {
        Timber.d("onEnterRegion: " + region);
        mHandler.removeCallbacks(mExitVenueRunnable);
        switch (region.getType()) {
            case IARegion.TYPE_VENUE:
                Timber.i("enter venue: %s, name: %s", region.getId(), region.getName());
                mCurrentFloorPlanId = null;
                mCurrentVenueId = region.getId();
                mListener.onEnterVenue(mCurrentVenueId, region.getName());
                break;
            case IARegion.TYPE_FLOOR_PLAN:
                Timber.i("enter floor plan: %s", region.getId());
                mCurrentFloorPlanId = region.getId();
                if (!mIsIndoors) {
                    mIsIndoors = true;
                    mListener.onEnterIndoors(mCurrentVenueId);
                }
                mPendingFloorChange = true;
                break;
            default:
                Timber.w("unsupported region type: %s" + region);
        }
    }

    @Override
    public void onExitRegion(IARegion region) {
        switch (region.getType()) {
            case IARegion.TYPE_FLOOR_PLAN:
                mCurrentFloorLevel = -1;
                mCurrentFloorPlanId = null;
                mHandler.postDelayed(mExitVenueRunnable, EXIT_VENUE_DELAY);
                break;
            case IARegion.TYPE_VENUE:
                Timber.i("exit venue, mIsIndoors: %b", mIsIndoors);
                if (mIsIndoors) {
                    mIsIndoors = false;
                    mListener.onExitIndoors(mCurrentVenueId);
                }
                mListener.onExitVenue(mCurrentVenueId);
                mCurrentVenueId = null;
                break;
            default:
                Timber.w(TAG, "unsupported region type: %s", region);
        }

    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener != null ? listener : new ListenerSupport();
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void start() {
        Timber.d("start requesting location updates");
        mIndoorManager.requestLocationUpdates(IALocationRequest.create(), this);
        mIndoorManager.registerRegionListener(this);
        mOutdoorManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        onLocationChanged(mOutdoorManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
    }

    @Override
    public void stop() {
        Timber.d("stop requesting location updates");
        mIndoorManager.removeLocationUpdates(this);
        mIndoorManager.unregisterRegionListener(this);
        mOutdoorManager.removeUpdates(this);
    }

    @Override
    public void destroy() {
        mIndoorManager.destroy();
    }

    @Override
    public boolean isIndoors() {
        return mIsIndoors;
    }

    @Override
    public String getVenueId() {
        return mCurrentVenueId;
    }

    @Nullable
    @Override
    public String getFloorId() {
        return mCurrentFloorPlanId;
    }

    @Nullable
    @Override
    public Location getLastLocation() {
        return mLastLocation;
    }

    @Override
    public String getFloorLevel() {
        return String.valueOf(mCurrentFloorLevel);
    }

    @Override
    public String getDebugId() {
        return mIndoorManager.getExtraInfo().traceId;
    }

    private Runnable mExitVenueRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.d("triggering timed venue exit for %s", mCurrentVenueId);
            // manually trigger exit venue after N seconds if it is not triggered by the server
            onExitRegion(IARegion.venue(mCurrentVenueId));
        }
    };
}
