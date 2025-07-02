package com.antbear.javaw8.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.List;
import java.util.Locale;

/**
 * Utility class to manage unit preferences and conversions.
 */
public class UnitPreferences {
    private static final String TAG = "UnitPreferences";
    
    // Preference keys
    private static final String PREF_USE_IMPERIAL = "use_imperial_units";
    private static final String PREF_UNIT_SYSTEM_INITIALIZED = "unit_system_initialized";
    
    // Conversion constants
    private static final double MILES_TO_KM = 1.60934;
    private static final double KM_TO_MILES = 0.621371;
    
    /**
     * Countries that primarily use imperial units.
     * (US, UK for some measures, Liberia, Myanmar)
     */
    private static final String[] IMPERIAL_COUNTRIES = {
        "US", "GB", "LR", "MM"
    };
    
    /**
     * Initialize the unit system preference based on locale/location if not already set.
     * @param context Application context
     * @param location Current location (optional)
     */
    public static void initializeUnitSystem(Context context, Location location) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Skip if already initialized
        if (prefs.getBoolean(PREF_UNIT_SYSTEM_INITIALIZED, false)) {
            return;
        }
        
        boolean useImperial = false;
        
        try {
            // First try to determine by location if available
            if (location != null) {
                String countryCode = getCountryCodeFromLocation(context, location);
                if (countryCode != null) {
                    useImperial = isImperialCountry(countryCode);
                    Log.d(TAG, "Determined unit system by location: " + (useImperial ? "Imperial" : "Metric") + 
                          " (Country: " + countryCode + ")");
                }
            }
            
            // If location-based detection fails, use locale
            if (location == null) {
                Locale locale = Resources.getSystem().getConfiguration().locale;
                String countryCode = locale.getCountry();
                useImperial = isImperialCountry(countryCode);
                Log.d(TAG, "Determined unit system by locale: " + (useImperial ? "Imperial" : "Metric") + 
                      " (Country: " + countryCode + ")");
            }
        } catch (Exception e) {
            // Default to imperial on any error
            useImperial = true;
            Log.e(TAG, "Error determining unit system, defaulting to imperial: " + e.getMessage());
        }
        
        // Save the preference
        prefs.edit()
            .putBoolean(PREF_USE_IMPERIAL, useImperial)
            .putBoolean(PREF_UNIT_SYSTEM_INITIALIZED, true)
            .apply();
    }
    
    /**
     * Check if a country uses the imperial system.
     * @param countryCode ISO 3166-1 country code
     * @return true if the country primarily uses imperial units
     */
    private static boolean isImperialCountry(String countryCode) {
        if (countryCode == null) return false;
        
        for (String code : IMPERIAL_COUNTRIES) {
            if (code.equalsIgnoreCase(countryCode)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the country code from a location.
     * @param context Application context
     * @param location Location to determine country
     * @return ISO 3166-1 country code or null if unavailable
     */
    private static String getCountryCodeFromLocation(Context context, Location location) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getCountryCode();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting country from location: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Check if the user prefers imperial units.
     * @param context Application context
     * @return true if imperial units are preferred
     */
    public static boolean useImperialUnits(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Default to imperial if not set
        return prefs.getBoolean(PREF_USE_IMPERIAL, true);
    }
    
    /**
     * Set the preferred unit system.
     * @param context Application context
     * @param useImperial true for imperial, false for metric
     */
    public static void setUseImperialUnits(Context context, boolean useImperial) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putBoolean(PREF_USE_IMPERIAL, useImperial)
            .putBoolean(PREF_UNIT_SYSTEM_INITIALIZED, true)
            .apply();
    }
    
    /**
     * Convert kilometers to miles.
     * @param kilometers Value in kilometers
     * @return Value in miles
     */
    public static double kilometersToMiles(double kilometers) {
        return kilometers * KM_TO_MILES;
    }
    
    /**
     * Convert miles to kilometers.
     * @param miles Value in miles
     * @return Value in kilometers
     */
    public static double milesToKilometers(double miles) {
        return miles * MILES_TO_KM;
    }
    
    /**
     * Format distance with appropriate units based on preferences.
     * @param context Application context
     * @param distanceInMiles Distance in miles
     * @return Formatted distance string
     */
    public static String formatDistance(Context context, double distanceInMiles) {
        if (useImperialUnits(context)) {
            return String.format(Locale.getDefault(), "%.1f miles", distanceInMiles);
        } else {
            double kilometers = milesToKilometers(distanceInMiles);
            return String.format(Locale.getDefault(), "%.1f km", kilometers);
        }
    }
    
    /**
     * Get the display unit for distance based on preferences.
     * @param context Application context
     * @return "miles" or "km"
     */
    public static String getDistanceUnit(Context context) {
        return useImperialUnits(context) ? "miles" : "km";
    }
}
