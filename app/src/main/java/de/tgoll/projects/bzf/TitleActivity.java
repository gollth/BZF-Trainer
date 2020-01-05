package de.tgoll.projects.bzf;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.security.InvalidParameterException;

public class TitleActivity extends AppCompatActivity {


    SharedPreferences settings;
    private BottomNavigationView navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.bzf);
        }

        ChangeLog changelog = new ChangeLog(this);
        if (changelog.firstRun()) {
            changelog.getFullLogDialog().show();
        }

        // The initially shown fragment in the tab host
        navigation = findViewById(R.id.navigation);
        String tab = settings.getString("navigation", getString(R.string.statistics));
        showFragment(tab, true);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return showFragment(item.getTitle().toString());
            }
        });
    }

    private int getNavigationID(String id) {
        if (id.equals(getString(R.string.azf))) return R.id.nav_azf;
        if (id.equals(getString(R.string.bzf)) ) return R.id.nav_bzf;
        if (id.equals(getString(R.string.settings))) return R.id.nav_settings;
        if (id.equals(getString(R.string.simulator))) return R.id.nav_sim;
        if (id.equals(getString(R.string.statistics))) return R.id.nav_stats;
        throw new InvalidParameterException("Fragment ID " + id + " unknown");
    }
    boolean showFragment(String id) {
        return showFragment(id, false);
    }
    boolean showFragment(String id, boolean forceLoad) {
        String current = settings.getString("navigation", getString(R.string.statistics));
        if (current.equals(id) && !forceLoad) return false;

        settings.edit().putString("navigation", id).apply();

        int tab = getNavigationID(id);
        if (forceLoad) navigation.setSelectedItemId(tab);
        switch(tab) {
            case R.id.nav_azf:   return load(new CatalogueFragment(this, "azf"));
            case R.id.nav_bzf:   return load(new CatalogueFragment(this, "bzf"));
            case R.id.nav_sim:   return load(new SimulatorFragment(this));
            case R.id.nav_stats: return load(new StatisticsFragment());
            case R.id.nav_settings: return load(new SettingsFragment());
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
}
