package com.antbear.javaw8;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.antbear.javaw8.utils.ThreadUtils;
import com.antbear.javaw8.utils.UiMessageHandler;
import com.antbear.javaw8.utils.UnitPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.antbear.javaw8.map.MapFactory;
import com.antbear.javaw8.map.MapProvider;

import com.antbear.javaw8.map.PlaceInfo;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final double SEARCH_RADIUS_MILES = 3.0; // 3 mile radius
    private static final double METERS_PER_MILE = 1609.34; // Conversion factor
    private static final long CAMERA_IDLE_DEBOUNCE_MS = 1000; // 1 second debounce for map movements
    
    private MapProvider mapProvider;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private boolean isSearchInProgress = false; // Flag to prevent overlapping searches
    private Handler cameraIdleHandler = new Handler(Looper.getMainLooper());
    private Runnable cameraIdleRunnable;
    
    // Handler for safe UI operations
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Map to track marker IDs
    private final Map<String, String> markerTitleById = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Initialize the map provider
        mapProvider = MapFactory.createMapProvider(requireContext());
        
        // Initialize unit preferences (defaults to imperial units)
        UnitPreferences.initializeUnitSystem(requireContext(), null);
        
        // Set up map ready listener
        mapProvider.setOnMapReadyListener(new MapProvider.OnMapReadyListener() {
            @Override
            public void onMapReady() {
                setupMap();
            }
        });
        
        // Create the map fragment
        mapProvider.createMapFragment(getChildFragmentManager(), R.id.map_container);
        
        return view;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // No fallback timer to cancel
    }

    // Remove fallback tracking as we're using only real data
    private int totalCoffeeShopsAdded = 0;
    
    /**
     * Shows a toast message safely on the main thread
     */
    private void showToast(final String message, final int duration) {
        UiMessageHandler.showToastFromFragment(this, message, duration);
    }
    
    /**
     * Set up the map after it's ready
     */
    private void setupMap() {
        Log.d(TAG, "Setting up map - map ready callback triggered");
        
        // Set up info window click listener to open directions
        mapProvider.setOnInfoWindowClickListener(new MapProvider.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(String markerId) {
                openDirections(markerId);
            }
        });
        
        // Default location (in case permission is denied)
        double defaultLat = 37.4220;
        double defaultLng = -122.0841; // Mountain View, CA
        Log.d(TAG, "Moving camera to default location: " + defaultLat + ", " + defaultLng);
        mapProvider.moveCamera(defaultLat, defaultLng, 12);
        
        // Enable my location button if permission is granted
        enableMyLocation();
    }
    
    /**
     * Open directions to a marker location
     */
    private void openDirections(String markerId) {
        // Get place info from marker ID
        PlaceInfo place = getPlaceFromMarkerId(markerId);
        if (place == null) return;
        
        // Create a URI for Google Maps directions
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + place.getLatitude() + "," + place.getLongitude() + "&mode=d");
        
        // Create an Intent from gmmIntentUri
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        
        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // If Google Maps isn't installed, open in browser
            Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + 
                                     place.getLatitude() + "," + place.getLongitude());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
            startActivity(browserIntent);
        }
    }
    
    /**
     * Get place info from a marker ID
     */
    private PlaceInfo getPlaceFromMarkerId(String markerId) {
        // In a real implementation, we would store and retrieve the actual PlaceInfo
        // For simplicity, we're creating a dummy place with the stored title
        String title = markerTitleById.get(markerId);
        if (title == null) return null;
        
        // Check if this is a sample coffee shop by ID prefix
        // Only sample coffee shops created with addSampleCoffeeShop should have this flag set to true
        boolean isSample = markerId.startsWith("sample_");
        
        return new PlaceInfo(
            markerId,
            title,
            lastKnownLocation != null ? lastKnownLocation.getLatitude() : 37.4220,
            lastKnownLocation != null ? lastKnownLocation.getLongitude() : -122.0841,
            "123 Main St",
            "555-123-4567",
            4.5f,
            isSample
        );
    }
    
    private void enableMyLocation() {
        Log.d(TAG, "Enabling my location...");
        
        // Check if permission is granted
        boolean hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        Log.d(TAG, "Location permission granted: " + hasPermission);
        
        if (hasPermission) {
            try {
                // Enable the my-location layer
                mapProvider.enableMyLocation(true);
                
                // Get the user's last known location
                Log.d(TAG, "Requesting last known location...");
                fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            Log.d(TAG, "Got last known location: " + location.getLatitude() + ", " + location.getLongitude());
                            
                            // Save the user's last known location
                            lastKnownLocation = location;
                            
                            // Initialize unit preferences based on location
                            UnitPreferences.initializeUnitSystem(requireContext(), location);
                            
                            // Got the user's location, center the map there
                            mapProvider.moveCamera(location.getLatitude(), location.getLongitude(), 15);
                            
                            // Search for nearby coffee shops
                            searchNearbyCoffeeShops();
                        } else {
                            Log.e(TAG, "Last known location is null from fusedLocationClient");
                            
                            // If location is null, use a default location to ensure we can search
                            lastKnownLocation = new Location("default");
                            lastKnownLocation.setLatitude(37.4220); // Mountain View
                            lastKnownLocation.setLongitude(-122.0841);
                            
                            // Search using default location
                            Log.d(TAG, "Using default location for search");
                            searchNearbyCoffeeShops();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting last location: " + e.getMessage(), e);
                        
                        // If there's an error, use a default location
                        lastKnownLocation = new Location("default");
                        lastKnownLocation.setLatitude(37.4220); // Mountain View
                        lastKnownLocation.setLongitude(-122.0841);
                        
                        // Search using default location
                        Log.d(TAG, "Using default location for search after location error");
                        searchNearbyCoffeeShops();
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in enableMyLocation: " + e.getMessage(), e);
            }
        } else {
            Log.d(TAG, "Requesting location permissions...");
            // Request permission
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location
                enableMyLocation();
            } else {
                // Permission denied
                showToast("Location permission is required to show your location on the map", Toast.LENGTH_LONG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    
    /**
     * Add a marker for a place
     */
    private void addPlaceMarker(PlaceInfo place) {
        String markerId = mapProvider.addMarker(
            place.getLatitude(),
            place.getLongitude(),
            place.getName(),
            place.createSnippet()
        );
        
        if (markerId != null) {
            markerTitleById.put(markerId, place.getName());
            totalCoffeeShopsAdded++;
        }
    }
    
    /**
     * Search for coffee shops near the user's location
     */
    private void searchNearbyCoffeeShops() {
        Log.d(TAG, "searchNearbyCoffeeShops called");
        
        // Reset the counter each time we start a new search
        totalCoffeeShopsAdded = 0;
        markerTitleById.clear();
        
        // Double-check for null location again
        if (lastKnownLocation == null) {
            Log.e(TAG, "Last known location is still null - cannot search for coffee shops");
            showToast("Unable to get your location. Using default location.", Toast.LENGTH_LONG);
            
            // Create a default location to allow search to continue
            lastKnownLocation = new Location("default");
            lastKnownLocation.setLatitude(37.4220); // Mountain View
            lastKnownLocation.setLongitude(-122.0841);
        }
        
        Log.d(TAG, "Search location: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            showToast("Location permission required to find nearby coffee shops", Toast.LENGTH_LONG);
            return;
        }
        
        // Get the appropriate search radius based on user's unit preference
        double searchRadiusMeters = getSearchRadiusInMeters();
        
        // Show toast with appropriate units
        String unitDisplay = UnitPreferences.formatDistance(requireContext(), SEARCH_RADIUS_MILES);
        showToast("Searching for coffee shops within " + unitDisplay + "...", Toast.LENGTH_SHORT);
        
        Log.d(TAG, "Starting map search for coffee shops at: " + 
              lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude() + 
              " with radius: " + searchRadiusMeters + " meters");
        
        // Search for coffee shops near the user's location using the map provider
        mapProvider.searchNearbyPlaces(
            "coffee shop", 
            lastKnownLocation.getLatitude(), 
            lastKnownLocation.getLongitude(), 
            searchRadiusMeters,
            new MapProvider.OnPlacesFoundListener() {
                @Override
                public void onPlacesFound(final PlaceInfo[] places) {
                    UiMessageHandler.runOnUiThreadIfFragmentAlive(HomeFragment.this, new UiMessageHandler.UiCallback() {
                        @Override
                        public void onUiThread() {
                            if (places.length > 0) {
                                for (PlaceInfo place : places) {
                                    addPlaceMarker(place);
                                }
                                String unitDisplay = UnitPreferences.formatDistance(requireContext(), SEARCH_RADIUS_MILES);
                                showToast("Found " + places.length + " coffee shops within " + unitDisplay, Toast.LENGTH_SHORT);
                            } else {
                                handlePlacesError("No coffee shops found");
                            }
                        }
                    });
                }
                
                @Override
                public void onPlacesError(final String errorMessage) {
                    Log.e(TAG, "Error finding places: " + errorMessage);
                    
                    // Log additional information to help debug network issues
                    double searchRadiusMeters = getSearchRadiusInMeters();
                    Log.e(TAG, "Network debug - Search parameters: latitude=" + 
                          lastKnownLocation.getLatitude() + ", longitude=" + 
                          lastKnownLocation.getLongitude() + ", radius=" + 
                          searchRadiusMeters + " meters (" + 
                          UnitPreferences.formatDistance(requireContext(), SEARCH_RADIUS_MILES) + ")");
                    
                    UiMessageHandler.runOnUiThreadIfFragmentAlive(HomeFragment.this, new UiMessageHandler.UiCallback() {
                        @Override
                        public void onUiThread() {
                            handlePlacesError(errorMessage);
                        }
                    });
                }
            }
        );
    }

    /**
     * Handle error from places search
     */
    private void handlePlacesError(String errorMessage) {
        // Safety check for fragment still attached
        if (!isAdded()) return;

        // Just show the error message without adding fallback coffee shops
        showToast(errorMessage, Toast.LENGTH_SHORT);
    }
    
    // onPause() is already defined above
    @Override
    public void onDestroy() {
        super.onDestroy();
        // No fallback timer to cancel anymore
        
        if (cameraIdleRunnable != null) {
            cameraIdleHandler.removeCallbacks(cameraIdleRunnable);
            cameraIdleRunnable = null;
        }
        
        // Clear any pending main handler callbacks
        mainHandler.removeCallbacksAndMessages(null);
        ThreadUtils.removeAllCallbacks();
        
        // Clean up map provider resources
        if (mapProvider != null) {
            mapProvider.onDestroy();
        }
    }
    
    // Removed fallback methods - only using real data
    
    /**
     * Get the search radius in meters based on user's unit preferences.
     * @return Search radius in meters
     */
    private double getSearchRadiusInMeters() {
        // We store the search radius in miles as the base unit
        if (UnitPreferences.useImperialUnits(requireContext())) {
            // For imperial units, convert miles directly to meters
            return SEARCH_RADIUS_MILES * METERS_PER_MILE;
        } else {
            // For metric units, convert miles to km first, then to meters
            double radiusKm = UnitPreferences.milesToKilometers(SEARCH_RADIUS_MILES);
            return radiusKm * 1000; // 1 km = 1000 meters
        }
    }
}
