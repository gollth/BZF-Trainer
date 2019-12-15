package de.tgoll.projects.bzf;

import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

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

    public void onBZFClick(View v) {
        startActivity(new Intent(this, CatalogueActivity.class).putExtra("key", "bzf"));
    }
    public void onAZFClick(View v) {
        startActivity(new Intent(this, CatalogueActivity.class).putExtra("key", "azf"));
    }
    public void onSimulatorClick(View v) {
        startActivity(new Intent(this, SimulatorActivity.class));
    }
    public void onSettingsClick(View v) {
        startActivity(new Intent(this, SettingsActivity.class));
    }
    public void onStatisticsClick(View v) {
        startActivity(new Intent(this, StatisticsActivity.class));
    }
    public void onFeedbackClick(View v) {
        Intent feedback = new Intent(
                Intent.ACTION_SENDTO,
                Uri.fromParts("mailto", "thoregoll@googlemail.com", null)
        ).putExtra(Intent.EXTRA_SUBJECT, "[BZF-Trainer] Feedback");
        startActivity(Intent.createChooser(feedback, "Mail"));
    }
}
