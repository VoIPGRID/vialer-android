package com.voipgrid.vialer.contacts
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.github.tamir7.contacts.Contact
import com.github.tamir7.contacts.Contacts
import com.voipgrid.vialer.VialerApplication.Companion.get
import java.io.ByteArrayInputStream

/**
 * Class that allows you to get information about contacts..
 */
class Contacts {

    private val q
        get() = Contacts.getQuery()

    /**
     * Method used to get the contact by the phone number.
     *
     * @param number
     * @return first contact matching the phone number
     */
    fun getContactByPhoneNumber(number: String?): Contact? {
        if (number == null) {
            return null
        }

        return try {

            val formattedNumber = PhoneNumberUtils.formatNumber(number, "NL")

            val contacts = Contacts
                    .getQuery()
                    .or(listOf(
                            q.whereEqualTo(Contact.Field.PhoneNumber, formattedNumber),
                            q.whereEqualTo(Contact.Field.PhoneNumber, formattedNumber.replace("+${ASSUMED_COUNTRY_CODE} ", "")),
                            q.whereEqualTo(Contact.Field.PhoneNumber, number)
                    ))
                    .find()

            if (contacts.isNotEmpty()) contacts[0] else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Method used to get the image by the phone number.
     *
     * @param number
     * @return bitmap image
     */
    fun getContactImageByPhoneNumber(number: String?): Bitmap? {
        val contact = getContactByPhoneNumber(number) ?: return null
        return openPhoto(contact.id)
    }

    /**
     * Method used to open the image.
     *
     * @param contactId
     * @return bitmap image
     */
    private fun openPhoto(contactId: Long): Bitmap? {
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        val photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
        val cursor = get().contentResolver.query(photoUri, arrayOf(ContactsContract.Contacts.Photo.PHOTO), null, null, null)
                ?: return null
        try {
            if (cursor.moveToFirst()) {
                val data = cursor.getBlob(0)
                if (data != null) {
                    return BitmapFactory.decodeStream(ByteArrayInputStream(data))
                }
            }
        } finally {
            cursor.close()
        }
        return null
    }

    /**
     * Method used to get the name of the contact by phone number.
     *
     * @param number
     * @return name of contact
     */
    fun getContactNameByPhoneNumber(number: String?): String? {
        val contact = getContactByPhoneNumber(number) ?: return null
        return contact.displayName
    }

    companion object {
        const val ASSUMED_COUNTRY_CODE = "31"
    }
}