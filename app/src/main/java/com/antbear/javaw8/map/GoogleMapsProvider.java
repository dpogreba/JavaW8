package com.antbear.javaw8.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
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
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;

// Import the Nearby Search API classes
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
                Log.d(TAG, "Initializing Places API with key: " + (apiKey != null ? "[Key exists]" : "[Key is null]"));
                Places.initialize(context.getApplicationContext(), apiKey);
            } else {
                Log.d(TAG, "Places API already initialized");
            }
            
            // Create a Places client
            placesClient = Places.createClient(context);
            if (placesClient != null) {
                Log.d(TAG, "Places client created successfully");
            } else {
                Log.e(TAG, "Places client creation failed - null client returned");
            }
            
            initialized = true;
            Log.d(TAG, "Google Maps provider initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Google Maps provider: " + e.getMessage(), e);
            e.printStackTrace();
            initialized = false;
        }
    }
    
    @Override
    public Fragment createMapFragment(FragmentManager fragmentManager, int containerId) {
        try {
            // Create a new SupportMapFragment
            mapFragment = SupportMapFragment.newInstance();
            
            // Add it to the container
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(containerId, mapFragment);
            transaction.commitNow(); // Use commitNow to ensure immediate execution
            
            // Get reference to the GoogleMap once it's created
            mapFragment.getMapAsync(map -> {
                try {
                    googleMap = map;
                    Log.d(TAG, "GoogleMap instance received successfully");
                    setupMap();
                    
                    // Notify listener
                    if (mapReadyListener != null) {
                        mapReadyListener.onMapReady();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up GoogleMap: " + e.getMessage(), e);
                }
            });
            
            return mapFragment;
        } catch (Exception e) {
            Log.e(TAG, "Error creating map fragment: " + e.getMessage(), e);
            
            // Return an empty fragment as fallback
            Fragment fallbackFragment = new Fragment();
            FragmentTransaction fallbackTransaction = fragmentManager.beginTransaction();
            fallbackTransaction.replace(containerId, fallbackFragment);
            fallbackTransaction.commitNow();
            
            // We'll still return our non-functional mapFragment so other code won't crash
            return fallbackFragment;
        }
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
        
        Log.d(TAG, "Adding marker at " + latitude + ", " + longitude + " with title: " + title);
        
        try {
            // Generate a unique ID for this marker
            String markerId = UUID.randomUUID().toString();
            
            // Create marker options
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .title(title)
                    .snippet(snippet)
                    .visible(true);
            
            // Set marker color based on type
            // Use default marker with a color instead of custom bitmap
            try {
                float markerHue = BitmapDescriptorFactory.HUE_AZURE; // Default color
                
                // Use brown color for coffee shops to match app theme
                if (title != null && (title.toLowerCase().contains("coffee") || 
                                      title.toLowerCase().contains("cafe") || 
                                      title.toLowerCase().contains("espresso"))) {
                    markerHue = BitmapDescriptorFactory.HUE_ORANGE;
                }
                
                options.icon(BitmapDescriptorFactory.defaultMarker(markerHue));
                Log.d(TAG, "Set marker color for: " + title);
            } catch (Exception e) {
                Log.e(TAG, "Error setting marker icon, using default: " + e.getMessage(), e);
            }
            
            // Add the marker to the map
            Marker marker = googleMap.addMarker(options);
            
            if (marker != null) {
                // Store the marker
                markersById.put(markerId, marker);
                markerIds.put(marker, markerId);
                
                Log.d(TAG, "Successfully added marker: " + title + " with ID: " + markerId);
                return markerId;
            } else {
                Log.e(TAG, "Failed to add marker: googleMap.addMarker returned null");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception adding marker: " + e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public void moveCamera(double latitude, double longitude, float zoomLevel) {
        if (googleMap == null) {
            Log.e(TAG, "Cannot move camera: map is not ready");
            return;
        }
        
        try {
            Log.d(TAG, "Moving camera to " + latitude + ", " + longitude + " with zoom level: " + zoomLevel);
            LatLng position = new LatLng(latitude, longitude);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoomLevel));
        } catch (Exception e) {
            Log.e(TAG, "Error moving camera: " + e.getMessage(), e);
        }
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
                Log.e(TAG, "Places API not initialized. Check if API key is valid and has Places API enabled.");
                listener.onPlacesError("Google Maps Places API not initialized - Please check API key");
            }
            return;
        }
        
        // Log the search parameters for debugging
        Log.d(TAG, "Searching for places with query: '" + query + 
              "' at [" + latitude + ", " + longitude + "] with radius: " + radius + " meters" +
              " using API key ending with: " + 
              context.getString(R.string.google_maps_key).substring(Math.max(0, context.getString(R.string.google_maps_key).length() - 8)));
        
        // Define the place type(s) to search for based on the query
        List<String> placeTypes = getPlaceTypesForQuery(query);
        
        // Use the direct findCurrentPlace API instead of autocomplete
        // This approach gives results similar to what you'd see in Google Maps directly
        
        // First create the basic fields to request
        List<Place.Field> placeFields = Arrays.asList(
            Place.Field.ID, 
            Place.Field.NAME,
            Place.Field.LAT_LNG, 
            Place.Field.ADDRESS,
            Place.Field.PHONE_NUMBER,
            Place.Field.RATING,
            Place.Field.TYPES
        );
        
        // Create a direct search for nearby places
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);
        
        try {
            // Check for location permissions
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                
                // No permission, so use a different approach with a manually specified location
                searchWithLocationBias(query, latitude, longitude, radius, listener, placeTypes);
                return;
            }
            
            // Use the direct Current Place API which gives more comprehensive results
            placesClient.findCurrentPlace(request).addOnSuccessListener((response) -> {
                List<PlaceInfo> places = new ArrayList<>();
                
                // If there are results, process them
                if (response.getPlaceLikelihoods().size() > 0) {
                    for (com.google.android.libraries.places.api.model.PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                        Place place = placeLikelihood.getPlace();
                        
                        // Check if the place matches our search criteria
                        if (isPlaceMatchingSearch(place, query, placeTypes)) {
                            LatLng location = place.getLatLng();
                            
                            // Skip places without location
                            if (location == null) continue;
                            
                            // Check if within radius
                            float[] results = new float[1];
                            Location.distanceBetween(latitude, longitude, 
                                                   location.latitude, location.longitude, results);
                            
                            if (results[0] <= radius) {
                                // Create place info object
                                PlaceInfo placeInfo = new PlaceInfo(
                                    place.getId() != null ? place.getId() : UUID.randomUUID().toString(),
                                    place.getName() != null ? place.getName() : "Unnamed Place",
                                    location.latitude,
                                    location.longitude,
                                    place.getAddress() != null ? place.getAddress() : "",
                                    place.getPhoneNumber() != null ? place.getPhoneNumber() : "",
                                    place.getRating() != null ? place.getRating().floatValue() : null,
                                    false // Not sample data
                                );
                                
                                places.add(placeInfo);
                                Log.d(TAG, "Found matching place: " + placeInfo.getName());
                            }
                        }
                    }
                }
                
                // If we found places, return them
                if (!places.isEmpty()) {
                    PlaceInfo[] placesArray = places.toArray(new PlaceInfo[0]);
                    ThreadUtils.runOnMainThread(() -> listener.onPlacesFound(placesArray));
                    Log.d(TAG, "Found " + places.size() + " matching places");
                } else {
                    // If no results or few results from current place, try the location bias approach as backup
                    Log.d(TAG, "No matches found with current place, trying location bias search");
                    searchWithLocationBias(query, latitude, longitude, radius, listener, placeTypes);
                }
            }).addOnFailureListener(exception -> {
                Log.e(TAG, "Error finding current places: " + exception.getMessage(), exception);
                // Fall back to the location bias approach
                searchWithLocationBias(query, latitude, longitude, radius, listener, placeTypes);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in searchNearbyPlaces: " + e.getMessage(), e);
            // Fall back to the location bias approach
            searchWithLocationBias(query, latitude, longitude, radius, listener, placeTypes);
        }
    }
    
    /**
     * Checks if a place matches our search criteria
     */
    private boolean isPlaceMatchingSearch(Place place, String query, List<String> placeTypes) {
        // If no query, any place passes
        if (query == null || query.isEmpty()) {
            return true;
        }
        
        // Get the place name
        String name = place.getName();
        if (name == null) {
            return false;
        }
        
        // Check name for query match
        String lowercaseName = name.toLowerCase();
        String lowercaseQuery = query.toLowerCase();
        
        // If the name contains the query, it's a match
        if (lowercaseName.contains(lowercaseQuery)) {
            return true;
        }
        
        // If query is "coffee", check for "cafe" too
        if (lowercaseQuery.equals("coffee") && lowercaseName.contains("cafe")) {
            return true;
        }
        
        // Check place types
        List<Place.Type> types = place.getTypes();
        if (types != null) {
            // Convert Place.Type to string representation
            List<String> placeTypeStrings = new ArrayList<>();
            for (Place.Type type : types) {
                placeTypeStrings.add(type.toString());
            }
            
            // Check for intersection between the place's types and our search types
            for (String typeString : placeTypeStrings) {
                if (placeTypes.contains(typeString)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Search for places using location bias (fallback method when current place API fails)
     */
    private void searchWithLocationBias(String query, double latitude, double longitude, double radius,
                                       OnPlacesFoundListener listener, List<String> placeTypes) {
        Log.d(TAG, "Using location bias search for: " + query);
        
        // Create a session token
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
        
        // Create a larger search area
        double enlargementFactor = 1.5; // Make it 50% larger than requested to get more results
        double latDelta = (radius * enlargementFactor) / 111000.0; // approx. 111km per degree of latitude
        double lngDelta = (radius * enlargementFactor) / (111000.0 * Math.cos(Math.toRadians(latitude))); // longitude degrees get wider at the equator
        
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(latitude - latDelta, longitude - lngDelta),
                new LatLng(latitude + latDelta, longitude + lngDelta));
        
        // Create autocomplete request with broader parameters
        FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                .setLocationBias(bounds)
                .setTypesFilter(placeTypes)
                .setSessionToken(token)
                .setQuery(query)
                .setCountries("US") // Focus on US results for better relevance
                .build();
        
        // Execute the request to get autocomplete predictions
        placesClient.findAutocompletePredictions(predictionsRequest).addOnSuccessListener(response -> {
            List<PlaceInfo> places = new ArrayList<>();
            
            if (response.getAutocompletePredictions().isEmpty()) {
                if (listener != null) {
                    listener.onPlacesError("No places found matching your search");
                }
                return;
            }
            
            // Use a counter to track how many fetch requests are completed
            final AtomicInteger pendingFetches = new AtomicInteger(response.getAutocompletePredictions().size());
            final AtomicBoolean errorReported = new AtomicBoolean(false);
            
            // For each prediction, fetch the full place details
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                String placeId = prediction.getPlaceId();
                
                // Build a list of place fields to request
                List<Place.Field> placeFields = Arrays.asList(
                    Place.Field.ID, 
                    Place.Field.NAME,
                    Place.Field.LAT_LNG, 
                    Place.Field.ADDRESS,
                    Place.Field.PHONE_NUMBER,
                    Place.Field.RATING
                );
                
                // Create a fetch request
                FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                    .setSessionToken(token)
                    .build();
                    
                // Execute the fetch request
                placesClient.fetchPlace(request).addOnSuccessListener(placeResponse -> {
                    Place place = placeResponse.getPlace();
            
                    // Extract place details
                    String id = place.getId();
                    String name = place.getName();
                    LatLng latLng = place.getLatLng();
                    String address = place.getAddress();
                    String phone = place.getPhoneNumber();
                    Double rating = place.getRating();
                    
                    // Skip places without coordinates
                    if (latLng == null) {
                        checkIfAllFetchesCompleted(pendingFetches, places, errorReported, listener);
                        return;
                    }
                    
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
                    
                    // Check if all fetches are completed
                    checkIfAllFetchesCompleted(pendingFetches, places, errorReported, listener);
                }).addOnFailureListener(exception -> {
                    Log.e(TAG, "Error fetching place details: " + exception.getMessage(), exception);
                    checkIfAllFetchesCompleted(pendingFetches, places, errorReported, listener);
                });
            }
        }).addOnFailureListener(exception -> {
            String errorMessage = exception.getMessage();
            Log.e(TAG, "Error finding places: " + errorMessage, exception);
            
            // Detailed logging for API key issues
            if (errorMessage != null && errorMessage.contains("API key")) {
                Log.e(TAG, "API KEY ERROR: Please verify your API key has the Places API enabled and is not expired or restricted");
                Log.e(TAG, "Using API key from strings.xml with SHA: " + String.valueOf(context.getString(R.string.google_maps_key).hashCode()));
            }
            
            if (listener != null) {
                // Provide a more user-friendly error message
                final String userMessage;
                if (errorMessage != null) {
                    if (errorMessage.contains("API key")) {
                        userMessage = "API key issue: Please contact support";
                    } else if (errorMessage.contains("network")) {
                        userMessage = "Network error: Please check your connection";
                    } else {
                        userMessage = "Error: " + errorMessage;
                    }
                } else {
                    userMessage = "Error searching for places";
                }
                
                ThreadUtils.runOnMainThread(() -> listener.onPlacesError(userMessage));
            }
        });
    }
    
    /**
     * Check if all fetch requests have been completed and return the results if they have
     */
    private void checkIfAllFetchesCompleted(AtomicInteger pendingFetches,
                                         List<PlaceInfo> places,
                                         AtomicBoolean errorReported,
                                         OnPlacesFoundListener listener) {
        // Decrement the counter of pending fetches
        int remaining = pendingFetches.decrementAndGet();
        
        // If there are still fetches pending, wait for them
        if (remaining > 0) {
            return;
        }
        
        // All fetches have completed, deliver the results
        ThreadUtils.runOnMainThread(() -> {
            try {
                if (places.isEmpty()) {
                    // No places found
                    if (!errorReported.getAndSet(true)) {
                        listener.onPlacesError("No coffee shops found nearby");
                    }
                } else {
                    // Convert the list to an array
                    PlaceInfo[] placesArray = places.toArray(new PlaceInfo[0]);
                    
                    // Report success
                    listener.onPlacesFound(placesArray);
                    Log.d(TAG, "Found " + placesArray.length + " places in total");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error delivering place results: " + e.getMessage());
                if (!errorReported.getAndSet(true)) {
                    listener.onPlacesError("Error processing place results");
                }
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
            // For coffee shops, use multiple related types to get more results
            return Arrays.asList(
                PlaceTypes.CAFE,
                PlaceTypes.RESTAURANT,
                PlaceTypes.BAKERY,
                PlaceTypes.FOOD,
                PlaceTypes.STORE
            );
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
    
    // Sample data method removed - using only real data
}
