package com.antbear.javaw8.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Utility class for thread-related operations
 */
public class ThreadUtils {
    private static final String TAG = "ThreadUtils";
    
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    
    /**
     * Checks if the current thread is the main (UI) thread
     * @return true if called from the main thread, false otherwise
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    
    /**
     * Executes the given runnable on the main thread
     * @param runnable The runnable to execute
     */
    public static void runOnMainThread(Runnable runnable) {
        if (runnable == null) return;
        
        if (isMainThread()) {
            // Already on main thread, execute directly
            runnable.run();
        } else {
            // Post to main thread
            MAIN_HANDLER.post(runnable);
        }
    }
    
    /**
     * Executes the given runnable on the main thread with a delay
     * @param runnable The runnable to execute
     * @param delayMillis The delay in milliseconds
     */
    public static void runOnMainThreadDelayed(Runnable runnable, long delayMillis) {
        if (runnable == null) return;
        MAIN_HANDLER.postDelayed(runnable, delayMillis);
    }
    
    /**
     * Removes any pending posts of the given runnable from the message queue
     * @param runnable The runnable to remove
     */
    public static void removeCallbacks(Runnable runnable) {
        if (runnable == null) return;
        MAIN_HANDLER.removeCallbacks(runnable);
    }
    
    /**
     * Removes all callbacks and messages from the main handler
     */
    public static void removeAllCallbacks() {
        MAIN_HANDLER.removeCallbacksAndMessages(null);
    }
    
    /**
     * Asserts that the current thread is the main thread
     * @throws IllegalStateException if not called from the main thread
     */
    public static void assertMainThread() {
        if (!isMainThread()) {
            String errorMsg = "This operation must be performed on the main thread!";
            Log.e(TAG, errorMsg, new IllegalStateException(errorMsg));
            throw new IllegalStateException(errorMsg);
        }
    }
    
    /**
     * Asserts that the current thread is NOT the main thread
     * @throws IllegalStateException if called from the main thread
     */
    public static void assertBackgroundThread() {
        if (isMainThread()) {
            String errorMsg = "This operation must be performed on a background thread!";
            Log.e(TAG, errorMsg, new IllegalStateException(errorMsg));
            throw new IllegalStateException(errorMsg);
        }
    }
}
