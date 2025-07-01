package com.antbear.javaw8;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

/**
 * Custom InfoWindow for osmdroid markers to display coffee shop details
 */
public class OsmInfoWindowAdapter extends InfoWindow {

    private static final String TAG = "OsmInfoWindow";
    private final Context context;
    private final String title;
    private final String snippet;

    public OsmInfoWindowAdapter(MapView mapView, Marker marker, String title, String snippet) {
        super(R.layout.map_info_window, mapView);
        this.context = mapView.getContext();
        this.title = title;
        this.snippet = snippet;
        
        // Add click listener to the info window
        mView.setOnClickListener(v -> {
            // This will trigger the marker's OnMarkerClickListener
            marker.closeInfoWindow();
            marker.showInfoWindow();
        });
    }

    @Override
    public void onOpen(Object item) {
        try {
            // Set the title
            TextView titleView = mView.findViewById(R.id.txt_title);
            if (title != null) {
                titleView.setText(title);
            } else {
                titleView.setText("Coffee Shop");
            }

            // Parse the snippet to extract different parts
            if (snippet != null) {
                String[] lines = snippet.split("\n");
                
                // Address is the first line
                TextView addressView = mView.findViewById(R.id.txt_address);
                if (lines.length > 0) {
                    addressView.setText(lines[0]);
                } else {
                    addressView.setText("Address not available");
                }
                
                // Phone number
                LinearLayout phoneContainer = mView.findViewById(R.id.phone_container);
                TextView phoneView = mView.findViewById(R.id.txt_phone);
                boolean hasPhone = false;
                
                // Rating
                LinearLayout ratingContainer = mView.findViewById(R.id.rating_container);
                TextView ratingView = mView.findViewById(R.id.txt_rating);
                boolean hasRating = false;
                
                // Sample data indicator
                TextView sampleIndicator = mView.findViewById(R.id.txt_sample_indicator);
                boolean isSampleData = false;
                
                // Process the remaining lines
                for (String line : lines) {
                    if (line.startsWith("Phone:")) {
                        phoneView.setText(line);
                        hasPhone = true;
                    } else if (line.startsWith("Rating:")) {
                        ratingView.setText(line);
                        hasRating = true;
                    } else if (line.equals("(Sample Data)")) {  // Exact match to avoid partial matches
                        sampleIndicator.setText("Sample data - real coffee shops may vary");
                        sampleIndicator.setVisibility(View.VISIBLE);
                        isSampleData = true;
                    }
                }
                
                // Show/hide containers based on data availability
                if (phoneContainer != null) {
                    phoneContainer.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
                }
                if (ratingContainer != null) {
                    ratingContainer.setVisibility(hasRating ? View.VISIBLE : View.GONE);
                }
                if (sampleIndicator != null) {
                    sampleIndicator.setVisibility(isSampleData ? View.VISIBLE : View.GONE);
                }
                
                // Ensure directions button has text
                Button directionsButton = mView.findViewById(R.id.btn_directions);
                if (directionsButton != null) {
                    directionsButton.setText("Get Directions");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rendering info window: " + e.getMessage());
        }
    }

    @Override
    public void onClose() {
        // Nothing to do here
    }
}
