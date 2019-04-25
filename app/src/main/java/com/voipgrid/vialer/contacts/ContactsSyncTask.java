package com.voipgrid.vialer.contacts;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.permissions.ContactsPermission;
import com.voipgrid.vialer.t9.T9DatabaseHelper;


/**
 * Class that handles the syncing of the contacts to the t9 database.
 */
public class ContactsSyncTask {
    private static final String TAG = ContactsSyncTask.class.getName();
    private static final boolean DEBUG = false;

    private Context mContext;
    private Logger mLogger;

    private static Progress progress = new Progress(0);

    /**
     * AsyncTask that adds Data entry in Contacts app with "Call with AppName" action.
     *
     * @param context context used for managing a ContentResolver which queries Contacts.
     */
    public ContactsSyncTask(Context context) {
        mContext = context;
        mLogger = new Logger(ContactsSyncTask.class);

        mLogger.d("onCreate");
    }

    /**
     * Make a query with a certain selection to a contentResolver.
     *
     * @param uri
     * @param selection
     */
    public Cursor query(Uri uri, String selection) {
        return mContext.getContentResolver()
                .query(uri,
                        null,
                        selection,
                        null,
                        null);  // gives you the list of contacts who has phone numbers
    }

    /**
     * execute a query to the Contacts database to get all contacts with a phone number.
     * @return
     */
    public Cursor queryAllContacts() {
        // gives you the list of contacts who has phone numbers
        return query(ContactsContract.Contacts.CONTENT_URI,
                ContactsContract.Data.HAS_PHONE_NUMBER + " = 1");
    }

    /**
     * Retrieve all the phone numbers of a certain contact.
     * @param contactId contact id of which we query its Phone CommonDataKind.
     * @return
     */
    public Cursor queryAllPhoneNumbers(String contactId) {
        // Gives the list of phone numbers for a given contact.
        return query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId);
    }

    /**
     * Runs the sync for all contacts.
     */
    public void fullSync() {
        mLogger.d("fullSync");

        // Check contacts permission. Do nothing if we don't have it. Since it's a background
        // job we can't really ask the user for permission.
        if (!ContactsPermission.hasPermission(mContext)) {
            mLogger.d("fullsync: no contact permission");
            // TODO VIALA-349 Delete sync account.
            return;
        }

        Cursor cursor = queryAllContacts();
        SyncUtils.setFullSyncInProgress(mContext, true);
        sync(cursor);
        SyncUtils.setFullSyncInProgress(mContext, false);
        SyncUtils.setRequiresFullContactSync(mContext, false);
    }

    /**
     * Create a SyncContact object from the given contact cursor.
     * @param contactCursor Cursor containing contacts.
     * @return Populated SyncContact object.
     */
    private SyncContact createSyncContactFromCursor(Cursor contactCursor) {
        long contactId =
                contactCursor.getLong(
                        contactCursor.getColumnIndex(
                                ContactsContract.Contacts._ID));
        String lookupKey =
                contactCursor.getString(
                        contactCursor.getColumnIndex(
                                ContactsContract.Contacts.LOOKUP_KEY));
        String displayName =
                contactCursor.getString(
                        contactCursor.getColumnIndex(
                                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
        String thumbnailUri =
                contactCursor.getString(
                        contactCursor.getColumnIndex(
                                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));

        return new SyncContact(contactId, lookupKey, displayName, thumbnailUri);
    }

    /**
     * Create a SyncContactNumber object from the give number cursor.
     * @param numberCursor Cursor containing phone numbers.
     * @return Populated SyncContact object or null.
     */
    private SyncContactNumber createSyncContactNumberFromCursor(Cursor numberCursor) {
        long dataId =
                numberCursor.getLong(
                        numberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));
        String number =
                numberCursor.getString(
                        numberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        int type =
                numberCursor.getInt(
                        numberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
        String label =
                numberCursor.getString(
                        numberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));

        // We do not want to sync numbers that are null.
        if (number == null) {
            return null;
        }

        // Strip whitespace.
        number = number.replace(" ", "");

        // We do not want to sync numbers that have no content.
        if (number.length() < 1){
            return null;
        }

        return new SyncContactNumber(dataId, number, type, label);
    }

    /**
     * Sync syncs the contacts in the given cursor for T9 and call with app button.
     * @param cursor The cursor of contact(s) to sync.
     */
    public void sync(Cursor cursor) {
        mLogger.d("sync");
        // Check contacts permission. Do nothing if we don't have it. Since it's a background
        // job we can't really ask the user for permission.
        if (!ContactsPermission.hasPermission(mContext)) {
            // TODO VIALA-349 Delete sync account.
            mLogger.d("sync: no contact permission");
            return;
        }

        progress = new Progress(cursor.getCount());
        T9DatabaseHelper t9Database = new T9DatabaseHelper(mContext);

        SyncContact syncContact;
        SyncContactNumber syncContactNumber;

        // Loop all contacts to sync.
        while (cursor.moveToNext()) {
            progress.setProcessed(cursor.getPosition() + 1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                long lastUpdated =
                        cursor.getLong(cursor.getColumnIndex(
                                ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP));
                // Skip the contact if it has not changed since the last sync AND a full sync
                // is not required.
                if (lastUpdated <= Long.parseLong(SyncUtils.getLastSync(mContext))
                        && !SyncUtils.requiresFullContactSync(mContext)) {
                    continue;
                }
            }

            syncContact = createSyncContactFromCursor(cursor);

            // Get all numbers for contact.
            Cursor numbers = queryAllPhoneNumbers(Long.toString(syncContact.getContactId()));

            if (numbers == null) {
                continue;
            }

            // Check if we have found phone numbers.
            if (numbers.getCount() <= 0) {
                // Close cursor before continue.
                numbers.close();
                continue;
            }

            // Loop all number belonging to a contact.
            while (numbers.moveToNext()) {
                syncContactNumber = createSyncContactNumberFromCursor(numbers);
                if (syncContactNumber != null) {
                    syncContact.addNumber(syncContactNumber);
                }
            }
            numbers.close();

            if (DEBUG) {
                Log.d(TAG, "Syncing contact: " +
                        syncContact.getContactId() +
                        " - " +
                        syncContact.getDisplayName());
            }

            // Sync the contact.
            t9Database.updateT9Contact(syncContact);
        }
        cursor.close();

        // Remove dead weight from t9 db.
        t9Database.afterSyncCleanup();
        SyncUtils.setLastSyncNow(mContext);
    }

    public static Progress getProgress() {
        return progress;
    }

    public static class Progress {
        private int processed, total;

        private Progress(int total) {
            this.total = total;
        }

        private void setProcessed(int processed) {
            this.processed = processed;
        }

        public int getProcessed() {
            return processed;
        }

        public int getTotal() {
            return total;
        }

        public boolean isComplete() {
            return total == processed;
        }
    }
}

