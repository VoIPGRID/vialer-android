package com.voipgrid.vialer.sync

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract.RawContacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.voipgrid.vialer.R

data class RawContact(val rawContactId: Long) {

    var contactId: Long? = null
    var accountType: String? = null

    var displayName: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var mobileNumber: String? = null

    public fun readRawContactData(cursor: Cursor) {
        contactId = cursor.getLong(cursor.getColumnIndex(RawContacts.CONTACT_ID))
        accountType = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE))
    }

    public fun readPhoneData(cursor: Cursor) {
        displayName = cursor.getString(cursor.getColumnIndex(Data.DISPLAY_NAME))
        mobileNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER))
    }

    public fun readGeneralData(cursor: Cursor) {
        firstName = cursor.getString(cursor.getColumnIndex(StructuredName.GIVEN_NAME))
        lastName = cursor.getString(cursor.getColumnIndex(StructuredName.FAMILY_NAME))
    }

    public fun isVialerContact(context: Context): Boolean {
        return accountType == context.getString(R.string.account_type)
    }

}