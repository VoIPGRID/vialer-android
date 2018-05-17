package com.voipgrid.vialer;

import com.github.anrwatchdog.ANRWatchDog;
import com.voipgrid.vialer.analytics.AnalyticsApplication;

/**
 * VialerApplication that extends the AnalyticsApplication
 * so the Analytics are all setup.
 */
public class VialerApplication extends AnalyticsApplication {

    private static VialerApplication sApplication;

    private ActivityLifecycleTracker mActivityLifecycle;

    public static VialerApplication get() {
        return sApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        mActivityLifecycle = new ActivityLifecycleTracker();
        registerActivityLifecycleCallbacks(mActivityLifecycle);
        new ANRWatchDog().start();
    }

    public static String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Checks whether there is an activity in the foreground currently.
     *
     * @return TRUE if an activity is being displayed to the user.
     */
    public boolean isApplicationVisible() {
        return mActivityLifecycle.isApplicationVisible();
    }
}
