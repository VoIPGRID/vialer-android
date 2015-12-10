package com.voipgrid.vialer.t9;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

import com.voipgrid.vialer.R;

/**
 * Created by karstenwestra on 23/10/15.
 * An AsyncTask class to retrieve a list of contacts data from the Android system.
 */
public class ListViewContactsLoader extends AsyncTask<CharSequence, Void, Cursor> {

    private final static String LOG_TAG = ListViewContactsLoader.class.getSimpleName();

    private final Context mContext;
    private SimpleCursorAdapter mContactsAdapter;
    private MatrixCursor mMatrixCursor;
    private ContentResolver mContentResolver;

    public ListViewContactsLoader(Context context, SimpleCursorAdapter contactsAdapter) {
        mContext = context;
        mContactsAdapter = contactsAdapter;
        mContentResolver = mContext.getContentResolver();
    }

    /**
     * @param sequence data string the app uses for t9 contact lookup.
     * @return the input CharSequence casted to String and null checked.
     */
    String searchString(CharSequence sequence) {
        if(sequence != null) {
            try {
                return sequence.toString();
            } catch (NullPointerException npe) {
                return "";
            }
        }
        return "";
    }

    /**
     *
     * @param constraintString a number the app needs to convert to possible
     *                         t9 string values that could contain a name of
     *                         a contact
     * @return a string builder containing a GLOB that can be used to search for
     * contact display_name using patterns.
     */
    @NonNull
    private StringBuilder t9LookupStringBuilder(String constraintString) {
        String[] t9Lookup = mContext.getResources().getStringArray(R.array.t9lookup);
        StringBuilder builder = new StringBuilder();
        for (int i = 0, constraintLength = constraintString.length(); i < constraintLength; ++i) {
            char c = constraintString.charAt(i);

            if (c >= '0' && c <= '9') {
                builder.append(t9Lookup[c - '0']);
            } else if (c == '+') {
                builder.append(c);
            } else {
                builder.append("[");
                builder.append(Character.toLowerCase(c));
                builder.append(Character.toUpperCase(c));
                builder.append("]");
            }
        }
        return builder;
    }

    /**
     * SELECTION parameter containing a set of fields to search for.
     * @return SQL string containing boolean clause used to match contacts
     * in ContentResolver query.
     */
    protected String getSelectionQueryString() {
        return Data.MIMETYPE + " = ?"                                   // PHONE_TYPE
                + " AND " + Phone.HAS_PHONE_NUMBER + " = 1"             // HAS_NUMBER
                + " AND"
                    +" ("
                        + Phone.DISPLAY_NAME_PRIMARY + " GLOB ?"        // T9 name search string
                        + " OR " + Phone.DATA3 + " LIKE ?"  // DATA3 contains normalized
                                                            // number without spaces.
                    + ")";
    }

    /**
     *
     * @param searchString t9 String to use as GLOB pattern for DISPLAY_NAME search
     * @param searchNumber phonenumber String to match NORMALIZED_NUMBER of Phone object.
     * @return String array used as where value set for ContentResolver query.
     */
    protected String[] getSelectionArguments(String searchString, String searchNumber) {
        return new String[] {
                mContext.getString(R.string.profile_mimetype),
                "*" + searchString + "*",
                "%" + searchNumber + "%"
        };
    }

    /**
     * Covert a cursor obtained from a ContentResolver query to a MatrixCursor that
     * can be manipulated dynamically
     * @param cursor Matrix cursor obtained from
     */
    void populateCursorWithCursor(Cursor cursor) {
        // Create a mutable cursor to manipulate for search.
        if (mMatrixCursor == null) {
            mMatrixCursor = new MatrixCursor(new String[] {"_id", "name", "photo", "number"});
        }
        for (boolean ok = cursor.moveToFirst(); ok; ok = cursor.moveToNext()) {
            long contactId = cursor.getInt(cursor.getColumnIndex(Data.CONTACT_ID));
            mMatrixCursor.addRow(new Object[]{
                    Long.toString(contactId),
                    cursor.getString(cursor.getColumnIndex(Data.DISPLAY_NAME_PRIMARY)),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
                    cursor.getString(cursor.getColumnIndex(Phone.DATA3))
            });
        }
    }

    /**
     *
     * @param params list of number options used to search for a contact through
     *               NUMBER or DISPLAY_NAME_PRIMARY of a Phone object attached to a Contact.
     * @return MatrixCursor with all data used to list Contacts matching the input search string.
     */
    @Override
    protected Cursor doInBackground(CharSequence... params) {
        // Check if we started with a T9 searchString.
        String searchNumber = (params.length > 0 ? searchString(params[0]) : "");
        // Then convert that searchNumber to a potential name.
        StringBuilder searchStringBuilder = t9LookupStringBuilder(searchNumber);

        String sortOrder = Data.DISPLAY_NAME_PRIMARY + " ASC";
        // We want to strip the leading zero because we are searching normalized numbers
        // (+XX625874469). The leading zero would lead to 0 matches.
        if (searchNumber.length() > 0){
            // LIMIT 20 because we are t9 searching on the number.
            sortOrder = sortOrder + " LIMIT 20";
            if (searchNumber.charAt(0) == '0'){
                searchNumber = searchNumber.substring(1);
            }
        }

        // Query for all ContactsContract.Data entries with mimetype phone_v2
        // and <app_name> typed data .
        Cursor dataCursor = mContentResolver.query(
                Data.CONTENT_URI,                                                    // URI
                new String[] {
                        Data.CONTACT_ID,
                        Data.DISPLAY_NAME_PRIMARY,
                        Phone.DATA3
                },                                                                // PROJECTION
                getSelectionQueryString(),                                                      // SELECTION
                getSelectionArguments(searchStringBuilder.toString(), searchNumber), // WHERE args
                sortOrder                         // SORT ORDER
        );
        // Dynamically populate a matrix cursor for use in t9 search list presentation
        populateCursorWithCursor(dataCursor);
        assert dataCursor != null; // properly clean up the search process.
        dataCursor.close();
        return mMatrixCursor;
    }

    /**
     * Swap the result cursor returned from doInBackground with the current cursor used to display
     * the list of contacts.
     * @param result
     */
    @Override
    protected void onPostExecute(Cursor result) {
        // Setting the cursor containing contacts to listview
        mContactsAdapter.swapCursor(result);
        mContactsAdapter.notifyDataSetChanged();
    }
}