package com.vb.deepdiary;


import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference passwordSwitch = preferenceScreen.getPreference(1);
        passwordSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                preferenceScreen.getPreference(2).setEnabled(true);
            } else {
                preferenceScreen.getPreference(2).setEnabled(false);
            }
            return true;
        });
    }
}
