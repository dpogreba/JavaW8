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
     * Always returns true for Google Maps.
     * 
     * @param context Application context
     * @return true (always use Google Maps)
     */
    public static boolean getPreference(Context context) {
        // Always return true to use Google Maps
        return true;
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
     * Toggle functionality is disabled since we always use Google Maps.
     * This method is kept for API compatibility but will always return true.
     * 
     * @param context Application context
     * @return true (always use Google Maps)
     */
    public static boolean togglePreference(Context context) {
        // Always return true since Google Maps is enforced
        return true;
    }
}
