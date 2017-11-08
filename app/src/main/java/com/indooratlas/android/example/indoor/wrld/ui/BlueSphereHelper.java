package com.indooratlas.android.example.indoor.wrld.ui;

import android.animation.ValueAnimator;
import android.location.Location;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.eegeo.mapapi.bluesphere.BlueSphere;
import com.eegeo.mapapi.geometry.LatLng;

import timber.log.Timber;

/**
 * Helper class to animate the blue dot.
 */
class BlueSphereHelper {

    private final BlueSphere mBlueSphere;
    private Location mCurrentLocation;
    private ValueAnimator mLocationChangeAnimator;
    private Interpolator mInterpolator;
    private int mNumSamples;
    private long mCreateMillis;

    BlueSphereHelper(BlueSphere blueSphere) {
        mBlueSphere = blueSphere;
        mBlueSphere.setEnabled(true);
        mBlueSphere.setElevation(0.0);
        mInterpolator = new AccelerateDecelerateInterpolator();
        mCreateMillis = SystemClock.elapsedRealtime();
    }

    void setDisplayOnMap(String mapId, int floorId) {
        Timber.d("setIndoorMap, mapId: %s, floorId: %d", mapId, floorId);
        mBlueSphere.setIndoorMap(mapId, floorId);
    }


    public void setLocation(Location location) {

        mNumSamples++;

        if (location.equals(mCurrentLocation)) {
            Timber.v("no change in location, ignoring");
            return;
        }

        if (mCurrentLocation != null) {

            if (mLocationChangeAnimator != null) {
                mLocationChangeAnimator.end();
            }

            mLocationChangeAnimator = ValueAnimator.ofFloat(0, 1);
            mLocationChangeAnimator.setDuration(Math.min(
                    (SystemClock.elapsedRealtime() - mCreateMillis) / mNumSamples,
                    1000)); // max animation duration
            mLocationChangeAnimator.setInterpolator(mInterpolator);
            mLocationChangeAnimator.addUpdateListener(new LocationAnimator(mCurrentLocation,
                    location));
            mLocationChangeAnimator.start();
        } else {
            applyLocation(location.getLatitude(), location.getLongitude(), location.getBearing(),
                    location.getAccuracy());
        }

        mCurrentLocation = location;
    }

    private void applyLocation(double latitude, double longitude, float bearing, float accuracy) {
        LatLng latLng = new LatLng(latitude, longitude);
        mBlueSphere.setPosition(latLng);
        mBlueSphere.setBearing(bearing + 180f);
        onUpdateLocation(latitude, longitude, bearing, accuracy);
    }

    /**
     * Invoked on each blue dot animation frame. Override for custom handling. No need to call
     * super.
     */
    protected void onUpdateLocation(double latitude, double longitude, float bearing,
                                    float accuracy) {
        // N/A
    }


    private class LocationAnimator implements ValueAnimator.AnimatorUpdateListener {

        private double mAnimateDeltaLat;
        private double mAnimateDeltaLon;
        private double mAnimateDeltaBearing;
        private Location mFromLocation;
        private Location mToLocation;

        private LocationAnimator(Location fromLocation, Location toLocation) {
            mFromLocation = fromLocation;
            mToLocation = toLocation;
            mAnimateDeltaLat = toLocation.getLatitude() - fromLocation.getLatitude();
            mAnimateDeltaLon = toLocation.getLongitude() - fromLocation.getLongitude();

            double fromBearingRad = fromLocation.getBearing() * Math.PI / 180;
            double toBearingRad = toLocation.getBearing() * Math.PI / 180;

            mAnimateDeltaBearing = Math.atan2(Math.sin(toBearingRad - fromBearingRad),
                    Math.cos(toBearingRad - fromBearingRad)) * (180 / Math.PI);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float fraction = valueAnimator.getAnimatedFraction();
            double newLat;
            double newLon;
            double newBearing;

            if (fraction == 1.0) {
                newLat = mToLocation.getLatitude();
                newLon = mToLocation.getLongitude();
                newBearing = mToLocation.getBearing();
            } else {
                newLat = (mFromLocation.getLatitude() + (mAnimateDeltaLat * fraction));
                newLon = (mFromLocation.getLongitude() + (mAnimateDeltaLon * fraction));
                newBearing = (mFromLocation.getBearing() + (mAnimateDeltaBearing * fraction));
            }
            applyLocation(newLat, newLon, (float) newBearing, mToLocation.getAccuracy());
        }
    }
}
