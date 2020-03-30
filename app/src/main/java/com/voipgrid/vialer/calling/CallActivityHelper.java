package com.voipgrid.vialer.calling;

import android.widget.TextView;

import com.voipgrid.vialer.contacts.Contacts;

public class CallActivityHelper {

    private final Contacts mContacts;

    public CallActivityHelper(Contacts contacts) {
        mContacts = contacts;
    }

    public void updateLabelsBasedOnPhoneNumber(TextView title, TextView subtitle, String number, String callerId) {
        String contactName = mContacts.getContactNameByPhoneNumber(number);

        if (contactName == null) {
            contactName = callerId;
        }

        if (contactName != null && !contactName.isEmpty()) {
            title.setText(contactName);
            subtitle.setText(number);
            return;
        }

        title.setText(number);
        subtitle.setText("");
    }
}
