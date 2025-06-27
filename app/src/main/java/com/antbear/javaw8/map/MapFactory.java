package com.antbear.javaw8.map;

import android.content.Context;
import android.util.Log;

/**
 * Factory class to create the map provider.
 * Currently configured to always use osmdroid.
 */
public class MapFactory {
    private static final String TAG = "MapFactory";
    
    /**
     * Create a map provider.
     * 
     * @param context Application context
     * @return The osmdroid map provider
     */
    public static MapProvider createMapProvider(Context context) {
        return createMapProvider(context, false);
    }
    
    /**
     * Create a specific map provider.
     * Currently always returns OsmdroidProvider regardless of the parameter.
     * 
     * @param context Application context
     * @param useGoogleMaps Ignored parameter (kept for future compatibility)
     * @return The osmdroid map provider
     */
    public static MapProvider createMapProvider(Context context, boolean useGoogleMaps) {
        Log.d(TAG, "Creating OSM provider (Google Maps support disabled)");
        MapProvider provider = new OsmdroidProvider();
        
        // Initialize the provider
        provider.initialize(context);
        
        return provider;
    }
    
    /**
     * Check if we're currently using Google Maps.
     * Currently always returns false as Google Maps is disabled.
     * 
     * @param context Application context
     * @return Always false (using osmdroid)
     */
    public static boolean isUsingGoogleMaps(Context context) {
        return false;
    }
}
