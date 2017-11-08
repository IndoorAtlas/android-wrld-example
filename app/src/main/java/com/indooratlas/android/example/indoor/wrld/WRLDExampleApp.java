package com.indooratlas.android.example.indoor.wrld;

import android.app.Application;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import timber.log.Timber;

/**
 *
 */
public class WRLDExampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        }

        Global.loadFrom(PreferenceManager.getDefaultSharedPreferences(this));
    }

    class DebugTree extends Timber.DebugTree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            super.log(priority, tag, message, t);
            if (Global.isDebug
                    && (priority == Log.INFO || priority == Log.WARN || priority == Log.ERROR)) {
                Toast.makeText(WRLDExampleApp.this, message, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
