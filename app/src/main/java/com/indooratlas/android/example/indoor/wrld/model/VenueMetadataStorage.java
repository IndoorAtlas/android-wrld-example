package com.indooratlas.android.example.indoor.wrld.model;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.indooratlas.android.example.indoor.wrld.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 */
public class VenueMetadataStorage {

    private static VenueMetadataStorage sSingleton;

    private VenueMetadata[] mVenues;

    private VenueMetadataStorage(Context context) {
        // never do i/o in main thread
        InputStream in = context.getResources().openRawResource(R.raw.venues);
        try {
            Gson gson = new Gson();
            mVenues = gson.fromJson(new InputStreamReader(in), VenueMetadata[].class);
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


    public static VenueMetadataStorage getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new VenueMetadataStorage(context);
        }
        return sSingleton;
    }

    @Nullable
    public VenueMetadata findByIaId(String id) {
        for (VenueMetadata venue : mVenues) {
            if (venue.iaId.equals(id)) {
                return venue;
            }
        }
        return null;
    }
}
