package com.indooratlas.android.example.indoor.wrld;

import android.content.SharedPreferences;

/**
 *
 */
public final class Global {

    public static String indoorAtlasSdkVersion;

    public static boolean isDebug;

    private Global() {
    }

    public static void loadFrom(SharedPreferences prefs) {
        isDebug = prefs.getBoolean("enable_debug", false);
    }

}
