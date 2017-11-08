package com.indooratlas.android.example.indoor.wrld;

import com.eegeo.mapapi.indoors.IndoorMap;

import java.util.Arrays;

/**
 *
 */
public final class Utils {

    private Utils() {
        // N/A
    }

    /***
     * Convenience method to write IndoorMap as string.
     */
    public static String toString(IndoorMap map) {
        if (map == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("id: ").append(map.id)
                .append(", name: ").append(map.name)
                .append(", floorCount: ").append(map.floorCount)
                .append(", userData: ").append(map.userData)
                .append(", floorIds: ").append(Arrays.toString(map.floorIds))
                .append(", floorNames: ").append(Arrays.toString(map.floorNames))
                .append(", floorNumbers: ").append(Arrays.toString(map.floorNumbers));
        return sb.toString();
    }


}
