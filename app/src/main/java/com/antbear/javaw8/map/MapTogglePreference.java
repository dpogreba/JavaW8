package com.antbear.javaw8.map;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class to manage map provider preferences.
 * This allows toggling between Google Maps and OSM.
 */
public class MapTogglePreference {
    private static final String PREFS_NAME = "com.antbear.javaw8.map_preferences";
    private static final String KEY_USE_GOOGLE_MAPS = "use_google_maps";
    
    /**
     * Get the current map provider preference.
     * 
     * @param context Application context
     * @return true to use Google Maps, false to use OSM
     */
    public static boolean getPreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Default to Google Maps (true) since we want to switch to it for better coffee shop results
        return prefs.getBoolean(KEY_USE_GOOGLE_MAPS, true);
    }
    
    /**
     * Set the map provider preference.
     * 
     * @param context Application context
     * @param useGoogleMaps true to use Google Maps, false to use OSM
     */
    public static void setPreference(Context context, boolean useGoogleMaps) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_USE_GOOGLE_MAPS, useGoogleMaps);
        editor.apply();
    }
    
    /**
     * Toggle the current map provider preference.
     * 
     * @param context Application context
     * @return the new preference value after toggling
     */
    public static boolean togglePreference(Context context) {
        boolean currentValue = getPreference(context);
        boolean newValue = !currentValue;
        setPreference(context, newValue);
        return newValue;
    }
}
