package com.antbear.javaw8.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * A utility class for safely displaying UI messages from any thread.
 * This class ensures all UI operations happen on the main thread.
 */
public class UiMessageHandler {
    private static final String TAG = "UiMessageHandler";
    
    /**
     * Shows a toast message safely from any thread
     * 
     * @param context The context to use for the Toast
     * @param message The message to display
     * @param duration The duration (Toast.LENGTH_SHORT or Toast.LENGTH_LONG)
     */
    public static void showToast(@Nullable final Context context, 
                               @NonNull final String message, 
                               final int duration) {
        if (context == null) {
            Log.e(TAG, "Cannot show toast: context is null");
            return;
        }
        
        ThreadUtils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(context.getApplicationContext(), message, duration).show();
                } catch (Exception e) {
                    // Catch any exceptions to prevent crashes
                    Log.e(TAG, "Error showing toast: " + e.getMessage(), e);
                }
            }
        });
    }
    
    /**
     * Shows a toast message safely from a fragment
     * 
     * @param fragment The fragment to use for context
     * @param message The message to display
     * @param duration The duration (Toast.LENGTH_SHORT or Toast.LENGTH_LONG)
     * @return true if the toast was shown, false otherwise
     */
    public static boolean showToastFromFragment(@Nullable final Fragment fragment, 
                                             @NonNull final String message, 
                                             final int duration) {
        if (fragment == null) {
            Log.e(TAG, "Cannot show toast: fragment is null");
            return false;
        }
        
        final boolean[] result = {false};
        
        ThreadUtils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                // Check if fragment is still attached to activity
                if (!fragment.isAdded() || fragment.getContext() == null) {
                    Log.w(TAG, "Cannot show toast: fragment is not attached to context");
                    return;
                }
                
                try {
                    Toast.makeText(fragment.requireContext(), message, duration).show();
                    result[0] = true;
                } catch (Exception e) {
                    Log.e(TAG, "Error showing toast from fragment: " + e.getMessage(), e);
                }
            }
        });
        
        return result[0];
    }
    
    /**
     * A callback interface for UI operations that need to happen on the main thread
     */
    public interface UiCallback {
        void onUiThread();
    }
    
    /**
     * Runs a callback on the UI thread
     * 
     * @param callback The callback to run
     */
    public static void runOnUiThread(@NonNull final UiCallback callback) {
        ThreadUtils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onUiThread();
                } catch (Exception e) {
                    Log.e(TAG, "Error in UI callback: " + e.getMessage(), e);
                }
            }
        });
    }
    
    /**
     * Runs a callback on the UI thread, but only if the fragment is still attached
     * 
     * @param fragment The fragment to check
     * @param callback The callback to run
     * @return true if the callback was executed, false otherwise
     */
    public static boolean runOnUiThreadIfFragmentAlive(@Nullable final Fragment fragment, 
                                                   @NonNull final UiCallback callback) {
        if (fragment == null) {
            Log.e(TAG, "Cannot run UI operation: fragment is null");
            return false;
        }
        
        final boolean[] result = {false};
        
        ThreadUtils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                // Check if fragment is still attached to activity
                if (!fragment.isAdded()) {
                    Log.w(TAG, "Cannot run UI operation: fragment is not attached");
                    return;
                }
                
                try {
                    callback.onUiThread();
                    result[0] = true;
                } catch (Exception e) {
                    Log.e(TAG, "Error in UI callback: " + e.getMessage(), e);
                }
            }
        });
        
        return result[0];
    }
}
