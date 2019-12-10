package com.voipgrid.vialer.call;

import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.sip.SipCall;

import androidx.annotation.Nullable;

public class CallDetail {

    private String phoneNumber, sipCallerId, contactsCallerId;

    private CallDetail(String phoneNumber, String sipCallerId, String contactsCallerId) {
        this.phoneNumber = phoneNumber;
        this.sipCallerId = sipCallerId;
        this.contactsCallerId = contactsCallerId;
    }

    public static @Nullable CallDetail fromSipCall(@Nullable SipCall sipCall) {
        if (sipCall == null) return null;

        return new CallDetail(sipCall.getPhoneNumber(), sipCall.getCallerId(), new Contacts().getContactNameByPhoneNumber(sipCall.getPhoneNumber()));
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Get the number for display, if a caller id exists that will be used, otherwise just the phone number.
     *
     * @return
     */
    public String getDisplayLabel() {
        String contactName = contactsCallerId;

        if (contactName == null) {
            contactName = sipCallerId;
        }

        if (contactName != null && !contactName.isEmpty()) {
            return contactName;
        }

        return phoneNumber;
    }
}
