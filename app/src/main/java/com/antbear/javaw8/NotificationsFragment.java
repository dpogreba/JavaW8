package com.antbear.javaw8;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class NotificationsFragment extends Fragment {

    // Theme related views
    private RadioGroup themeRadioGroup;
    private RadioButton themeSystem, themeLight, themeDark;
    
    // Location related views
    private SeekBar searchRadiusSeekBar;
    private TextView searchRadiusValue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        
        // Initialize views
        initThemeControls(view);
        initLocationControls(view);
        
        return view;
    }
    
    private void initThemeControls(View view) {
        // Get theme radio buttons
        themeRadioGroup = view.findViewById(R.id.theme_radio_group);
        themeSystem = view.findViewById(R.id.theme_system);
        themeLight = view.findViewById(R.id.theme_light);
        themeDark = view.findViewById(R.id.theme_dark);
        
        // Set the radio button corresponding to the current theme
        int currentTheme = ThemeUtils.getThemePreference(requireContext());
        switch (currentTheme) {
            case ThemeUtils.MODE_LIGHT:
                themeLight.setChecked(true);
                break;
            case ThemeUtils.MODE_DARK:
                themeDark.setChecked(true);
                break;
            case ThemeUtils.MODE_SYSTEM:
            default:
                themeSystem.setChecked(true);
                break;
        }
        
        // Set listener for theme changes
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int themeMode;
            
            if (checkedId == R.id.theme_light) {
                themeMode = ThemeUtils.MODE_LIGHT;
            } else if (checkedId == R.id.theme_dark) {
                themeMode = ThemeUtils.MODE_DARK;
            } else {
                themeMode = ThemeUtils.MODE_SYSTEM;
            }
            
            // Save and apply the new theme
            ThemeUtils.saveThemePreference(requireContext(), themeMode);
            ThemeUtils.applyThemeMode(themeMode);
        });
    }
    
    private void initLocationControls(View view) {
        // Initialize location controls
        searchRadiusSeekBar = view.findViewById(R.id.search_radius_seekbar);
        searchRadiusValue = view.findViewById(R.id.search_radius_value);
        
        // Update the text when the seek bar value changes
        searchRadiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update the displayed value (add 1 to avoid 0 km)
                updateRadiusText(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Could save the preference here if needed
            }
        });
        
        // Set initial text
        updateRadiusText(searchRadiusSeekBar.getProgress());
    }
    
    private void updateRadiusText(int progress) {
        // Display the radius value (add 1 to avoid 0 km)
        searchRadiusValue.setText((progress + 1) + " km");
    }
}
