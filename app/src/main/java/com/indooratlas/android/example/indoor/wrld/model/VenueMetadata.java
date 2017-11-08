package com.indooratlas.android.example.indoor.wrld.model;

import android.support.annotation.Nullable;

import java.util.Arrays;

/**
 * Class that models venue metadata used within this app.
 */
public class VenueMetadata {

    public String name;

    public String iaId;

    public String wrldId;

    public FloorMetadata[] floors;

    public double[] coordinates;

    @Nullable
    public FloorMetadata findFloorByIaId(String id) {
        for (FloorMetadata floor : floors) {
            if (floor.iaId.equals(id)) {
                return floor;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VenueMetadata{");
        sb.append("name='").append(name).append('\'');
        sb.append(", iaId='").append(iaId).append('\'');
        sb.append(", wrldId='").append(wrldId).append('\'');
        sb.append(", floors=").append(Arrays.toString(floors));
        sb.append('}');
        return sb.toString();
    }

    public static class FloorMetadata {

        public String name;

        public String iaId;

        public int zOrder;

        public int index;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("FloorMetadata{");
            sb.append("name='").append(name).append('\'');
            sb.append(", iaId='").append(iaId).append('\'');
            sb.append(", zOrder=").append(zOrder);
            sb.append(", index=").append(index);
            sb.append('}');
            return sb.toString();
        }
    }
}
