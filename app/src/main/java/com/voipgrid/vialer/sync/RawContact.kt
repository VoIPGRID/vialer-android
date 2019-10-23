package com.voipgrid.vialer.sync

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract.*
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.voipgrid.vialer.R

data class RawContact(val rawContactId: Long) {

    var contactId: Long? = null
    var accountType: String? = null

    var displayName: String? = null
    var firstName: String? = null
    var lastName: String? = null
    val phoneNumbers = ArrayList<String>()

    public fun readRawContactData(cursor: Cursor, default: Boolean) {
        contactId = cursor.getLong(cursor.getColumnIndex(if (default) Contacts._ID else RawContacts.CONTACT_ID))
        accountType = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE))
    }

    public fun readPhoneData(cursor: Cursor) {
        displayName = cursor.getString(cursor.getColumnIndex(Data.DISPLAY_NAME))
        phoneNumbers.add(cursor.getString(cursor.getColumnIndex(Phone.NUMBER)))
    }

    public fun readNameData(cursor: Cursor) {
        firstName = cursor.getString(cursor.getColumnIndex(StructuredName.GIVEN_NAME))
        lastName = cursor.getString(cursor.getColumnIndex(StructuredName.FAMILY_NAME))
    }

    public fun isVialerContact(context: Context): Boolean {
        return accountType == context.getString(R.string.account_type)
    }

}