package de.tgoll.projects.bzf;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

    private SharedPreferences settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        Preference button = findPreference(getString(R.string.settings_reset));
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                settings.edit()
                        .remove("azf-history")
                        .remove("bzf-history")
                        .remove("azf-state")
                        .remove("bzf-state")
                        .apply();
                Toast.makeText(getApplicationContext(), getString(R.string.settings_reset_toast), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        Preference openSpeechSettingsButton = findPreference(getString(R.string.settings_open_speech_api));
        openSpeechSettingsButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS));
                return false;
            }
        });

        Preference openTTSSettingsButton = findPreference(getString(R.string.settings_open_tts_api));
        openTTSSettingsButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent().setAction("com.android.settings.TTS_SETTINGS").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return false;
            }
        });
    }
}
