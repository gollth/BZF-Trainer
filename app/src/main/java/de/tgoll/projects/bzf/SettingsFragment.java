package de.tgoll.projects.bzf;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences settings;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.settings, rootKey);

        settings = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Preference button = findPreference(getString(R.string.settings_reset));
        button.setOnPreferenceClickListener(preference -> {
            settings.edit()
                    .remove("azf-history")
                    .remove("bzf-history")
                    .remove("sim-history")
                    .remove("azf-state")
                    .remove("bzf-state")
                    .apply();

            Activity context = getActivity();
            if (context == null) return false;
            View container = context.findViewById(R.id.fragment);

            Snackbar.make(container, getString(R.string.settings_reset_toast), Snackbar.LENGTH_SHORT).show();
            return false;
        });

        Preference openSpeechSettingsButton = findPreference(getString(R.string.settings_open_speech_api));
        openSpeechSettingsButton.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS));
            return false;
        });

        Preference openTTSSettingsButton = findPreference(getString(R.string.settings_open_tts_api));
        openTTSSettingsButton.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent().setAction("com.android.settings.TTS_SETTINGS").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return false;
        });

        Preference feedback = findPreference(getString(R.string.feedback));
        feedback.setOnPreferenceClickListener(preference -> {
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
        });

        Preference darkMode = findPreference(getString(R.string.settings_theme));
        darkMode.setOnPreferenceChangeListener((preference, dark) -> {
            TitleActivity.restart(requireContext());
            return true;
        });
    }
}
