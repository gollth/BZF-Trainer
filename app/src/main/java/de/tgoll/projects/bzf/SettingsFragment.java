package de.tgoll.projects.bzf;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences settings;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.settings, rootKey);

        settings = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Preference button = findPreference(getString(R.string.settings_reset));
        assert button != null;
        button.setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.settings_reset_alert)
                .setCancelable(true)
                .setNegativeButton(R.string.negative, (d, btn) -> d.cancel())
                .setPositiveButton(R.string.positive, (d, btn) -> {
                    settings.edit()
                            .remove("azf-history")
                            .remove("bzf-history")
                            .remove("sim-history")
                            .remove("azf-state")
                            .remove("bzf-state")
                            .apply();
                    Snackbar.make(requireView(), getString(R.string.settings_reset_toast), Snackbar.LENGTH_SHORT).show();
                }
            ).show();
            return false;
        });

        Preference openSpeechSettingsButton = findPreference(getString(R.string.settings_open_speech_api));
        assert openSpeechSettingsButton != null;
        openSpeechSettingsButton.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS));
            return false;
        });

        Preference openTTSSettingsButton = findPreference(getString(R.string.settings_open_tts_api));
        assert openTTSSettingsButton != null;
        openTTSSettingsButton.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent().setAction("com.android.settings.TTS_SETTINGS").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return false;
        });

        EditTextPreference callsign = findPreference(getString(R.string.settings_sim_callsign));
        assert callsign != null;
        callsign.setOnBindEditTextListener(txt -> txt.setInputType(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        ));

        EditTextPreference aircraft = findPreference(getString(R.string.settings_sim_aircraft));
        assert aircraft != null;
        aircraft.setOnBindEditTextListener(txt -> txt.setInputType(InputType.TYPE_CLASS_TEXT));

        Preference feedback = findPreference(getString(R.string.feedback));
        assert feedback != null;
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

        Activity activity = requireActivity();
        Shop shop = new Shop(activity);

        SwitchPreference darkMode = findPreference(getString(R.string.settings_theme));
        assert darkMode != null;
        darkMode.setOnPreferenceChangeListener((preference, dark) -> {
            if (!shop.isPurchased(Shop.SKU_DARK_MODE)) {
                // If the Dark Mode was not yet purchased, show the user the dialog
                // but don't switch dark mode on
                shop.show(false);
                return false;
            }

            TitleActivity.restart(requireActivity());
            return true;
        });
        if (!shop.isPurchased(Shop.SKU_DARK_MODE)) {
            darkMode.setChecked(false);
        }

        Preference questionStats = findPreference(getString(R.string.settings_question_stats));
        assert questionStats != null;
        questionStats.setEnabled(shop.isPurchased(Shop.SKU_QUESTION_FILTER));

    }
}
