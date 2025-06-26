package main.java.com.antbear.javaw8;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Custom InfoWindowAdapter to display coffee shop details in a stylish card layout
 */
public class CoffeeShopInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private static final String TAG = "CoffeeShopInfoWindow";
    private final View mWindow;
    private final Context mContext;

    public CoffeeShopInfoWindowAdapter(Context context) {
        mContext = context;
        try {
            mWindow = LayoutInflater.from(context).inflate(R.layout.map_info_window, null);
            Log.d(TAG, "Custom info window successfully inflated");
        } catch (Exception e) {
            Log.e(TAG, "Error inflating custom info window: " + e.getMessage());
            throw e; // Re-throw to ensure we fail early if there's a problem
        }
    }

    /**
     * Set up the window with marker data
     */
    private void renderWindowText(Marker marker, View view) {
        try {
            // Set the title
            String title = marker.getTitle();
            TextView titleView = view.findViewById(R.id.txt_title);
            if (title != null) {
                titleView.setText(title);
            } else {
                titleView.setText("Coffee Shop");
            }

            // Parse the snippet to extract different parts
            String snippet = marker.getSnippet();
            if (snippet != null) {
                String[] lines = snippet.split("\n");
                
                // Address is the first line
                TextView addressView = view.findViewById(R.id.txt_address);
                if (lines.length > 0) {
                    addressView.setText(lines[0]);
                } else {
                    addressView.setText("Address not available");
                }
                
                // Phone number
                LinearLayout phoneContainer = view.findViewById(R.id.phone_container);
                TextView phoneView = view.findViewById(R.id.txt_phone);
                boolean hasPhone = false;
                
                // Rating
                LinearLayout ratingContainer = view.findViewById(R.id.rating_container);
                TextView ratingView = view.findViewById(R.id.txt_rating);
                boolean hasRating = false;
                
                // Sample data indicator
                TextView sampleIndicator = view.findViewById(R.id.txt_sample_indicator);
                boolean isSampleData = false;
                
                // Process the remaining lines
                for (String line : lines) {
                    if (line.startsWith("Phone:")) {
                        phoneView.setText(line);
                        hasPhone = true;
                    } else if (line.startsWith("Rating:")) {
                        ratingView.setText(line);
                        hasRating = true;
                    } else if (line.contains("(Sample Data)")) {
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
                Button directionsButton = view.findViewById(R.id.btn_directions);
                if (directionsButton != null) {
                    directionsButton.setText("Get Directions");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rendering info window: " + e.getMessage());
            // We won't attempt to recover here - let the default info window handle it
        }
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        try {
            renderWindowText(marker, mWindow);
            return mWindow;
        } catch (Exception e) {
            Log.e(TAG, "Error in getInfoWindow: " + e.getMessage());
            return null; // Fall back to default window
        }
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        // We're using getInfoWindow instead
        return null;
    }
}
