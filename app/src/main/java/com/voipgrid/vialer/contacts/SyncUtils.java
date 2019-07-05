package com.voipgrid.vialer.contacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.voipgrid.vialer.logging.Logger;

/**
 * SyncUtils provides functions to handling actions related to the contact sync.
 */
public class SyncUtils {
    private static final String TAG = SyncUtils.class.getName();

    /**
     * Function to check if a full contact sync is required.
     * @param context
     * @return
     */
    public static boolean requiresFullContactSync(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getBoolean(SyncConstants.FULL_SYNC_TOGGLE, true);
    }

    /**
     * Set if a full contact sync is required.
     * @param context
     * @param fullContactSync
     */
    public static void setRequiresFullContactSync(Context context, boolean fullContactSync) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(
                SyncConstants.FULL_SYNC_TOGGLE, fullContactSync).apply();
    }

    /**
     * Returns time of last contact sync.
     * @param context
     * @return
     */
    public static String getLastSync(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getString(SyncConstants.LAST_SYNC, "0");
    }

    /**
     * Set time of last contact sync to now.
     * @param context
     */
    public static void setLastSyncNow(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        prefs.edit().putString(SyncConstants.LAST_SYNC, Long.toString(System.currentTimeMillis())).apply();
    }

    /**
     * Set the value of the fullSyncInProgress setting.
     * @param inProgress
     */
    public static void setFullSyncInProgress(Context context, boolean inProgress) {
        new Logger(SyncUtils.class).d(TAG + " setFullSyncInProgress");
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(SyncConstants.FULL_SYNC_INPROGRESS, inProgress).apply();
    }

}
