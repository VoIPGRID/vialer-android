package com.voipgrid.vialer.callrecord;

import android.graphics.Bitmap;

import com.github.tamir7.contacts.Contact;
import com.voipgrid.contacts.Contacts;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.permissions.ContactsPermission;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;

public class CachedContacts {

    private final Map<String, Contact> contactsCache = new HashMap<>();
    private final Map<String, Bitmap> contactImagesCache = new HashMap<>();

    private final Contacts contacts;

    public CachedContacts(Contacts contacts) {
        this.contacts = contacts;
    }

    @Nullable Contact getContact(String number) {
        if (!ContactsPermission.hasPermission(VialerApplication.get())) {
            return null;
        }

        if (!contactsCache.containsKey(number)) {
            contactsCache.put(number, contacts.getContactByPhoneNumber(number));
        }

        return contactsCache.get(number);
    }

    @Nullable Bitmap getContactImage(String number) {
        if (!ContactsPermission.hasPermission(VialerApplication.get())) {
            return null;
        }

        if (!contactImagesCache.containsKey(number)) {
            contactImagesCache.put(number, contacts.getContactImageByPhoneNumber(number));
        }

        return contactImagesCache.get(number);
    }

    public void clear() {
        contactsCache.clear();
        contactImagesCache.clear();
    }
}
