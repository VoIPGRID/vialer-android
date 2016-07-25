package com.voipgrid.vialer.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.logging.RemoteLogger;

/**
 * SyncUtils provides functions to handling actions related to the contact sync.
 */
public class SyncUtils {
    private static final String TAG = SyncUtils.class.getName();
    /**
     * Check if their is a sync account present. If not create one.
     *
     * @param context The context used to get the AccountManager, Strings and ContentResolver.
     */
    private static Account checkSyncAccount(Context context) {
        AccountManager am = AccountManager.get(context);
        Account[] accounts;
        accounts = am.getAccountsByType(context.getString(R.string.account_type));
        Account account;
        if (accounts == null || accounts.length <= 0) {
            account = new Account(context.getString(R.string.contacts_app_name),
                    context.getString(R.string.account_type));
            am.addAccountExplicitly(account, "", null);
            ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
        } else {
            account = accounts[0];
        }
        return account;
    }

    /**
     * Function to initiate a contact sync. The only function that should be used to initiate
     * a contact sync.
     *
     * @param context
     */
    public static void requestContactSync(Context context) {
        new RemoteLogger(context).d(TAG + " requestContactSync");
        // Check contacts permission. Do nothing if we don't have it. Since it's a background
        // job we can't really ask the user for permission.
        if (!ContactsPermission.hasPermission(context)) {
            // TODO VIALA-349 Delete sync account.
            return;
        }

        // No need to request sync when a full sync is in progress.
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SyncConstants.FULL_SYNC_INPROGRESS, false)) {
            return;
        }

        Account account = checkSyncAccount(context);

        ContentResolver.requestSync(account, ContactsContract.AUTHORITY, new Bundle());
    }

    /**
     * Function to set the periodic sync interval (1 day) for a full contact sync.
     * @param context
     */
    public static void setPeriodicSync(Context context) {
        new RemoteLogger(context).d(TAG + " setPeriodicSync");
        // Check contacts permission. Do nothing if we don't have it. Since it's a background
        // job we can't really ask the user for permission.
        if (!ContactsPermission.hasPermission(context)) {
            // TODO VIALA-349 Delete sync account.
            return;
        }
        Account account = checkSyncAccount(context);

        // Full sync every day.
        ContentResolver.addPeriodicSync(
                account,
                ContactsContract.AUTHORITY,
                new Bundle(),
                SyncConstants.PERIODIC_SYNC_INTERVAL);
    }

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
        new RemoteLogger(context).d(TAG + " setFullSyncInProgress");
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(SyncConstants.FULL_SYNC_INPROGRESS, inProgress).apply();
    }

}
