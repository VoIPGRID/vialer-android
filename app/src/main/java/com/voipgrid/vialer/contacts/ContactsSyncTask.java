package com.voipgrid.vialer.contacts;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;

import com.voipgrid.vialer.t9.T9DatabaseHelper;

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

        // Strip whitespace.
        number = number.replace(" ", "");

        // We do not want to sync numbers that are null or have no content.
        if (number == null || number.length() < 1){
            return null;
        }

        return new SyncContactNumber(dataId, number, type, label);
    }

    /**
     * Sync syncs the contacts in the given cursor for T9 and call with app button.
     * @param cursor The cursor of contact(s) to sync.
     */
    public void sync(Cursor cursor) {
        // Check contacts permission. Do nothing if we don't have it. Since it's a background
        // job we can't really ask the user for permission.
        if (!ContactsPermission.hasPermission(mContext)) {
            // TODO VIALA-349 Delete sync account.
            return;
        }

        T9DatabaseHelper t9Database = new T9DatabaseHelper(mContext);

        SyncContact syncContact;
        SyncContactNumber syncContactNumber;

        // Loop all contacts to sync.
        while (cursor.moveToNext()) {

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

            // Sync the contact.
            ContactsManager.syncContact(mContext, syncContact, t9Database);
        }
        cursor.close();

        // Remove dead weight from t9 db.
        t9Database.afterSyncCleanup();
        SyncUtils.setLastSyncNow(mContext);
    }
}
