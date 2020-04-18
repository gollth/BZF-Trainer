package de.tgoll.projects.bzf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.security.InvalidParameterException;

public class TitleActivity extends AppCompatActivity {

    public static void restart(Context context) {
        Intent intent = new Intent(context, TitleActivity.class);
        context.startActivity(intent);
        if (context instanceof Activity) ((Activity)context).finishAffinity();
    }

    public static @ColorInt int lookupColor(@NonNull Context context, @AttrRes int id) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(id, typedValue, true);
        return typedValue.data;
    }

    SharedPreferences settings;
    private BottomNavigationView navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            int background = lookupColor(this, R.attr.colorPrimarySurface);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.bzf);
            actionBar.setBackgroundDrawable(new ColorDrawable(background));
        }

        ChangeLog changelog = new ChangeLog(this);
        if (changelog.firstRun()) {
            changelog.getFullLogDialog().show();
        }

        // The initially shown fragment in the tab host
        navigation = findViewById(R.id.navigation);
        String tab = settings.getString("navigation", getString(R.string.statistics));
        showFragment(tab, true);
        navigation.setOnNavigationItemSelectedListener(item -> showFragment(item.getTitle().toString()));
    }

    int getActiveFragment() {
        return navigation.getSelectedItemId();
    }

    private int getNavigationID(String id) {
        if (id.equals(getString(R.string.azf))) return R.id.nav_azf;
        if (id.equals(getString(R.string.bzf)) ) return R.id.nav_bzf;
        if (id.equals(getString(R.string.settings))) return R.id.nav_settings;
        if (id.equals(getString(R.string.simulator))) return R.id.nav_sim;
        if (id.equals(getString(R.string.statistics))) return R.id.nav_stats;
        throw new InvalidParameterException("Fragment ID " + id + " unknown");
    }
    private void setNavigation(int id) {
        MenuItem item = navigation.getMenu().findItem(id);
        if (item != null) item.setChecked(true);
    }
    boolean showFragment(String id) {
        return showFragment(id, false);
    }
    boolean showFragment(String id, boolean forceLoad) {
        String current = settings.getString("navigation", getString(R.string.statistics));
        if (current.equals(id) && !forceLoad) return false;

        settings.edit().putString("navigation", id).apply();

        int tab = getNavigationID(id);
        if (forceLoad) setNavigation(tab);
        switch(tab) {
            case R.id.nav_azf:      return load(CatalogueFragment.newInstance("azf"));
            case R.id.nav_bzf:      return load(CatalogueFragment.newInstance("bzf"));
            case R.id.nav_sim:      return load(new SimulatorFragment());
            case R.id.nav_stats:    return load(new StatisticsFragment());
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        switch (getActiveFragment()) {
            case R.id.nav_azf:
            case R.id.nav_bzf:
            case R.id.nav_sim:
                getMenuInflater().inflate(R.menu.main, menu);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // let the fragment handle the reset case
        if (item.getItemId() == R.id.menu_restart) return false;

        return super.onOptionsItemSelected(item);
    }
}
