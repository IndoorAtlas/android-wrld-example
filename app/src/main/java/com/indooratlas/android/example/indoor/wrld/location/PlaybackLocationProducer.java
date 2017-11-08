package com.indooratlas.android.example.indoor.wrld.location;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import com.indooratlas.android.example.indoor.wrld.R;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Helper class for testing UI logic by producing location events from a proprietary JSON file.
 */
public class PlaybackLocationProducer implements LocationProducer {

    public static final long DEFAULT_DELAY = 1000;
    private PlaybackHandler mHandler;

    private LocationProducer.Listener mListener;

    private long mUpdateDelayMillis;

    private Context mContext;

    private JSONArray mEvents;

    private boolean mLooping;

    private boolean mIsIndoors;

    private volatile String mCurrentVenueId;

    private volatile String mCurrentFloorId;

    private volatile String mCurrentFloorLevel;

    private int mPlaybackResourceId;

    private volatile Location mLastLocation;

    public PlaybackLocationProducer(Context context, long updateDelayMillis) {
        mContext = context;
        mUpdateDelayMillis = updateDelayMillis;
        HandlerThread thread = new HandlerThread("PlaybackHandler");
        thread.start();
        mHandler = new PlaybackHandler(this, thread.getLooper());
        mPlaybackResourceId = R.raw.playback_kamppi;
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    public PlaybackLocationProducer setLooping(boolean isLooping) {
        mLooping = isLooping;
        return this;
    }

    public PlaybackLocationProducer setData(int resourceId) {
        mPlaybackResourceId = resourceId;
        return this;
    }

    @Override
    public void start() {
        mIsIndoors = false;
        mHandler.sendEmptyMessage(0);
    }

    @Override
    public void stop() {
        mHandler.sendEmptyMessage(PlaybackHandler.CMD_STOP);
    }

    @Override
    public void destroy() {
        mHandler.sendEmptyMessage(PlaybackHandler.CMD_QUIT);
        mHandler = null;
    }

    @Override
    public String getDebugId() {
        // N/A
        return null;
    }

    /**
     * Parse events into memory unless already parsed.
     */
    private void init() throws IOException, JSONException {
        if (mEvents != null) {
            return;
        }
        InputStream in = null;
        try {
            in = mContext.getResources().openRawResource(mPlaybackResourceId);
            mEvents = new JSONArray(IOUtils.toString(in));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Invoke listener with N'th event.
     *
     * @return index of the event actually sent or -1 if there are not more events to be sent.
     */
    private int dispatchEvent(int index) throws JSONException {
        if (index < 0 || index >= mEvents.length()) {
            Timber.e("asked to dispatch index %d  but we only have %d items",
                    index, mEvents.length());
            return -1;
        }
        JSONObject event = mEvents.getJSONObject(index);
        Timber.v("processing event: %s", event);
        switch (event.getString("type")) {
            case "location":
                JSONArray coordinates = event.getJSONArray("coordinates");
                Location location = new Location("Playback");
                location.setLatitude(coordinates.getDouble(0));
                location.setLongitude(coordinates.getDouble(1));
                mLastLocation = location;
                mListener.onLocationChanged(mIsIndoors
                        ? LocationUpdate.indoor(mLastLocation)
                        : LocationUpdate.outdoor(mLastLocation));
                break;
            case "context":
                switch (event.getString("kind")) {
                    case "venue-entry":
                        mCurrentVenueId = event.getString("id");
                        mListener.onEnterVenue(mCurrentVenueId, event.optString("name"));
                        break;
                    case "floor-entry":
                        if (!mIsIndoors) {
                            mIsIndoors = true;
                            mListener.onEnterIndoors(mCurrentVenueId);
                        }
                        mCurrentFloorId = event.getString("id");
                        mCurrentFloorLevel = event.optString("floorLevel", null);
                        mListener.onFloorChanged(mCurrentVenueId, mCurrentFloorId,
                                mCurrentFloorLevel);
                        break;
                    case "venue-exit":
                        final String venueId = mCurrentVenueId;
                        mIsIndoors = false;
                        mCurrentVenueId = null;
                        mCurrentFloorId = null;
                        mListener.onExitVenue(venueId);
                        mListener.onExitIndoors(venueId);
                        break;
                }
                break;
        }
        if (index == mEvents.length() - 1) {
            if (mLooping) {
                index = 0;
            } else {
                // end of items, no looping
                index = -1;
            }
        }
        return index;
    }

    @Override
    public boolean isIndoors() {
        return mIsIndoors;
    }

    @Override
    public String getVenueId() {
        return mCurrentVenueId;
    }

    @Override
    public String getFloorId() {
        return mCurrentFloorId;
    }

    @Nullable
    @Override
    public String getFloorLevel() {
        return mCurrentFloorLevel;
    }

    @Nullable
    @Override
    public Location getLastLocation() {
        return mLastLocation;
    }

    private static class PlaybackHandler extends Handler {

        private final WeakReference<PlaybackLocationProducer> mProducerRef;

        private static final int CMD_STOP = -1;
        private static final int CMD_QUIT = -2;

        private PlaybackHandler(PlaybackLocationProducer producer, Looper looper) {
            super(looper);
            mProducerRef = new WeakReference<>(producer);
        }

        @Override
        public void handleMessage(Message msg) {

            final int what = msg.what;
            PlaybackLocationProducer producer = mProducerRef.get();
            if (producer == null || what == CMD_QUIT) {
                getLooper().quit();
                return;
            }

            if (what == CMD_STOP) {
                removeCallbacksAndMessages(null);
                return;
            }

            try {
                producer.init();
                int index = producer.dispatchEvent(what);
                if (index >= 0) {
                    // process next event after a configured delay
                    sendMessageDelayed(Message.obtain(null, index + 1), producer.mUpdateDelayMillis);
                } else {
                    Timber.d("end of playback reached, not looping");
                }
            } catch (Exception e) {
                Timber.e(e, "processing playback data failed");
                getLooper().quit();
            }

        }

    }
}
