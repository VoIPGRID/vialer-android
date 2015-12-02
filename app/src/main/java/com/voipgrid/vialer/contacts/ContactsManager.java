package com.voipgrid.vialer.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.voipgrid.vialer.R;

import java.util.ArrayList;
import java.util.List;


public class ContactsManager {

    private static final String LOG_TAG = ContactsManager.class.getSimpleName();

    /**
     * Check if their is a sync account present. If not create one.
     *
     * @param context The context used to get the AccountManager, Strings and ContentResolver.
     */
    private static void checkSyncAccount(Context context){
        AccountManager am = AccountManager.get(context);
        Account[] accounts;
        accounts = am.getAccountsByType(context.getString(R.string.account_type));
        if (accounts == null || accounts.length <= 0) {
            Log.d(LOG_TAG, "Created sync account");
            Account account = new Account(context.getString(R.string.contacts_app_name), context.getString(R.string.account_type));
            am.addAccountExplicitly(account, "", null);
            ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
        }
    }

    /**
     * Function for syncing a contact.
     *
     * @param context Context for Strings and ContentResolver.
     * @param displayName The display name of the contact.
     * @param phoneNumbers The phone numbers of the contact.
     */
    public static void syncContact(Context context, String displayName, List<String> phoneNumbers) {
        String where = ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " = ? AND "
                + ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND "
                + ContactsContract.RawContacts.ACCOUNT_NAME + " = ?";
        String[] whereArg = new String[] {
                displayName,
                context.getString(R.string.account_type),
                context.getString(R.string.contacts_app_name)};

        // Make sure SyncAccount requirement is met.
        checkSyncAccount(context);

        // TODO VIALA-340: Duplicate contacts with same name.
        ContentResolver resolver = context.getContentResolver();
        Cursor sameName = resolver.query(ContactsContract.RawContacts.CONTENT_URI, null, where, whereArg, null);

        if (sameName != null) {
            // Prevent duplicate entries in RawContactsArray.
            if (sameName.getCount() == 0) {
                sameName.close();
                // Not an existing record so create app contact.
                addAppContact(context, displayName, phoneNumbers);
            } else {
                sameName.moveToFirst();
                String contactId = sameName.getString(sameName.getColumnIndex(ContactsContract.Contacts._ID));
                sameName.close();
                // Does exist, take first contact and update it.
                // TODO VIALA-340: Duplicate contacts with same name.
                updateAppContact(context, contactId, phoneNumbers);
            }
        }
    }

    /**
     * Function for adding a app related contact to the existing contact.
     *
     * @param context Context for Strings and ContentResolver.
     * @param displayName Display name of the existing contact.
     * @param phoneNumbers Phone numbers that need to added.
     */
    private static void addAppContact(Context context, String displayName, List<String> phoneNumbers) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        // Insert RawContact to which is root for DATA entries
        ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI, true))
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, context.getString(R.string.contacts_app_name))
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, context.getString(R.string.account_type))
                .build());

        // Add Setting to manage and edit contact data
        ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Settings.CONTENT_URI, true))
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, context.getString(R.string.contacts_app_name))
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, context.getString(R.string.account_type))
                .withValue(ContactsContract.Settings.UNGROUPED_VISIBLE, 0)
                .build());

        // Add a name DATA item for the contact
        ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build());

        addAppContactActionInsertsToOps(context, ops, phoneNumbers);

        ContentResolver resolver = context.getContentResolver();

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Function for updating a app contact related to a existing contact.
     *
     * @param context Context for Strings and ContentResolver.
     * @param contactId Id of the app contact that needs to be updated.
     * @param phoneNumbers New phone numbers of the related contact.
     */
    private static void updateAppContact(Context context, String contactId, List<String> phoneNumbers) {
        // Initialization.
        String[] projection;
        String selection;
        String[] selectionArgs;
        String mimetype = context.getString(R.string.profile_mimetype);
        ContentResolver resolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        List<String> currentPhoneNumbers = new ArrayList<>();

        // Set arguments for query.
        projection = new String[] { ContactsContract.Data.DATA3 };  // DATA3 contains normalized phone number
        selection = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND "
                + ContactsContract.Data.MIMETYPE + " = ?";
        selectionArgs = new String[] {
                contactId,
                mimetype,
        };

        // Get current app contact rows with mimetype.
        Cursor current = resolver.query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);
        while(current.moveToNext()){
            // Add all DATA3 (Normalized phone numbers) entries to currentPhoneNumbers.
            currentPhoneNumbers.add(current.getString(current.getColumnIndex(ContactsContract.Data.DATA3)));
        }
        current.close();

        // Create copies for comparison.
        List<String> phoneNumbersCopy = new ArrayList<>(phoneNumbers);
        List<String> currentPhoneNumbersCopy = new ArrayList<>(currentPhoneNumbers);

        // Remove list entries from each other. This modifies the 2 list to contain numbers that
        // should be added or deleted from the app contact.
        phoneNumbers.removeAll(currentPhoneNumbersCopy);
        currentPhoneNumbers.removeAll(phoneNumbersCopy);

        // These numbers need to be added to the app contact.
        if (phoneNumbers.size() > 0) {
            addAppContactActionInsertsToOps(context, ops, Long.parseLong(contactId), phoneNumbers);
        }
        // These numbers need to be deleted from the app contact.
        if (currentPhoneNumbers.size() > 0){
            addAppContactActionDeletesToOps(context, ops, contactId, currentPhoneNumbers);
        }

        // If we have operations execute them.
        if (ops.size() > 0){
            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Function to add app contact action inserts to a contact created in the current ops to ops.
     *
     * @param context Context for Strings.
     * @param ops ArrayList with current operations (including a new contact).
     * @param phoneNumbers List of phone numbers to add.
     */
    private static void addAppContactActionInsertsToOps(Context context, ArrayList<ContentProviderOperation> ops, List<String> phoneNumbers){
        addAppContactActionInsertsToOps(context, ops, null, phoneNumbers);
    }

    /**
     * Function to add app contact action inserts for a (existing) app contact to ops.
     *
     * @param context Context for Strings.
     * @param ops List of already existing operations.
     * @param contactId Long with app contact id or null for freshly created contact in ops.
     * @param phoneNumbers List with phone numbers to add.
     */
    private static void addAppContactActionInsertsToOps(Context context, ArrayList<ContentProviderOperation> ops, Long contactId,  List<String> phoneNumbers) {
        // Initialization.
        String contactActionString;
        String phoneNumber;
        String appName = context.getString(R.string.app_name);
        String mimeType = context.getString(R.string.profile_mimetype);

        // Add a DATA item for our custom sample action for all numbers.
        for (int i = 0; i < phoneNumbers.size(); i++){
            phoneNumber = phoneNumbers.get(i);
            contactActionString = context.getString(
                    R.string.contact_call_action_name,
                    appName,
                    phoneNumber);
            // Create builder.
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true));

            if (contactId == null) {
                // No contact id so add it with a back reference to the app contact that will be
                // created and is already added to ops.
                builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
            } else {
                // Link to existing app contact.
                builder.withValue(ContactsContract.Data.RAW_CONTACT_ID, contactId);
            }

            ContentProviderOperation op = builder.withValue(ContactsContract.Data.MIMETYPE, mimeType)
                    .withValue(ContactsContract.Data.DATA2, contactActionString)   // DATA summary
                    .withValue(ContactsContract.Data.DATA3, phoneNumber)   // DATA desc
                    .build();

            ops.add(op);
        }
    }

    /**
     * Function to add deletes of phone numbers from the app contact to ops.
     *
     * @param context Context for Strings.
     * @param ops List with already existing operations.
     * @param contactId String with the app contact id.
     * @param currentPhoneNumbers List of phone numbers to delete.
     */
    private static void addAppContactActionDeletesToOps(Context context, ArrayList<ContentProviderOperation> ops, String contactId, List<String> currentPhoneNumbers) {
        String mimetype = context.getString(R.string.profile_mimetype);
        String selection = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND "
                + ContactsContract.Data.MIMETYPE + " = ? AND "
                + ContactsContract.Data.DATA3 + " = ?";  // DATA3 contains normalized phone number
        String[] selectionArgs;

        // Delete a DATA item from the remaining in the list.
        for (int i = 0; i < currentPhoneNumbers.size(); i++) {
            selectionArgs = new String[] {
                    contactId,
                    mimetype,
                    currentPhoneNumbers.get(i),
            };
            ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                    .withSelection(selection, selectionArgs)
                    .build());
        }
    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
        if (isSyncOperation) {
            return uri.buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                    .build();
        }
        return uri;
    }
}
