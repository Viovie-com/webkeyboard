package com.viovie.webkeyboard.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.viovie.webkeyboard.R;

public class SettingsActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    public static final String PREF_PORT = "pref_port";
    public static final String PREF_HTTPS = "pref_https";
    public static final String PREF_PASSWORD = "pref_password";

    public static final int DEFAULT_PORT = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_settings);

        EditTextPreference portPref = (EditTextPreference) findPreference(PREF_PORT);
        if (portPref != null) {
            portPref.setOnPreferenceChangeListener(this);
        }

        updateSummaries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateSummaries();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummaries();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (PREF_PORT.equals(preference.getKey())) {
            String val = String.valueOf(newValue).trim();
            try {
                int port = Integer.parseInt(val);
                if (port < 1024 || port > 65535) {
                    Toast.makeText(this, R.string.pref_invalid_port, Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.pref_invalid_port, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void updateSummaries() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        EditTextPreference portPref = (EditTextPreference) findPreference(PREF_PORT);
        if (portPref != null) {
            String port = prefs.getString(PREF_PORT, String.valueOf(DEFAULT_PORT));
            portPref.setSummary(port);
        }

        EditTextPreference passPref = (EditTextPreference) findPreference(PREF_PASSWORD);
        if (passPref != null) {
            String pass = prefs.getString(PREF_PASSWORD, "");
            if (pass == null || pass.trim().isEmpty()) {
                passPref.setSummary(R.string.pref_password_not_set);
            } else {
                passPref.setSummary(R.string.pref_password_set);
            }
        }
    }
}
