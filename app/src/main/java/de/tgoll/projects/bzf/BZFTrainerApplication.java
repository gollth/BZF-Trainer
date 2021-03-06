package de.tgoll.projects.bzf;

import android.app.Application;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class BZFTrainerApplication extends Application {

    private Thread.UncaughtExceptionHandler defaultHandler;
    private Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
            new Thread(() -> {
                Looper.prepare();
                Toast.makeText(getApplicationContext(),
                        R.string.crash_msg,
                        Toast.LENGTH_LONG).show();

                // As a last resort, try to remove the saved state from the application,
                // since it might contain outdated information
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit()
                        .remove("navigation")
                        // very sensitive data, dont delete
                        //.remove("azf-state")
                        //.remove("bzf-state")
                        //.remove("azf-history")
                        //.remove("bzf-history")
                        //.remove("sim-history")
                        .apply();

                Looper.loop();
            }).start();

            // Now shutdown VM gracefully
            defaultHandler.uncaughtException(thread, ex);

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

}
