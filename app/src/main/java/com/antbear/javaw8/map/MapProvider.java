package com.antbear.javaw8.map;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Interface defining the contract for map providers (Google Maps, osmdroid, etc.)
 * This abstraction allows for easy switching between map providers.
 */
public interface MapProvider {
    /**
     * Initialize the map provider with the necessary configurations.
     * @param context The application context
     */
    void initialize(Context context);
    
    /**
     * Create the map fragment to be added to the UI.
     * @param fragmentManager The fragment manager to use for transactions
     * @param containerId The ID of the container to add the fragment to
     * @return The created map fragment
     */
    Fragment createMapFragment(FragmentManager fragmentManager, int containerId);
    
    /**
     * Check if the provider has been properly initialized.
     * @return true if initialized, false otherwise
     */
    boolean isInitialized();
    
    /**
     * Add a marker to the map.
     * @param latitude The latitude of the marker
     * @param longitude The longitude of the marker
     * @param title The title of the marker
     * @param snippet The snippet/description for the marker
     * @return A unique identifier for the marker
     */
    String addMarker(double latitude, double longitude, String title, String snippet);
    
    /**
     * Move the camera to a specific location.
     * @param latitude The latitude to move to
     * @param longitude The longitude to move to
     * @param zoomLevel The zoom level to set
     */
    void moveCamera(double latitude, double longitude, float zoomLevel);
    
    /**
     * Set up a listener for map ready events.
     * @param listener The listener to set
     */
    void setOnMapReadyListener(OnMapReadyListener listener);
    
    /**
     * Search for points of interest near a location.
     * @param query The search query (e.g., "coffee shop")
     * @param latitude The latitude to search around
     * @param longitude The longitude to search around
     * @param radius The radius to search within (in meters)
     * @param listener The listener for search results
     */
    void searchNearbyPlaces(String query, double latitude, double longitude, double radius, 
                           OnPlacesFoundListener listener);
    
    /**
     * Set a click listener for markers.
     * @param listener The listener to set
     */
    void setOnMarkerClickListener(OnMarkerClickListener listener);
    
    /**
     * Set a click listener for info windows.
     * @param listener The listener to set
     */
    void setOnInfoWindowClickListener(OnInfoWindowClickListener listener);
    
    /**
     * Enable my location features if permissions are granted.
     * @param hasPermission Whether location permissions are granted
     */
    void enableMyLocation(boolean hasPermission);
    
    /**
     * Clean up resources when the provider is no longer needed.
     */
    void onDestroy();
    
    // Listener interfaces
    
    /**
     * Listener for map ready events.
     */
    interface OnMapReadyListener {
        void onMapReady();
    }
    
    /**
     * Listener for place search results.
     */
    interface OnPlacesFoundListener {
        void onPlacesFound(PlaceInfo[] places);
        void onPlacesError(String errorMessage);
    }
    
    /**
     * Listener for marker click events.
     */
    interface OnMarkerClickListener {
        boolean onMarkerClick(String markerId);
    }
    
    /**
     * Listener for info window click events.
     */
    interface OnInfoWindowClickListener {
        void onInfoWindowClick(String markerId);
    }
}
