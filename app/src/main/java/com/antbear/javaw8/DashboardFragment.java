package com.antbear.javaw8;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.antbear.javaw8.utils.UnitPreferences;

/**
 * Dashboard fragment that shows application settings
 */
public class DashboardFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "DashboardFragment";
    
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        
        // Set the initial state of the unit preference switch based on current preferences
        SwitchPreference unitSwitch = findPreference("use_imperial_units");
        if (unitSwitch != null) {
            unitSwitch.setChecked(UnitPreferences.useImperialUnits(requireContext()));
        }
        
        // Set the version number
        Preference versionPreference = findPreference("app_version");
        if (versionPreference != null) {
            try {
                String versionName = requireContext().getPackageManager()
                        .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                versionPreference.setSummary(versionName);
            } catch (Exception e) {
                versionPreference.setSummary("1.0.0");
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Register preference change listener
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Unregister preference change listener
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("use_imperial_units")) {
            boolean useImperial = sharedPreferences.getBoolean(key, true);
            
            // Update our UnitPreferences utility
            UnitPreferences.setUseImperialUnits(requireContext(), useImperial);
            
            // Show toast to user
            String message = useImperial ? 
                getString(R.string.unit_system_changed_imperial) : 
                getString(R.string.unit_system_changed_metric);
            
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
