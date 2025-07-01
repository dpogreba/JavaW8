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
    private static final double SEARCH_RADIUS_METERS = 5000; // 5 km radius - increased for better coverage
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
        // Cancel any pending fallback timer to prevent memory leaks
        if (fallbackRunnable != null) {
            fallbackHandler.removeCallbacks(fallbackRunnable);
        }
    }

    // Track added coffee shops for fallback decision
    private int totalCoffeeShopsAdded = 0;
    private static final int FALLBACK_TIMEOUT_MS = 5000; // 5 seconds
    private Handler fallbackHandler = new Handler(Looper.getMainLooper());
    private Runnable fallbackRunnable;
    
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
        // Set up info window click listener to open directions
        mapProvider.setOnInfoWindowClickListener(new MapProvider.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(String markerId) {
                openDirections(markerId);
            }
        });
        
        // Enable my location button if permission is granted
        enableMyLocation();
        
        // Default location (in case permission is denied)
        double defaultLat = 37.4220;
        double defaultLng = -122.0841; // Mountain View, CA
        mapProvider.moveCamera(defaultLat, defaultLng, 12);
        
        // Add a test marker
        mapProvider.addMarker(defaultLat, defaultLng, "Test Marker", "This is a test marker");
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
        // Check if permission is granted
        boolean hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        if (hasPermission) {
            // Enable the my-location layer
            mapProvider.enableMyLocation(true);
            
            // Get the user's last known location
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        // Save the user's last known location
                        lastKnownLocation = location;
                        
                        // Got the user's location, center the map there
                        mapProvider.moveCamera(location.getLatitude(), location.getLongitude(), 15);
                        
                        // Search for nearby coffee shops
                        searchNearbyCoffeeShops();
                    }
                });
        } else {
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
        // Reset the counter each time we start a new search
        totalCoffeeShopsAdded = 0;
        markerTitleById.clear();
        
        // Start fallback timer
        startFallbackTimer();
        
        if (lastKnownLocation == null) {
            Log.e(TAG, "Last known location is null - cannot search for coffee shops");
            showToast("Unable to get your location. Using fallback locations.", Toast.LENGTH_LONG);
            addFallbackCoffeeShops();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            showToast("Location permission required to find nearby coffee shops", Toast.LENGTH_LONG);
            addFallbackCoffeeShops();
            return;
        }
        
        // Show toast to let user know we're searching
        showToast("Searching for coffee shops nearby...", Toast.LENGTH_SHORT);
        Log.d(TAG, "Starting map search for coffee shops at: " + 
              lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
        
        // Search for coffee shops near the user's location using the map provider
        mapProvider.searchNearbyPlaces(
            "coffee shop", 
            lastKnownLocation.getLatitude(), 
            lastKnownLocation.getLongitude(), 
            SEARCH_RADIUS_METERS,
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
                                showToast("Found " + places.length + " coffee shops", Toast.LENGTH_SHORT);
                            } else {
                                handlePlacesError("No coffee shops found");
                            }
                        }
                    });
                }
                
                @Override
                public void onPlacesError(final String errorMessage) {
                    Log.e(TAG, "Error finding places: " + errorMessage);
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

        showToast(errorMessage, Toast.LENGTH_SHORT);
        addFallbackCoffeeShops();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel any pending timers to prevent memory leaks
        if (fallbackRunnable != null) {
            fallbackHandler.removeCallbacks(fallbackRunnable);
            fallbackRunnable = null;
        }
        
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
    
    /**
     * Starts a timer that will add fallback coffee shops if no real ones are found
     */
    private void startFallbackTimer() {
        // Cancel any existing fallback timer
        if (fallbackRunnable != null) {
            fallbackHandler.removeCallbacks(fallbackRunnable);
        }
        
        // Create a new fallback runnable
        fallbackRunnable = new Runnable() {
            @Override
            public void run() {
                // Check if any coffee shops were added
                if (totalCoffeeShopsAdded == 0) {
                    Log.d(TAG, "Fallback timer triggered - no coffee shops were found after " + 
                         (FALLBACK_TIMEOUT_MS/1000) + " seconds");
                    
                    // Only add fallbacks if we haven't added any coffee shops yet
                    UiMessageHandler.runOnUiThreadIfFragmentAlive(HomeFragment.this, new UiMessageHandler.UiCallback() {
                        @Override
                        public void onUiThread() {
                            addFallbackCoffeeShops();
                        }
                    });
                } else {
                    Log.d(TAG, "Fallback timer ignored - " + totalCoffeeShopsAdded + 
                         " coffee shops were already added");
                }
            }
        };
        
        // Schedule the fallback runnable
        fallbackHandler.postDelayed(fallbackRunnable, FALLBACK_TIMEOUT_MS);
        Log.d(TAG, "Fallback timer started - will check for markers in " + 
             (FALLBACK_TIMEOUT_MS/1000) + " seconds");
    }
    
    /**
     * Adds hardcoded fallback coffee shop locations when the Places API fails
     */
    private void addFallbackCoffeeShops() {
        Log.d(TAG, "Adding fallback coffee shop markers");
        showToast("Using sample coffee shop locations", Toast.LENGTH_LONG);
        
        // Clear any existing markers
        markerTitleById.clear();
        totalCoffeeShopsAdded = 0;
        
        // Center point for our fallbacks - use user location if available, otherwise default
        double centerLat = (lastKnownLocation != null) ? lastKnownLocation.getLatitude() : 37.4220;
        double centerLng = (lastKnownLocation != null) ? lastKnownLocation.getLongitude() : -122.0841;
        
        // Move camera to this location
        mapProvider.moveCamera(centerLat, centerLng, 14);
        
        // Add sample coffee shops around the center
        addSampleCoffeeShop("JavaW8 Coffee House", 
                centerLat + 0.003, centerLng + 0.003,
                "123 Coffee Lane", "555-123-4567", 4.8f);
        
        addSampleCoffeeShop("Brew & Bean", 
                centerLat - 0.002, centerLng + 0.001,
                "456 Espresso Ave", "555-987-6543", 4.5f);
        
        addSampleCoffeeShop("Caffeine Corner", 
                centerLat + 0.001, centerLng - 0.002,
                "789 Latte Blvd", "555-246-1357", 4.2f);
        
        addSampleCoffeeShop("Mobile Mocha", 
                centerLat - 0.001, centerLng - 0.001,
                "321 Android St", "555-369-8521", 4.7f);
    }
    
    /**
     * Add a sample coffee shop marker with the specified details
     */
    private void addSampleCoffeeShop(String name, double lat, double lng, 
                                    String address, String phone, float rating) {
        // Create a place info object for this sample coffee shop
        PlaceInfo place = new PlaceInfo(
            "sample_" + name.replace(" ", "_").toLowerCase(),
            name,
            lat,
            lng,
            address,
            phone,
            rating,
            true // This is sample data
        );
        
        // Add a marker for this place
        addPlaceMarker(place);
    }
}
