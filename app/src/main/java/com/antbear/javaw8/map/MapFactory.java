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
     * Will automatically fall back to OSM if Google Maps fails to initialize properly.
     * 
     * @param context Application context
     * @return The map provider (Google Maps or OSM)
     */
    public static MapProvider createMapProvider(Context context) {
        // Check if we should use Google Maps based on preference
        boolean useGoogleMaps = MapTogglePreference.getPreference(context);
        
        if (useGoogleMaps) {
            try {
                Log.d(TAG, "Creating Google Maps provider");
                GoogleMapsProvider googleProvider = new GoogleMapsProvider();
                googleProvider.initialize(context);
                
                // Use Google Maps only if it was properly initialized
                if (googleProvider.isInitialized()) {
                    Log.d(TAG, "Google Maps provider initialized successfully");
                    return googleProvider;
                } else {
                    // If Google Maps failed to initialize, fall back to OSM
                    Log.w(TAG, "Google Maps provider failed to initialize, falling back to OSM");
                    // Temporarily switch preference to OSM
                    MapTogglePreference.setPreference(context, false);
                    return createOsmProvider(context);
                }
            } catch (Exception e) {
                // If there was an exception creating the Google Maps provider, fall back to OSM
                Log.e(TAG, "Exception creating Google Maps provider, falling back to OSM: " + e.getMessage());
                // Temporarily switch preference to OSM
                MapTogglePreference.setPreference(context, false);
                return createOsmProvider(context);
            }
        } else {
            // OSM was already the preferred provider
            return createOsmProvider(context);
        }
    }
    
    /**
     * Helper method to create an OSM provider
     */
    private static MapProvider createOsmProvider(Context context) {
        Log.d(TAG, "Creating OSM provider");
        OsmdroidProvider osmProvider = new OsmdroidProvider();
        osmProvider.initialize(context);
        return osmProvider;
    }
}
