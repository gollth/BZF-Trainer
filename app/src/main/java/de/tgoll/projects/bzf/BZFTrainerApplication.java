package de.tgoll.projects.bzf;

import android.app.Application;
import android.os.Looper;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;

public class BZFTrainerApplication extends Application {

    private Thread.UncaughtExceptionHandler defaultHandler;
    private Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(),
                                    R.string.crash_msg,
                            Toast.LENGTH_LONG).show();

                    Looper.loop();
                }
            }).start();

            // Now shutdown VM gracefully
            defaultHandler.uncaughtException(thread, ex);

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        CrashlyticsCore crashlytics = new CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build();
        Fabric.with(this, crashlytics);

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

}
