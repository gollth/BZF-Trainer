package de.tgoll.projects.bzf;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class ChangeLog {

    private final Context context;
    private String lastVersion, thisVersion;

    // this is the key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "PREFS_VERSION_KEY";

    private static final String NO_VERSION = "";

    ChangeLog(Context context) {
        this(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    private ChangeLog(Context context, SharedPreferences sp) {
        this.context = context;

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, NO_VERSION);
        Log.d(TAG, "lastVersion: " + lastVersion);
        try {
            this.thisVersion = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    0).versionName;
        } catch (NameNotFoundException e) {
            this.thisVersion = NO_VERSION;
            Log.e(TAG, "could not get version name from manifest!");
            e.printStackTrace();
        }
        Log.d(TAG, "appVersion: " + this.thisVersion);
    }

    /**
     * @return <code>true</code> if this version of your app is started the first time
     */
    boolean firstRun() {
        return !this.lastVersion.equals(this.thisVersion);
    }

    /**
     * @return an AlertDialog with a full change log displayed
     */
    Dialog getFullLogDialog() {
        return this.getDialog();
    }

    private Dialog getDialog() {
        WebView wv = new WebView(this.context);

        wv.setBackgroundColor(Util.lookupColor(context, R.attr.backgroundColor));
        wv.loadDataWithBaseURL(null, this.getLog(), "text/html", "UTF-8", null);

        return new MaterialAlertDialogBuilder(context)
            .setTitle(context.getResources().getString(R.string.changelog_full_title))
            .setView(wv)
            .setCancelable(false)
            .setPositiveButton(context.getResources().getString(R.string.changelog_ok_button), (dialog, which) -> updateVersionInPreferences())
            .create();
    }

    private void updateVersionInPreferences() {
        // save new version number to preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VERSION_KEY, thisVersion);
        editor.apply();
    }

    /** modes for HTML-Lists (bullet, numbered) */
    private enum Listmode {
        NONE, ORDERED, UNORDERED,
    }

    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;

    private String getLog() {
        // read changelog.txt file
        sb = new StringBuffer();
        try {
            InputStream ins = context.getResources().openRawResource(R.raw.changelog);
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));

            String line;

            String textColor = String.format("#%06X", (0xFFFFFF & Util.lookupColor(context, R.attr.colorOnBackground)));
            String h1Color = String.format("#%06X", (0xFFFFFF & Util.lookupColor(context, R.attr.colorPrimary)));
            String h2Color = String.format("#%06X", (0xFFFFFF & Util.lookupColor(context, R.attr.colorSecondaryVariant)));

            while ((line = br.readLine()) != null) {
                line = line.trim();
                char marker = line.length() > 0 ? line.charAt(0) : 0;
                if (marker == '$') {
                    // begin of a version section
                    this.closeList();
                } else {
                    switch (marker) {
                    case '%':
                        // line contains version title
                        this.closeList();
                        sb.append("<div class='title' style='color: ").append(h1Color).append(";'>").append(line.substring(1).trim()).append("</div>\n");
                        break;
                    case '_':
                        // line contains version title
                        this.closeList();
                        sb.append("<div class='subtitle' style='color: ").append(h2Color).append(";'>").append(line.substring(1).trim()).append("</div>\n");
                        break;
                    case '!':
                        // line contains free text
                        this.closeList();
                        sb.append("<div class='freetext' style='color: ").append(textColor).append(";'>").append(line.substring(1).trim()).append("</div>\n");
                        break;
                    case '#':
                        // line contains numbered list item
                        this.openList(Listmode.ORDERED);
                        sb.append("<li style='color: ").append(textColor).append(";'>").append(line.substring(1).trim()).append("</li>\n");
                        break;
                    case '*':
                        // line contains bullet list item
                        this.openList(Listmode.UNORDERED);
                        sb.append("<li style='color: ").append(textColor).append(";'>").append(line.substring(1).trim()).append("</li>\n");
                        break;
                    default:
                        // no special character: just use line as is
                        this.closeList();
                        sb.append(line).append("\n");
                    }
                }
            }
            this.closeList();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private void openList(Listmode listMode) {
        if (this.listMode != listMode) {
            closeList();
            if (listMode == Listmode.ORDERED) {
                sb.append("<div class='list'><ol>\n");
            } else if (listMode == Listmode.UNORDERED) {
                sb.append("<div class='list'><ul>\n");
            }
            this.listMode = listMode;
        }
    }

    private void closeList() {
        if (this.listMode == Listmode.ORDERED) {
            sb.append("</ol></div>\n");
        } else if (this.listMode == Listmode.UNORDERED) {
            sb.append("</ul></div>\n");
        }
        this.listMode = Listmode.NONE;
    }

    private static final String TAG = "ChangeLog";
}
