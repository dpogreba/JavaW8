package com.antbear.javaw8.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;

/**
 * Factory class to create the appropriate map provider based on settings.
 */
public class MapFactory {
    private static final String TAG = "MapFactory";
    private static final String PREF_USE_GOOGLE_MAPS = "use_google_maps";
    
    /**
     * Create a map provider based on the saved preference.
     * 
     * @param context Application context
     * @return The appropriate map provider
     */
    public static MapProvider createMapProvider(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useGoogleMaps = prefs.getBoolean(PREF_USE_GOOGLE_MAPS, true); // Default to Google Maps
        
        return createMapProvider(context, useGoogleMaps);
    }
    
    /**
     * Create a specific map provider.
     * 
     * @param context Application context
     * @param useGoogleMaps Whether to use Google Maps (true) or osmdroid (false)
     * @return The requested map provider
     */
    public static MapProvider createMapProvider(Context context, boolean useGoogleMaps) {
        MapProvider provider;
        
        if (useGoogleMaps) {
            Log.d(TAG, "Creating Google Maps provider");
            provider = new GoogleMapsProvider();
        } else {
            Log.d(TAG, "Creating OSM provider");
            provider = new OsmdroidProvider();
        }
        
        // Initialize the provider
        provider.initialize(context);
        
        return provider;
    }
    
    /**
     * Toggle between map providers and save the preference.
     * 
     * @param context Application context
     * @return The new map provider
     */
    public static MapProvider toggleMapProvider(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean currentlyUsingGoogle = prefs.getBoolean(PREF_USE_GOOGLE_MAPS, true);
        
        // Toggle the preference
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_USE_GOOGLE_MAPS, !currentlyUsingGoogle);
        editor.apply();
        
        // Create and return the new provider
        return createMapProvider(context, !currentlyUsingGoogle);
    }
    
    /**
     * Check if we're currently using Google Maps.
     * 
     * @param context Application context
     * @return true if using Google Maps, false if using osmdroid
     */
    public static boolean isUsingGoogleMaps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_USE_GOOGLE_MAPS, true);
    }
}
