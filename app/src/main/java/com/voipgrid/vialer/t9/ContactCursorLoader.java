package com.voipgrid.vialer.t9;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import java.util.List;

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
    void populateMaxtrixCursor(List<T9Match> matches) {
        // Create a mutable cursor to manipulate for search.
        if (mMatrixCursor == null) {
            mMatrixCursor = new MatrixCursor(new String[] {"_id", "name", "photo", "number"});
        }

        T9Match match;
        boolean addResult;
        String displayName;
        String number;

        for (int i = 0; i < matches.size(); i++) {
            match = matches.get(i);
            displayName = match.getDisplayName();
            number = match.getNumber();
            addResult = false;

            if (mT9Query.length() != 0) {
                // Only allowed T9 chars for name matching.
                if (mT9Query.substring(0, 1).matches("[2-9]")) {
                    if (T9NameMatcher.T9QueryMatchesName(mT9Query, displayName)) {
                        addResult = true;
                        displayName = T9NameMatcher.highlightMatchedPart(mT9Query, displayName);
                    }
                }

                if (number != null) {
                    if (number.startsWith("+")) {
                        if(("0" + number.substring(3)).startsWith(mT9Query)) {
                            addResult = true;
                            number = "<b>" + number.substring(0, mT9Query.length() + 2) + "</b>" + number.substring(mT9Query.length() + 2);
                        }
                    }

                    if (number.startsWith(mT9Query)) {
                        addResult = true;
                        number = "<b>" + number.substring(0, mT9Query.length()) + "</b>" + number.substring(mT9Query.length());
                    }
                }
            } else {
                // No query so add all 20 results.
                addResult = true;
            }


            if (addResult) {
                mMatrixCursor.addRow(new Object[]{
                        Long.toString(match.getContactId()),
                        displayName,
                        match.getThumbnailUri(),
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

        List<T9Match> matches = t9Database.getT9Matches(mT9Query);

        // Populate a new cursor that is UI friendly.
        populateMaxtrixCursor(matches);

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
