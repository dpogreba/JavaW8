package com.antbear.javaw8.map;

import android.content.Context;
import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.gms.common.api.ApiException;
import com.antbear.javaw8.CoffeeShopInfoWindowAdapter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of MapProvider using Google Maps and Places API.
 */
public class GoogleMapsProvider implements MapProvider, OnMapReadyCallback {
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
            // Initialize Places API
            if (!Places.isInitialized()) {
                Places.initialize(context, getApiKey(context));
                Log.d(TAG, "Places API initialized");
            }
            placesClient = Places.createClient(context);
            initialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Google Maps provider: " + e.getMessage(), e);
            initialized = false;
        }
    }
    
    private String getApiKey(Context context) {
        // Get the API key from resources
        int resId = context.getResources().getIdentifier("maps_api_key", "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        
        Log.e(TAG, "Maps API key not found in resources");
        return "";
    }
    
    @Override
    public Fragment createMapFragment(FragmentManager fragmentManager, int containerId) {
        // Create a new SupportMapFragment
        mapFragment = SupportMapFragment.newInstance();
        
        // Add it to the container
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(containerId, mapFragment);
        transaction.commit();
        
        // Set up the callback
        mapFragment.getMapAsync(this);
        
        return mapFragment;
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
        
        // Create and add the marker
        MarkerOptions options = new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title(title)
                .snippet(snippet);
        
        Marker marker = googleMap.addMarker(options);
        
        if (marker != null) {
            // Store the mapping between ID and marker
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
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        Log.d(TAG, "Google Map is ready");
        
        // Configure map settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);
        
        // Set up marker click listener
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (markerClickListener != null) {
                    String markerId = markerIds.get(marker);
                    if (markerId != null) {
                        return markerClickListener.onMarkerClick(markerId);
                    }
                }
                return false;
            }
        });
        
        // Set custom info window adapter
        try {
            googleMap.setInfoWindowAdapter(new CoffeeShopInfoWindowAdapter(context));
            Log.d(TAG, "Custom info window adapter set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom info window adapter: " + e.getMessage(), e);
        }
        
        // Set up info window click listener
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (infoWindowClickListener != null) {
                    String markerId = markerIds.get(marker);
                    if (markerId != null) {
                        infoWindowClickListener.onInfoWindowClick(markerId);
                    }
                }
            }
        });
        
        // Notify listener
        if (mapReadyListener != null) {
            mapReadyListener.onMapReady();
        }
    }
    
    @Override
    public void searchNearbyPlaces(String query, double latitude, double longitude, double radius, 
                                 OnPlacesFoundListener listener) {
        if (!initialized || placesClient == null) {
            if (listener != null) {
                listener.onPlacesError("Places API not initialized");
            }
            return;
        }
        
        // Calculate bounds for the search area (approximately)
        double latDelta = radius / 111000.0; // approximate meters to degrees
        double lngDelta = radius / (111000.0 * Math.cos(Math.toRadians(latitude)));
        
        LatLng southwest = new LatLng(latitude - latDelta, longitude - lngDelta);
        LatLng northeast = new LatLng(latitude + latDelta, longitude + lngDelta);
        RectangularBounds bounds = RectangularBounds.newInstance(southwest, northeast);
        
        // Create a request to find places
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationBias(bounds)
                .setOrigin(new LatLng(latitude, longitude))
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setQuery(query)
                .build();
        
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener(response -> {
                if (response.getAutocompletePredictions().isEmpty()) {
                    if (listener != null) {
                        listener.onPlacesError("No places found matching '" + query + "'");
                    }
                    return;
                }
                
                // Limit to 10 places to avoid overwhelming the map
                int count = Math.min(10, response.getAutocompletePredictions().size());
                PlaceInfo[] places = new PlaceInfo[count];
                final int[] completedRequests = {0};
                
                for (int i = 0; i < count; i++) {
                    final int index = i;
                    String placeId = response.getAutocompletePredictions().get(i).getPlaceId();
                    fetchPlaceDetails(placeId, new FetchPlaceCallback() {
                        @Override
                        public void onPlaceFetched(PlaceInfo placeInfo) {
                            places[index] = placeInfo;
                            checkCompletion();
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Error fetching place details: " + errorMessage);
                            checkCompletion();
                        }
                        
                        private void checkCompletion() {
                            completedRequests[0]++;
                            if (completedRequests[0] >= count && listener != null) {
                                // Filter out nulls (places that failed to fetch)
                                int validPlaces = 0;
                                for (PlaceInfo place : places) {
                                    if (place != null) validPlaces++;
                                }
                                
                                PlaceInfo[] result = new PlaceInfo[validPlaces];
                                int j = 0;
                                for (PlaceInfo place : places) {
                                    if (place != null) {
                                        result[j++] = place;
                                    }
                                }
                                
                                listener.onPlacesFound(result);
                            }
                        }
                    });
                }
            })
            .addOnFailureListener(e -> {
                String errorMessage = "Error finding places";
                
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    int statusCode = apiException.getStatusCode();
                    errorMessage = getErrorMessageForStatusCode(statusCode);
                    Log.e(TAG, "Places API error: " + statusCode + " - " + errorMessage);
                } else {
                    Log.e(TAG, "Error finding places: " + e.getMessage(), e);
                }
                
                if (listener != null) {
                    listener.onPlacesError(errorMessage);
                }
            });
    }
    
    private String getErrorMessageForStatusCode(int statusCode) {
        switch (statusCode) {
            case 7: // NETWORK_ERROR
                return "Network error - please check your connection";
            case 8: // INTERNAL_ERROR
                return "Google Places API internal error";
            case 9: // INVALID_REQUEST
                return "Invalid Places API request";
            case 13: // OPERATION_NOT_ALLOWED
                return "Places API not enabled or API key issues";
            case 16: // API_KEY_INVALID
                return "Invalid Google API key";
            case 17: // BILLING_DISABLED
                return "Billing not enabled on Google Cloud Console";
            default:
                return "Error finding places (code: " + statusCode + ")";
        }
    }
    
    private void fetchPlaceDetails(String placeId, FetchPlaceCallback callback) {
        // Specify the fields to return
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.PHONE_NUMBER,
                Place.Field.RATING);
        
        // Construct a request object
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);
        
        placesClient.fetchPlace(request)
            .addOnSuccessListener(response -> {
                Place place = response.getPlace();
                
                if (place.getLatLng() == null) {
                    callback.onError("Place has no location data");
                    return;
                }
                
                PlaceInfo placeInfo = new PlaceInfo(
                        place.getId(),
                        place.getName(),
                        place.getLatLng().latitude,
                        place.getLatLng().longitude,
                        place.getAddress(),
                        place.getPhoneNumber(),
                        place.getRating(),
                        false
                );
                
                callback.onPlaceFetched(placeInfo);
            })
            .addOnFailureListener(e -> {
                String errorMessage = "Error fetching place details";
                
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    int statusCode = apiException.getStatusCode();
                    Log.e(TAG, "Places API error fetching place: " + statusCode);
                    errorMessage += " (" + statusCode + ")";
                } else {
                    Log.e(TAG, "Error fetching place details: " + e.getMessage(), e);
                }
                
                callback.onError(errorMessage);
            });
    }
    
    interface FetchPlaceCallback {
        void onPlaceFetched(PlaceInfo placeInfo);
        void onError(String errorMessage);
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
            Log.e(TAG, "Security exception enabling my location: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onDestroy() {
        // Clean up resources
        markersById.clear();
        markerIds.clear();
        mapReadyListener = null;
        markerClickListener = null;
        infoWindowClickListener = null;
    }
}
