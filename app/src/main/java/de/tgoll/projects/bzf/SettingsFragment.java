package de.tgoll.projects.bzf;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences settings;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        final Context context = getContext();
        if (context == null) return;

        setPreferencesFromResource(R.xml.settings, rootKey);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        Preference button = findPreference(getString(R.string.settings_reset));
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                settings.edit()
                        .remove("azf-history")
                        .remove("bzf-history")
                        .remove("sim-history")
                        .remove("azf-state")
                        .remove("bzf-state")
                        .apply();
                Toast.makeText(context.getApplicationContext(), getString(R.string.settings_reset_toast), Toast.LENGTH_SHORT).show();
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

        Preference feedback = findPreference(getString(R.string.feedback));
        feedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(Intent.createChooser(
                        new Intent(
                            Intent.ACTION_SENDTO,
                            Uri.fromParts(
                                    "mailto",
                                    "thoregoll@googlemail.com",
                                    null
                            )
                    ).putExtra(
                            Intent.EXTRA_SUBJECT,
                            "[BZF-Trainer] Feedback"),
                            "Mail"
                        )
                );
                return false;
            }
        });
    }
}
