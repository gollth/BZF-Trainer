package de.tgoll.projects.bzf;

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
                settings.edit().remove("history").remove("SavedState").apply();
                Toast.makeText(getApplicationContext(), getString(R.string.settings_reset_toast), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }
}
