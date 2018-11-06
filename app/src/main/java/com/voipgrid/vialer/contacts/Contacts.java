package com.voipgrid.vialer.contacts;

import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import com.github.tamir7.contacts.Contact;
import com.github.tamir7.contacts.Query;
import com.voipgrid.vialer.VialerApplication;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Class that allows you to get information about contacts..
 */

public class Contacts {

    /**
     * Method used to get the contact by the phone number.
     *
     * @param number
     * @return first contact matching the phone number
     */
    public Contact getContactByPhoneNumber(String number) {
        if (number == null) {
            return null;
        }

        Query q = com.github.tamir7.contacts.Contacts.getQuery();
        q.whereEqualTo(Contact.Field.PhoneNumber, number);
        List<Contact> contacts = q.find();

        if (contacts == null || contacts.isEmpty()) {
            return null;
        }

        return contacts.get(0);
    }

    /**
     * Method used to get the image by the phone number.
     *
     * @param number
     * @return bitmap image
     */
    public Bitmap getContactImageByPhoneNumber(String number) {
        Contact contact = getContactByPhoneNumber(number);

        if (contact == null) {
            return null;
        }

        return openPhoto(contact.getId());
    }

    /**
     * Method used to open the image.
     *
     * @param contactId
     * @return bitmap image
     */
    private Bitmap openPhoto(long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = VialerApplication.get().getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                }
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    /**
     * Method used to get the name of the contact by phone number.
     *
     * @param number
     * @return name of contact
     */
    public String getContactNameByPhoneNumber(String number) {
        Contact contact = getContactByPhoneNumber(number);

        if (contact == null) {
            return null;
        }

        return contact.getDisplayName();
    }

}