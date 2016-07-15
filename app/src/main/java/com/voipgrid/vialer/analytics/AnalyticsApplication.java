package com.voipgrid.vialer.analytics;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.util.JsonStorage;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app, such as
 * the {@link Tracker}.
 */
public class AnalyticsApplication extends Application {

    private Tracker mTracker;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.tracker);
        }

        JsonStorage storage = new JsonStorage(this);
        SystemUser systemuser = (SystemUser) storage.get(SystemUser.class);

        // Set client id as custom dimension on index 1.
        if (systemuser != null){
            String clientId = systemuser.getClient();
            if (clientId != null) {
                mTracker.set("&cd1", clientId);
            }
        }
        return mTracker;
    }
}
