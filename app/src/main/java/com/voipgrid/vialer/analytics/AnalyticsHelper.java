package com.voipgrid.vialer.analytics;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Helper class to send events to Google Analytics
 */
public class AnalyticsHelper {

    private Tracker mTracker;

    public AnalyticsHelper(Tracker tracker) {
        mTracker = tracker;
    }

    /**
     * Send an Event to Google Analytics
     * @param dimension
     * @param category
     * @param action
     * @param label
     */
    public void send(String dimension, String category, String action, String label ) {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, dimension)
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }
}
