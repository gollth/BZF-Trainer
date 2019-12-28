package de.tgoll.projects.bzf;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.security.InvalidParameterException;

public class TitleActivity extends AppCompatActivity {


    SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);

        settings = PreferenceManager.getDefaultSharedPreferences(this);


        // The initially shown fragment in the tab host
        showFragment(settings.getString("navigation", getString(R.string.statistics)));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.bzf);
        }

        ChangeLog changelog = new ChangeLog(this);
        if (changelog.firstRun()) {
            changelog.getFullLogDialog().show();
        }

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return showFragment(item.getItemId());
            }
        });
    }

    private boolean showFragment(String id) {
        if (id.equals(getString(R.string.azf))) return showFragment(R.id.nav_azf);
        if (id.equals(getString(R.string.bzf)) )return showFragment(R.id.nav_bzf);
        if (id.equals(getString(R.string.settings))) return showFragment(R.id.nav_settings);
        if (id.equals(getString(R.string.simulator))) return showFragment(R.id.nav_sim);
        if (id.equals(getString(R.string.statistics))) return showFragment(R.id.nav_stats);
        throw new InvalidParameterException("Fragement ID " + id + " unknown");
    }
    private boolean showFragment(int id) {
        int current = settings.getInt("navigation", -1);
        if (current == id) return false;

        settings.edit().putInt("navigation", id).apply();

        switch(id) {
            case R.id.nav_azf:   return load(new CatalogueFragment("azf"));
            case R.id.nav_bzf:   return load(new CatalogueFragment("bzf"));
            case R.id.nav_stats: return load(new StatisticsFragment());
            default: return false;
        }
    }

    private boolean load(Fragment fragment) {
        if (fragment == null) return false;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, fragment)
                .commit();
        return true;
    }


    public void onSimulatorClick(View v) {
        startActivity(new Intent(this, SimulatorActivity.class));
    }
    public void onSettingsClick(View v) {
        startActivity(new Intent(this, SettingsActivity.class));
    }
    public void onFeedbackClick(View v) {
        Intent feedback = new Intent(
                Intent.ACTION_SENDTO,
                Uri.fromParts("mailto", "thoregoll@googlemail.com", null)
        ).putExtra(Intent.EXTRA_SUBJECT, "[BZF-Trainer] Feedback");
        startActivity(Intent.createChooser(feedback, "Mail"));
    }
}
