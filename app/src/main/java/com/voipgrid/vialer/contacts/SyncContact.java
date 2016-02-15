package com.voipgrid.vialer.contacts;

import java.util.List;

/**
 * Class for storing all information needed for the contact to be synced.
 */
public class SyncContact {
    private long contactId;
    private String displayName;
    private List<String> normalizedPhoneNumbers;
    private List<String> phoneNumbers;

    public SyncContact(long contactId, String displayName, List<String> normalizedPhoneNumbers, List<String> phoneNumbers) {
        setContactId(contactId);
        setDisplayName(displayName);
        setNormalizedPhoneNumbers(normalizedPhoneNumbers);
        setPhoneNumbers(phoneNumbers);
    }

    public long getContactId() {
        return contactId;
    }

    private void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public String getDisplayName() {
        return displayName;
    }

    private void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getNormalizedPhoneNumbers() {
        return normalizedPhoneNumbers;
    }

    private void setNormalizedPhoneNumbers(List<String> normalizedPhoneNumbers) {
        this.normalizedPhoneNumbers = normalizedPhoneNumbers;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    private void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }
}
