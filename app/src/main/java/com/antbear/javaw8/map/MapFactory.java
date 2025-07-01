package com.antbear.javaw8.map;

import android.content.Context;
import android.util.Log;

/**
 * Factory class to create map providers (Google Maps or OSM).
 */
public class MapFactory {
    private static final String TAG = "MapFactory";
    
    /**
     * Create the appropriate map provider based on preference.
     * 
     * @param context Application context
     * @return The map provider (Google Maps or OSM)
     */
    public static MapProvider createMapProvider(Context context) {
        // Check if we should use Google Maps based on preference
        boolean useGoogleMaps = MapTogglePreference.getPreference(context);
        
        if (useGoogleMaps) {
            Log.d(TAG, "Creating Google Maps provider");
            MapProvider provider = new GoogleMapsProvider();
            provider.initialize(context);
            return provider;
        } else {
            Log.d(TAG, "Creating OSM provider");
            MapProvider provider = new OsmdroidProvider();
            provider.initialize(context);
            return provider;
        }
    }
}
