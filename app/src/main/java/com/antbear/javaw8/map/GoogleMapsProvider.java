package com.antbear.javaw8.map;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.antbear.javaw8.R;
import com.antbear.javaw8.utils.ThreadUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindNearbyPlacesRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of MapProvider using Google Maps.
 */
public class GoogleMapsProvider implements MapProvider {
    private static final String TAG = "GoogleMapsProvider";
    
    private Context context;
    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private PlacesClient placesClient;
    private boolean initialized = false;
    
    // Listeners
    private OnMapReadyListener mapReadyListener;
    private OnMarkerClickListener markerClickListener;
    private OnInfoWindowClickListener infoWindowClickListener;
    
    // Maps to keep track of markers
    private Map<String, Marker> markersById = new HashMap<>();
    private Map<Marker, String> markerIds = new HashMap<>();
    
    @Override
    public void initialize(Context context) {
        this.context = context;
        
        try {
            // Initialize the Google Maps Places API
            if (!Places.isInitialized()) {
                String apiKey = context.getString(R.string.google_maps_key);
                Places.initialize(context.getApplicationContext(), apiKey);
            }
            
            // Create a Places client
            placesClient = Places.createClient(context);
            
            initialized = true;
            Log.d(TAG, "Google Maps provider initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Google Maps provider: " + e.getMessage(), e);
            initialized = false;
        }
    }
    
    @Override
    public Fragment createMapFragment(FragmentManager fragmentManager, int containerId) {
        // Create a new SupportMapFragment
        mapFragment = SupportMapFragment.newInstance();
        
        // Add it to the container
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(containerId, mapFragment);
        transaction.commit();
        
        // Get reference to the GoogleMap once it's created
        mapFragment.getMapAsync(map -> {
            googleMap = map;
            setupMap();
            
            // Notify listener
            if (mapReadyListener != null) {
                mapReadyListener.onMapReady();
            }
        });
        
        return mapFragment;
    }
    
    private void setupMap() {
        if (googleMap == null) return;
        
        try {
            // Set up map UI settings
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            
            // Set up marker click listener
            googleMap.setOnMarkerClickListener(marker -> {
                String markerId = markerIds.get(marker);
                
                if (markerId != null && markerClickListener != null) {
                    return markerClickListener.onMarkerClick(markerId);
                }
                
                return false;
            });
            
            // Set up info window click listener
            googleMap.setOnInfoWindowClickListener(marker -> {
                String markerId = markerIds.get(marker);
                
                if (markerId != null && infoWindowClickListener != null) {
                    infoWindowClickListener.onInfoWindowClick(markerId);
                }
            });
            
            Log.d(TAG, "Google Maps setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Google Maps: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isInitialized() {
        return initialized && googleMap != null;
    }
    
    @Override
    public String addMarker(double latitude, double longitude, String title, String snippet) {
        if (googleMap == null) {
            Log.e(TAG, "Cannot add marker: map is not ready");
            return null;
        }
        
        // Generate a unique ID for this marker
        String markerId = UUID.randomUUID().toString();
        
        // Create marker options
        MarkerOptions options = new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title(title)
                .snippet(snippet);
        
        // Set coffee icon if available
        try {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_coffee));
        } catch (Exception e) {
            Log.e(TAG, "Error setting marker icon: " + e.getMessage());
        }
        
        // Add the marker to the map
        Marker marker = googleMap.addMarker(options);
        
        if (marker != null) {
            // Store the marker
            markersById.put(markerId, marker);
            markerIds.put(marker, markerId);
            
            Log.d(TAG, "Added marker: " + title + " with ID: " + markerId);
        }
        
        return markerId;
    }
    
    @Override
    public void moveCamera(double latitude, double longitude, float zoomLevel) {
        if (googleMap == null) {
            Log.e(TAG, "Cannot move camera: map is not ready");
            return;
        }
        
        LatLng position = new LatLng(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoomLevel));
    }
    
    @Override
    public void setOnMapReadyListener(OnMapReadyListener listener) {
        this.mapReadyListener = listener;
        
        // If map is already ready, trigger the callback immediately
        if (googleMap != null && mapReadyListener != null) {
            mapReadyListener.onMapReady();
        }
    }
    
    @Override
    public void searchNearbyPlaces(String query, double latitude, double longitude, double radius, 
                                 OnPlacesFoundListener listener) {
        if (!initialized || placesClient == null) {
            if (listener != null) {
                listener.onPlacesError("Google Maps Places API not initialized");
            }
            return;
        }
        
        // Define the place type(s) to search for based on the query
        List<String> placeTypes = getPlaceTypesForQuery(query);
        
        // Create a rectangular search bound approximately matching the radius
        // (converting meters to lat/lng differences)
        double latDelta = radius / 111000.0; // approx. 111km per degree of latitude
        double lngDelta = radius / (111000.0 * Math.cos(Math.toRadians(latitude))); // longitude degrees get wider at the equator
        
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(latitude - latDelta, longitude - lngDelta),
                new LatLng(latitude + latDelta, longitude + lngDelta));
        
        // Build the request
        FindNearbyPlacesRequest.Builder requestBuilder = FindNearbyPlacesRequest.builder()
                .setLocationBias(bounds);
        
        // Use either place types or a text query based on what we're searching for
        if (!placeTypes.isEmpty()) {
            requestBuilder.setPlaceTypes(placeTypes);
        } else {
            requestBuilder.setQuery(query);
        }
        
        Log.d(TAG, "Searching for places with types: " + placeTypes + " or query: " + query);
        
        // Execute the request
        placesClient.findNearbyPlaces(requestBuilder.build()).addOnSuccessListener(response -> {
            List<PlaceInfo> places = new ArrayList<>();
            
            // Process each place result
            for (com.google.android.libraries.places.api.model.Place place : response.getPlaces()) {
                // Extract place details
                String id = place.getId();
                String name = place.getName();
                LatLng latLng = place.getLatLng();
                String address = place.getAddress();
                String phone = place.getPhoneNumber();
                Double rating = place.getRating();
                
                // Skip places without coordinates
                if (latLng == null) continue;
                
                // Create PlaceInfo object
                PlaceInfo placeInfo = new PlaceInfo(
                        id != null ? id : UUID.randomUUID().toString(),
                        name != null ? name : "Unnamed Place",
                        latLng.latitude,
                        latLng.longitude,
                        address != null ? address : "",
                        phone != null ? phone : "",
                        rating != null ? rating.floatValue() : null,
                        false // Not sample data
                );
                
                places.add(placeInfo);
                Log.d(TAG, "Found place: " + name + " at " + latLng.latitude + "," + latLng.longitude);
            }
            
            // Return results to listener
            if (places.isEmpty()) {
                if (listener != null) {
                    listener.onPlacesError("No places found matching your search");
                }
            } else {
                if (listener != null) {
                    PlaceInfo[] placesArray = places.toArray(new PlaceInfo[0]);
                    ThreadUtils.runOnMainThread(() -> listener.onPlacesFound(placesArray));
                }
            }
        }).addOnFailureListener(exception -> {
            Log.e(TAG, "Error finding places: " + exception.getMessage(), exception);
            
            if (listener != null) {
                ThreadUtils.runOnMainThread(() -> listener.onPlacesError("Error searching for places: " + exception.getMessage()));
            }
        });
    }
    
    /**
     * Maps search queries to appropriate Google Maps Place Types
     */
    private List<String> getPlaceTypesForQuery(String query) {
        // Convert query to lowercase for easier matching
        String lowerQuery = query.toLowerCase();
        
        // Map specific queries to place types
        if (lowerQuery.contains("coffee") || lowerQuery.contains("cafe")) {
            // For coffee shops, we specifically want to return ONLY coffee shops
            return Arrays.asList(PlaceTypes.CAFE);
        } else if (lowerQuery.contains("restaurant")) {
            return Arrays.asList(PlaceTypes.RESTAURANT);
        } else if (lowerQuery.contains("bar")) {
            return Arrays.asList(PlaceTypes.BAR);
        } else if (lowerQuery.contains("hotel")) {
            return Arrays.asList(PlaceTypes.LODGING);
        } else if (lowerQuery.contains("park")) {
            return Arrays.asList(PlaceTypes.PARK);
        } else if (lowerQuery.contains("shop") || lowerQuery.contains("store")) {
            return Arrays.asList(PlaceTypes.STORE);
        } else if (lowerQuery.contains("gas") || lowerQuery.contains("fuel")) {
            return Arrays.asList(PlaceTypes.GAS_STATION);
        } else if (lowerQuery.contains("school")) {
            return Arrays.asList(PlaceTypes.SCHOOL);
        } else if (lowerQuery.contains("hospital")) {
            return Arrays.asList(PlaceTypes.HOSPITAL);
        } else if (lowerQuery.contains("bank")) {
            return Arrays.asList(PlaceTypes.BANK);
        }
        
        // For general queries, return an empty list to use the text search
        return new ArrayList<>();
    }
    
    @Override
    public void setOnMarkerClickListener(OnMarkerClickListener listener) {
        this.markerClickListener = listener;
    }
    
    @Override
    public void setOnInfoWindowClickListener(OnInfoWindowClickListener listener) {
        this.infoWindowClickListener = listener;
    }
    
    @Override
    public void enableMyLocation(boolean hasPermission) {
        if (googleMap == null) {
            Log.e(TAG, "Cannot enable my location: map is not ready");
            return;
        }
        
        try {
            googleMap.setMyLocationEnabled(hasPermission);
        } catch (SecurityException e) {
            Log.e(TAG, "Error enabling my location: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onDestroy() {
        // Clear marker maps
        markersById.clear();
        markerIds.clear();
        
        // Clear listeners
        mapReadyListener = null;
        markerClickListener = null;
        infoWindowClickListener = null;
        
        // Clear references
        googleMap = null;
        mapFragment = null;
    }
}
