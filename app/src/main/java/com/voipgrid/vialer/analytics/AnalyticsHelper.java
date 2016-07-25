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
     * Function to create a basic EventBuidler.
     * @param category Category for event.
     * @param action Action for event.
     * @param label Label for action.
     * @return
     */
    private HitBuilders.EventBuilder createEventBuilder(String category, String action, String label) {
        return new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label);
    }

    /**
     * Send a event to GA.
     * @param category Category for event.
     * @param action Action for event.
     * @param label Label for action.
     * @param value Value for action/event.
     */
    public void sendEvent(String category, String action, String label, long value) {
        mTracker.send(createEventBuilder(category, action, label).setValue(value).build());
    }

    /**
     * Send a event to GA.
     * @param category Category for event.
     * @param action Action for event.
     * @param label Label for action.
     */
    public void sendEvent(String category, String action, String label) {
        mTracker.send(createEventBuilder(category, action, label).build());
    }

    /**
     * Send exception to GA.
     * @param description Description of the exception.
     */
    public void sendException(String description) {
        sendException(description, false);
    }

    /**
     * Send exception to GA.
     * @param description Description of the exception.
     * @param fatal Whether is is a fatal exception or not.
     */
    public void sendException(String description, boolean fatal) {
        mTracker.send(
                new HitBuilders.ExceptionBuilder()
                        .setDescription(description)
                        .setFatal(fatal)
                        .build()
        );
    }

    /**
     * Send a custom screen to track with GA
     * @param screenName String the screen to track
     */
    public void sendScreenViewTrack(String screenName) {
        // Set the screen to track
        mTracker.setScreenName(screenName);

        // Send a screen view.
        mTracker.send(
                new HitBuilders.ScreenViewBuilder().build()
        );
    }

    /**
     * Send a timing to GA.
     * @param category Category of the timing.
     * @param name Name of the timing.
     * @param interval The value of the timing.
     */
    public void sendTiming(String category, String name, long interval) {
        mTracker.send(
                new HitBuilders.TimingBuilder()
                        .setCategory(category)
                        .setVariable(name)
                        .setValue(interval)
                        .build()
        );
    }
}
