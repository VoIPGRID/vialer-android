package com.voipgrid.vialer.contacts;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

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
    public void sync() {
        // Check contacts permission. Do nothing if we don't have it. Since it's a background
        // job we can't really ask the user for permission.
        if (!ContactsPermission.hasPermission(mContext)) {
            // TODO VIALA-349 Delete sync account.
            return;
        }
        // Gives you the list of contacts who have phone numbers.
        Cursor cursor = queryAllContacts();

        while (cursor.moveToNext()) {
            String contactId = getColumnFromCursor(ContactsContract.Contacts._ID, cursor);
            String name = getColumnFromCursor(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    cursor);

            Cursor phones = queryAllPhoneNumbers(contactId);

            if (phones.getCount() <= 0) {
                // Close cursor before continue.
                phones.close();
                continue;
            }

            List<String> phoneNumbers = new ArrayList<>(phones.getCount());
            String normalizedPhoneNumber;

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
                if (!phoneNumbers.contains(normalizedPhoneNumber)){
                    phoneNumbers.add(normalizedPhoneNumber);
                }

            }
            phones.close();

            // Found no normalized phone numbers so don't sync the contact.
            if (phoneNumbers.size() <= 0){
                continue;
            }
            ContactsManager.syncContact(mContext, name, phoneNumbers);
        }
        cursor.close();
    }
}
