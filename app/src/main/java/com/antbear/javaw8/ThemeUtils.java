package com.antbear.javaw8;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class for managing theme preferences and application
 */
public class ThemeUtils {
    private static final String THEME_PREF_KEY = "app_theme";
    
    // Theme mode constants
    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;
    
    /**
     * Apply the saved theme mode or default to system mode if not set
     * @param context Application context
     */
    public static void applyTheme(Context context) {
        int themeMode = getThemePreference(context);
        applyThemeMode(themeMode);
    }
    
    /**
     * Apply a specific theme mode
     * @param themeMode The theme mode constant to apply
     */
    public static void applyThemeMode(int themeMode) {
        switch (themeMode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
        }
    }
    
    /**
     * Save the user's theme preference
     * @param context Application context
     * @param themeMode The theme mode to save
     */
    public static void saveThemePreference(Context context, int themeMode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(THEME_PREF_KEY, themeMode).apply();
    }
    
    /**
     * Get the user's saved theme preference
     * @param context Application context
     * @return The saved theme mode or MODE_SYSTEM if not set
     */
    public static int getThemePreference(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(THEME_PREF_KEY, MODE_SYSTEM);
    }
}
