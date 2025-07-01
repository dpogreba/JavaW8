package com.antbear.javaw8.utils;
// Fix package declaration to match expected path

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * Utility class for network-related operations.
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    /**
     * Checks if the device has an active internet connection.
     * @param context Application context
     * @return true if internet is available, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null in isNetworkAvailable");
            return false;
        }

        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null");
            return false;
        }

        // For Android 10 (API 29) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                Log.d(TAG, "No active network found");
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                Log.d(TAG, "Network capabilities not found");
                return false;
            }

            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            
            // Log connection type for debugging
            String connectionType = "unknown";
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                connectionType = "WiFi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                connectionType = "Cellular";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                connectionType = "Ethernet";
            }
            
            Log.d(TAG, "Network available: " + hasInternet + ", type: " + connectionType);
            return hasInternet;
        } 
        // For Android 9 (API 28) and below
        else {
            try {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                Log.d(TAG, "Network available (legacy): " + isConnected + 
                       ", type: " + (activeNetworkInfo != null ? activeNetworkInfo.getTypeName() : "none"));
                return isConnected;
            } catch (Exception e) {
                Log.e(TAG, "Error checking network state: " + e.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Performs a simple ping test to verify if a host is reachable.
     * This is a more robust way to check actual internet connectivity.
     * 
     * Note: This method should not be called on the main thread.
     * 
     * @param host The host to ping (e.g., "api.openstreetmap.org")
     * @return true if host is reachable, false otherwise
     */
    public static boolean isHostReachable(String host) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 " + host);
            int exitValue = ipProcess.waitFor();
            Log.d(TAG, "Ping " + host + " result: " + (exitValue == 0 ? "successful" : "failed"));
            return (exitValue == 0);
        } catch (Exception e) {
            Log.e(TAG, "Error pinging host " + host + ": " + e.getMessage());
            return false;
        }
    }
}
