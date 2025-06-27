package com.antbear.javaw8.map;

import android.content.Context;
import android.util.Log;

/**
 * Helper class for map provider preferences.
 * Currently configured to always use osmdroid.
 */
public class MapTogglePreference {
    private static final String TAG = "MapTogglePreference";
    
    /**
     * Check if Google Maps is the selected provider.
     * Currently always returns false as Google Maps is disabled.
     * 
     * @param context The application context
     * @return Always false (using osmdroid)
     */
    public static boolean isUsingGoogleMaps(Context context) {
        return false; // Always use osmdroid
    }
    
    /**
     * Set the map provider preference.
     * This method is kept for API compatibility but has no effect.
     * 
     * @param context The application context
     * @param useGoogleMaps Ignored parameter
     */
    public static void setUseGoogleMaps(Context context, boolean useGoogleMaps) {
        // No-op - we always use osmdroid regardless of the setting
        if (useGoogleMaps) {
            Log.d(TAG, "Attempted to set Google Maps as provider, but Google Maps support is disabled");
        }
    }
}
