package com.voipgrid.vialer.contacts;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import androidx.annotation.Nullable;
import android.util.Log;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.VialerApplication;

/**
 * UpdateChangedContactsService listens for changed contacts and syncs them for t9 search.
 * NOTE: Never start this service with API level < 18!
 */
public class UpdateChangedContactsService extends Service {

    private static final String LOG_TAG = UpdateChangedContactsService.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG){
            Log.d(LOG_TAG, "Starting update changed contacts service");
        }

        // Create observer.
        ContactUpdateContentObserver observer = new ContactUpdateContentObserver(new Handler(), VialerApplication.get());

        // Register observer with service.
        getApplicationContext()
                .getContentResolver()
                .registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Run indefinitely.
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG){
            // This service should run indefinitely.
            Log.d(LOG_TAG, "Stopping update changed contacts service");
        }
    }


    private static class ContactUpdateContentObserver extends ContentObserver {
        private final String LOG_TAG = ContactUpdateContentObserver.class.getName();

        private static Thread syncThread;

        private SharedPreferences mPrefs;
        private Context mContext;

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        public ContactUpdateContentObserver(Handler handler, Context context) {
            super(handler);

            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            mContext = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if(syncThread != null && syncThread.isAlive()) return;

            syncThread = new Thread(this::syncUpdatedContacts);

            syncThread.start();
        }

        /**
         * Runs the tasks required to handle a contact being updated/added.
         *
         */
        private void syncUpdatedContacts() {
            // Updating during a full sync would trigger a loop.
            if (mPrefs.getBoolean(SyncConstants.FULL_SYNC_INPROGRESS, true)) {
                return;
            }

            // Get changed contacts.
            Cursor updatedContacts = getUpdatedContactsCursor();

            // No results to sync.
            if (updatedContacts == null || updatedContacts.getCount() <= 0){
                if (updatedContacts != null) {
                    updatedContacts.close();
                }
                return;
            }


            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Contact changed. Start syncing changed contacts.");
            }

            // Sync changed contacts.
            new ContactsSyncTask(mContext).sync(updatedContacts);

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Done syncing changed contacts.");
            }
        }

        /**
         * Get the selection string for contact query.
         * @return
         */
        private String getSelection() {
            // Contacts who have a phone number and are changed since last sync.
            return ContactsContract.Data.HAS_PHONE_NUMBER + " = 1 AND " +
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " > " +
                    SyncUtils.getLastSync(mContext);
        }

        /**
         * Get a cursor with the contacts that are updated.
         * @return
         */
        private Cursor getUpdatedContactsCursor() {
            Cursor cursor = mContext.getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null,
                    getSelection(),
                    null,
                    null,
                    null);

            return cursor;
        }
    }
}
