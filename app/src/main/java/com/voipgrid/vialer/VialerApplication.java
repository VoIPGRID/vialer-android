package com.voipgrid.vialer;

import com.github.anrwatchdog.ANRWatchDog;
import com.github.tamir7.contacts.Contacts;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.dagger.DaggerVialerComponent;
import com.voipgrid.vialer.dagger.VialerComponent;
import com.voipgrid.vialer.dagger.VialerModule;

/**
 * VialerApplication that extends the AnalyticsApplication
 * so the Analytics are all setup.
 */
public class VialerApplication extends AnalyticsApplication {

    private static VialerApplication sApplication;

    private ActivityLifecycleTracker mActivityLifecycle;

    private VialerComponent mComponent;

    public static VialerApplication get() {
        return sApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mComponent = DaggerVialerComponent
                .builder()
                .vialerModule(new VialerModule(this))
                .build();
        sApplication = this;
        mActivityLifecycle = new ActivityLifecycleTracker();
        registerActivityLifecycleCallbacks(mActivityLifecycle);
        //new ANRWatchDog().start();
        Contacts.initialize(this);
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

    /**
     * Return the main dagger component.
     *
     * @return
     */
    public VialerComponent component() {
        return mComponent;
    }
}
