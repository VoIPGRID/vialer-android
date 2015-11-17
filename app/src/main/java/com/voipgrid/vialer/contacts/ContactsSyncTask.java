package com.voipgrid.vialer.contacts;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

public class ContactsSyncTask extends AsyncTask {

    private static final String TAG = ContactsSyncTask.class.getSimpleName();
    private Context mContext;
    private ContactsSyncListener mListener;

    /**
     * AsyncTask that adds Data entry in Contacts app with "Call with AppName" action.
     *
     * @param context context used for managing a ContentResolver which queries Contacts.
     * @param listener Add result listener to act on Contact sync.
     */
    public ContactsSyncTask(Context context, ContactsSyncListener listener) {
        mContext = context;
        if (listener != null) {
            mListener = listener;
        }
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
        // gives you the list of phone numbers given a contact
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

    @Override
    public Object doInBackground(Object[] params) {
        // gives you the list of contacts who has phone numbers
        Cursor cursor = queryAllContacts();

        while (cursor.moveToNext()) {

            String contactId = getColumnFromCursor(ContactsContract.Contacts._ID, cursor);
            String name = getColumnFromCursor(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, cursor);

            Cursor phones = queryAllPhoneNumbers(contactId);
            if (phones.getCount() <= 0) {
                continue;
            }

            boolean hasFirst = phones.moveToFirst();
            List<String> phoneNumbers = new ArrayList<String>(phones.getCount());
            String normalizedPhoneNumber;

            for(boolean ok = hasFirst; ok; ok = phones.moveToNext()) {
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
            ContactsManager.syncContact(mContext, name, phoneNumbers);
        }
        cursor.close();
        if (mListener != null) {
            // Return a SyncSuccess callBack if anyone is listening.
            mListener.onSyncSuccess();
        }
        return null;
    }

    public interface ContactsSyncListener {
        void onSyncSuccess();
    }
}
