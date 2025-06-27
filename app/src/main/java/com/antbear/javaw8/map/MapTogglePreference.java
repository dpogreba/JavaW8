package com.antbear.javaw8.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;

/**
 * Helper class to manage map provider preferences.
 */
public class MapTogglePreference {
    private static final String TAG = "MapTogglePreference";
    private static final String PREF_USE_GOOGLE_MAPS = "use_google_maps";
    
    /**
     * Check if Google Maps is the selected provider.
     * 
     * @param context The application context
     * @return true if using Google Maps, false if using osmdroid
     */
    public static boolean isUsingGoogleMaps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_USE_GOOGLE_MAPS, true); // Default to Google Maps
    }
    
    /**
     * Set the map provider preference.
     * 
     * @param context The application context
     * @param useGoogleMaps true to use Google Maps, false to use osmdroid
     */
    public static void setUseGoogleMaps(Context context, boolean useGoogleMaps) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_USE_GOOGLE_MAPS, useGoogleMaps);
        editor.apply();
        
        Log.d(TAG, "Map provider set to: " + (useGoogleMaps ? "Google Maps" : "osmdroid"));
    }
    
    /**
     * Toggle the map provider preference.
     * 
     * @param context The application context
     * @return true if now using Google Maps, false if now using osmdroid
     */
    public static boolean toggleMapProvider(Context context) {
        boolean currentlyUsingGoogle = isUsingGoogleMaps(context);
        
        // Toggle the preference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_USE_GOOGLE_MAPS, !currentlyUsingGoogle);
        editor.apply();
        
        Log.d(TAG, "Map provider toggled to: " + (!currentlyUsingGoogle ? "Google Maps" : "osmdroid"));
        
        return !currentlyUsingGoogle;
    }
}
