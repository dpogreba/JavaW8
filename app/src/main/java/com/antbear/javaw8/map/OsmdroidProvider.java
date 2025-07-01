package com.antbear.javaw8.map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewGroup;
import java.io.File;

import com.antbear.javaw8.utils.ThreadUtils;
import com.antbear.javaw8.utils.UiMessageHandler;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implementation of MapProvider using osmdroid.
 */
public class OsmdroidProvider implements MapProvider {
    private static final String TAG = "OsmdroidProvider";
    private static final String OVERPASS_API_URL = "https://overpass-api.de/api/interpreter";
    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/search";
    
    private Context context;
    private MapView mapView;
    private OsmMapFragment mapFragment;
    private boolean initialized = false;
    private OkHttpClient httpClient;
    private MyLocationNewOverlay myLocationOverlay;
    
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
            // Get application context to avoid memory leaks
            Context appContext = context.getApplicationContext();
            
            // Configure osmdroid
            org.osmdroid.config.Configuration.getInstance().load(
                appContext, 
                PreferenceManager.getDefaultSharedPreferences(appContext)
            );
            
            // Set a detailed user agent string to avoid getting banned by tile servers
            String userAgent = appContext.getPackageName() + "/" + 
                               getAppVersionName(appContext) + " " +
                               "osmdroid/6.1.10"; // Hardcoded version instead of BuildConfig.VERSION_NAME
            org.osmdroid.config.Configuration.getInstance().setUserAgentValue(userAgent);
            
            // Set cache paths explicitly
            File cacheDir = getCacheDir(appContext);
            if (cacheDir != null) {
                org.osmdroid.config.Configuration.getInstance().setOsmdroidTileCache(cacheDir);
                org.osmdroid.config.Configuration.getInstance().setOsmdroidBasePath(cacheDir);
                Log.d(TAG, "OSMdroid cache dir set to: " + cacheDir.getAbsolutePath());
            } else {
                Log.w(TAG, "Could not set OSMdroid cache directory, using default");
            }
            
            // Set user-agent header for OkHttp requests
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            
            httpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("User-Agent", userAgent)
                                .build();
                        return chain.proceed(request);
                    })
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
            
            initialized = true;
            Log.d(TAG, "osmdroid initialized successfully with user agent: " + userAgent);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing osmdroid provider: " + e.getMessage(), e);
            initialized = false;
        }
    }
    
    /**
     * Get app version name for user agent string
     */
    private String getAppVersionName(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting app version: " + e.getMessage());
            return "1.0";
        }
    }
    
    /**
     * Get a suitable cache directory for map tiles
     */
    private File getCacheDir(Context context) {
        try {
            // Try the recommended OSMdroid way first
            File osmCacheDir = new File(context.getCacheDir(), "osmdroid");
            if (!osmCacheDir.exists()) {
                if (osmCacheDir.mkdirs()) {
                    Log.d(TAG, "Created osmdroid cache directory");
                }
            }
            
            if (osmCacheDir.exists() && osmCacheDir.canWrite()) {
                return osmCacheDir;
            }
            
            // Fall back to the app's main cache directory
            File fallbackDir = context.getCacheDir();
            if (fallbackDir.exists() && fallbackDir.canWrite()) {
                Log.d(TAG, "Using app cache directory for osmdroid");
                return fallbackDir;
            }
            
            Log.w(TAG, "No suitable cache directory found");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error creating cache directory: " + e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public Fragment createMapFragment(FragmentManager fragmentManager, int containerId) {
        // Create a new fragment to host the MapView
        mapFragment = new OsmMapFragment();
        
        // Add it to the container
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(containerId, mapFragment);
        transaction.commit();
        
        // Get reference to the MapView once it's created
        mapFragment.setMapReadyCallback(new OsmMapFragment.MapReadyCallback() {
            @Override
            public void onMapReady(MapView mapView) {
                OsmdroidProvider.this.mapView = mapView;
                setupMap();
                
                // Notify listener
                if (mapReadyListener != null) {
                    mapReadyListener.onMapReady();
                }
            }
        });
        
        return mapFragment;
    }
    
    private void setupMap() {
        if (mapView == null) return;
        
        try {
            // Configure the map
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.setBuiltInZoomControls(true);
            mapView.setTilesScaledToDpi(true);
            
            // Hardware acceleration is enabled by default in newer Android versions
            // mapView.setHardwareAccelerationEnabled(true); // Not available in this OSMdroid version
            
            // Enable tile downloading
            mapView.setUseDataConnection(true);
            
            // Set a high tile download threads count
            org.osmdroid.config.Configuration.getInstance().setTileDownloadThreads((short)8);
            
            // Set tile download maximum queue size
            org.osmdroid.config.Configuration.getInstance().setTileDownloadMaxQueueSize((short)128);
            
            // Set tile filesystem threads count
            org.osmdroid.config.Configuration.getInstance().setTileFileSystemThreads((short)8);
            
            // Set maximum tile cache size
            org.osmdroid.config.Configuration.getInstance().setTileFileSystemMaxQueueSize((short)128);
            
            // Set up location overlay
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), mapView);
            myLocationOverlay.enableMyLocation();
            mapView.getOverlays().add(myLocationOverlay);
            
            // Set default zoom
            IMapController mapController = mapView.getController();
            mapController.setZoom(14.0);
            
            // Add a debug listener to report tile loading issues
            mapView.addOnFirstLayoutListener((v, left, top, right, bottom) -> {
                Log.d(TAG, "Map first layout event - size: " + (right-left) + "x" + (bottom-top));
                
                // Force tiles to load after layout
                mapView.invalidate();
            });
            
            Log.d(TAG, "osmdroid map setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up map: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isInitialized() {
        return initialized && mapView != null;
    }
    
    @Override
    public String addMarker(double latitude, double longitude, String title, String snippet) {
        if (mapView == null) {
            Log.e(TAG, "Cannot add marker: map is not ready");
            return null;
        }
        
        // Generate a unique ID for this marker
        String markerId = UUID.randomUUID().toString();
        
        // Create marker
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setTitle(title);
        marker.setSnippet(snippet);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        // Set a coffee icon if available
        try {
            int coffeeIconId = context.getResources().getIdentifier(
                    "map_marker_coffee", "drawable", context.getPackageName());
            if (coffeeIconId != 0) {
                Drawable icon = ContextCompat.getDrawable(context, coffeeIconId);
                marker.setIcon(icon);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting marker icon: " + e.getMessage());
        }
        
        // Set custom info window
        marker.setInfoWindow(new com.antbear.javaw8.OsmInfoWindowAdapter(mapView, marker, title, snippet));
        
        // Set up marker click listener
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m, MapView mapView) {
                Log.d(TAG, "Marker clicked: " + title);
                
                // Show the info window for this marker
                m.showInfoWindow();
                
                // Notify the listener if set
                if (markerClickListener != null) {
                    String id = markerIds.get(m);
                    if (id != null) {
                        return markerClickListener.onMarkerClick(id);
                    }
                }
                
                // Return true to indicate we've handled the event
                return true;
            }
        });
        
        // Add to the map
        mapView.getOverlays().add(marker);
        
        // Store the marker
        markersById.put(markerId, marker);
        markerIds.put(marker, markerId);
        
        // Refresh the map
        mapView.invalidate();
        
        Log.d(TAG, "Added marker: " + title + " with ID: " + markerId);
        
        return markerId;
    }
    
    @Override
    public void moveCamera(double latitude, double longitude, float zoomLevel) {
        if (mapView == null) {
            Log.e(TAG, "Cannot move camera: map is not ready");
            return;
        }
        
        IMapController mapController = mapView.getController();
        mapController.setZoom((double) zoomLevel);
        mapController.setCenter(new GeoPoint(latitude, longitude));
    }
    
    @Override
    public void setOnMapReadyListener(OnMapReadyListener listener) {
        this.mapReadyListener = listener;
        
        // If map is already ready, trigger the callback immediately
        if (mapView != null && mapReadyListener != null) {
            mapReadyListener.onMapReady();
        }
    }
    
    @Override
    public void searchNearbyPlaces(String query, double latitude, double longitude, double radius, 
                                 OnPlacesFoundListener listener) {
        if (!initialized) {
            if (listener != null) {
                listener.onPlacesError("osmdroid provider not initialized");
            }
            return;
        }
        
        // We'll use Overpass API to search for POIs
        searchWithOverpass(query, latitude, longitude, radius, listener);
    }
    
    private void searchWithOverpass(String query, double latitude, double longitude, double radius,
                                 OnPlacesFoundListener listener) {
        try {
            // Convert query to appropriate OSM tags
            String osmTag = getOsmTagForQuery(query);
            
            // Build Overpass query
            String overpassQuery = String.format(
                    "[out:json];node[\"%s\"](around:%f,%f,%f);out;",
                    osmTag, radius, latitude, longitude);
            
            // Encode the query
            String encodedQuery = URLEncoder.encode(overpassQuery, "UTF-8");
            String url = OVERPASS_API_URL + "?data=" + encodedQuery;
            
            // Build the request
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", context.getPackageName())
                    .build();
            
            // Execute the request
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Overpass API request failed: " + e.getMessage(), e);
                    
                    if (listener != null) {
                        // Use our thread utility to ensure main thread callback
                        ThreadUtils.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    listener.onPlacesError("Network error while searching for places");
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error in onPlacesError callback: " + ex.getMessage(), ex);
                                }
                            }
                        });
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Overpass API error: " + response.code());
                        
                        if (listener != null) {
                            final int responseCode = response.code();
                            // Use our thread utility to ensure main thread callback
                            ThreadUtils.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        listener.onPlacesError("Error from Overpass API: " + responseCode);
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Error in onPlacesError callback: " + ex.getMessage(), ex);
                                    }
                                }
                            });
                        }
                        return;
                    }
                    
                    try {
                        // Get the response body
                        String responseBody = response.body().string();
                        
                        // Parse the response on the background thread
                        final List<PlaceInfo> places = parseOverpassResponse(responseBody);
                        
                        if (places.isEmpty()) {
                            if (listener != null) {
                                final String queryText = query;
                                // Notify about no places found on the main thread
                                ThreadUtils.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            listener.onPlacesError("No places found matching '" + queryText + "'");
                                        } catch (Exception ex) {
                                            Log.e(TAG, "Error in onPlacesError callback: " + ex.getMessage(), ex);
                                        }
                                    }
                                });
                            }
                            return;
                        }
                        
                        // Convert to array
                        final PlaceInfo[] placeArray = places.toArray(new PlaceInfo[0]);
                        
                        if (listener != null) {
                            // Deliver success callback on main thread
                            ThreadUtils.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        listener.onPlacesFound(placeArray);
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Error in onPlacesFound callback: " + ex.getMessage(), ex);
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Overpass response: " + e.getMessage(), e);
                        
                        if (listener != null) {
                            // Deliver error callback on main thread
                            ThreadUtils.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        listener.onPlacesError("Error processing search results");
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Error in onPlacesError callback: " + ex.getMessage(), ex);
                                    }
                                }
                            });
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error building Overpass request: " + e.getMessage(), e);
            
            if (listener != null) {
                // Use our thread utility to ensure main thread callback
                ThreadUtils.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listener.onPlacesError("Error preparing search request");
                        } catch (Exception ex) {
                            Log.e(TAG, "Error in onPlacesError callback: " + ex.getMessage(), ex);
                        }
                    }
                });
            }
        }
    }
    
    private String getOsmTagForQuery(String query) {
        // Map common search queries to OSM tags
        query = query.toLowerCase().trim();
        
        if (query.contains("coffee") || query.contains("cafe")) {
            return "amenity=cafe";
        } else if (query.contains("restaurant")) {
            return "amenity=restaurant";
        } else if (query.contains("bar")) {
            return "amenity=bar";
        } else if (query.contains("hotel")) {
            return "tourism=hotel";
        } else if (query.contains("park")) {
            return "leisure=park";
        } else if (query.contains("shop") || query.contains("store")) {
            return "shop";
        } else if (query.contains("gas") || query.contains("fuel")) {
            return "amenity=fuel";
        } else if (query.contains("school")) {
            return "amenity=school";
        } else if (query.contains("hospital")) {
            return "amenity=hospital";
        } else if (query.contains("bank")) {
            return "amenity=bank";
        }
        
        // Default to amenity for generic searches
        return "name";
    }
    
    private List<PlaceInfo> parseOverpassResponse(String response) throws JSONException {
        List<PlaceInfo> places = new ArrayList<>();
        
        JSONObject root = new JSONObject(response);
        JSONArray elements = root.getJSONArray("elements");
        
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            
            // Skip non-node elements (we only want points, not ways or relations)
            if (!element.getString("type").equals("node")) {
                continue;
            }
            
            JSONObject tags = element.optJSONObject("tags");
            if (tags == null) continue;
            
            String id = element.getString("id");
            double lat = element.getDouble("lat");
            double lon = element.getDouble("lon");
            
            // Extract name
            String name = tags.optString("name", "Unnamed Place");
            
            // Extract address components
            String street = tags.optString("addr:street", "");
            String houseNumber = tags.optString("addr:housenumber", "");
            String city = tags.optString("addr:city", "");
            String address = "";
            
            if (!street.isEmpty()) {
                if (!houseNumber.isEmpty()) {
                    address = houseNumber + " " + street;
                } else {
                    address = street;
                }
                
                if (!city.isEmpty()) {
                    address += ", " + city;
                }
            }
            
            // Extract phone
            String phone = tags.optString("phone", "");
            
            // Create place info
            PlaceInfo place = new PlaceInfo(
                    id,
                    name,
                    lat,
                    lon,
                    address,
                    phone,
                    null, // OSM doesn't have ratings
                    false
            );
            
            places.add(place);
        }
        
        return places;
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
        if (mapView == null || myLocationOverlay == null) {
            Log.e(TAG, "Cannot enable my location: map is not ready");
            return;
        }
        
        if (hasPermission) {
            myLocationOverlay.enableMyLocation();
        } else {
            myLocationOverlay.disableMyLocation();
        }
    }
    
    @Override
    public void onDestroy() {
        // Clean up resources
        if (mapView != null) {
            try {
                mapView.onDetach();
                Log.d(TAG, "MapView detached");
            } catch (Exception e) {
                Log.e(TAG, "Error detaching MapView: " + e.getMessage(), e);
            }
        }
        
        if (myLocationOverlay != null) {
            try {
                myLocationOverlay.disableMyLocation();
                Log.d(TAG, "Location overlay disabled");
            } catch (Exception e) {
                Log.e(TAG, "Error disabling location overlay: " + e.getMessage(), e);
            }
        }
        
        // Clear cache if desired
        /*
        try {
            org.osmdroid.config.Configuration.getInstance().getTileFileSystemProvider().clearCurrentCache();
            Log.d(TAG, "Tile cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing tile cache: " + e.getMessage(), e);
        }
        */
        
        markersById.clear();
        markerIds.clear();
        mapReadyListener = null;
        markerClickListener = null;
        infoWindowClickListener = null;
        httpClient = null;
    }
    
    /**
     * Custom Fragment to host the osmdroid MapView.
     */
    public static class OsmMapFragment extends Fragment {
        private MapView mapView;
        private MapReadyCallback callback;
        
        public interface MapReadyCallback {
            void onMapReady(MapView mapView);
        }
        
        public void setMapReadyCallback(MapReadyCallback callback) {
            this.callback = callback;
            
            // If map is already ready, trigger the callback immediately
            if (mapView != null && callback != null) {
                callback.onMapReady(mapView);
            }
        }
        
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, ViewGroup container, android.os.Bundle savedInstanceState) {
            // Create the MapView
            mapView = new MapView(getActivity());
            mapView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            
            // Notify that the map is ready
            if (callback != null) {
                callback.onMapReady(mapView);
            }
            
            return mapView;
        }
        
        @Override
        public void onResume() {
            super.onResume();
            if (mapView != null) {
                mapView.onResume();
            }
        }
        
        @Override
        public void onPause() {
            super.onPause();
            if (mapView != null) {
                mapView.onPause();
            }
        }
        
        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mapView != null) {
                mapView.onDetach();
            }
        }
    }
    
    /**
     * Custom InfoWindow for osmdroid markers.
     */
    private static class OsmInfoWindow extends org.osmdroid.views.overlay.infowindow.InfoWindow {
        private final String title;
        private final String snippet;
        private final InfoWindowClickListener clickListener;
        
        public interface InfoWindowClickListener {
            void onInfoWindowClick(Marker marker);
        }
        
        public OsmInfoWindow(MapView mapView, Marker marker, String title, String snippet, InfoWindowClickListener listener) {
            super(org.osmdroid.library.R.layout.bonuspack_bubble, mapView);
            this.title = title;
            this.snippet = snippet;
            this.clickListener = listener;
            
            // Add click listener to the info window
            mView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onInfoWindowClick(marker);
                }
                close();
            });
        }
        
        @Override
        public void onOpen(Object item) {
            if (!(item instanceof Marker)) return;
            
            // Set title and snippet text
            android.widget.TextView titleView = mView.findViewById(org.osmdroid.library.R.id.bubble_title);
            android.widget.TextView snippetView = mView.findViewById(org.osmdroid.library.R.id.bubble_description);
            
            if (titleView != null) titleView.setText(title);
            if (snippetView != null) snippetView.setText(snippet);
        }
        
        @Override
        public void onClose() {
            // Nothing to do here
        }
    }
}
