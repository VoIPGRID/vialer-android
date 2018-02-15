package com.voipgrid.vialer;

import com.voipgrid.vialer.analytics.AnalyticsApplication;

/**
 * VialerApplication that extends the AnalyticsApplication
 * so the Analytics are all setup.
 */
public class VialerApplication extends AnalyticsApplication {

    private static VialerApplication sApplication;

    public static VialerApplication get() {
        return sApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }

    public static String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }
}
