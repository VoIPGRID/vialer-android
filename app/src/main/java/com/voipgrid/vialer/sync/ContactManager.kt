package com.voipgrid.vialer.sync

import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract.*
import android.provider.ContactsContract.CommonDataKinds.*
import com.voipgrid.vialer.R
import kotlin.reflect.KFunction2

class ContactManager(private val account: Account, private val contentResolver: ContentResolver) {

    val vialerContacts: List<RawContact>
        get() {
            val builder = RawContacts.CONTENT_URI.buildUpon()
            builder.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
            builder.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
            return readRawContacts(builder.build())
        }

    val rawContacts: List<RawContact>
        get() {
            return readRawContacts(Contacts.CONTENT_URI)
        }

    public fun getRawContactsUri(synced: Boolean): Uri {
        val builder = RawContacts.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
        builder.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
        if (synced) {
            builder.appendQueryParameter(CALLER_IS_SYNCADAPTER, true.toString())
        }
        return builder.build()
    }

    public fun mergeContact(masterContactId: Long, vialerContactId: Long) {
        val contentValues = ContentValues(3)
        contentValues.put(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER)
        contentValues.put(AggregationExceptions.RAW_CONTACT_ID1, masterContactId)
        contentValues.put(AggregationExceptions.RAW_CONTACT_ID2, vialerContactId)
        contentResolver.update(AggregationExceptions.CONTENT_URI, contentValues, null, null)
    }

    public fun addContact(context: Context, firstName: String, lastName: String, displayName: String, number: String): Long {
        val id = insertRawVialerContact(number)
        if (id == -1L) {
            return -1
        }
        insertName(id, displayName, firstName, null, lastName, null)
        insertMobilePhone(id, number)
        insertVialerCallSupport(id, number, context)
        return id
    }

    public fun deleteContact(context: Context, rawContactId: Long, contactId: Long) {
        deleteRawContact(rawContactId)
        deleteDataContact(context, contactId)
    }

    public fun deleteVialerContact(context: Context, rawContactId: Long, contactId: Long) {
        deleteRawContact(rawContactId)
        deleteDataContact(context, contactId)
    }

    private fun insertRawVialerContact(phoneNumber: String): Long {
        val contentValues = ContentValues(3)
        contentValues.put(RawContacts.ACCOUNT_NAME, account.name)
        contentValues.put(RawContacts.ACCOUNT_TYPE, account.type)
        contentValues.put(RawContacts.SYNC1, phoneNumber)
        val uri = contentResolver.insert(RawContacts.CONTENT_URI, contentValues) ?: return -1
        return ContentUris.parseId(uri)
    }

    private fun insertName(id: Long, displayName: String, firstName: String, middleName: String?, lastName: String, prefix: String?) {
        val contentValues = ContentValues(5)
        contentValues.put(Data.RAW_CONTACT_ID, id)
        contentValues.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        contentValues.put(StructuredName.DISPLAY_NAME, displayName)
        contentValues.put(StructuredName.GIVEN_NAME, firstName)
        contentValues.put(StructuredName.FAMILY_NAME, lastName)
        if (prefix != null) {
            contentValues.put(StructuredName.PREFIX, prefix)
        }
        if (middleName != null) {
            contentValues.put(StructuredName.MIDDLE_NAME, middleName)
        }
        contentResolver.insert(Data.CONTENT_URI, contentValues)
    }

    private fun insertNickName(id: Long, nickName: String) {
        val contentValues = ContentValues(3)
        contentValues.put(Data.RAW_CONTACT_ID, id)
        contentValues.put(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
        contentValues.put(Nickname.NAME, nickName)
        contentResolver.insert(Data.CONTENT_URI, contentValues)
    }

    private fun insertMobilePhone(id: Long, phoneNumber: String) {
        val contentValues = ContentValues(4)
        contentValues.put(Data.RAW_CONTACT_ID, id)
        contentValues.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
        contentValues.put(Phone.TYPE, Phone.TYPE_MOBILE)
        contentValues.put(Phone.NUMBER, phoneNumber)
        contentResolver.insert(Data.CONTENT_URI, contentValues)
    }

    public fun ensureVialerCallSupport(id: Long, number: String, context: Context) {
        val mimeType = context.getString(R.string.call_mime_type)
        val cursor = contentResolver.query(Data.CONTENT_URI, null, Data.MIMETYPE + " = ?", arrayOf(mimeType), null)
                ?: return
        if (cursor.count == 0) {
            insertVialerCallSupport(id, number, context)
        }
    }

    public fun insertVialerCallSupport(id: Long, number: String, context: Context) {
        val contentValues = ContentValues(5)
        contentValues.put(Data.RAW_CONTACT_ID, id)
        contentValues.put(Data.MIMETYPE, context.getString(R.string.call_mime_type))
        contentValues.put(Data.DATA1, number)
        contentValues.put(Data.DATA2, context.getString(R.string.app_name))
        contentValues.put(Data.DATA3, context.getString(R.string.call_with, number))
        contentResolver.insert(Data.CONTENT_URI, contentValues)
    }

    public fun deleteVialerCallSupport(id: Long, number: String, context: Context) {
        val where = java.lang.StringBuilder()
        where.append(Data.RAW_CONTACT_ID).append(" = ").append(id)
        where.append(" AND ")
        where.append(Data.MIMETYPE).append(" = '").append(context.getString(R.string.call_mime_type)).append("'")
        where.append(" AND ")
        where.append(Data.DATA1).append(" = '").append(number).append("'")
        contentResolver.delete(Data.CONTENT_URI, where.toString(), null)
    }

    private fun insertIMField(id: Long, title: String, content: String) {
        val contentValues = ContentValues()
        contentValues.put(Data.RAW_CONTACT_ID, id)
        contentValues.put(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE)
        contentValues.put(Im.TYPE, Im.TYPE_CUSTOM)
        contentValues.put(Im.LABEL, title)
        contentValues.put(Im.PROTOCOL, Im.PROTOCOL_CUSTOM)
        contentValues.put(Im.CUSTOM_PROTOCOL, title)
        contentValues.put(Im.DATA, content)
        contentResolver.insert(Data.CONTENT_URI, contentValues)
    }

    public fun hasVialerCallSupport(context: Context, number: String): Boolean {
        val selection = Data.MIMETYPE + " = ? AND " + Data.DATA1 + " = ?"
        val c = contentResolver.query(Data.CONTENT_URI, null, selection, arrayOf(context.getString(R.string.call_mime_type), number), null) ?: return false
        if (c.count == 0) {
            return false
        }
        return true
    }

    public fun getVialerCallSupportNumbers(rawContactId: Long, context: Context): List<String> {
        val list = ArrayList<String>()
        val selection = Data.MIMETYPE + " = ? AND " + Data.RAW_CONTACT_ID + " = ?"
        val c = contentResolver.query(Data.CONTENT_URI, null, selection, arrayOf(context.getString(R.string.call_mime_type), rawContactId.toString()), null) ?: return list
        while(c.moveToNext()) {
            val number = c.getString(c.getColumnIndex(Data.DATA1))
            list.add(number)
        }
        return list
    }

    private fun deleteRawContact(rawContactId: Long) {
        contentResolver.delete(getRawContactsUri(true), RawContacts._ID + " = ?", arrayOf(rawContactId.toString()))
    }

    private fun deleteDataContact(context: Context, contactId: Long) {
        val uri = Data.CONTENT_URI.buildUpon().appendQueryParameter(CALLER_IS_SYNCADAPTER, true.toString()).build()
        val mimeType = context.getString(R.string.call_mime_type)
        contentResolver.delete(uri, Data.MIMETYPE + " = ? AND " + RawContacts.CONTACT_ID + " = ?", arrayOf(mimeType, contactId.toString()))
    }

    public fun clearVialerContacts(context: Context) {
        val vialerContacts = vialerContacts
        if (vialerContacts.isEmpty()) {
            return
        }
        for (i in vialerContacts) {
            val contactId = i.contactId ?: continue
            deleteVialerContact(context, i.rawContactId, contactId)
        }
    }

    private fun readRawContacts(contentUri: Uri): List<RawContact> {
        val contacts = ArrayList<RawContact>()
        val cursor = contentResolver.query(contentUri, null, null, null, null) ?: return contacts
        //read contact data
        while (cursor.moveToNext()) {
            val default = contentUri == Contacts.CONTENT_URI
            val column = if (default) Contacts.NAME_RAW_CONTACT_ID else RawContacts._ID
            val rawContactId = cursor.getLong(cursor.getColumnIndex(column))
            val rawContact = RawContact(rawContactId)
            rawContact.readRawContactData(cursor, default)
            contacts.add(rawContact)
        }
        cursor.close()
        if (contacts.isEmpty()) {
            return contacts
        }
        val builder = StringBuilder(Data.CONTACT_ID).append(" IN (")
        val lastContact = contacts.last()
        for (rawContact in contacts) {
            val contactId = rawContact.contactId ?: continue
            builder.append(contactId)
            if (rawContact != lastContact) {
                builder.append(", ")
            }
        }
        val where = builder.append(")").toString()
        //read phone data
        readDataChunk(Phone.CONTENT_URI, where, contacts, RawContact::readPhoneData)
        //read name data
        readDataChunk(Data.CONTENT_URI, where + " AND " + Data.MIMETYPE + " = '" + StructuredName.CONTENT_ITEM_TYPE + "'", contacts, RawContact::readNameData)
        return contacts
    }

    private fun readDataChunk(uri: Uri, where: String, contacts: List<RawContact>, action: KFunction2<RawContact, @ParameterName(name = "cursor") Cursor, Unit>) {
        val cursor = contentResolver.query(uri, null, where, null, null) ?: return
        while (cursor.moveToNext()) {
            val contactId = cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID))
            val contact = contacts.find {
                it.contactId == contactId
            } ?: continue
            action(contact, cursor)
        }
        cursor.close()
    }

}