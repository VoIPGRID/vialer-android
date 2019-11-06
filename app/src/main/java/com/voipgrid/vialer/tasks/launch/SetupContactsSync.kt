package com.voipgrid.vialer.tasks.launch

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.sync.ContactSyncAdapter

class SetupContactsSync: OnLaunchTask {

    override fun execute(application: VialerApplication) {
        if (application.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                application.checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val account = ContactSyncAdapter.createSyncAccount(application) ?: return
        val authority = ContactsContract.AUTHORITY
        ContentResolver.setIsSyncable(account, authority, 1)
        ContentResolver.setSyncAutomatically(account, authority, true)
        val observer = ContactObserver(null, application)
        application.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        //it's currently only possible to access the last sync time using reflection :(
        //so just always sync at startup
        ContentResolver.requestSync(account, authority, Bundle())
    }

    class ContactObserver(val handler: Handler?, private val context: Context) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            ContentResolver.requestSync(ContactSyncAdapter.createSyncAccount(context), ContactsContract.AUTHORITY, Bundle())
        }

    }

}