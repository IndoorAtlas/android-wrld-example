/*
 *
 */
package com.indooratlas.android.example.indoor.wrld.location;

import android.location.Location;
import android.support.annotation.Nullable;

/**
 * Abstracts location producing to make simulation & testing easier.
 */
public interface LocationProducer {

    /**
     * Called to start producing location updates.
     */
    void start();

    /**
     * Called to stop producing location updates.
     */
    void stop();

    /**
     * Called to release any allocated resources. There will be no further calls to this instance.
     */
    void destroy();

    /**
     * Use this listener as the "consumer" of the location updates.
     */
    void setListener(Listener listener);

    /**
     * Returns {@code true} if it appears that user is indoors.
     */
    boolean isIndoors();


    /**
     * Returns the currently detected venue id or {@code null}. The venue id is an IndoorAtlas
     * specific id (a.k.a location id).
     */
    @Nullable
    String getVenueId();

    /**
     * Returns the currently detected floor id or {@code null}. The floor id is an IndoorAtlas
     * specific id (a.k.a floor plan id).
     */
    @Nullable
    String getFloorId();


    /**
     * Returns the currently detected floor level of {@code null}.
     */
    @Nullable
    String getFloorLevel();


    /**
     * Convenience method for returning last seen location or {@code null} if non have been heard
     * yet.
     */
    @Nullable
    Location getLastLocation();

    /**
     * Id that identifies the positioning session for debugging purposes.
     */
    String getDebugId();

    /**
     * The consumer interface.
     */
    interface Listener {

        /**
         * Triggered for for both indoor and outdoor location updates.
         */
        void onLocationChanged(LocationUpdate update);

        /**
         * Triggered when user enters first floor or switches from one floor to another. The switch
         * can also be on the same floor from one floor plan to another.
         */
        void onFloorChanged(String venueId, String floorId, String floorLevel);

        /**
         * Triggered when it appears that user has entered indoor space. To be more exact, area
         * which is mapped.
         */
        void onEnterIndoors(String venueId);

        /**
         * Triggered when it appears that user leaves indoor space.
         */
        void onExitIndoors(String venueId);

        /**
         * Triggered when it appears that user has entered near proximity of a venue.
         */
        void onEnterVenue(String venueId, @Nullable String venueName);

        /**
         * Triggered when it appears that user has exited near proximity of a venue.
         */
        void onExitVenue(String venueId);

    }

    class ListenerSupport implements Listener {

        @Override
        public void onLocationChanged(LocationUpdate update) {

        }

        @Override
        public void onFloorChanged(String venueId, String floorId, String floorLevel) {

        }

        @Override
        public void onEnterIndoors(String venueId) {

        }

        @Override
        public void onExitIndoors(String venueId) {

        }

        @Override
        public void onEnterVenue(String venueId, @Nullable String venueName) {

        }

        @Override
        public void onExitVenue(String venueId) {

        }
    }

    class LocationUpdate {

        public enum Source {
            INDOOR,
            OUTDOOR
        }

        public final Location location;

        public final Source source;

        private LocationUpdate(Source source, Location location) {
            this.source = source;
            this.location = location;
        }


        public static LocationUpdate indoor(Location location) {
            return new LocationUpdate(Source.INDOOR, location);
        }

        public static LocationUpdate outdoor(Location location) {
            return new LocationUpdate(Source.OUTDOOR, location);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LocationUpdate{");
            sb.append("location=").append(location);
            sb.append(", source=").append(source);
            sb.append('}');
            return sb.toString();
        }
    }
}
