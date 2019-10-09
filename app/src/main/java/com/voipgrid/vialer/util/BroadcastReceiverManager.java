package com.voipgrid.vialer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * This is a single class that provides convenient methods to register/unregister receivers.
 *
 */
public class BroadcastReceiverManager {

    private final LocalBroadcastManager mLocalBroadcastManager;
    private final Context mContext;

    public BroadcastReceiverManager(LocalBroadcastManager localBroadcastManager, Context context) {
        mLocalBroadcastManager = localBroadcastManager;
        mContext = context;
    }

    /**
     * Create the BroadcastManager from a context.
     *
     * @param context
     * @return A new instance of BroadcastManager
     */
    public static BroadcastReceiverManager fromContext(Context context) {
        return new BroadcastReceiverManager(LocalBroadcastManager.getInstance(context), context);
    }

    /**
     * Register a receiver to listen to the provided events in the local broadcast manager.
     *
     * @param broadcastReceiver The receiver that will be listening for the events..
     * @param events The events to listen for.
     */
    public BroadcastReceiver registerReceiverViaLocalBroadcastManager(BroadcastReceiver broadcastReceiver, String... events) {
        mLocalBroadcastManager.registerReceiver(broadcastReceiver, eventsToIntentFilter(events, null));

        return broadcastReceiver;
    }

    /**
     * Register a receiver to listen to the provided events in the global broadcast manager.
     *
     * @param broadcastReceiver The receiver that will be listening for the events..
     * @param events The events to listen for.
     */
    public void registerReceiverViaGlobalBroadcastManager(BroadcastReceiver broadcastReceiver, String... events) {
        mContext.registerReceiver(broadcastReceiver, eventsToIntentFilter(events, null));
    }

    public void registerReceiverViaGlobalBroadcastManager(BroadcastReceiver broadcastReceiver, int priority, String... events) {
        mContext.registerReceiver(broadcastReceiver, eventsToIntentFilter(events, priority));
    }

    /**
     * Convert an array of events into an intent filter with each event added to it.
     *
     * @param events The array of events
     * @return The constructed intent filter
     */
    private IntentFilter eventsToIntentFilter(String[] events, Integer priority) {
        IntentFilter filter = new IntentFilter();

        if (priority != null) {
            filter.setPriority(priority);
        }

        for (String event : events) {
            filter.addAction(event);
        }

        return filter;
    }

    /**
     * Attempts to unregister from both the local and global broadcast managers.
     *
     * @param receiver The receiver to unregister.
     */
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (receiver == null) {
            return;
        }

        try {
            mContext.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {

        }

        try {
            mLocalBroadcastManager.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {

        }
    }

    /**
     * Unregister a group of receivers from the local broadcast manager.
     *
     * @param receivers An array of receivers to be unregistered
     */
    public void unregisterReceiver(BroadcastReceiver... receivers) {
        for (BroadcastReceiver broadcastReceiver : receivers) {
            unregisterReceiver(broadcastReceiver);
        }
    }
}
