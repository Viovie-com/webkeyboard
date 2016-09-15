package com.viovie.webkeyboard;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Window;

public class SettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_settings);

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
        RemoteKeyboardService.self.updateFullscreenMode();
    }

}