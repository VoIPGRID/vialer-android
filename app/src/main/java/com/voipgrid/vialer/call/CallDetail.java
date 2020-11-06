package com.voipgrid.vialer.call;

import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.phonelib.SessionExtensionsKt;

import org.openvoipalliance.phonelib.model.Call;

public class CallDetail {

    private String identifier, phoneNumber, sipCallerId, contactsCallerId;

    private CallDetail(String identifier, String phoneNumber, String sipCallerId, String contactsCallerId) {
        this.identifier = identifier;
        this.phoneNumber = phoneNumber;
        this.sipCallerId = sipCallerId;
        this.contactsCallerId = contactsCallerId;
    }

    public static CallDetail fromSipCall(Call sipCall) {
        return new CallDetail(sipCall.getCallId(), sipCall.getPhoneNumber(), sipCall.getDisplayName(), new Contacts().getContactNameByPhoneNumber(sipCall.getPhoneNumber()));
    }

    public String getIdentifier() {
        return identifier;
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
