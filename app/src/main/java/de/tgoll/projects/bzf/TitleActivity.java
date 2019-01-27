package de.tgoll.projects.bzf;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class TitleActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.bzf);
        }

        ChangeLog changelog = new ChangeLog(this);
        if (changelog.firstRun()) {
            changelog.getFullLogDialog().show();

            // Remove the catalog progress on a new version, since the questions/answers might have changed
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().remove("SavedState").apply();


        }
    }

    public void onButtonClick (View v) {


        switch(v.getId()) {
            case R.id.title_btn_main:
                startActivity(new Intent(this, CatalogueActivity.class));
                break;
            case R.id.title_btn_sim:
                startActivity(new Intent(this, SimulatorActivity.class));
                break;
            case R.id.title_btn_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.title_btn_feedback:
                Intent feedback = new Intent(
                        Intent.ACTION_SENDTO,
                        Uri.fromParts("mailto", "thoregoll@googlemail.com", null)
                ).putExtra(Intent.EXTRA_SUBJECT, "[BZF-Trainer] Feedback");

                startActivity(Intent.createChooser(feedback, "Mail"));
                break;
        }

    }
}
