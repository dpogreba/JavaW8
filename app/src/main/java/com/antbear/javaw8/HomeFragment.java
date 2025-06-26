package com.antbear.javaw8;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final double SEARCH_RADIUS_METERS = 2000; // 2 km radius
    
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private Location lastKnownLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.maps_api_key));
        }
        placesClient = Places.createClient(requireContext());
        
        // Get the SupportMapFragment and request the Google Map asynchronously
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
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
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel any pending fallback timer to prevent memory leaks
        if (fallbackRunnable != null) {
            fallbackHandler.removeCallbacks(fallbackRunnable);
            fallbackRunnable = null;
        }
    }

    // Track added coffee shops for fallback decision
    private int totalCoffeeShopsAdded = 0;
    private static final int FALLBACK_TIMEOUT_MS = 5000; // 5 seconds
    private Handler fallbackHandler = new Handler(Looper.getMainLooper());
    private Runnable fallbackRunnable;
    
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Set custom info window
        mMap.setInfoWindowAdapter(this);
        
        // Set up info window click listener to open directions
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {
                // Get the location of the coffee shop
                LatLng position = marker.getPosition();
                
                // Create a URI for Google Maps directions
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + position.latitude + "," + position.longitude + "&mode=d");
                
                // Create an Intent from gmmIntentUri
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                
                // Check if Google Maps is installed
                if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    // If Google Maps isn't installed, open in browser
                    Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + 
                                             position.latitude + "," + position.longitude);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
                    startActivity(browserIntent);
                }
            }
        });
        
        // Enable my location button if permission is granted
        enableMyLocation();
        
        // Default location (in case permission is denied)
        LatLng defaultLocation = new LatLng(37.4220, -122.0841); // Mountain View, CA
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
    }
    
    // InfoWindowAdapter implementation
    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        return null; // Use default info window contents
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        return null; // Use default info window
    }
    
    private void enableMyLocation() {
        // Check if permission is granted
        if (ActivityCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            // Enable the my-location layer
            mMap.setMyLocationEnabled(true);
            
            // Get the user's last known location
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Save the user's last known location
                            lastKnownLocation = location;
                            
                            // Got the user's location, center the map there
                            LatLng userLocation = new LatLng(location.getLatitude(), 
                                                           location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                            
                            // Search for nearby coffee shops
                            searchNearbyCoffeeShops();
                        }
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
                Toast.makeText(requireContext(), 
                    "Location permission is required to show your location on the map", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void searchNearbyCoffeeShops() {
        // Reset the counter each time we start a new search
        totalCoffeeShopsAdded = 0;
        
        // Start fallback timer
        startFallbackTimer();
        
        if (lastKnownLocation == null) {
            Log.e(TAG, "Last known location is null - cannot search for coffee shops");
            Toast.makeText(requireContext(), "Unable to get your location. Using fallback locations.", Toast.LENGTH_LONG).show();
            addFallbackCoffeeShops();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            Toast.makeText(requireContext(), "Location permission required to find nearby coffee shops", Toast.LENGTH_LONG).show();
            addFallbackCoffeeShops();
            return;
        }
        
        // Show toast to let user know we're searching
        Toast.makeText(requireContext(), "Searching for coffee shops nearby...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Starting Places API search for coffee shops at: " + 
              lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
        
        // Use FindCurrentPlaceRequest to search for coffee shops nearby
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.NAME,
                Place.Field.ID,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.RATING,
                Place.Field.TYPES);
        
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);
        
        placesClient.findCurrentPlace(request)
            .addOnSuccessListener(requireActivity(), new OnSuccessListener<FindCurrentPlaceResponse>() {
                @Override
                public void onSuccess(FindCurrentPlaceResponse response) {
                    // Process places with "cafe" type
                    processCoffeeShops(response);
                }
            })
            .addOnFailureListener(requireActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        Log.e(TAG, "Place not found: " + apiException.getStatusCode());
                    }
                    // As a fallback, use alternative search method
                    searchCoffeeShopsNearby();
                }
            });
    }
    
    private void processCoffeeShops(FindCurrentPlaceResponse response) {
        boolean foundCoffeeShops = false;
        int totalPlaces = 0;
        int coffeeShops = 0;
        
        // Process the results
        for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
            totalPlaces++;
            Place place = placeLikelihood.getPlace();
            
            // Check if this place is a coffee shop/cafe
            if (isCoffeeShop(place)) {
                coffeeShops++;
                foundCoffeeShops = true;
                addCoffeeShopMarker(place);
                Log.d(TAG, "Added coffee shop marker: " + place.getName() + 
                      " at " + place.getLatLng().latitude + "," + place.getLatLng().longitude);
            }
        }
        
        Log.d(TAG, "Found " + totalPlaces + " total places, " + coffeeShops + " coffee shops");
        
        if (!foundCoffeeShops) {
            Log.w(TAG, "No coffee shops found in current place results, trying wider search");
            // If no coffee shops found in the immediate vicinity, try a wider search
            searchCoffeeShopsNearby();
        } else {
            Toast.makeText(requireContext(), "Found " + coffeeShops + " coffee shops nearby", 
                           Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean isCoffeeShop(Place place) {
        List<Place.Type> placeTypes = place.getTypes();
        if (placeTypes == null) return false;
        
        // Check for place types typically associated with coffee shops
        return placeTypes.contains(Place.Type.CAFE) || 
               placeTypes.contains(Place.Type.RESTAURANT) && place.getName().toLowerCase().contains("coffee");
    }
    
    private void searchCoffeeShopsNearby() {
        if (lastKnownLocation == null) return;
        
        // This method uses FindAutocompletePredictionsRequest as an alternative approach
        // to find coffee shops when FindCurrentPlace doesn't return the expected results
        
        // Calculate bounds for the search area
        double latDelta = SEARCH_RADIUS_METERS / 111000.0; // approximate meters to degrees
        double lngDelta = SEARCH_RADIUS_METERS / (111000.0 * Math.cos(Math.toRadians(lastKnownLocation.getLatitude())));
        
        LatLng southwest = new LatLng(
                lastKnownLocation.getLatitude() - latDelta,
                lastKnownLocation.getLongitude() - lngDelta);
        LatLng northeast = new LatLng(
                lastKnownLocation.getLatitude() + latDelta,
                lastKnownLocation.getLongitude() + lngDelta);
        
        RectangularBounds bounds = RectangularBounds.newInstance(southwest, northeast);
        
        // Create a request to find coffee shops
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationBias(bounds)
                .setOrigin(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setQuery("coffee shop")
                .build();
        
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener(requireActivity(), new OnSuccessListener<FindAutocompletePredictionsResponse>() {
                @Override
                public void onSuccess(FindAutocompletePredictionsResponse response) {
                    // Process the predictions and fetch details for each place
                    processPredictions(response);
                }
            })
            .addOnFailureListener(requireActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error finding coffee shops: " + e.getMessage());
                        Toast.makeText(requireContext(), "Unable to find coffee shops nearby", Toast.LENGTH_SHORT).show();
                        // Call fallback method when search fails
                        addFallbackCoffeeShops();
                    }
            });
    }
    
    private void processPredictions(FindAutocompletePredictionsResponse response) {
        if (response.getAutocompletePredictions().isEmpty()) {
            Log.w(TAG, "No coffee shop predictions found, using fallbacks");
            Toast.makeText(requireContext(), "No coffee shops found nearby", Toast.LENGTH_SHORT).show();
            addFallbackCoffeeShops();
            return;
        }
        
        // Limit to 10 places to avoid overwhelming the map
        int count = Math.min(10, response.getAutocompletePredictions().size());
        Log.d(TAG, "Found " + count + " coffee shop predictions, fetching details");
        
        for (int i = 0; i < count; i++) {
            String placeId = response.getAutocompletePredictions().get(i).getPlaceId();
            fetchPlaceDetails(placeId);
        }
    }
    
    private void fetchPlaceDetails(String placeId) {
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
            .addOnSuccessListener(requireActivity(), new OnSuccessListener<FetchPlaceResponse>() {
                @Override
                public void onSuccess(FetchPlaceResponse response) {
                    Place place = response.getPlace();
                    addCoffeeShopMarker(place);
                }
            })
            .addOnFailureListener(requireActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        Log.e(TAG, "Place not found: " + apiException.getStatusCode());
                    }
                }
            });
    }
    
    private void addCoffeeShopMarker(Place place) {
        if (place.getLatLng() == null) return;
        
        // Create a marker for this coffee shop
        MarkerOptions markerOptions = new MarkerOptions()
                .position(place.getLatLng())
                .title(place.getName())
                .snippet(getPlaceSnippet(place))
                .icon(getCoffeeMarkerIcon());
        
        // Add the marker to the map
        mMap.addMarker(markerOptions);
        
        // Increment counter to track how many real coffee shops were added
        totalCoffeeShopsAdded++;
    }
    
    private String getPlaceSnippet(Place place) {
        StringBuilder snippet = new StringBuilder();
        
        // Add address if available
        if (place.getAddress() != null) {
            snippet.append(place.getAddress());
        }
        
        // Add phone number if available
        if (place.getPhoneNumber() != null) {
            if (snippet.length() > 0) {
                snippet.append("\n");
            }
            snippet.append("Phone: ").append(place.getPhoneNumber());
        }
        
        // Add rating if available
        if (place.getRating() != null) {
            if (snippet.length() > 0) {
                snippet.append("\n");
            }
            snippet.append("Rating: ").append(place.getRating()).append(" ★");
        }
        
        // Add directions instruction
        if (snippet.length() > 0) {
            snippet.append("\n");
        }
        snippet.append("Tap to get directions");
        
        return snippet.toString();
    }
    
    private BitmapDescriptor getCoffeeMarkerIcon() {
        // Convert drawable to BitmapDescriptor for map marker
        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_coffee);
        if (drawable == null) {
            // Use default marker with orange color (closest to brown) if drawable is not available
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        }
        
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        
        return BitmapDescriptorFactory.fromBitmap(bitmap);
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
                    if (isAdded()) { // Make sure fragment is still attached
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addFallbackCoffeeShops();
                            }
                        });
                    }
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
        Toast.makeText(requireContext(), "Using sample coffee shop locations", Toast.LENGTH_LONG).show();
        
        // Center point for our fallbacks - use user location if available, otherwise default
        LatLng center;
        if (lastKnownLocation != null) {
            center = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        } else {
            // Default to Mountain View, CA
            center = new LatLng(37.4220, -122.0841);
        }
        
        // Add coffee shops around the center point
        addSampleCoffeeShop(
                "JavaW8 Coffee House", 
                new LatLng(center.latitude + 0.003, center.longitude + 0.003),
                "123 Coffee Lane",
                "555-123-4567",
                4.8f);
        
        addSampleCoffeeShop(
                "Brew & Bean", 
                new LatLng(center.latitude - 0.002, center.longitude + 0.001),
                "456 Espresso Ave",
                "555-987-6543",
                4.5f);
        
        addSampleCoffeeShop(
                "Caffeine Corner", 
                new LatLng(center.latitude + 0.001, center.longitude - 0.002),
                "789 Latte Blvd",
                "555-246-1357",
                4.2f);
        
        addSampleCoffeeShop(
                "Mobile Mocha", 
                new LatLng(center.latitude - 0.001, center.longitude - 0.001),
                "321 Android St",
                "555-369-8521",
                4.7f);
    }
    
    /**
     * Helper method to add a sample coffee shop marker
     */
    private void addSampleCoffeeShop(String name, LatLng location, String address, String phone, float rating) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .title(name)
                .snippet(buildSnippet(address, phone, rating))
                .icon(getCoffeeMarkerIcon());
        
        mMap.addMarker(markerOptions);
        
        // Log the sample coffee shop
        Log.d(TAG, "Added sample coffee shop marker: " + name + " at " + location.latitude + "," + location.longitude);
    }
    
    /**
     * Helper method to build the snippet for a sample coffee shop
     */
    private String buildSnippet(String address, String phone, float rating) {
        StringBuilder snippet = new StringBuilder();
        
        // Add address
        snippet.append(address);
        
        // Add phone number
        snippet.append("\nPhone: ").append(phone);
        
        // Add rating
        snippet.append("\nRating: ").append(rating).append(" ★");
        
        // Add sample indicator
        snippet.append("\n(Sample Data)");
        
        // Add directions instruction
        snippet.append("\nTap to get directions");
        
        return snippet.toString();
    }
}
