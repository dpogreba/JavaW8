package com.antbear.javaw8.map;

import android.content.Context;
import android.util.Log;

/**
 * Factory class to create the osmdroid map provider.
 */
public class MapFactory {
    private static final String TAG = "MapFactory";
    
    /**
     * Create the osmdroid map provider.
     * 
     * @param context Application context
     * @return The osmdroid map provider
     */
    public static MapProvider createMapProvider(Context context) {
        Log.d(TAG, "Creating OSM provider");
        MapProvider provider = new OsmdroidProvider();
        
        // Initialize the provider
        provider.initialize(context);
        
        return provider;
    }
}
