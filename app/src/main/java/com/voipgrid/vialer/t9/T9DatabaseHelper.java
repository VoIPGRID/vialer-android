package com.voipgrid.vialer.t9;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;

import com.voipgrid.vialer.contacts.SyncContact;
import com.voipgrid.vialer.contacts.SyncContactNumber;
import com.voipgrid.vialer.contacts.SyncUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for accessing the t9 contact database.
 */
public class T9DatabaseHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "t9.db";

    private static final int MAX_RESULTS = 20;

    private Context mContext;

    public T9DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    /**
     * Expandable interface if we need more tables in the future.
     */
    public interface Tables {
        String T9_CONTACT = "t9_contact";
        String T9_QUERY = "t9_query";
    }

    /**
     * Interface for the columns required in the t9_contact table.
     */
    public interface T9ContactColumns extends BaseColumns {
        String DATA_ID = "data_id";
        String CONTACT_ID = "contact_id";
        String LOOKUP_KEY = "lookup_key";
        String DISPLAY_NAME = "display_name";
        String THUMBNAIL_URI = "thumbnail_uri";
        String NUMBER = "number";
        String TYPE = "type";
        String LABEL = "label";
    }

    /**
     * Interface for the columns required in the t9_query table.
     */
    public interface T9QueryColumns extends BaseColumns {
        String T9_QUERY = "t9_query";
        String CONTACT_ID = "contact_id";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        setupTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Data is re-creatable so it is safe to drop the db on an upgrade.
        dropTables(db);
        onCreate(db);
        // Make sure a full contact sync is done.
        SyncUtils.setRequiresFullContactSync(mContext, true);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Function to setup the database tables.
     * @param db
     */
    private void setupTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.T9_CONTACT + " (" +
                T9ContactColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                T9ContactColumns.DATA_ID + " INTEGER, " +
                T9ContactColumns.CONTACT_ID + " INTEGER, " +
                T9ContactColumns.LOOKUP_KEY + " TEXT," +
                T9ContactColumns.DISPLAY_NAME + " TEXT, " +
                T9ContactColumns.THUMBNAIL_URI + " TEXT, " +
                T9ContactColumns.NUMBER + " TEXT, " +
                T9ContactColumns.TYPE + " INTEGER, " +
                T9ContactColumns.LABEL + " TEXT" +
                ");");

        db.execSQL("CREATE TABLE " + Tables.T9_QUERY + " (" +
                T9QueryColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                T9QueryColumns.T9_QUERY + " TEXT COLLATE NOCASE, " +
                T9QueryColumns.CONTACT_ID + " INTEGER" +
                ");");


        db.execSQL("CREATE INDEX IF NOT EXISTS contact_contact_id_index ON " +
                Tables.T9_CONTACT + " (" + T9ContactColumns.CONTACT_ID  + ");");

        db.execSQL("CREATE INDEX IF NOT EXISTS contact_sort_index ON " +
                Tables.T9_CONTACT + " (" + T9ContactColumns.DISPLAY_NAME + ");");

        db.execSQL("CREATE INDEX IF NOT EXISTS t9_query_index ON " +
                Tables.T9_QUERY + " (" + T9QueryColumns.T9_QUERY + ");");

        db.execSQL("CREATE INDEX IF NOT EXISTS t9_query_contact_id_index ON " +
                Tables.T9_QUERY + " (" + T9QueryColumns.CONTACT_ID + ");");
    }

    /**
     * Function to drop the database tables.
     * @param db
     */
    private void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.T9_CONTACT);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.T9_QUERY);
    }

    /**
     * Function to optimize the indexes and table after a sync.
     */
    public void afterSyncCleanup() {
        SQLiteDatabase db = getReadableDatabase();
        analyzeDB(db);
        db.close();
    }

    /**
     * Analyze tables and indexes.
     * @param db
     */
    private void analyzeDB(SQLiteDatabase db) {
        db.execSQL("ANALYZE " + Tables.T9_CONTACT);
        db.execSQL("ANALYZE " + Tables.T9_QUERY);
        db.execSQL("ANALYZE contact_contact_id_index");
        db.execSQL("ANALYZE contact_sort_index");
        db.execSQL("ANALYZE t9_query_index");
        db.execSQL("ANALYZE t9_query_contact_id_index");
    }

    /**
     * Function to insert a contact in the t9 contact table.
     * @param syncContact Contact to be synced.
     */
    public void insertT9Contact(SyncContact syncContact) {
        SQLiteDatabase db = getReadableDatabase();
        insertPhoneNumberQueries(db, syncContact);
        insertDisplayNameQueries(db, syncContact);
        db.close();
    }

    /**
     * Function to update a t9 contact. This deletes all old entries and creates new ones with
     * the new data.
     * @param syncContact Contact to be synced.
     */
    public void updateT9Contact(SyncContact syncContact) {
        SQLiteDatabase db = getReadableDatabase();
        removeContactEntries(db, syncContact);
        db.close();
        insertT9Contact(syncContact);
    }

    /**
     * Function that tries to remove all entries for the given contact.
     * @param db
     * @param syncContact Contact to remove.
     */
    private void removeContactEntries(SQLiteDatabase db, SyncContact syncContact) {
        // Multiple deletes to try and delete the contact even though the contact_id has changed.
        // This introduces a bug where contacts with the exact same name but different contacts
        // get deleted as well.
        int contactDeleteResult = db.delete(Tables.T9_CONTACT, T9ContactColumns.CONTACT_ID + "=" +
                        syncContact.getContactId(), null);
        int queryDeleteResults = db.delete(Tables.T9_QUERY, T9ContactColumns.CONTACT_ID + "=" +
                syncContact.getContactId(), null);

        // Nothing deleted yet, try delete by name.
        if (contactDeleteResult == 0 && queryDeleteResults == 0) {
            db.delete(Tables.T9_QUERY,
                    T9QueryColumns.CONTACT_ID + " IN " +
                            "(SELECT " + T9ContactColumns.CONTACT_ID + " FROM " + Tables.T9_CONTACT +
                            " WHERE " + T9ContactColumns.DISPLAY_NAME + " = ?)",
                    new String[] {syncContact.getDisplayName()});
            db.delete(Tables.T9_CONTACT,
                    T9ContactColumns.DISPLAY_NAME + " = ?",
                    new String[] {syncContact.getDisplayName()});
        }
    }

    /**
     * Function for inserting T9 queries for phone numbers.
     * @param db
     * @param syncContact Contact with phone numbers.
     */
    private void insertPhoneNumberQueries(SQLiteDatabase db, SyncContact syncContact) {
        List<SyncContactNumber> numbers = syncContact.getNumbers();

        try {
            final String contactSqlInsert = "INSERT INTO " + Tables.T9_CONTACT + " (" +
                    T9ContactColumns.DATA_ID + ", " +        // 1
                    T9ContactColumns.CONTACT_ID + ", " +     // 2
                    T9ContactColumns.LOOKUP_KEY + ", " +     // 3
                    T9ContactColumns.DISPLAY_NAME + ", " +   // 4
                    T9ContactColumns.THUMBNAIL_URI + ", " +  // 5
                    T9ContactColumns.NUMBER + "," +          // 6
                    T9ContactColumns.TYPE + ", " +           // 7
                    T9ContactColumns.LABEL  + ") " +         // 8
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            final SQLiteStatement contactInsert = db.compileStatement(contactSqlInsert);

            final String querySqlInsert = "INSERT INTO " + Tables.T9_QUERY + " (" +
                    T9QueryColumns.CONTACT_ID + ", " +  // 1
                    T9QueryColumns.T9_QUERY  + ") " +   // 2
                    " VALUES (?, ?)";
            final SQLiteStatement queryInsert = db.compileStatement(querySqlInsert);

            SyncContactNumber number;
            ArrayList<String> numberQueries;
            String query;

            for (int i = 0; i < numbers.size(); i++) {
                number = numbers.get(i);

                // Insert contact record for number.
                contactInsert.bindLong(1, number.getDataId());
                contactInsert.bindLong(2, syncContact.getContactId());
                if (syncContact.getLookupKey() != null) {
                    contactInsert.bindString(3, syncContact.getLookupKey());
                }
                if (syncContact.getDisplayName() != null) {
                    contactInsert.bindString(4, syncContact.getDisplayName());
                }
                if (syncContact.getThumbnailUri() != null) {
                    contactInsert.bindString(5, syncContact.getThumbnailUri());
                }
                contactInsert.bindString(6, number.getNumber());
                contactInsert.bindLong(7, number.getType());
                if (number.getLabel() != null) {
                    contactInsert.bindString(8, number.getLabel());
                }
                contactInsert.executeInsert();
                contactInsert.clearBindings();

                // Insert queries for phone numbers.
                numberQueries = T9Query.generateT9NumberQueries(number.getNumber());

                for (int j = 0; j < numberQueries.size(); j++) {
                    query = numberQueries.get(j);
                    queryInsert.bindLong(1, syncContact.getContactId());
                    queryInsert.bindString(2, query);

                    queryInsert.executeInsert();
                    queryInsert.clearBindings();
                }
            }
        } finally {

        }
    }

    /**
     * Function for inserting T9 queries for the contacts name.
     * @param db
     * @param syncContact The contact with a name.
     */
    private void insertDisplayNameQueries(SQLiteDatabase db, SyncContact syncContact) {
        try {
            final String sqlInsert = "INSERT INTO " + Tables.T9_QUERY + " (" +
                    T9QueryColumns.CONTACT_ID + ", " +
                    T9QueryColumns.T9_QUERY  + ") " +
                    " VALUES (?, ?)";
            final SQLiteStatement insert = db.compileStatement(sqlInsert);

            // Computes a list of prefixes of a given contact name.
            ArrayList<String> T9NameQueries =
                    T9Query.generateT9NameQueries(syncContact.getDisplayName());
            String query;
            for (int i = 0; i < T9NameQueries.size(); i++) {
                query = T9NameQueries.get(i);
                insert.bindLong(1, syncContact.getContactId());
                insert.bindString(2, query);
                insert.executeInsert();
                insert.clearBindings();
            }
        } finally {

        }
    }

    /**
     * Function that tries to find contacts that match the t9 query.
     * @param T9Query
     * @return
     */
    public List<T9Match> getT9Matches(String T9Query) {
        List<T9Match> matchList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        if (db == null) {
            // Database not ready yet.
            return matchList;
        }

        // Match as 'starts with'.
        String prefixQuery = T9Query + "%";

        final Cursor cursor = db.rawQuery(
                "SELECT " +
                T9ContactColumns.CONTACT_ID + ", " +     // 0
                T9ContactColumns.LOOKUP_KEY + ", " +     // 1
                T9ContactColumns.DISPLAY_NAME + ", " +   // 2
                T9ContactColumns.THUMBNAIL_URI + ", " +  // 3
                T9ContactColumns.NUMBER + ", " +         // 4
                T9ContactColumns.TYPE + ", " +           // 5
                T9ContactColumns.LABEL +                 // 6
                " FROM " + Tables.T9_CONTACT + " WHERE " +
                        T9ContactColumns.CONTACT_ID + " IN " +
                        " (SELECT " + T9QueryColumns.CONTACT_ID +
                        " FROM " + Tables.T9_QUERY +
                        " WHERE " + Tables.T9_QUERY + "." + T9QueryColumns.T9_QUERY +
                        " LIKE '" + prefixQuery + "')" +
                " ORDER BY " + T9ContactColumns.DISPLAY_NAME + " ASC",
                null);

        if (cursor == null) {
            return matchList;
        }

        T9Match match;

        // Loop results and add them to matches.
        while ((cursor.moveToNext()) && (matchList.size() < MAX_RESULTS)) {
            match = new T9Match(
                    cursor.getLong(0),    // contactId
                    cursor.getString(1),  // lookupKey
                    cursor.getString(2),  // displayName
                    cursor.getString(3),  // thumbnailUri
                    cursor.getString(4),  // number
                    cursor.getInt(5),     // type
                    cursor.getString(6),  // label
                    T9Query
            );

            // We do not want duplicates.
            if (matchList.contains(match)) {
                continue;
            }
            matchList.add(match);
        }

        // Close resources.
        cursor.close();
        db.close();

        return matchList;
    }

    /**
     * Searches the database for a single random contact.
     *
     * @return The display name of the chosen contact.
     */
    public String getRandomContactName() {
        SQLiteDatabase db = getReadableDatabase();

        final Cursor cursor = db.rawQuery(
                "SELECT " + T9ContactColumns.DISPLAY_NAME + " FROM " + Tables.T9_CONTACT +  " ORDER BY RANDOM() LIMIT 1",
                null
        );

        if (cursor == null) {
            return null;
        }

        if (cursor.getCount() <= 0) return null;

        cursor.moveToFirst();

        String displayName = cursor.getString(0);

        // Close resources.
        cursor.close();
        db.close();

        return displayName;
    }

}
