package com.voipgrid.vialer.t9;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

/**
 * AsyncTaskLoader for loading t9 matches.
 */
public class ContactCursorLoader extends AsyncTaskLoader<Cursor> {

    Context mContext;
    String mT9Query = "";
    MatrixCursor mMatrixCursor;

    private Cursor mCursor;

    private final String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC";

    public ContactCursorLoader(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * Function to set the t9Query used for loading.
     * @param t9Query
     */
    public void setT9Query(String t9Query) {
        mT9Query = t9Query;
    }

    /**
     * Covert a cursor obtained from a ContentResolver query to a MatrixCursor that
     * can be manipulated dynamically.
     * @param cursor Matrix cursor obtained from
     */
    void populateMaxtrixCursor(Cursor cursor, String t9query) {
        // Create a mutable cursor to manipulate for search.
        if (mMatrixCursor == null) {
            mMatrixCursor = new MatrixCursor(new String[] {"_id", "name", "photo", "number"});
        }

        while (cursor.moveToNext()) {
            long contactId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
            String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            boolean addResult = false;

            if (t9query.length() != 0) {
                // Only allowed T9 chars for name matching.
                if (t9query.substring(0, 1).matches("[2-9]")){
                    if (T9NameMatcher.T9QueryMatchesName(t9query, displayName)) {
                        addResult = true;
                    }
                }

                if (number != null && number.startsWith(t9query)) {
                    addResult = true;
                }
            } else {
                // No query so add all 20 results.
                addResult = true;
            }

            if (addResult) {
                mMatrixCursor.addRow(new Object[]{
                        Long.toString(contactId),
                        displayName,
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
                        number,
                });
            }

        }
    }

    @Override
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            closeCursor(cursor);
            return;
        }
        // Hold a reference to the old data so it doesn't get garbage collected.
        Cursor oldCursor = mCursor;
        mCursor = cursor;
        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the client.
            super.deliverResult(cursor);
        }
        // Invalidate the old data as we don't need it any more.
        if (oldCursor != null && oldCursor != cursor) {
            closeCursor(oldCursor);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mCursor);
        }
        if (mCursor == null) {
            // Force loads every time as our results change with queries.
            forceLoad();
        }
    }

    @Override
    public Cursor loadInBackground() {
        // No t9Query means nothing to load.
        if (mT9Query.length() == 0) {
            return null;
        }

        // Setup database handler.
        T9DatabaseHelper t9Database = new T9DatabaseHelper(mContext);
        Object[] contactIdMatches = t9Database.getT9ContactIdMatches(mT9Query).toArray();

        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " IN " + "(" + TextUtils.join(", ", contactIdMatches) + ")";

        Uri URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(ContactsContract.Directory.DEFAULT))
                .build();

        // Query contact info based on found contact id matches.
        Cursor dataCursor = mContext.getContentResolver().query(
                URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                },
                selection,
                null,
                sortOrder
        );

        // Populate a new cursor that is UI friendly.
        populateMaxtrixCursor(dataCursor, mT9Query);

        assert dataCursor != null; // properly clean up the search process.
        dataCursor.close();
        return mMatrixCursor;
    }

    @Override
    protected void onStopLoading() {
        // The Loader is in a stopped state, so we should attempt to cancel the current load.
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();

        if (mCursor != null) {
            closeCursor(mCursor);
            mCursor = null;
        }
    }

    @Override
    public void onCanceled(Cursor cursor) {
        super.onCanceled(cursor);
        closeCursor(cursor);
    }


    private void closeCursor(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
