package com.voipgrid.vialer.contacts;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.models.SystemUser;

import java.util.ArrayList;

public class ContactsManager {

    private static final String TAG = ContactsManager.class.getSimpleName();

    public static String[] PROJECTION;

    static {
        PROJECTION = new String[]{
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.Data.DISPLAY_NAME,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.CONTACT_ID};
    }

    public static void add(Activity context, SystemUser systemUser) {
        addNewAccount(context, context.getString(R.string.contacts_app_name), context.getString(R.string.authtoken_type_full_access));
        ContactsManager.addContact(context, systemUser);
    }

    /** Private convenience method */
    private static void addNewAccount(Activity activity, String accountType, String authTokenType) {
        AccountManager.get(activity)
            .addAccount(accountType, authTokenType, null, null, activity, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> future) {
                    try {
                        Bundle bnd = future.getResult();
                        assert(bnd != null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, null);
    }

    /**
     * Add Data to a Contact entry for "AppName" to enable ann action "<Appname> call <number>".
     *
     * @param context The context used to create a content resolver.
     * @param contact The Contact data to stored.
     */
    public static void addContact(Context context, SystemUser contact) {
        ContentResolver resolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();


        String where = ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " = ? AND "
                + ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND "
                + ContactsContract.RawContacts.ACCOUNT_NAME + " = ?";
        String[] whereArg = new String[] {contact.getFirstName(), context.getString(R.string.account_type), context.getString(R.string.contacts_app_name)};

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
                .withValue(ContactsContract.Settings.UNGROUPED_VISIBLE, 1)
                    .build());

            // Add a name DATA item for the contact
            ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.getFirstName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.getLastName())
                    .build());

            // Add a phone number entry for our RawContact type
            ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.getMobileNumber())
                    .build());

            // Create '<app_name> call <number>' string.
            String contactActionString = context.getString(
                    R.string.contact_call_action_name,
                    context.getString(R.string.app_name),
                    contact.getMobileNumber());

            // Add a DATA item for our custom sample action
            ops.add(ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, context.getString(R.string.profile_mimetype))
                    .withValue(ContactsContract.Data.DATA1, Integer.valueOf(context.getString(R.string.account_token)))      // Profile token
                    .withValue(ContactsContract.Data.DATA2, contact.getMobileNumber())   // DATA summary
                    .withValue(ContactsContract.Data.DATA3, contactActionString)   // DATA desc
                    .build());
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
