package com.indooratlas.android.example.indoor.wrld.ui;

import com.eegeo.mapapi.EegeoMap;

/**
 *
 */
public abstract class OneTimeCameraMoveListener implements EegeoMap.OnCameraMoveListener {

    private boolean mHandled;

    @Override
    public final void onCameraMove() {
        if (!mHandled) {
            mHandled = true;
            onCameraMoveOnce();
        }
    }

    public abstract void onCameraMoveOnce();
}
