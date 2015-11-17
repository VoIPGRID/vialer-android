package com.voipgrid.vialer.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
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
     * @param context
     * @param displayName The display name of the contact.
     * @param phoneNumbers The phone numbers of the contact.
     */
    public static void syncContact(Context context, String displayName, List<String> phoneNumbers) {
        ContentResolver resolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        String where = ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " = ? AND "
                + ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND "
                + ContactsContract.RawContacts.ACCOUNT_NAME + " = ?";
        String[] whereArg = new String[] {
                displayName,
                context.getString(R.string.account_type),
                context.getString(R.string.contacts_app_name)};

        // Make sure SyncAccount requirement is met.
        checkSyncAccount(context);

        Cursor sameName = resolver.query(ContactsContract.RawContacts.CONTENT_URI, null, where, whereArg, null);
        if (sameName != null && sameName.getCount() == 0) { // Prevent duplicate entries in RawContactsArray
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
                ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, mimeType)
                        .withValue(ContactsContract.Data.DATA2, contactActionString)   // DATA summary
                        .withValue(ContactsContract.Data.DATA3, phoneNumber)   // DATA desc
                        .build());
            }

            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        sameName.close();
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
