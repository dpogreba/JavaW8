package com.antbear.javaw8.map;

import android.content.Context;
import android.util.Log;

/**
 * Factory class to create map providers (exclusively Google Maps).
 */
public class MapFactory {
    private static final String TAG = "MapFactory";
    
    /**
     * Create a Google Maps provider.
     * 
     * @param context Application context
     * @return The Google Maps provider
     */
    public static MapProvider createMapProvider(Context context) {
        Log.d(TAG, "Creating Google Maps provider");
        
        try {
            // Always create a Google Maps provider
            GoogleMapsProvider googleProvider = new GoogleMapsProvider();
            googleProvider.initialize(context);
            
            if (!googleProvider.isInitialized()) {
                Log.e(TAG, "Warning: Google Maps provider failed to initialize properly. Some features may not work correctly.");
            }
            
            // Always return the Google Maps provider, even if initialization wasn't fully successful
            return googleProvider;
        } catch (Exception e) {
            // Just log the exception and continue with potentially limited functionality
            Log.e(TAG, "Exception during Google Maps provider creation: " + e.getMessage());
            
            // Still create and return a Google Maps provider instance
            GoogleMapsProvider provider = new GoogleMapsProvider();
            provider.initialize(context);
            return provider;
        }
    }
}
