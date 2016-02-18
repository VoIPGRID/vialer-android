package com.voipgrid.vialer.contacts;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;

import com.voipgrid.vialer.t9.T9DatabaseHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Class acting as a layer between the syncadapter and the contactsmanager.
 */
public class ContactsSyncTask {

    private Context mContext;

    /**
     * AsyncTask that adds Data entry in Contacts app with "Call with AppName" action.
     *
     * @param context context used for managing a ContentResolver which queries Contacts.
     */
    public ContactsSyncTask(Context context) {
        mContext = context;
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
     * Get a specific column data field given a column name from a cursor.
     *
     * @param columnName
     * @param cursor
     * @return
     */
    public String getColumnFromCursor(String columnName, Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    /**
     * Runs the sync for all contacts.
     */
    public void fullSync() {
        // Check contacts permission. Do nothing if we don't have it. Since it's a background
        // job we can't really ask the user for permission.
        if (!ContactsPermission.hasPermission(mContext)) {
            // TODO VIALA-349 Delete sync account.
            return;
        }
        // Gives you the list of contacts who have phone numbers.
        Cursor cursor = queryAllContacts();
        SyncUtils.setFullSyncInProgress(mContext, true);
        sync(cursor);
        SyncUtils.setFullSyncInProgress(mContext, false);
        SyncUtils.setRequiresFullContactSync(mContext, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startService(new Intent(mContext, UpdateChangedContactsService.class));
        }
    }

    /**
     * Sync syncs the contacts in the given cursor for T9 and call with app button.
     * @param cursor The cursor of contacts to sync.
     */
    public void sync(Cursor cursor) {
        // Check contacts permission. Do nothing if we don't have it. Since it's a background
        // job we can't really ask the user for permission.
        if (!ContactsPermission.hasPermission(mContext)) {
            // TODO VIALA-349 Delete sync account.
            return;
        }

        T9DatabaseHelper t9Database = new T9DatabaseHelper(mContext);

        while (cursor.moveToNext()) {
            long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            String name = getColumnFromCursor(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    cursor);

            Cursor phones = queryAllPhoneNumbers(Long.toString(contactId));

            if (phones.getCount() <= 0) {
                // Close cursor before continue.
                phones.close();
                continue;
            }

            List<String> normalizedPhoneNumbers = new ArrayList<>();
            String normalizedPhoneNumber;
            List<String> phoneNumbers = new ArrayList<>();
            String phoneNumber;

            while (phones.moveToNext()) {
                normalizedPhoneNumber = getColumnFromCursor(
                        ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                        phones
                );

                // We do not want to synchronize null numbers.
                if (normalizedPhoneNumber == null){
                    continue;
                }

                // Avoid duplicate phone numbers.
                if (!normalizedPhoneNumbers.contains(normalizedPhoneNumber)){
                    normalizedPhoneNumbers.add(normalizedPhoneNumber);
                }

                phoneNumber = getColumnFromCursor(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        phones
                );

                // We do not want to synchronize null numbers.
                if (phoneNumber == null || phoneNumber.length() < 1) {
                    continue;
                }

                // Avoid duplicate phone numbers.
                if (!phoneNumbers.contains(phoneNumber)){
                    phoneNumbers.add(phoneNumber.replace(" ", ""));
                }

            }
            phones.close();

            // Found no normalized phone numbers so don't sync the contact.
            if (normalizedPhoneNumbers.size() <= 0){
                continue;
            }
            SyncContact syncContact = new SyncContact(
                    contactId,
                    name,
                    normalizedPhoneNumbers,
                    phoneNumbers
            );

            ContactsManager.syncContact(mContext, syncContact, t9Database);
        }
        cursor.close();

        // Remove dead weight from t9 db.
        t9Database.afterSyncCleanup();
        SyncUtils.setLastSyncNow(mContext);
    }
}
