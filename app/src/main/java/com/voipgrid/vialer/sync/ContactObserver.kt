package com.voipgrid.vialer.sync

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract

class ContactObserver(val handler: Handler?, private val context: Context) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        ContentResolver.requestSync(ContactSyncAdapter.createSyncAccount(context), ContactsContract.AUTHORITY, Bundle())
    }

}