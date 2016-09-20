package com.viovie.webkeyboard.activity;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.viovie.webkeyboard.R;
import com.viovie.webkeyboard.service.RemoteKeyboardService;

public class SettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Load the legacy preferences headers
            addPreferencesFromResource(R.xml.settings_preferences);
        } else {
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new SettingPreferenceFragment())
                    .commit();
        }

        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(this);

        //FIXME: TO WEB
//		if (sharedPref.getString(TelnetEditorShell.PREF_PASSWORD, "").equals("")) {
//			findPreference(TelnetEditorShell.PREF_PASSWORD).setSummary(
//					R.string.msg_password_not_set);
//		}
//		else {
//			findPreference(TelnetEditorShell.PREF_PASSWORD).setSummary(
//					R.string.msg_password_set);
//		}

    }

    public static class SettingPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_preferences);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        Preference preference = findPreference(key);
        if (preference instanceof ListPreference) {
            preference.setSummary(((ListPreference) preference).getEntry());
        }
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(this);
        //FIXME: TO WEB
//		if (sharedPref.getString(TelnetEditorShell.PREF_PASSWORD, "").equals("")) {
//			findPreference(TelnetEditorShell.PREF_PASSWORD).setSummary(
//					R.string.msg_password_not_set);
//		}
//		else {
//			findPreference(TelnetEditorShell.PREF_PASSWORD).setSummary(
//					R.string.msg_password_set);
//		}

        if (RemoteKeyboardService.self != null) {
            RemoteKeyboardService.self.updateFullscreenMode();
        }
    }

}