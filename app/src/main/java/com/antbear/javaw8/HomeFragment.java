package com.antbear.javaw8;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Set custom info window
        mMap.setInfoWindowAdapter(this);
        
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
        if (lastKnownLocation == null) {
            Log.d(TAG, "Last known location is null");
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
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
        
        // Process the results
        for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
            Place place = placeLikelihood.getPlace();
            
            // Check if this place is a coffee shop/cafe
            if (isCoffeeShop(place)) {
                foundCoffeeShops = true;
                addCoffeeShopMarker(place);
            }
        }
        
        if (!foundCoffeeShops) {
            // If no coffee shops found in the immediate vicinity, try a wider search
            searchCoffeeShopsNearby();
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
                }
            });
    }
    
    private void processPredictions(FindAutocompletePredictionsResponse response) {
        if (response.getAutocompletePredictions().isEmpty()) {
            Toast.makeText(requireContext(), "No coffee shops found nearby", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Limit to 10 places to avoid overwhelming the map
        int count = Math.min(10, response.getAutocompletePredictions().size());
        
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
    }
    
    private String getPlaceSnippet(Place place) {
        StringBuilder snippet = new StringBuilder();
        
        // Add address if available
        if (place.getAddress() != null) {
            snippet.append(place.getAddress());
        }
        
        // Add rating if available
        if (place.getRating() != null) {
            if (snippet.length() > 0) {
                snippet.append("\n");
            }
            snippet.append("Rating: ").append(place.getRating()).append(" ★");
        }
        
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
}
